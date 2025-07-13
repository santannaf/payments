package santannaf.payments.payments;

import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;

public class RedisHashHelper {
    @SuppressWarnings("unchecked")
    public static HashOperations<String, String, Object> getTypedHashOps(RedisTemplate<String, Object> redisTemplate) {
        return (HashOperations<String, String, Object>) (HashOperations<?, ?, ?>) redisTemplate.opsForHash();
    }
}
