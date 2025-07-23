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

import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.locks.LockSupport;

@RestController
@RequestMapping(path = {"/payments"})
public class PaymentsController implements CommandLineRunner {
    private final RedisTemplate<String, Object> redisTemplate;
    private final RestTemplate restTemplate;
    private final String defaultURL;

    private final Queue<Payment> paymentQueue;

    public PaymentsController(RedisTemplate<String, Object> redisTemplate,
                              RestTemplate restTemplate,
                              Queue<Payment> paymentQueue,
                              @Value("${services.payment.processor.default.url}") String defaultURL) {
        this.redisTemplate = redisTemplate;
        this.restTemplate = restTemplate;
        this.paymentQueue = paymentQueue;
        this.defaultURL = defaultURL;
    }

    @PostMapping
    ResponseEntity<Void> pay(@RequestBody Payment payment) {
        paymentQueue.offer(payment);
        return ResponseEntity.accepted().build();
    }

    private void sendProcessor(final Payment payment) {
        final String url = getUrl();
        final HttpEntity<Payment> entity = new HttpEntity<>(payment);
        ResponseEntity<Void> response;
        boolean success = false;
        try {
            response = restTemplate.postForEntity(url, entity, Void.class);
            success = response.getStatusCode().is2xxSuccessful();
            if (success) updateSummary(payment);
        } catch (Exception _) {
        }

        if (!success) paymentQueue.add(payment);
    }

    private String getUrl() {
        return defaultURL + "/payments";
    }

    private void updateSummary(final Payment payment) {
        final String key = "summary:default";
        final double score = payment.getRequestedAt().toEpochMilli();
        String uniqueId = Long.toHexString(ThreadLocalRandom.current().nextLong());
        String entryJson = payment.getAmount() + "|1|" + uniqueId;
        redisTemplate.opsForZSet().add(key, entryJson, score);
    }

    @Override
    public void run(String... args) {
        processDefault();
    }

    private final ExecutorService workers = Executors.newVirtualThreadPerTaskExecutor();

    void processDefault() {
        startWorkers(paymentQueue);
    }

    private void startWorkers(Queue<Payment> paymentQueueFallback) {
        for (int i = 1; i <= 5; i++) {
            workers.submit(() -> {
                while (true) {
                    final Payment payment = paymentQueueFallback.poll();
                    if (payment == null) LockSupport.parkNanos(10_000_000L);
                    else sendProcessor(payment);
                }
            });
        }
    }
}
