package museon_online.astor_butler.domain.timeline;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.BoundStatementBuilder;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.driver.api.core.cql.SimpleStatement;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "astor.timeline.scylla.enabled", havingValue = "true")
@Slf4j
public class ScyllaFsmTimelineWriter implements FsmTimelineWriter {

    private final ObjectMapper objectMapper;
    private final CqlSession session;
    private final PreparedStatement insertStatement;

    public ScyllaFsmTimelineWriter(
            ObjectMapper objectMapper,
            @Value("${astor.timeline.scylla.host}") String host,
            @Value("${astor.timeline.scylla.port}") int port,
            @Value("${astor.timeline.scylla.datacenter}") String datacenter,
            @Value("${astor.timeline.scylla.keyspace}") String keyspace,
            @Value("${astor.timeline.scylla.table}") String table,
            @Value("${astor.timeline.scylla.schema-timeout-seconds:30}") long schemaTimeoutSeconds
    ) {
        this.objectMapper = objectMapper;
        this.session = CqlSession.builder()
                .addContactPoint(new InetSocketAddress(host, port))
                .withLocalDatacenter(datacenter)
                .build();
        this.insertStatement = initialize(keyspace, table, Duration.ofSeconds(schemaTimeoutSeconds));
    }

    @Override
    public void append(FsmTimelineEvent event) {
        if (event == null) {
            return;
        }
        if (insertStatement == null) {
            log.warn("FSM timeline append skipped: Scylla writer is not initialized");
            return;
        }
        try {
            BoundStatementBuilder builder = insertStatement.boundStatementBuilder()
                    .setString(0, event.guestId())
                    .setInstant(1, event.occurredAt() == null ? Instant.now() : event.occurredAt())
                    .setString(2, event.eventId())
                    .setString(3, event.channel())
                    .setLong(4, event.chatId())
                    .setString(6, event.previousState())
                    .setString(7, event.nextState())
                    .setString(8, event.intent())
                    .setList(10, event.actions() == null ? List.of() : event.actions(), String.class)
                    .setString(11, event.correlationId())
                    .setString(12, event.rawText())
                    .setString(13, event.normalizedText())
                    .setString(14, metadataJson(event.metadata()));
            if (event.telegramUserId() == null) {
                builder.setToNull(5);
            } else {
                builder.setLong(5, event.telegramUserId());
            }
            if (event.confidence() == null) {
                builder.setToNull(9);
            } else {
                builder.setDouble(9, event.confidence());
            }
            session.execute(builder.build());
        } catch (Exception e) {
            log.warn("FSM timeline append skipped: {}", e.getMessage());
        }
    }

    @PreDestroy
    public void close() {
        session.close();
    }

    private PreparedStatement initialize(String keyspace, String table, Duration schemaTimeout) {
        try {
            ensureSchema(keyspace, table, schemaTimeout);
            PreparedStatement statement = session.prepare(SimpleStatement.builder("""
                    INSERT INTO %s.%s (
                        guest_id,
                        occurred_at,
                        event_id,
                        channel,
                        chat_id,
                        telegram_user_id,
                        previous_state,
                        next_state,
                        intent,
                        confidence,
                        actions,
                        correlation_id,
                        raw_text,
                        normalized_text,
                        metadata_json
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """.formatted(keyspace, table))
                    .setTimeout(schemaTimeout)
                    .build());
            log.info("FSM timeline writer initialized: {}.{}", keyspace, table);
            return statement;
        } catch (Exception e) {
            log.warn("FSM timeline writer disabled: Scylla schema initialization failed: {}", e.getMessage());
            return null;
        }
    }

    private void ensureSchema(String keyspace, String table, Duration schemaTimeout) {
        session.execute(SimpleStatement.builder("""
                        CREATE KEYSPACE IF NOT EXISTS %s
                        WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1}
                        """.formatted(keyspace))
                .setTimeout(schemaTimeout)
                .build());
        session.execute(SimpleStatement.builder("""
                        CREATE TABLE IF NOT EXISTS %s.%s (
                            guest_id text,
                            occurred_at timestamp,
                            event_id text,
                            channel text,
                            chat_id bigint,
                            telegram_user_id bigint,
                            previous_state text,
                            next_state text,
                            intent text,
                            confidence double,
                            actions list<text>,
                            correlation_id text,
                            raw_text text,
                            normalized_text text,
                            metadata_json text,
                            PRIMARY KEY ((guest_id), occurred_at, event_id)
                        ) WITH CLUSTERING ORDER BY (occurred_at DESC, event_id ASC)
                        """.formatted(keyspace, table))
                .setTimeout(schemaTimeout)
                .build());
    }

    private String metadataJson(Map<String, Object> metadata) throws JsonProcessingException {
        return objectMapper.writeValueAsString(metadata == null ? Map.of() : metadata);
    }
}
