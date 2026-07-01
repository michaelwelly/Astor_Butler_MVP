package museon_online.astor_butler.kafka;

import museon_online.astor_butler.fsm.core.BotState;
import museon_online.astor_butler.service.message.IncomingMessage;
import museon_online.astor_butler.service.message.OutgoingMessage;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class UserEventFactory {

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

    public Map<String, Object> userMessageReceived(
            IncomingMessage incoming,
            BotState previousState,
            OutgoingMessage outgoing
    ) {
        String eventId = eventId(incoming);
        Map<String, Object> event = envelope(incoming, eventId, "USER_MESSAGE_RECEIVED");
        event.put("payload", userMessagePayload(incoming, previousState, outgoing));
        return event;
    }

    public Map<String, Object> llmResponseGenerated(
            IncomingMessage incoming,
            BotState state,
            String stage,
            String prompt,
            String response,
            boolean fallbackUsed
    ) {
        String eventId = llmEventId(incoming, stage);
        Map<String, Object> event = envelope(incoming, eventId, "LLM_RESPONSE_GENERATED");
        event.put("sourceMessageEventId", eventId(incoming));
        event.put("payload", llmPayload(incoming, state, stage, prompt, response, fallbackUsed));
        return event;
    }

    private Map<String, Object> envelope(IncomingMessage incoming, String eventId, String eventType) {
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("eventId", eventId);
        event.put("eventType", eventType);
        event.put("eventVersion", EVENT_VERSION);
        event.put("occurredAt", Instant.now().toString());
        event.put("source", SOURCE);
        event.put("channel", incoming.channel() == null ? null : incoming.channel().name());
        event.put("idempotencyKey", eventId);
        event.put("actor", actor(incoming));
        return event;
    }

    private Map<String, Object> actor(IncomingMessage incoming) {
        Map<String, Object> actor = new LinkedHashMap<>();
        actor.put("telegramUserId", incoming.telegramUserId());
        actor.put("chatId", incoming.chatId());
        actor.put("username", incoming.username());
        actor.put("firstName", incoming.firstName());
        actor.put("lastName", incoming.lastName());
        return actor;
    }

    private Map<String, Object> userMessagePayload(
            IncomingMessage incoming,
            BotState previousState,
            OutgoingMessage outgoing
    ) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("chatId", incoming.chatId());
        payload.put("telegramUserId", incoming.telegramUserId());
        payload.put("telegramMessageId", incoming.telegramMessageId());
        payload.put("telegramUpdateId", incoming.telegramUpdateId());
        payload.put("text", incoming.text());
        payload.put("mediaKind", payloadValue(incoming, "mediaKind"));
        payload.put("transcriptionStatus", payloadValue(incoming, "transcriptionStatus"));
        payload.put("transcript", payloadValue(incoming, "transcript"));
        payload.put("storageObjectKey", payloadValue(incoming, "storageObjectKey"));
        payload.put("contactPhonePresent", incoming.contactPhone() != null && !incoming.contactPhone().isBlank());
        payload.put("previousState", previousState == null ? null : previousState.name());
        payload.put("nextState", outgoing == null ? null : outgoing.nextState());
        payload.put("actions", outgoing == null ? List.of() : outgoing.actions());
        payload.put("fallback", outgoing != null && outgoing.fallback());
        payload.put("outgoingText", outgoing == null ? null : outgoing.text());
        payload.put("dialogKey", dialogKey(incoming));
        return payload;
    }

    private Map<String, Object> llmPayload(
            IncomingMessage incoming,
            BotState state,
            String stage,
            String prompt,
            String response,
            boolean fallbackUsed
    ) {
        Map<String, Object> payload = new LinkedHashMap<>();
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

    private String payloadValue(IncomingMessage incoming, String key) {
        if (incoming == null || incoming.payload() == null) {
            return null;
        }
        Object value = incoming.payload().get(key);
        return value == null ? null : String.valueOf(value);
    }

    private String dialogKey(IncomingMessage incoming) {
        if (incoming == null) {
            return "dialog:unknown";
        }
        if (incoming.channel() != null && incoming.chatId() != null) {
            return incoming.channel().name().toLowerCase() + ":chat:" + incoming.chatId();
        }
        if (incoming.telegramUserId() != null) {
            return "telegram:user:" + incoming.telegramUserId();
        }
        return "dialog:" + eventId(incoming);
    }
}
