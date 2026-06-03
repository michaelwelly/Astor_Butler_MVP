package museon_online.astor_butler.analytics;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class AnalyticsProcessedEventRepository {

    private final JdbcTemplate jdbcTemplate;

    public boolean markProcessed(String consumerName, String eventId) {
        int inserted = jdbcTemplate.update("""
                INSERT INTO processed_kafka_events (id, consumer_name, event_id)
                VALUES (?, ?, ?)
                ON CONFLICT ON CONSTRAINT uq_processed_kafka_events_consumer_event DO NOTHING
                """,
                UUID.randomUUID(),
                consumerName,
                eventId
        );
        return inserted > 0;
    }
}
