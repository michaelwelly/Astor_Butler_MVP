package museon_online.astor_butler.domain.booking;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class EventBookingRepository {

    private static final Long DEFAULT_MANAGER_TELEGRAM_ID = 876857557L;

    private final JdbcTemplate jdbcTemplate;

    public EventBookingOrder createAwaitingManagerReview(EventBookingCommand command) {
        Long id = jdbcTemplate.queryForObject("""
                INSERT INTO event_booking_orders (
                    chat_id, telegram_user_id, user_id, venue_code, status, source,
                    event_type, requested_date, requested_time_text, guest_count,
                    budget_text, menu_preferences, technical_requirements, contact,
                    guest_name, guest_phone, guest_comment, manager_telegram_id,
                    manager_chat_id, created_at, updated_at
                )
                VALUES (?, ?, ?, ?, ?, 'TELEGRAM', ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                RETURNING id
                """,
                Long.class,
                command.chatId(),
                command.telegramUserId(),
                command.userId(),
                normalizeVenue(command.venueCode()),
                EventBookingStatus.AWAITING_MANAGER_REVIEW.name(),
                blankToNull(command.eventType()),
                date(command.requestedDate()),
                blankToNull(command.requestedTimeText()),
                command.guestCount(),
                blankToNull(command.budgetText()),
                blankToNull(command.menuPreferences()),
                blankToNull(command.technicalRequirements()),
                blankToNull(command.contact()),
                blankToNull(command.guestName()),
                blankToNull(command.guestPhone()),
                blankToNull(command.guestComment()),
                command.managerTelegramId() == null ? DEFAULT_MANAGER_TELEGRAM_ID : command.managerTelegramId(),
                blankToNull(command.managerChatId())
        );
        return findOrder(id).orElseThrow();
    }

    public Optional<EventBookingOrder> findOrder(Long id) {
        List<EventBookingOrder> result = jdbcTemplate.query("""
                SELECT *
                FROM event_booking_orders
                WHERE id = ?
                """,
                orderMapper(),
                id
        );
        return result.stream().findFirst();
    }

    public List<EventBookingOrder> findOrdersByChatId(Long chatId, int limit) {
        return jdbcTemplate.query("""
                SELECT *
                FROM event_booking_orders
                WHERE chat_id = ?
                ORDER BY created_at DESC
                LIMIT ?
                """,
                orderMapper(),
                chatId,
                limit
        );
    }

    public List<EventBookingOrder> findActiveOrdersByChatId(Long chatId) {
        return jdbcTemplate.query("""
                SELECT *
                FROM event_booking_orders
                WHERE chat_id = ?
                  AND status IN ('AWAITING_MANAGER_REVIEW', 'MANAGER_CLARIFICATION_REQUESTED', 'CONFIRMED')
                ORDER BY created_at DESC
                """,
                orderMapper(),
                chatId
        );
    }

    private RowMapper<EventBookingOrder> orderMapper() {
        return (rs, rowNum) -> new EventBookingOrder(
                rs.getLong("id"),
                rs.getLong("chat_id"),
                nullableLong(rs, "telegram_user_id"),
                nullableLong(rs, "user_id"),
                rs.getString("venue_code"),
                EventBookingStatus.valueOf(rs.getString("status")),
                rs.getString("source"),
                rs.getString("event_type"),
                localDate(rs, "requested_date"),
                rs.getString("requested_time_text"),
                nullableInt(rs, "guest_count"),
                rs.getString("budget_text"),
                rs.getString("menu_preferences"),
                rs.getString("technical_requirements"),
                rs.getString("contact"),
                rs.getString("guest_name"),
                rs.getString("guest_phone"),
                rs.getString("guest_comment"),
                nullableLong(rs, "manager_telegram_id"),
                nullableLong(rs, "manager_user_id"),
                rs.getString("manager_chat_id"),
                rs.getString("external_id"),
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

    private Date date(LocalDate date) {
        return date == null ? null : Date.valueOf(date);
    }

    private LocalDate localDate(ResultSet rs, String column) throws java.sql.SQLException {
        Date date = rs.getDate(column);
        return date == null ? null : date.toLocalDate();
    }

    private Instant instant(ResultSet rs, String column) throws java.sql.SQLException {
        Timestamp timestamp = rs.getTimestamp(column);
        return timestamp == null ? null : timestamp.toInstant();
    }

    private Long nullableLong(ResultSet rs, String column) throws java.sql.SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    private Integer nullableInt(ResultSet rs, String column) throws java.sql.SQLException {
        int value = rs.getInt(column);
        return rs.wasNull() ? null : value;
    }
}
