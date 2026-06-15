package museon_online.astor_butler.domain.payment;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class TelegramStarPaymentRepository {

    private final JdbcTemplate jdbcTemplate;

    public TelegramStarPayment createDraft(TelegramStarPaymentCommand command) {
        String payload = "stars:%s:%s".formatted(
                command.purpose().name().toLowerCase(),
                UUID.randomUUID()
        );
        Long id = jdbcTemplate.queryForObject("""
                INSERT INTO telegram_star_payments (
                    chat_id, telegram_user_id, user_id, venue_code, status, source, purpose,
                    related_entity_type, related_entity_id, title, description, payload, currency,
                    star_amount, provider_token, created_at, updated_at
                )
                VALUES (?, ?, ?, ?, ?, 'TELEGRAM', ?, ?, ?, ?, ?, ?, 'XTR', ?, '', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                RETURNING id
                """,
                Long.class,
                command.chatId(),
                command.telegramUserId(),
                command.userId(),
                normalizeVenue(command.venueCode()),
                TelegramStarPaymentStatus.DRAFT.name(),
                command.purpose().name(),
                blankToNull(command.relatedEntityType()),
                command.relatedEntityId(),
                command.title().trim(),
                blankToNull(command.description()),
                payload,
                command.starAmount()
        );
        return find(id).orElseThrow();
    }

    public Optional<TelegramStarPayment> find(Long id) {
        List<TelegramStarPayment> result = jdbcTemplate.query("""
                SELECT *
                FROM telegram_star_payments
                WHERE id = ?
                """,
                mapper(),
                id
        );
        return result.stream().findFirst();
    }

    public Optional<TelegramStarPayment> findByPayload(String payload) {
        List<TelegramStarPayment> result = jdbcTemplate.query("""
                SELECT *
                FROM telegram_star_payments
                WHERE payload = ?
                """,
                mapper(),
                payload
        );
        return result.stream().findFirst();
    }

    public List<TelegramStarPayment> findByChatId(Long chatId, int limit) {
        return jdbcTemplate.query("""
                SELECT *
                FROM telegram_star_payments
                WHERE chat_id = ?
                ORDER BY created_at DESC
                LIMIT ?
                """,
                mapper(),
                chatId,
                limit
        );
    }

    private RowMapper<TelegramStarPayment> mapper() {
        return (rs, rowNum) -> new TelegramStarPayment(
                rs.getLong("id"),
                rs.getLong("chat_id"),
                nullableLong(rs, "telegram_user_id"),
                nullableLong(rs, "user_id"),
                rs.getString("venue_code"),
                TelegramStarPaymentStatus.valueOf(rs.getString("status")),
                rs.getString("source"),
                TelegramStarPaymentPurpose.valueOf(rs.getString("purpose")),
                rs.getString("related_entity_type"),
                nullableLong(rs, "related_entity_id"),
                rs.getString("title"),
                rs.getString("description"),
                rs.getString("payload"),
                rs.getString("currency"),
                rs.getLong("star_amount"),
                rs.getString("provider_token"),
                nullableLong(rs, "invoice_message_id"),
                rs.getString("pre_checkout_query_id"),
                rs.getString("telegram_payment_charge_id"),
                rs.getString("provider_payment_charge_id"),
                rs.getString("failure_reason"),
                rs.getString("metadata_json"),
                instant(rs, "created_at"),
                instant(rs, "updated_at")
        );
    }

    private String normalizeVenue(String venueCode) {
        return venueCode == null || venueCode.isBlank() ? "AERIS" : venueCode.trim().toUpperCase();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private Instant instant(ResultSet rs, String column) throws java.sql.SQLException {
        Timestamp timestamp = rs.getTimestamp(column);
        return timestamp == null ? null : timestamp.toInstant();
    }

    private Long nullableLong(ResultSet rs, String column) throws java.sql.SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }
}
