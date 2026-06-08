package museon_online.astor_butler.domain.consent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import museon_online.astor_butler.service.message.IncomingMessage;
import org.postgresql.util.PGobject;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConsentVaultService {

    public static final String PRIVACY_POLICY = "PRIVACY_POLICY";
    public static final String CURRENT_POLICY_VERSION = "2026-06-02-local";

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public void grantPrivacyPolicyFromTelegramContact(IncomingMessage incoming) {
        if (incoming == null || incoming.telegramUserId() == null) {
            return;
        }

        Map<String, Object> evidence = Map.of(
                "channel", incoming.channel().name(),
                "chatId", incoming.chatId(),
                "telegramUserId", incoming.telegramUserId(),
                "messageId", incoming.telegramMessageId() == null ? "" : incoming.telegramMessageId(),
                "updateId", incoming.telegramUpdateId() == null ? "" : incoming.telegramUpdateId(),
                "contactPhonePresent", incoming.contactPhone() != null && !incoming.contactPhone().isBlank(),
                "correlationId", incoming.correlationId() == null ? "" : incoming.correlationId()
        );

        Long userId = findUserIdByTelegramUserId(incoming.telegramUserId());
        jdbcTemplate.update("""
                INSERT INTO user_consents (
                    id, user_id, telegram_user_id, chat_id, consent_type, policy_version, status,
                    source, evidence, granted_at, updated_at
                )
                VALUES (?, ?, ?, ?, ?, ?, 'GRANTED', ?, ?, ?, CURRENT_TIMESTAMP)
                ON CONFLICT ON CONSTRAINT uq_user_consents_telegram_policy
                DO UPDATE SET
                    user_id = COALESCE(user_consents.user_id, EXCLUDED.user_id),
                    chat_id = EXCLUDED.chat_id,
                    status = 'GRANTED',
                    source = EXCLUDED.source,
                    evidence = EXCLUDED.evidence,
                    granted_at = EXCLUDED.granted_at,
                    revoked_at = NULL,
                    updated_at = CURRENT_TIMESTAMP
                """,
                UUID.randomUUID(),
                userId,
                incoming.telegramUserId(),
                incoming.chatId(),
                PRIVACY_POLICY,
                CURRENT_POLICY_VERSION,
                "TELEGRAM_CONTACT_FLOW",
                jsonb(evidence),
                OffsetDateTime.ofInstant(Instant.now(), ZoneOffset.UTC)
        );

        log.info(
                "Consent granted: userId={}, telegramUserId={}, type={}, version={}",
                userId,
                incoming.telegramUserId(),
                PRIVACY_POLICY,
                CURRENT_POLICY_VERSION
        );
    }

    public boolean hasGrantedPrivacyPolicy(Long telegramUserId) {
        if (telegramUserId == null) {
            return false;
        }
        Boolean granted = jdbcTemplate.query("""
                SELECT EXISTS (
                    SELECT 1
                    FROM user_consents
                    WHERE telegram_user_id = ?
                      AND consent_type = ?
                      AND policy_version = ?
                      AND status = 'GRANTED'
                      AND revoked_at IS NULL
                )
                """,
                resultSet -> resultSet.next() && resultSet.getBoolean(1),
                telegramUserId,
                PRIVACY_POLICY,
                CURRENT_POLICY_VERSION
        );
        return Boolean.TRUE.equals(granted);
    }

    private Long findUserIdByTelegramUserId(Long telegramUserId) {
        return jdbcTemplate.query("""
                SELECT user_id
                FROM telegram_profiles
                WHERE telegram_user_id = ?
                """,
                resultSet -> resultSet.next() ? resultSet.getObject("user_id", Long.class) : null,
                telegramUserId
        );
    }

    private PGobject jsonb(Map<String, Object> value) {
        PGobject object = new PGobject();
        object.setType("jsonb");
        try {
            object.setValue(objectMapper.writeValueAsString(value == null ? Map.of() : value));
            return object;
        } catch (SQLException | JsonProcessingException e) {
            throw new IllegalStateException("Cannot serialize consent evidence", e);
        }
    }
}
