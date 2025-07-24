package santannaf.payments.payments;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Queue;

@RestController
@RequestMapping(path = {"/payments"})
public class PaymentsController implements PaymentProcess {
    private static final Duration TIMEOUT = Duration.ofSeconds(1);

    private final RedisTemplate<String, String> redisTemplate;
    private final String defaultURL;
    private final Queue<Payment> queue;
    private final HttpClient httpClient;

    public PaymentsController(RedisTemplate<String, String> redisTemplate,
                              Queue<Payment> queue,
                              @Value("${services.payment.processor.default.url}") String defaultURL,
                              HttpClient httpClient) {
        this.redisTemplate = redisTemplate;
        this.queue = queue;
        this.defaultURL = defaultURL + "/payments";
        this.httpClient = httpClient;
    }

    @PostMapping
    void pay(@RequestBody Payment payment) {
        queue.offer(payment);
    }

    private void updateSummary(final Payment payment) {
        final double score = payment.getEpochMilli();
        String entryJson = payment.getEntryJson();
        redisTemplate.opsForZSet().add(payment.getKey(), entryJson, score);
    }

    @Override
    public void sendPayment(final Payment payment) {
        try {
            if (callDefault(payment)) {
                updateSummary(payment);
                return;
            }
        } catch (Exception _) {
        }

        queue.offer(payment);
    }

    protected final URI url() {
        return URI.create(defaultURL);
    }

    private boolean callDefault(final Payment payment) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(url())
                .header("Content-Type", "application/json")
                .timeout(TIMEOUT)
                .POST(HttpRequest.BodyPublishers.ofString(payment.json()))
                .build();

        HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
        var code = response.statusCode();
        return code >= 200 && code < 300;
    }
}
