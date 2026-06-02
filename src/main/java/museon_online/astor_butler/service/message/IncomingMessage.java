package museon_online.astor_butler.service.message;

import java.time.Instant;
import java.util.Map;

public record IncomingMessage(
        MessageChannel channel,
        String externalUserId,
        Long chatId,
        String text,
        String contactPhone,
        String firstName,
        String username,
        String correlationId,
        Instant receivedAt,
        Map<String, Object> payload
) {
    public static IncomingMessage telegram(
            Long chatId,
            String text,
            String contactPhone,
            String firstName,
            String username,
            String correlationId
    ) {
        return new IncomingMessage(
                MessageChannel.TELEGRAM,
                chatId == null ? null : chatId.toString(),
                chatId,
                text,
                contactPhone,
                firstName,
                username,
                correlationId,
                Instant.now(),
                Map.of()
        );
    }
}
