package museon_online.astor_butler.domain.auth;

import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class TelegramLoginVerifierTest {

    private static final String BOT_TOKEN = "123456:ABC-DEF";
    private static final Instant NOW = Instant.ofEpochSecond(1_700_000_000);

    private final TelegramLoginVerifier verifier = new TelegramLoginVerifier(Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void acceptsValidTelegramLoginPayload() {
        Map<String, String> payload = validPayload();

        assertThat(verifier.isValid(payload, BOT_TOKEN)).isTrue();
    }

    @Test
    void rejectsTamperedPayload() {
        Map<String, String> payload = validPayload();
        payload.put("username", "evil");

        assertThat(verifier.isValid(payload, BOT_TOKEN)).isFalse();
    }

    @Test
    void rejectsExpiredPayload() {
        Map<String, String> payload = validPayload();
        payload.put("auth_date", String.valueOf(NOW.minus(Duration.ofDays(3)).getEpochSecond()));

        assertThat(verifier.isValid(payload, BOT_TOKEN)).isFalse();
    }

    private Map<String, String> validPayload() {
        Map<String, String> payload = new HashMap<>();
        payload.put("id", "42");
        payload.put("first_name", "Astor");
        payload.put("username", "astor_guest");
        payload.put("auth_date", String.valueOf(NOW.getEpochSecond()));
        payload.put("hash", calculateHash(payload));
        return payload;
    }

    private String calculateHash(Map<String, String> payload) {
        try {
            byte[] secret = MessageDigest.getInstance("SHA-256").digest(BOT_TOKEN.getBytes(StandardCharsets.UTF_8));
            Mac hmac = Mac.getInstance("HmacSHA256");
            hmac.init(new SecretKeySpec(secret, "HmacSHA256"));
            String dataCheckString = payload.entrySet().stream()
                    .filter(entry -> !"hash".equals(entry.getKey()))
                    .sorted(Map.Entry.comparingByKey())
                    .map(entry -> entry.getKey() + "=" + entry.getValue())
                    .collect(Collectors.joining("\n"));
            return HexFormat.of().formatHex(hmac.doFinal(dataCheckString.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }
}
