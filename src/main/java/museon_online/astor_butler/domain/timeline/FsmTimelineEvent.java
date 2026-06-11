package museon_online.astor_butler.domain.timeline;

import museon_online.astor_butler.fsm.core.BotState;
import museon_online.astor_butler.service.message.IncomingMessage;
import museon_online.astor_butler.service.message.OutgoingMessage;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record FsmTimelineEvent(
        String guestId,
        Instant occurredAt,
        String eventId,
        String channel,
        Long chatId,
        Long telegramUserId,
        String previousState,
        String nextState,
        String intent,
        Double confidence,
        List<String> actions,
        String correlationId,
        String rawText,
        String normalizedText,
        Map<String, Object> metadata
) {
    public static FsmTimelineEvent from(IncomingMessage incoming, BotState previousState, OutgoingMessage outgoing) {
        String guestId = incoming.telegramUserId() == null
                ? "chat:" + incoming.chatId()
                : "telegram:" + incoming.telegramUserId();
        String eventId = incoming.correlationId() == null || incoming.correlationId().isBlank()
                ? guestId + ":" + incoming.chatId() + ":" + Instant.now().toEpochMilli()
                : incoming.correlationId();
        List<String> actions = outgoing.actions() == null ? List.of() : List.copyOf(outgoing.actions());
        String intent = actions.isEmpty() ? "UNKNOWN" : actions.get(0);
        return new FsmTimelineEvent(
                guestId,
                outgoing.createdAt() == null ? Instant.now() : outgoing.createdAt(),
                eventId,
                incoming.channel() == null ? "UNKNOWN" : incoming.channel().name(),
                incoming.chatId(),
                incoming.telegramUserId(),
                previousState == null ? null : previousState.name(),
                outgoing.nextState(),
                intent,
                null,
                actions,
                incoming.correlationId(),
                incoming.text(),
                incoming.text(),
                outgoing.metadata() == null ? Map.of() : Map.copyOf(outgoing.metadata())
        );
    }
}
