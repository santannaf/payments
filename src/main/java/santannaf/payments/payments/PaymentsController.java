package santannaf.payments.payments;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;

@RestController
@RequestMapping(path = {"/payments"})
public class PaymentsController implements CommandLineRunner {
    private final RedisTemplate<String, Object> redisTemplate;
    private final RestTemplate restTemplate;

    @Value("${services.payment.processor.default.url}")
    String defaultURL;
    @Value("${services.payment.processor.fallback.url}")
    String fallbackURL;

    private final BlockingQueue<Payment> paymentQueue;

    public PaymentsController(RedisTemplate<String, Object> redisTemplate, RestTemplate restTemplate, BlockingQueue<Payment> paymentQueue) {
        this.redisTemplate = redisTemplate;
        this.restTemplate = restTemplate;
        this.paymentQueue = paymentQueue;
    }

    @PostMapping
    void pay(@RequestBody Payment payment) {
        Thread.ofVirtual().start(() -> sendProcessor(payment, selectorProcessor()));
    }

    private void sendProcessor(Payment payment, String processor) {
        String url = getUrl(processor);
        HttpEntity<Payment> entity = new HttpEntity<>(payment);
        ResponseEntity<Void> response;
        boolean success = false;
        try {
            response = restTemplate.postForEntity(url, entity, Void.class);
            success = response.getStatusCode().is2xxSuccessful();
            if (success) updateSummary(processor, payment);
        } catch (Exception ignored) {
        }

        if (!success) {
            try {
                Thread.sleep(10);
                paymentQueue.add(payment);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private String getUrl(String processor) {
        if (processor.equals("default")) return defaultURL + "/payments";
        else return fallbackURL + "/payments";
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

    @Override
    public void run(String... args) {
        while (true) {
            try {
                Payment payment = paymentQueue.take();
                sendProcessor(payment, selectorProcessor());
            } catch (Exception ignored) {
            }
        }
    }
}
