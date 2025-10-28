package museon_online.astor_butler.fsm.storage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import museon_online.astor_butler.fsm.core.BotState;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisFSMStorage implements FSMStorage {

    private final RedisTemplate<String, String> redisTemplate;
    private static final String PREFIX = "fsm_stage:";

    @Override
    public void setState(Long chatId, BotState state) {
        log.debug("🔸 Redis setState: {} -> {}", chatId, state);
        log.info("🧭 setState: chatId={} → {}", chatId, state);
        redisTemplate.opsForValue().set(PREFIX + chatId, state.name());
    }

    @Override
    public BotState getState(Long chatId) {
        String value = redisTemplate.opsForValue().get(PREFIX + chatId);
        log.debug("🔹 Redis getState for chatId={} → {}", chatId, value);
        return value != null ? BotState.valueOf(value) : null;
    }

    @Override
    public void clear(Long chatId) {
        log.debug("❌ Redis clear state for chatId={}", chatId);
        redisTemplate.delete(PREFIX + chatId);
    }
}