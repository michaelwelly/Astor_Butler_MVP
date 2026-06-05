package museon_online.astor_butler.kafka;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import museon_online.astor_butler.fsm.core.BotState;
import museon_online.astor_butler.service.message.IncomingMessage;
import museon_online.astor_butler.service.message.OutgoingMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserEventProducer {

    private final UserEventFactory userEventFactory;
    private final OutboxEventRepository outboxEventRepository;

    @Value("${astor.kafka.enabled:true}")
    private boolean enabled;

    @Value("${astor.kafka.outbox-user-events-topic:astor.user.events}")
    private String outboxTopic;

    @Value("${astor.kafka.outbox-enabled:true}")
    private boolean outboxEnabled;

    @PostConstruct
    public void init() {
        if (!enabled) {
            log.info("Kafka event outbox disabled because astor.kafka.enabled=false");
            return;
        }

        log.info("Kafka events use PostgreSQL outbox only: topic={}", outboxTopic);
    }

    public void publishIncomingMessage(IncomingMessage incoming, BotState previousState, OutgoingMessage outgoing) {
        if (!enabled || incoming == null) {
            return;
        }

        String eventId = userEventFactory.eventId(incoming);
        String partitionKey = userEventFactory.partitionKey(incoming);
        Map<String, Object> event = userEventFactory.userMessageReceived(incoming, previousState, outgoing);
        appendOutbox(partitionKey, "USER_MESSAGE_RECEIVED", event, eventId);
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

        String eventId = userEventFactory.llmEventId(incoming, stage);
        String partitionKey = userEventFactory.partitionKey(incoming);
        Map<String, Object> event = userEventFactory.llmResponseGenerated(
                incoming,
                state,
                stage,
                prompt,
                response,
                fallbackUsed
        );
        appendOutbox(partitionKey, "LLM_RESPONSE_GENERATED", event, eventId);
    }

    private void appendOutbox(String partitionKey, String eventType, Map<String, Object> event, String eventId) {
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
}
