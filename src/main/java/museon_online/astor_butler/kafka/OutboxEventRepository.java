package museon_online.astor_butler.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.postgresql.util.PGobject;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class OutboxEventRepository {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public void append(String topic, String eventKey, String eventType, Map<String, Object> event) {
        jdbcTemplate.update("""
                INSERT INTO outbox_events (
                    id, aggregatetype, aggregateid, type, event_version, payload, timestamp
                )
                VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
                """,
                UUID.randomUUID(),
                topic,
                eventKey,
                eventType,
                version(event),
                jsonb(event)
        );
    }

    private String version(Map<String, Object> event) {
        Object value = event == null ? null : event.get("eventVersion");
        return value == null ? "1.0" : value.toString();
    }

    private PGobject jsonb(Map<String, Object> value) {
        PGobject object = new PGobject();
        object.setType("jsonb");
        try {
            object.setValue(objectMapper.writeValueAsString(value == null ? Map.of() : value));
            return object;
        } catch (SQLException | JsonProcessingException e) {
            throw new IllegalStateException("Cannot serialize outbox event payload", e);
        }
    }
}
