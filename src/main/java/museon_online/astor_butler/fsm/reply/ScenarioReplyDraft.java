package museon_online.astor_butler.fsm.reply;

import museon_online.astor_butler.domain.semantic.SemanticSearchResult;
import museon_online.astor_butler.service.message.IncomingMessage;

import java.util.List;

public record ScenarioReplyDraft(
        String venueCode,
        String channel,
        Long chatId,
        Long telegramUserId,
        String correlationId,
        String scenario,
        String state,
        String purpose,
        String guestText,
        String fallbackText,
        List<SemanticSearchResult> ragContext
) {

    public ScenarioReplyDraft {
        if (ragContext == null) {
            ragContext = List.of();
        }
    }

    public static ScenarioReplyDraft of(
            String scenario,
            String state,
            String purpose,
            String guestText,
            String fallbackText,
            List<SemanticSearchResult> ragContext
    ) {
        return new ScenarioReplyDraft(null, null, null, null, null, scenario, state, purpose, guestText, fallbackText, ragContext);
    }

    public static ScenarioReplyDraft of(
            IncomingMessage incoming,
            String venueCode,
            String scenario,
            String state,
            String purpose,
            String guestText,
            String fallbackText,
            List<SemanticSearchResult> ragContext
    ) {
        return new ScenarioReplyDraft(
                venueCode,
                incoming == null || incoming.channel() == null ? null : incoming.channel().name(),
                incoming == null ? null : incoming.chatId(),
                incoming == null ? null : incoming.telegramUserId(),
                incoming == null ? null : incoming.correlationId(),
                scenario,
                state,
                purpose,
                guestText,
                fallbackText,
                ragContext
        );
    }
}
