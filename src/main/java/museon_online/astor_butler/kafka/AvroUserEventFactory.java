package museon_online.astor_butler.kafka;

import museon_online.astor_butler.fsm.core.BotState;
import museon_online.astor_butler.service.message.IncomingMessage;
import museon_online.astor_butler.service.message.OutgoingMessage;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
public class AvroUserEventFactory {

    private static final String SOURCE = "astor-butler-backend";
    private static final String EVENT_VERSION = "1.0";

    public String partitionKey(IncomingMessage incoming) {
        if (incoming.telegramUserId() != null) {
            return "telegram:user:" + incoming.telegramUserId();
        }
        if (incoming.chatId() != null) {
            return "chat:" + incoming.chatId();
        }
        return "event:" + eventId(incoming);
    }

    public String eventId(IncomingMessage incoming) {
        if (incoming.channel() != null && incoming.correlationId() != null && !incoming.correlationId().isBlank()) {
            return incoming.channel().name().toLowerCase() + ":update:" + incoming.correlationId();
        }
        if (incoming.telegramUpdateId() != null) {
            return "telegram:update:" + incoming.telegramUpdateId();
        }
        return "chat:" + incoming.chatId() + ":" + incoming.receivedAt().toEpochMilli();
    }

    public String llmEventId(IncomingMessage incoming, String stage) {
        return eventId(incoming) + ":llm:" + normalizeStage(stage);
    }

    public GenericRecord userMessageReceived(
            IncomingMessage incoming,
            BotState previousState,
            OutgoingMessage outgoing
    ) {
        String eventId = eventId(incoming);
        GenericRecord event = new GenericData.Record(KafkaEventSchemas.USER_MESSAGE_RECEIVED);
        event.put("eventId", eventId);
        event.put("eventType", "USER_MESSAGE_RECEIVED");
        event.put("eventVersion", EVENT_VERSION);
        event.put("occurredAt", Instant.now().toString());
        event.put("source", SOURCE);
        event.put("channel", incoming.channel() == null ? null : incoming.channel().name());
        event.put("idempotencyKey", eventId);
        event.put("actor", actor(KafkaEventSchemas.USER_MESSAGE_RECEIVED, incoming));
        event.put("payload", userMessagePayload(incoming, previousState, outgoing));
        return event;
    }

    public GenericRecord llmResponseGenerated(
            IncomingMessage incoming,
            BotState state,
            String stage,
            String prompt,
            String response,
            boolean fallbackUsed
    ) {
        String eventId = llmEventId(incoming, stage);
        GenericRecord event = new GenericData.Record(KafkaEventSchemas.LLM_RESPONSE_GENERATED);
        event.put("eventId", eventId);
        event.put("eventType", "LLM_RESPONSE_GENERATED");
        event.put("eventVersion", EVENT_VERSION);
        event.put("occurredAt", Instant.now().toString());
        event.put("source", SOURCE);
        event.put("channel", incoming.channel() == null ? null : incoming.channel().name());
        event.put("idempotencyKey", eventId);
        event.put("sourceMessageEventId", eventId(incoming));
        event.put("actor", actor(KafkaEventSchemas.LLM_RESPONSE_GENERATED, incoming));
        event.put("payload", llmPayload(incoming, state, stage, prompt, response, fallbackUsed));
        return event;
    }

    private GenericRecord actor(Schema eventSchema, IncomingMessage incoming) {
        GenericRecord actor = new GenericData.Record(eventSchema.getField("actor").schema());
        actor.put("telegramUserId", incoming.telegramUserId());
        actor.put("chatId", incoming.chatId());
        actor.put("username", incoming.username());
        actor.put("firstName", incoming.firstName());
        actor.put("lastName", incoming.lastName());
        return actor;
    }

    private GenericRecord userMessagePayload(
            IncomingMessage incoming,
            BotState previousState,
            OutgoingMessage outgoing
    ) {
        GenericRecord payload = new GenericData.Record(
                KafkaEventSchemas.USER_MESSAGE_RECEIVED.getField("payload").schema()
        );
        payload.put("chatId", incoming.chatId());
        payload.put("telegramUserId", incoming.telegramUserId());
        payload.put("telegramMessageId", incoming.telegramMessageId());
        payload.put("telegramUpdateId", incoming.telegramUpdateId());
        payload.put("text", incoming.text());
        payload.put("contactPhonePresent", incoming.contactPhone() != null && !incoming.contactPhone().isBlank());
        payload.put("previousState", previousState == null ? null : previousState.name());
        payload.put("nextState", outgoing == null ? null : outgoing.nextState());
        payload.put("actions", outgoing == null ? List.of() : outgoing.actions());
        payload.put("fallback", outgoing != null && outgoing.fallback());
        return payload;
    }

    private GenericRecord llmPayload(
            IncomingMessage incoming,
            BotState state,
            String stage,
            String prompt,
            String response,
            boolean fallbackUsed
    ) {
        GenericRecord payload = new GenericData.Record(
                KafkaEventSchemas.LLM_RESPONSE_GENERATED.getField("payload").schema()
        );
        payload.put("chatId", incoming.chatId());
        payload.put("telegramUserId", incoming.telegramUserId());
        payload.put("telegramMessageId", incoming.telegramMessageId());
        payload.put("telegramUpdateId", incoming.telegramUpdateId());
        payload.put("state", state == null ? null : state.name());
        payload.put("stage", stage);
        payload.put("inputText", incoming.text());
        payload.put("responseText", response);
        payload.put("fallbackUsed", fallbackUsed);
        payload.put("prompt", prompt);
        return payload;
    }

    private String normalizeStage(String stage) {
        if (stage == null || stage.isBlank()) {
            return "unknown";
        }
        return stage.trim().toLowerCase().replaceAll("[^a-z0-9_-]+", "_");
    }
}
