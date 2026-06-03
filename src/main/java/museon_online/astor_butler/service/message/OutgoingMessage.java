package museon_online.astor_butler.service.message;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record OutgoingMessage(
        MessageChannel channel,
        String externalUserId,
        Long chatId,
        String text,
        String nextState,
        boolean html,
        boolean requestContact,
        boolean removeKeyboard,
        boolean fallback,
        AdminAlert adminAlert,
        List<String> actions,
        Map<String, Object> metadata,
        Instant createdAt
) {
    public static OutgoingMessage of(
            IncomingMessage incoming,
            String text,
            String nextState,
            boolean html,
            boolean requestContact,
            boolean removeKeyboard,
            boolean fallback,
            AdminAlert adminAlert,
            List<String> actions
    ) {
        return new OutgoingMessage(
                incoming.channel(),
                incoming.externalUserId(),
                incoming.chatId(),
                text,
                nextState,
                html,
                requestContact,
                removeKeyboard,
                fallback,
                adminAlert == null ? AdminAlert.none() : adminAlert,
                actions == null ? List.of() : actions,
                Map.of("correlationId", incoming.correlationId() == null ? "" : incoming.correlationId()),
                Instant.now()
        );
    }
}
