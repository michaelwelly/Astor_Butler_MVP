package museon_online.astor_butler.domain.auth;

import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
public class TelegramLoginVerifier {

    private static final String HASH_FIELD = "hash";
    private static final String AUTH_DATE_FIELD = "auth_date";
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final Duration DEFAULT_MAX_AGE = Duration.ofHours(24);

    private final Clock clock;

    public TelegramLoginVerifier() {
        this(Clock.systemUTC());
    }

    TelegramLoginVerifier(Clock clock) {
        this.clock = clock;
    }

    public boolean isValid(Map<String, String> payload, String botToken) {
        return isValid(payload, botToken, DEFAULT_MAX_AGE);
    }

    public boolean isValid(Map<String, String> payload, String botToken, Duration maxAge) {
        if (payload == null || payload.isEmpty() || isBlank(botToken) || maxAge == null) {
            return false;
        }
        String providedHash = payload.get(HASH_FIELD);
        String authDate = payload.get(AUTH_DATE_FIELD);
        if (isBlank(providedHash) || isBlank(authDate) || !isFresh(authDate, maxAge)) {
            return false;
        }

        String expectedHash = calculateHash(payload, botToken);
        return MessageDigest.isEqual(
                expectedHash.getBytes(StandardCharsets.UTF_8),
                providedHash.getBytes(StandardCharsets.UTF_8)
        );
    }

    private boolean isFresh(String authDate, Duration maxAge) {
        try {
            long epochSeconds = Long.parseLong(authDate);
            Instant authenticatedAt = Instant.ofEpochSecond(epochSeconds);
            Instant now = Instant.now(clock);
            return !authenticatedAt.isAfter(now) && !authenticatedAt.isBefore(now.minus(maxAge));
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    private String calculateHash(Map<String, String> payload, String botToken) {
        try {
            byte[] secret = MessageDigest.getInstance("SHA-256")
                    .digest(botToken.getBytes(StandardCharsets.UTF_8));
            Mac hmac = Mac.getInstance(HMAC_ALGORITHM);
            hmac.init(new SecretKeySpec(secret, HMAC_ALGORITHM));
            return HexFormat.of().formatHex(hmac.doFinal(dataCheckString(payload).getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to verify Telegram login payload", exception);
        }
    }

    private String dataCheckString(Map<String, String> payload) {
        return payload.entrySet().stream()
                .filter(entry -> !HASH_FIELD.equals(entry.getKey()))
                .filter(entry -> entry.getValue() != null)
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining("\n"));
    }

    private boolean isBlank(String value) {
        return Objects.isNull(value) || value.isBlank();
    }
}
