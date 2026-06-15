package museon_online.astor_butler.domain.feedback;

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
public class FeedbackRepository {

    private final JdbcTemplate jdbcTemplate;

    public GuestFeedback create(GuestFeedbackCommand command, FeedbackType type, FeedbackSentiment sentiment, FeedbackPriority priority) {
        Long id = jdbcTemplate.queryForObject("""
                INSERT INTO guest_feedback (
                    chat_id, telegram_user_id, user_id, venue_code, status, source, feedback_type,
                    sentiment, priority, guest_name, text, previous_state, correlation_id, admin_chat_id,
                    created_at, updated_at
                )
                VALUES (?, ?, ?, ?, ?, 'TELEGRAM', ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                RETURNING id
                """,
                Long.class,
                command.chatId(),
                command.telegramUserId(),
                command.userId(),
                normalizeVenue(command.venueCode()),
                FeedbackStatus.OPEN.name(),
                type.name(),
                sentiment.name(),
                priority.name(),
                blankToNull(command.guestName()),
                command.text().trim(),
                blankToNull(command.previousState()),
                blankToNull(command.correlationId()),
                blankToNull(command.adminChatId())
        );
        return find(id).orElseThrow();
    }

    public Optional<GuestFeedback> find(Long id) {
        List<GuestFeedback> result = jdbcTemplate.query("""
                SELECT *
                FROM guest_feedback
                WHERE id = ?
                """,
                mapper(),
                id
        );
        return result.stream().findFirst();
    }

    public List<GuestFeedback> findByChatId(Long chatId, int limit) {
        return jdbcTemplate.query("""
                SELECT *
                FROM guest_feedback
                WHERE chat_id = ?
                ORDER BY created_at DESC
                LIMIT ?
                """,
                mapper(),
                chatId,
                limit
        );
    }

    public List<GuestFeedback> findOpen(int limit) {
        return jdbcTemplate.query("""
                SELECT *
                FROM guest_feedback
                WHERE status IN ('OPEN', 'IN_REVIEW')
                ORDER BY CASE priority WHEN 'URGENT' THEN 0 WHEN 'HIGH' THEN 1 WHEN 'NORMAL' THEN 2 ELSE 3 END, created_at DESC
                LIMIT ?
                """,
                mapper(),
                limit
        );
    }

    private RowMapper<GuestFeedback> mapper() {
        return (rs, rowNum) -> new GuestFeedback(
                rs.getLong("id"),
                rs.getLong("chat_id"),
                nullableLong(rs, "telegram_user_id"),
                nullableLong(rs, "user_id"),
                rs.getString("venue_code"),
                FeedbackStatus.valueOf(rs.getString("status")),
                rs.getString("source"),
                FeedbackType.valueOf(rs.getString("feedback_type")),
                FeedbackSentiment.valueOf(rs.getString("sentiment")),
                FeedbackPriority.valueOf(rs.getString("priority")),
                rs.getString("guest_name"),
                rs.getString("text"),
                rs.getString("previous_state"),
                rs.getString("correlation_id"),
                rs.getString("admin_chat_id"),
                nullableLong(rs, "admin_message_id"),
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
