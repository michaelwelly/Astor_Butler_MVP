package museon_online.astor_butler.domain.concierge;

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
public class ConciergeRequestRepository {

    private final JdbcTemplate jdbcTemplate;

    public ConciergeRequest create(ConciergeRequestCommand command) {
        Long id = jdbcTemplate.queryForObject("""
                INSERT INTO concierge_requests (
                    chat_id, telegram_user_id, user_id, venue_code, request_type, status, source,
                    guest_name, request_text, manager_chat_id, captured_from_state, correlation_id,
                    metadata_json, created_at, updated_at
                )
                VALUES (?, ?, ?, ?, ?, 'PENDING_TEAM', 'TELEGRAM', ?, ?, ?, ?, ?, COALESCE(?::jsonb, '{}'::jsonb),
                        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                RETURNING id
                """,
                Long.class,
                command.chatId(),
                command.telegramUserId(),
                command.userId(),
                normalizeVenue(command.venueCode()),
                command.requestType().name(),
                blankToNull(command.guestName()),
                command.requestText().trim(),
                blankToNull(command.managerChatId()),
                blankToNull(command.capturedFromState()),
                blankToNull(command.correlationId()),
                blankToNull(command.metadataJson())
        );
        return findById(id).orElseThrow();
    }

    public Optional<ConciergeRequest> findById(Long id) {
        List<ConciergeRequest> result = jdbcTemplate.query("""
                SELECT *
                FROM concierge_requests
                WHERE id = ?
                """,
                mapper(),
                id
        );
        return result.stream().findFirst();
    }

    public List<ConciergeRequest> findByChatId(Long chatId, int limit) {
        return jdbcTemplate.query("""
                SELECT *
                FROM concierge_requests
                WHERE chat_id = ?
                ORDER BY created_at DESC
                LIMIT ?
                """,
                mapper(),
                chatId,
                limit
        );
    }

    public ConciergeRequest updateStatus(Long id, ConciergeRequestStatus status) {
        jdbcTemplate.update("""
                UPDATE concierge_requests
                SET status = ?,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """,
                status.name(),
                id
        );
        return findById(id).orElseThrow();
    }

    private RowMapper<ConciergeRequest> mapper() {
        return (rs, rowNum) -> new ConciergeRequest(
                rs.getLong("id"),
                rs.getLong("chat_id"),
                nullableLong(rs, "telegram_user_id"),
                nullableLong(rs, "user_id"),
                rs.getString("venue_code"),
                ConciergeRequestType.valueOf(rs.getString("request_type")),
                ConciergeRequestStatus.valueOf(rs.getString("status")),
                rs.getString("source"),
                rs.getString("guest_name"),
                rs.getString("request_text"),
                rs.getString("manager_chat_id"),
                rs.getString("captured_from_state"),
                rs.getString("correlation_id"),
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
