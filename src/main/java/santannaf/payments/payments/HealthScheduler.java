package santannaf.payments.payments;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class HealthScheduler {
    private final RestTemplate restTemplate;
    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${services.payment.processor.default.url}")
    String ppDefaultUrl;

    public HealthScheduler(RestTemplate restTemplate, RedisTemplate<String, Object> redisTemplate) {
        this.restTemplate = restTemplate;
        this.redisTemplate = redisTemplate;
    }

    record HealthResponse(boolean failing, int minResponseTime) {
    }

    @Scheduled(fixedRate = 5000)
    public void scheduleHealth() {
        try {
            var response = restTemplate.getForEntity(ppDefaultUrl + "/payments/service-health", HealthResponse.class).getBody();
            assert response != null;
            System.out.println("System Payment Processor health: " + !response.failing());
            redisTemplate.opsForValue().set("processor:health", !response.failing());
        } catch (Exception error) {
            redisTemplate.opsForValue().set("processor:health", false);
        }
    }
}
