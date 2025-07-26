package santannaf.payments.payments;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Service
public class PaymentServiceImpl implements PaymentService {
    private final RedisTemplate<String, String> redisTemplate;
    private final URI defaultURL;
    private final URI fallbackURL;
    private final HttpClient httpClient;
    private final Duration timeout;
    private final int tentatives;

    private final BlockingQueue<Payment> queue = new LinkedBlockingQueue<>();

    public PaymentServiceImpl(RedisTemplate<String, String> redisTemplate,
                              @Value("${services.payment.processor.default.url}") String defaultURL,
                              @Value("${services.payment.processor.fallback.url}") String fallbackURL,
                              @Value("${services.payment.processor.default.timeout.ms}") int TIMEOUT,
                              @Value("${services.payment.processor.default.tentatives}") int TENTATIVES,
                              @Value("${fixed.pool.workers.thread}") int workers,
                              HttpClient httpClient
    ) {
        this.redisTemplate = redisTemplate;
        this.defaultURL = URI.create(defaultURL + "/payments");
        this.fallbackURL = URI.create(fallbackURL + "/payments");
        this.httpClient = httpClient;
        this.timeout = Duration.ofMillis(TIMEOUT);
        this.tentatives = TENTATIVES;

        for (int i = 0; i < workers; i++) {
            Thread.startVirtualThread(this::consumer);
        }
    }

    @Override
    public void producer(Payment payment) {
        this.queue.offer(payment);
    }

    void consumer() {
        while (true) {
            sendPayment(getPayment());
        }
    }

    Payment getPayment() {
        try {
            return queue.take();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    void sendPayment(final Payment payment) {
        final String json = payment.json();
        for (int i = 0; i < tentatives; i++) {
            try {
                var result = callDefault(payment, defaultURL, payment.getKeyDefault(), json);
                if (result) return;
            } catch (Exception _) {
            }
        }

        try {
            var result = callDefault(payment, fallbackURL, payment.getKeyFallback(), json);
            if (result) return;
        } catch (Exception _) {
        }

        queue.offer(payment);
    }

    final boolean callDefault(final Payment payment,
                              final URI uri,
                              final String key,
                              final String json) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .header("Content-Type", "application/json")
                .timeout(timeout)
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
        final int code = response.statusCode();
        if (code >= 200 && code < 300) {
            updateSummaryDefault(payment, key);
            return true;
        }

        return false;
    }

    final void updateSummaryDefault(final Payment payment, final String key) {
        final double score = payment.getRequestedAt().toEpochMilli();
        final String entryJson = payment.getEntryJson();
        redisTemplate.opsForZSet().add(key, entryJson, score);
    }
}
