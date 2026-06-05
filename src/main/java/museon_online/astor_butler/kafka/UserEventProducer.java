package museon_online.astor_butler.kafka;

import io.confluent.kafka.serializers.KafkaAvroSerializer;
import io.confluent.kafka.serializers.KafkaAvroSerializerConfig;
import io.confluent.kafka.serializers.subject.RecordNameStrategy;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import museon_online.astor_butler.fsm.core.BotState;
import museon_online.astor_butler.service.message.IncomingMessage;
import museon_online.astor_butler.service.message.OutgoingMessage;
import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Properties;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserEventProducer {

    private final AvroUserEventFactory avroUserEventFactory;
    private final OutboxEventRepository outboxEventRepository;

    @Value("${astor.kafka.enabled:true}")
    private boolean enabled;

    @Value("${astor.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    @Value("${astor.kafka.schema-registry-url:http://localhost:8082}")
    private String schemaRegistryUrl;

    @Value("${astor.kafka.user-events-topic:astor.user.events}")
    private String topic;

    @Value("${astor.kafka.outbox-user-events-topic:astor.outbox.user.events}")
    private String outboxTopic;

    @Value("${astor.kafka.log-published-events:true}")
    private boolean logPublishedEvents;

    @Value("${astor.kafka.outbox-enabled:true}")
    private boolean outboxEnabled;

    @Value("${astor.kafka.direct-publish-enabled:false}")
    private boolean directPublishEnabled;

    private KafkaProducer<String, GenericRecord> producer;

    @PostConstruct
    public void init() {
        if (!enabled) {
            log.info("Kafka producer disabled");
            return;
        }

        if (!directPublishEnabled) {
            log.info("Kafka direct producer disabled; events will use outbox only: outboxTopic={}", outboxTopic);
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
        if (!enabled || incoming == null) {
            return;
        }

        String eventId = avroUserEventFactory.eventId(incoming);
        String partitionKey = avroUserEventFactory.partitionKey(incoming);
        GenericRecord event = avroUserEventFactory.userMessageReceived(incoming, previousState, outgoing);
        appendOutbox(partitionKey, "USER_MESSAGE_RECEIVED", event, eventId);
        sendDirectly(partitionKey, event, eventId, "user event");
    }

    public void publishLlmResponse(
            IncomingMessage incoming,
            BotState state,
            String stage,
            String prompt,
            String response,
            boolean fallbackUsed
    ) {
        if (!enabled || incoming == null) {
            return;
        }

        String eventId = avroUserEventFactory.llmEventId(incoming, stage);
        String partitionKey = avroUserEventFactory.partitionKey(incoming);
        GenericRecord event = avroUserEventFactory.llmResponseGenerated(
                incoming,
                state,
                stage,
                prompt,
                response,
                fallbackUsed
        );
        appendOutbox(partitionKey, "LLM_RESPONSE_GENERATED", event, eventId);
        sendDirectly(partitionKey, event, eventId, "llm response");
    }

    private void appendOutbox(String partitionKey, String eventType, GenericRecord event, String eventId) {
        if (!outboxEnabled) {
            return;
        }
        try {
            outboxEventRepository.append(outboxTopic, partitionKey, eventType, event);
        } catch (Exception e) {
            log.warn(
                    "Kafka outbox append failed: topic={}, partitionKey={}, eventId={}, reason={}",
                    outboxTopic,
                    partitionKey,
                    eventId,
                    e.getMessage()
            );
        }
    }

    private Properties producerProperties() {
        Properties properties = new Properties();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class.getName());
        properties.put(KafkaAvroSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG, schemaRegistryUrl);
        properties.put(KafkaAvroSerializerConfig.AUTO_REGISTER_SCHEMAS, "true");
        properties.put(KafkaAvroSerializerConfig.VALUE_SUBJECT_NAME_STRATEGY, RecordNameStrategy.class.getName());
        properties.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, "true");
        properties.put(ProducerConfig.ACKS_CONFIG, "all");
        properties.put(ProducerConfig.RETRIES_CONFIG, String.valueOf(Integer.MAX_VALUE));
        properties.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, "5");
        properties.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, "120000");
        properties.put(ProducerConfig.CLIENT_ID_CONFIG, "astor-butler-user-event-producer");
        return properties;
    }

    private void sendDirectly(String partitionKey, GenericRecord event, String eventId, String label) {
        if (!directPublishEnabled || producer == null) {
            return;
        }
        send(partitionKey, event, eventId, label);
    }

    private void send(String partitionKey, GenericRecord event, String eventId, String label) {
        producer.send(new ProducerRecord<>(topic, partitionKey, event), (metadata, exception) -> {
            if (exception != null) {
                log.warn(
                        "Kafka {} publish failed: partitionKey={}, eventId={}, reason={}",
                        label,
                        partitionKey,
                        eventId,
                        exception.getMessage()
                );
                return;
            }

            if (logPublishedEvents) {
                log.info(
                        "Kafka {} published: partitionKey={}, eventId={}, topic={}, partition={}, offset={}",
                        label,
                        partitionKey,
                        eventId,
                        metadata.topic(),
                        metadata.partition(),
                        metadata.offset()
                );
            } else {
                log.debug(
                        "Kafka {} published: partitionKey={}, eventId={}, topic={}, partition={}, offset={}",
                        label,
                        partitionKey,
                        eventId,
                        metadata.topic(),
                        metadata.partition(),
                        metadata.offset()
                );
            }
        });
    }
}
