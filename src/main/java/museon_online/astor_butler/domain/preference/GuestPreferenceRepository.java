package museon_online.astor_butler.domain.preference;

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
public class GuestPreferenceRepository {

    private final JdbcTemplate jdbcTemplate;

    public GuestPreference create(GuestPreferenceCommand command) {
        Long id = jdbcTemplate.queryForObject("""
                INSERT INTO guest_preferences (
                    chat_id, telegram_user_id, user_id, venue_code, category, preference_text,
                    source, status, confidence, captured_from_state, correlation_id, metadata_json,
                    created_at, updated_at
                )
                VALUES (?, ?, ?, ?, ?, ?, 'TELEGRAM', 'ACTIVE', 1.000, ?, ?, COALESCE(?::jsonb, '{}'::jsonb),
                        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                RETURNING id
                """,
                Long.class,
                command.chatId(),
                command.telegramUserId(),
                command.userId(),
                normalizeVenue(command.venueCode()),
                command.category().name(),
                command.preferenceText().trim(),
                blankToNull(command.capturedFromState()),
                blankToNull(command.correlationId()),
                blankToNull(command.metadataJson())
        );
        return findById(id).orElseThrow();
    }

    public Optional<GuestPreference> findById(Long id) {
        List<GuestPreference> result = jdbcTemplate.query("""
                SELECT *
                FROM guest_preferences
                WHERE id = ?
                """,
                mapper(),
                id
        );
        return result.stream().findFirst();
    }

    public List<GuestPreference> findActiveByChatId(Long chatId, int limit) {
        return jdbcTemplate.query("""
                SELECT *
                FROM guest_preferences
                WHERE chat_id = ?
                  AND status = 'ACTIVE'
                ORDER BY created_at DESC
                LIMIT ?
                """,
                mapper(),
                chatId,
                limit
        );
    }

    private RowMapper<GuestPreference> mapper() {
        return (rs, rowNum) -> new GuestPreference(
                rs.getLong("id"),
                rs.getLong("chat_id"),
                nullableLong(rs, "telegram_user_id"),
                nullableLong(rs, "user_id"),
                rs.getString("venue_code"),
                GuestPreferenceCategory.valueOf(rs.getString("category")),
                rs.getString("preference_text"),
                rs.getString("source"),
                GuestPreferenceStatus.valueOf(rs.getString("status")),
                rs.getDouble("confidence"),
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
