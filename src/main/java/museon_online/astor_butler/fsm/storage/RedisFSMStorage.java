package museon_online.astor_butler.fsm.storage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import museon_online.astor_butler.fsm.core.BotState;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisFSMStorage implements FSMStorage {

    private final StringRedisTemplate redisTemplate;

    @Value("${astor.redis.fsm-ttl-seconds:259200}")
    private long fsmTtlSeconds;

    @Value("${astor.redis.key-prefix:astor}")
    private String keyPrefix;

    @Override
    public void setState(Long chatId, BotState state) {
        log.debug("🔸 Redis setState: {} -> {}", chatId, state);
        log.info("🧭 setState: chatId={} → {}", chatId, state);
        redisTemplate.opsForValue().set(stateKey(chatId), state.name(), Duration.ofSeconds(fsmTtlSeconds));
    }

    @Override
    public BotState getState(Long chatId) {
        String value = redisTemplate.opsForValue().get(stateKey(chatId));
        log.debug("🔹 Redis getState for chatId={} → {}", chatId, value);
        return value != null ? BotState.valueOf(value) : null;
    }

    @Override
    public void clear(Long chatId) {
        log.debug("❌ Redis clear state for chatId={}", chatId);
        redisTemplate.delete(stateKey(chatId));
    }

    private String stateKey(Long chatId) {
        return keyPrefix + ":fsm:telegram:" + chatId + ":state";
    }
}
