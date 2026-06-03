package museon_online.astor_butler.fsm.core.idempotency;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private final StringRedisTemplate redisTemplate;

    @Value("${astor.redis.idempotency-ttl-seconds:86400}")
    private long idempotencyTtlSeconds;

    @Value("${astor.redis.key-prefix:astor}")
    private String keyPrefix;

    /**
     * @return true — если событие новое и принято
     *         false — если событие уже обрабатывалось
     */
    public boolean accept(String eventId) {
        if (eventId == null || eventId.isBlank()) {
            eventId = UUID.randomUUID().toString();
        }

        Boolean accepted = redisTemplate.opsForValue().setIfAbsent(
                idempotencyKey(eventId),
                "1",
                Duration.ofSeconds(idempotencyTtlSeconds)
        );
        return Boolean.TRUE.equals(accepted);
    }

    private String idempotencyKey(String eventId) {
        return keyPrefix + ":idem:telegram:" + eventId;
    }
}
