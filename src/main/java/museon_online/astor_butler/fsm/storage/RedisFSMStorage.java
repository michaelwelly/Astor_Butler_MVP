package museon_online.astor_butler.fsm.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class RedisFSMStorage implements FSMStorage {

    private final RedisTemplate<String, String> redisTemplate;
    private final Duration ttl;

    public RedisFSMStorage(RedisTemplate<String, String> redisTemplate,
                           @Value("${fsm.redis.ttl-seconds:900}") long ttlSeconds) {
        this.redisTemplate = redisTemplate;
        this.ttl = Duration.ofSeconds(ttlSeconds);
    }

    @Override
    public void setState(String userId, String state) {
        redisTemplate.opsForValue().set(key(userId), state, ttl);
    }

    @Override
    public String getState(String userId) {
        return redisTemplate.opsForValue().get(key(userId));
    }

    @Override
    public void clear(String userId) {
        redisTemplate.delete(key(userId));
    }

    private String key(String userId) {
        return "fsm:state:" + userId;
    }
}