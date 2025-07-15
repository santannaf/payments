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
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RestController
@RequestMapping(path = {"/payments"})
public class PaymentsController implements CommandLineRunner {
    private final RedisTemplate<String, Object> redisTemplate;
    private final RestTemplate restTemplate;

    @Value("${services.payment.processor.default.url}")
    String defaultURL;
    @Value("${services.payment.processor.fallback.url}")
    String fallbackURL;

    private final Queue<Payment> paymentQueue;

    public PaymentsController(RedisTemplate<String, Object> redisTemplate,
                              RestTemplate restTemplate,
                              Queue<Payment> paymentQueue) {
        this.redisTemplate = redisTemplate;
        this.restTemplate = restTemplate;
        this.paymentQueue = paymentQueue;
    }

    @PostMapping
    void pay(@RequestBody Payment payment) {
        paymentQueue.offer(payment);
    }

    private void sendProcessor(Payment payment) {
        String url = getUrl();
        HttpEntity<Payment> entity = new HttpEntity<>(payment);
        ResponseEntity<Void> response;
        boolean success = false;
        try {
            response = restTemplate.postForEntity(url, entity, Void.class);
            success = response.getStatusCode().is2xxSuccessful();
            if (success) updateSummary(payment);
        } catch (Exception ignored) {
        }

        if (!success) paymentQueue.add(payment);
    }

    private String getUrl() {
        return defaultURL + "/payments";
    }

    private void updateSummary(Payment payment) {
        String key = "summary:default";
        double score = payment.getRequestedAt().toEpochMilli();
        String uniqueId = UUID.randomUUID().toString().substring(0, 8);
        String entryJson = String.format(Locale.US, "%.1f|1|%s", payment.getAmount(), uniqueId);
        redisTemplate.opsForZSet().add(key, entryJson, score);
    }

    @Override
    public void run(String... args) {
        processDefault();
    }

    private final ExecutorService workers = Executors.newVirtualThreadPerTaskExecutor();

    void processDefault() {
        workers(paymentQueue);
    }

    private void workers(Queue<Payment> paymentQueueFallback) {
        for (int i = 1; i <= 5; i++) {
            workers.submit(() -> {
                while (true) {
                    try {
                        Payment payment = paymentQueueFallback.poll();
                        if (payment == null) Thread.sleep(25);
                        else sendProcessor(payment);
                    } catch (InterruptedException ignored) {
                    }
                }
            });
        }
    }
}
