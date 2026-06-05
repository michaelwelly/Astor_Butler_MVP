package museon_online.astor_butler.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericRecord;
import org.postgresql.util.PGobject;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class OutboxEventRepository {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public void append(String topic, String eventKey, String eventType, GenericRecord event) {
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
                jsonb(toMap(event))
        );
    }

    private String version(GenericRecord event) {
        Object value = event == null ? null : event.get("eventVersion");
        return value == null ? "1.0" : value.toString();
    }

    private Map<String, Object> toMap(GenericRecord record) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (record == null) {
            return result;
        }
        for (Schema.Field field : record.getSchema().getFields()) {
            result.put(field.name(), value(record.get(field.name())));
        }
        return result;
    }

    private Object value(Object value) {
        if (value instanceof GenericRecord nested) {
            return toMap(nested);
        }
        if (value instanceof Iterable<?> iterable) {
            return objectMapper.convertValue(iterable, Object.class);
        }
        return value;
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
