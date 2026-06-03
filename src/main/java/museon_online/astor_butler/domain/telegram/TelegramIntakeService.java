package museon_online.astor_butler.domain.telegram;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import museon_online.astor_butler.service.message.IncomingMessage;
import museon_online.astor_butler.service.message.MessageChannel;
import org.postgresql.util.PGobject;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TelegramIntakeService {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public void capture(IncomingMessage incoming) {
        if (incoming == null || incoming.chatId() == null) {
            return;
        }

        upsertUser(incoming);
        upsertTelegramProfile(incoming);
        saveMessage(incoming);
    }

    private void upsertUser(IncomingMessage incoming) {
        if (incoming.telegramUserId() == null) {
            return;
        }

        jdbcTemplate.update("""
                INSERT INTO users (telegram_id, first_name, last_name, username, phone, role, updated_at)
                VALUES (?, ?, ?, ?, ?, 'GUEST', CURRENT_TIMESTAMP)
                ON CONFLICT (telegram_id)
                DO UPDATE SET
                    first_name = COALESCE(EXCLUDED.first_name, users.first_name),
                    last_name = COALESCE(EXCLUDED.last_name, users.last_name),
                    username = COALESCE(EXCLUDED.username, users.username),
                    phone = COALESCE(EXCLUDED.phone, users.phone),
                    updated_at = CURRENT_TIMESTAMP
                """,
                incoming.telegramUserId(),
                incoming.firstName(),
                incoming.lastName(),
                incoming.username(),
                incoming.contactPhone()
        );
    }

    private void upsertTelegramProfile(IncomingMessage incoming) {
        if (incoming.channel() != MessageChannel.TELEGRAM || incoming.telegramUserId() == null) {
            return;
        }

        jdbcTemplate.update("""
                INSERT INTO telegram_profiles (
                    telegram_user_id, chat_id, username, first_name, last_name, language_code,
                    is_bot, phone_number, source_channel, updated_at, last_seen_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                ON CONFLICT (telegram_user_id)
                DO UPDATE SET
                    chat_id = EXCLUDED.chat_id,
                    username = COALESCE(EXCLUDED.username, telegram_profiles.username),
                    first_name = COALESCE(EXCLUDED.first_name, telegram_profiles.first_name),
                    last_name = COALESCE(EXCLUDED.last_name, telegram_profiles.last_name),
                    language_code = COALESCE(EXCLUDED.language_code, telegram_profiles.language_code),
                    is_bot = COALESCE(EXCLUDED.is_bot, telegram_profiles.is_bot),
                    phone_number = COALESCE(EXCLUDED.phone_number, telegram_profiles.phone_number),
                    source_channel = EXCLUDED.source_channel,
                    updated_at = CURRENT_TIMESTAMP,
                    last_seen_at = CURRENT_TIMESTAMP
                """,
                incoming.telegramUserId(),
                incoming.chatId(),
                incoming.username(),
                incoming.firstName(),
                incoming.lastName(),
                incoming.languageCode(),
                incoming.bot() == null ? false : incoming.bot(),
                incoming.contactPhone(),
                incoming.channel().name()
        );
    }

    private void saveMessage(IncomingMessage incoming) {
        String eventId = incoming.correlationId() == null || incoming.correlationId().isBlank()
                ? UUID.randomUUID().toString()
                : incoming.correlationId();

        int inserted = jdbcTemplate.update("""
                INSERT INTO telegram_messages (
                    id, event_id, telegram_user_id, chat_id, message_id, update_id, message_kind,
                    text, contact_phone, raw_payload, received_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT ON CONSTRAINT uq_telegram_messages_event_id DO NOTHING
                """,
                UUID.randomUUID(),
                eventId,
                incoming.telegramUserId(),
                incoming.chatId(),
                incoming.telegramMessageId(),
                incoming.telegramUpdateId(),
                messageKind(incoming),
                incoming.text(),
                incoming.contactPhone(),
                jsonb(rawPayload(incoming)),
                OffsetDateTime.ofInstant(incoming.receivedAt(), ZoneOffset.UTC)
        );

        if (inserted > 0) {
            log.info("Telegram message stored: eventId={}, chatId={}", eventId, incoming.chatId());
        }
    }

    private String messageKind(IncomingMessage incoming) {
        if (incoming.contactPhone() != null && !incoming.contactPhone().isBlank()) {
            return "CONTACT";
        }
        String text = incoming.text() == null ? "" : incoming.text().trim();
        if (text.startsWith("/")) {
            return "COMMAND";
        }
        if (!text.isBlank()) {
            return "TEXT";
        }
        return "UNKNOWN";
    }

    private Map<String, Object> rawPayload(IncomingMessage incoming) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("channel", incoming.channel().name());
        payload.put("externalUserId", incoming.externalUserId());
        payload.put("chatId", incoming.chatId());
        payload.put("telegramUserId", incoming.telegramUserId());
        payload.put("messageId", incoming.telegramMessageId());
        payload.put("updateId", incoming.telegramUpdateId());
        payload.put("firstName", incoming.firstName());
        payload.put("lastName", incoming.lastName());
        payload.put("username", incoming.username());
        payload.put("languageCode", incoming.languageCode());
        payload.put("bot", incoming.bot());
        payload.put("payload", incoming.payload() == null ? Map.of() : incoming.payload());
        return payload;
    }

    private PGobject jsonb(Map<String, Object> value) {
        PGobject object = new PGobject();
        object.setType("jsonb");
        try {
            object.setValue(objectMapper.writeValueAsString(value == null ? Map.of() : value));
            return object;
        } catch (SQLException | JsonProcessingException e) {
            throw new IllegalStateException("Cannot serialize Telegram message payload", e);
        }
    }
}
