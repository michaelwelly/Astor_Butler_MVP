package museon_online.astor_butler.fsm.storage;

import lombok.RequiredArgsConstructor;
import museon_online.astor_butler.fsm.core.BotState;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RedisFSMStorage implements FSMStorage {

    private final RedisTemplate<String, String> redisTemplate;
    private static final String PREFIX = "fsm:";

    @Override
    public void setState(Long chatId, BotState state) {
        redisTemplate.opsForValue().set(PREFIX + chatId, state.name());
    }

    @Override
    public BotState getState(Long chatId) {
        String value = redisTemplate.opsForValue().get(PREFIX + chatId);
        return value != null ? BotState.valueOf(value) : null;
    }

    @Override
    public void clear(Long chatId) {
        redisTemplate.delete(PREFIX + chatId);
    }
}