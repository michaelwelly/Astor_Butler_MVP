package museon_online.astor_butler.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import museon_online.astor_butler.fsm.core.BotState;
import museon_online.astor_butler.service.message.IncomingMessage;
import museon_online.astor_butler.service.message.OutgoingMessage;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserEventProducer {

    private final ObjectMapper objectMapper;

    @Value("${astor.kafka.enabled:true}")
    private boolean enabled;

    @Value("${astor.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    @Value("${astor.kafka.user-events-topic:astor.user.events}")
    private String topic;

    @Value("${astor.kafka.log-published-events:true}")
    private boolean logPublishedEvents;

    private KafkaProducer<String, String> producer;

    @PostConstruct
    public void init() {
        if (!enabled) {
            log.info("Kafka producer disabled");
            return;
        }

        producer = new KafkaProducer<>(producerProperties());
        log.info("Kafka user event producer initialized: topic={}, bootstrapServers={}", topic, bootstrapServers);
    }

    @PreDestroy
    public void close() {
        if (producer != null) {
            producer.close();
        }
    }

    public void publishIncomingMessage(IncomingMessage incoming, BotState previousState, OutgoingMessage outgoing) {
        if (!enabled || producer == null || incoming == null) {
            return;
        }

        String idempotencyKey = idempotencyKey(incoming);
        try {
            String payload = objectMapper.writeValueAsString(payload(incoming, previousState, outgoing, idempotencyKey));
            send(idempotencyKey, payload, "user event");
        } catch (JsonProcessingException e) {
            log.warn("Cannot serialize Kafka user event: key={}", idempotencyKey, e);
        }
    }

    public void publishLlmResponse(
            IncomingMessage incoming,
            BotState state,
            String stage,
            String prompt,
            String response,
            boolean fallbackUsed
    ) {
        if (!enabled || producer == null || incoming == null) {
            return;
        }

        String idempotencyKey = idempotencyKey(incoming) + ":llm:" + normalizeStage(stage);
        try {
            String payload = objectMapper.writeValueAsString(llmPayload(
                    incoming,
                    state,
                    stage,
                    prompt,
                    response,
                    fallbackUsed,
                    idempotencyKey
            ));
            send(idempotencyKey, payload, "llm response");
        } catch (JsonProcessingException e) {
            log.warn("Cannot serialize Kafka LLM response event: key={}", idempotencyKey, e);
        }
    }

    private Properties producerProperties() {
        Properties properties = new Properties();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, "true");
        properties.put(ProducerConfig.ACKS_CONFIG, "all");
        properties.put(ProducerConfig.RETRIES_CONFIG, String.valueOf(Integer.MAX_VALUE));
        properties.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, "5");
        properties.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, "120000");
        properties.put(ProducerConfig.CLIENT_ID_CONFIG, "astor-butler-user-event-producer");
        return properties;
    }

    private String idempotencyKey(IncomingMessage incoming) {
        if (incoming.channel() != null && incoming.correlationId() != null && !incoming.correlationId().isBlank()) {
            return incoming.channel().name().toLowerCase() + ":" + incoming.correlationId();
        }
        if (incoming.telegramUpdateId() != null) {
            return "telegram:" + incoming.telegramUpdateId();
        }
        return "chat:" + incoming.chatId() + ":" + incoming.receivedAt().toEpochMilli();
    }

    private void send(String key, String payload, String label) {
        producer.send(new ProducerRecord<>(topic, key, payload), (metadata, exception) -> {
            if (exception != null) {
                log.warn("Kafka {} publish failed: key={}, reason={}", label, key, exception.getMessage());
                return;
            }

            if (logPublishedEvents) {
                log.info(
                        "Kafka {} published: key={}, topic={}, partition={}, offset={}",
                        label,
                        key,
                        metadata.topic(),
                        metadata.partition(),
                        metadata.offset()
                );
            } else {
                log.debug(
                        "Kafka {} published: key={}, topic={}, partition={}, offset={}",
                        label,
                        key,
                        metadata.topic(),
                        metadata.partition(),
                        metadata.offset()
                );
            }
        });
    }

    private Map<String, Object> payload(
            IncomingMessage incoming,
            BotState previousState,
            OutgoingMessage outgoing,
            String idempotencyKey
    ) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("eventId", idempotencyKey);
        value.put("eventType", "USER_MESSAGE_RECEIVED");
        value.put("occurredAt", Instant.now());
        value.put("channel", incoming.channel().name());
        value.put("chatId", incoming.chatId());
        value.put("telegramUserId", incoming.telegramUserId());
        value.put("telegramMessageId", incoming.telegramMessageId());
        value.put("telegramUpdateId", incoming.telegramUpdateId());
        value.put("username", incoming.username());
        value.put("firstName", incoming.firstName());
        value.put("lastName", incoming.lastName());
        value.put("text", incoming.text());
        value.put("contactPhonePresent", incoming.contactPhone() != null && !incoming.contactPhone().isBlank());
        value.put("previousState", previousState == null ? null : previousState.name());
        value.put("nextState", outgoing == null ? null : outgoing.nextState());
        value.put("actions", outgoing == null ? null : outgoing.actions());
        value.put("fallback", outgoing != null && outgoing.fallback());
        return value;
    }

    private Map<String, Object> llmPayload(
            IncomingMessage incoming,
            BotState state,
            String stage,
            String prompt,
            String response,
            boolean fallbackUsed,
            String idempotencyKey
    ) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("eventId", idempotencyKey);
        value.put("eventType", "LLM_RESPONSE_GENERATED");
        value.put("occurredAt", Instant.now());
        value.put("channel", incoming.channel().name());
        value.put("chatId", incoming.chatId());
        value.put("telegramUserId", incoming.telegramUserId());
        value.put("telegramMessageId", incoming.telegramMessageId());
        value.put("telegramUpdateId", incoming.telegramUpdateId());
        value.put("username", incoming.username());
        value.put("firstName", incoming.firstName());
        value.put("lastName", incoming.lastName());
        value.put("state", state == null ? null : state.name());
        value.put("stage", stage);
        value.put("inputText", incoming.text());
        value.put("responseText", response);
        value.put("fallbackUsed", fallbackUsed);
        value.put("prompt", prompt);
        value.put("sourceMessageEventId", idempotencyKey(incoming));
        return value;
    }

    private String normalizeStage(String stage) {
        if (stage == null || stage.isBlank()) {
            return "unknown";
        }
        return stage.trim().toLowerCase().replaceAll("[^a-z0-9_-]+", "_");
    }
}
