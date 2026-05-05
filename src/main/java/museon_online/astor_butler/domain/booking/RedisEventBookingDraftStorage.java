package museon_online.astor_butler.domain.booking;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisEventBookingDraftStorage implements EventBookingDraftStorage {

    private static final String PREFIX = "event_booking_draft:";

    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public EventBookingDraft getOrCreate(Long chatId) {
        Object value = redisTemplate.opsForValue().get(key(chatId));
        if (value instanceof EventBookingDraft draft) {
            return draft;
        }

        if (value != null) {
            log.warn("Unexpected draft payload for chatId={}: {}", chatId, value.getClass().getName());
        }

        return new EventBookingDraft();
    }

    @Override
    public void save(Long chatId, EventBookingDraft draft) {
        redisTemplate.opsForValue().set(key(chatId), draft);
    }

    @Override
    public void clear(Long chatId) {
        redisTemplate.delete(key(chatId));
    }

    private String key(Long chatId) {
        return PREFIX + chatId;
    }
}
