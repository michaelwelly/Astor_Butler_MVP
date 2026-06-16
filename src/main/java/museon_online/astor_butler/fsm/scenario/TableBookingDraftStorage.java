package museon_online.astor_butler.fsm.scenario;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class TableBookingDraftStorage {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${astor.redis.key-prefix:astor}")
    private String keyPrefix;

    @Value("${astor.booking.draft-ttl-seconds:86400}")
    private long draftTtlSeconds;

    public void save(Long chatId, Draft draft) {
        if (chatId == null || draft == null) {
            return;
        }
        try {
            redisTemplate.opsForValue().set(key(chatId), objectMapper.writeValueAsString(draft), Duration.ofSeconds(draftTtlSeconds));
        } catch (JsonProcessingException e) {
            log.warn("Table booking draft serialization failed: chatId={}, reason={}", chatId, e.getMessage());
        }
    }

    public Optional<Draft> find(Long chatId) {
        if (chatId == null) {
            return Optional.empty();
        }
        String value = redisTemplate.opsForValue().get(key(chatId));
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(value, Draft.class));
        } catch (JsonProcessingException e) {
            log.warn("Table booking draft deserialization failed: chatId={}, reason={}", chatId, e.getMessage());
            return Optional.empty();
        }
    }

    public void clear(Long chatId) {
        if (chatId != null) {
            redisTemplate.delete(key(chatId));
        }
    }

    private String key(Long chatId) {
        return keyPrefix + ":booking:table:draft:telegram:" + chatId;
    }

    public record Draft(
            String venueCode,
            Instant requestedStartAt,
            Instant requestedEndAt,
            LocalDate requestedDate,
            LocalTime requestedTime,
            Integer partySize,
            String preferredZone,
            String seatingPreference,
            String originalText
    ) {
        public Draft(
                String venueCode,
                Instant requestedStartAt,
                Instant requestedEndAt,
                Integer partySize,
                String originalText
        ) {
            this(venueCode, requestedStartAt, requestedEndAt, null, null, partySize, null, null, originalText);
        }
    }
}
