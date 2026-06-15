package museon_online.astor_butler.fsm.storage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import museon_online.astor_butler.fsm.core.BotState;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

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
        return BotState.fromStorageValue(value);
    }

    @Override
    public void clear(Long chatId) {
        log.debug("❌ Redis clear state for chatId={}", chatId);
        redisTemplate.delete(stateKey(chatId));
        clearPendingIntents(chatId);
    }

    @Override
    public void setPendingIntents(Long chatId, List<String> intents) {
        if (intents == null || intents.isEmpty()) {
            clearPendingIntents(chatId);
            return;
        }
        redisTemplate.opsForValue().set(
                pendingIntentsKey(chatId),
                String.join(",", intents),
                Duration.ofSeconds(fsmTtlSeconds)
        );
    }

    @Override
    public List<String> getPendingIntents(Long chatId) {
        String value = redisTemplate.opsForValue().get(pendingIntentsKey(chatId));
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(intent -> !intent.isBlank())
                .toList();
    }

    @Override
    public void clearPendingIntents(Long chatId) {
        redisTemplate.delete(pendingIntentsKey(chatId));
    }

    private String stateKey(Long chatId) {
        return keyPrefix + ":fsm:telegram:" + chatId + ":state";
    }

    private String pendingIntentsKey(Long chatId) {
        return keyPrefix + ":fsm:telegram:" + chatId + ":pending-intents";
    }
}
