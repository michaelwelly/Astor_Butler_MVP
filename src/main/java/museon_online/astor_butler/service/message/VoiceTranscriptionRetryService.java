package museon_online.astor_butler.service.message;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class VoiceTranscriptionRetryService {

    private final StringRedisTemplate redisTemplate;

    @Value("${astor.redis.key-prefix:astor}")
    private String keyPrefix;

    @Value("${astor.speech-to-text.retry-ttl-seconds:1800}")
    private long retryTtlSeconds;

    public long recordFailure(Long chatId) {
        if (chatId == null) {
            return 1;
        }
        Long attempts = redisTemplate.opsForValue().increment(key(chatId));
        redisTemplate.expire(key(chatId), Duration.ofSeconds(retryTtlSeconds));
        return attempts == null ? 1 : attempts;
    }

    public void reset(Long chatId) {
        if (chatId != null) {
            redisTemplate.delete(key(chatId));
        }
    }

    private String key(Long chatId) {
        return keyPrefix + ":voice:stt:retry:telegram:" + chatId;
    }
}
