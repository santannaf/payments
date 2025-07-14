package santannaf.payments.payments;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.Locale;
import java.util.UUID;

@RestController
@RequestMapping(path = {"/payments"})
public class PaymentsController {
    private final RedisTemplate<String, Object> redisTemplate;
    private final RestTemplate restTemplate;

    @Value("${services.payment.processor.default.url}")
    String defaultURL;
    @Value("${services.payment.processor.fallback.url}")
    String fallbackURL;

    public PaymentsController(RedisTemplate<String, Object> redisTemplate, RestTemplate restTemplate) {
        this.redisTemplate = redisTemplate;
        this.restTemplate = restTemplate;
    }

    @PostMapping
    void pay(@RequestBody Payment payment) {
        Thread.ofVirtual().start(() -> sendProcessor(payment, selectorProcessor()));
    }

    private final long[] backoffMillis = {800, 1500};
    private static final int MAX_RETRIES = 3;

    private void sendProcessor(Payment payment, String processor) {
        String url;
        if (processor.equals("default")) url = defaultURL + "/payments";
        else url = fallbackURL + "/payments";

        HttpEntity<Payment> entity = new HttpEntity<>(payment);
        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            try {
                var response = restTemplate.postForEntity(url, entity, Void.class);
                if (response.getStatusCode().is2xxSuccessful()) updateSummary(processor, payment);
            } catch (Exception ignored) {
                try {
                    var response = restTemplate.postForEntity(fallbackURL + "/payments", entity, Void.class);
                    if (response.getStatusCode().is2xxSuccessful()) updateSummary("fallback", payment);
                } catch (Exception _) {
                }
            }

            if (attempt < MAX_RETRIES - 1) {
                try {
                    Thread.sleep(backoffMillis[attempt]);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            } else sendProcessor(payment, selectorProcessor());
        }
    }

    private String selectorProcessor() {
        var isHealth = redisTemplate.opsForValue().get("processor:health");
        if (isHealth != null && (boolean) isHealth) return "default";
        return "fallback";
    }

    private void updateSummary(String processor, Payment payment) {
        String key = "summary:" + processor;
        double score = payment.getRequestedAt().toEpochMilli();
        String uniqueId = UUID.randomUUID().toString().substring(0, 8);
        String entryJson = String.format(Locale.US, "%.1f|1|%s", payment.getAmount(), uniqueId);
        redisTemplate.opsForZSet().add(key, entryJson, score);
    }
}
