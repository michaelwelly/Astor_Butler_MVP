package museon_online.astor_butler.domain.donation;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class DonationRepository {

    private final JdbcTemplate jdbcTemplate;

    public List<DonationInitiative> findActiveInitiatives(String venueCode) {
        return jdbcTemplate.query("""
                SELECT *
                FROM donation_initiatives
                WHERE venue_code = ?
                  AND active = TRUE
                ORDER BY title
                """,
                initiativeMapper(),
                normalizeVenue(venueCode)
        );
    }

    public Optional<DonationInitiative> findInitiative(Long id) {
        List<DonationInitiative> result = jdbcTemplate.query("""
                SELECT *
                FROM donation_initiatives
                WHERE id = ?
                """,
                initiativeMapper(),
                id
        );
        return result.stream().findFirst();
    }

    public Optional<DonationInitiative> findDefaultInitiative(String venueCode) {
        List<DonationInitiative> result = jdbcTemplate.query("""
                SELECT *
                FROM donation_initiatives
                WHERE venue_code = ?
                  AND active = TRUE
                ORDER BY CASE WHEN metadata_json ->> 'default' = 'true' THEN 0 ELSE 1 END, title
                LIMIT 1
                """,
                initiativeMapper(),
                normalizeVenue(venueCode)
        );
        return result.stream().findFirst();
    }

    public DonationOrder createDraft(DonationOrderCommand command, DonationInitiative initiative) {
        Long id = jdbcTemplate.queryForObject("""
                INSERT INTO donation_orders (
                    chat_id, telegram_user_id, user_id, venue_code, initiative_id, initiative_title,
                    status, source, amount_minor, currency, anonymous, guest_name, guest_comment,
                    created_at, updated_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, 'TELEGRAM', ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                RETURNING id
                """,
                Long.class,
                command.chatId(),
                command.telegramUserId(),
                command.userId(),
                normalizeVenue(command.venueCode()),
                initiative == null ? command.initiativeId() : initiative.id(),
                initiative == null ? blankToNull(command.initiativeTitle()) : initiative.title(),
                DonationOrderStatus.AWAITING_GUEST_CONFIRMATION.name(),
                command.amountMinor(),
                currency(command.currency()),
                command.anonymous() == null || command.anonymous(),
                blankToNull(command.guestName()),
                blankToNull(command.guestComment())
        );
        return findOrder(id).orElseThrow();
    }

    public Optional<DonationOrder> findOrder(Long id) {
        List<DonationOrder> result = jdbcTemplate.query("""
                SELECT *
                FROM donation_orders
                WHERE id = ?
                """,
                orderMapper(),
                id
        );
        return result.stream().findFirst();
    }

    public Optional<DonationOrder> findLatestAwaitingGuestConfirmation(Long chatId) {
        List<DonationOrder> result = jdbcTemplate.query("""
                SELECT *
                FROM donation_orders
                WHERE chat_id = ?
                  AND status = ?
                ORDER BY created_at DESC
                LIMIT 1
                """,
                orderMapper(),
                chatId,
                DonationOrderStatus.AWAITING_GUEST_CONFIRMATION.name()
        );
        return result.stream().findFirst();
    }

    public List<DonationOrder> findOrdersByChatId(Long chatId, int limit) {
        return jdbcTemplate.query("""
                SELECT *
                FROM donation_orders
                WHERE chat_id = ?
                ORDER BY created_at DESC
                LIMIT ?
                """,
                orderMapper(),
                chatId,
                limit
        );
    }

    public DonationOrder updateStatus(Long id, DonationOrderStatus status) {
        jdbcTemplate.update("""
                UPDATE donation_orders
                SET status = ?,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """,
                status.name(),
                id
        );
        return findOrder(id).orElseThrow();
    }

    private RowMapper<DonationInitiative> initiativeMapper() {
        return (rs, rowNum) -> new DonationInitiative(
                rs.getLong("id"),
                rs.getString("venue_code"),
                rs.getString("initiative_code"),
                rs.getString("title"),
                rs.getString("description"),
                rs.getString("sbp_link"),
                rs.getBoolean("active"),
                rs.getString("metadata_json"),
                instant(rs, "created_at"),
                instant(rs, "updated_at")
        );
    }

    private RowMapper<DonationOrder> orderMapper() {
        return (rs, rowNum) -> new DonationOrder(
                rs.getLong("id"),
                rs.getLong("chat_id"),
                nullableLong(rs, "telegram_user_id"),
                nullableLong(rs, "user_id"),
                rs.getString("venue_code"),
                nullableLong(rs, "initiative_id"),
                rs.getString("initiative_title"),
                DonationOrderStatus.valueOf(rs.getString("status")),
                rs.getString("source"),
                nullableLong(rs, "amount_minor"),
                rs.getString("currency"),
                rs.getBoolean("anonymous"),
                rs.getString("guest_name"),
                rs.getString("guest_comment"),
                rs.getString("sbp_url"),
                rs.getString("payment_external_id"),
                instant(rs, "created_at"),
                instant(rs, "updated_at")
        );
    }

    private String normalizeVenue(String venueCode) {
        return venueCode == null || venueCode.isBlank() ? "AERIS" : venueCode.trim().toUpperCase();
    }

    private String currency(String currency) {
        return currency == null || currency.isBlank() ? "RUB" : currency.trim().toUpperCase();
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
