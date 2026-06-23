package museon_online.astor_butler.domain.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.postgresql.util.PGobject;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class WebSessionRepository {

    private static final long WEB_CHAT_ID_BASE = 9_000_000_000_000L;
    private static final long WEB_CHAT_ID_RANGE = 900_000_000_000L;

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public WebSessionResolution upsert(
            String siteCode,
            String sessionId,
            String externalUserId,
            Long requestedChatId,
            String referrer,
            String landingPage,
            String userAgentHash,
            Map<String, Object> metadata
    ) {
        String safeSessionId = requireSessionId(sessionId);
        String safeSiteCode = siteCode == null || siteCode.isBlank() ? "c3flex" : siteCode.trim().toLowerCase();
        String safeExternalUserId = externalUserId == null || externalUserId.isBlank()
                ? "web:anon:" + safeSessionId
                : externalUserId.trim();
        Long safeChatId = requestedChatId == null ? stableChatId(safeSessionId) : requestedChatId;

        return jdbcTemplate.queryForObject("""
                INSERT INTO web_sessions (
                    id, session_id, site_code, external_user_id, chat_id,
                    referrer, landing_page, user_agent_hash, metadata_json,
                    first_seen_at, last_seen_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                ON CONFLICT (session_id)
                DO UPDATE SET
                    site_code = EXCLUDED.site_code,
                    external_user_id = EXCLUDED.external_user_id,
                    last_seen_at = CURRENT_TIMESTAMP,
                    referrer = COALESCE(EXCLUDED.referrer, web_sessions.referrer),
                    landing_page = COALESCE(EXCLUDED.landing_page, web_sessions.landing_page),
                    user_agent_hash = COALESCE(EXCLUDED.user_agent_hash, web_sessions.user_agent_hash),
                    metadata_json = web_sessions.metadata_json || EXCLUDED.metadata_json
                RETURNING id, session_id, external_user_id, chat_id
                """,
                (rs, rowNum) -> new WebSessionResolution(
                        rs.getObject("id", UUID.class),
                        rs.getString("session_id"),
                        rs.getString("external_user_id"),
                        rs.getLong("chat_id")
                ),
                UUID.randomUUID(),
                safeSessionId,
                safeSiteCode,
                safeExternalUserId,
                safeChatId,
                blankToNull(referrer),
                blankToNull(landingPage),
                blankToNull(userAgentHash),
                jsonb(metadata)
        );
    }

    public void appendMessage(WebSessionResolution session, String correlationId, String direction,
                              String text, Map<String, Object> payload) {
        if (session == null || session.id() == null) {
            return;
        }
        jdbcTemplate.update("""
                INSERT INTO web_messages (
                    id, web_session_id, correlation_id, direction, text, payload_json, created_at
                )
                VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
                """,
                UUID.randomUUID(),
                session.id(),
                blankToNull(correlationId),
                direction,
                text,
                jsonb(payload)
        );
    }

    private Long stableChatId(String sessionId) {
        UUID uuid = UUID.nameUUIDFromBytes(("web-session:" + sessionId).getBytes(StandardCharsets.UTF_8));
        long mixed = uuid.getMostSignificantBits() ^ uuid.getLeastSignificantBits();
        return WEB_CHAT_ID_BASE + Math.floorMod(mixed, WEB_CHAT_ID_RANGE);
    }

    private String requireSessionId(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new IllegalArgumentException("WEB sessionId is required");
        }
        return sessionId.trim();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private PGobject jsonb(Map<String, Object> value) {
        PGobject object = new PGobject();
        object.setType("jsonb");
        try {
            object.setValue(objectMapper.writeValueAsString(value == null ? Map.of() : value));
            return object;
        } catch (SQLException | JsonProcessingException e) {
            throw new IllegalStateException("Cannot serialize web session payload", e);
        }
    }
}
