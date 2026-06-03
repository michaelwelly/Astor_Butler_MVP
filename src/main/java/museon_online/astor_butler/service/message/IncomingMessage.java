package museon_online.astor_butler.service.message;

import java.time.Instant;
import java.util.Map;

public record IncomingMessage(
        MessageChannel channel,
        String externalUserId,
        Long chatId,
        Long telegramUserId,
        Integer telegramMessageId,
        Integer telegramUpdateId,
        String text,
        String contactPhone,
        String firstName,
        String lastName,
        String username,
        String languageCode,
        Boolean bot,
        String correlationId,
        Instant receivedAt,
        Map<String, Object> payload
) {
    public static IncomingMessage telegram(
            Long chatId,
            Long telegramUserId,
            Integer telegramMessageId,
            Integer telegramUpdateId,
            String text,
            String contactPhone,
            String firstName,
            String lastName,
            String username,
            String languageCode,
            Boolean bot,
            String correlationId
    ) {
        return new IncomingMessage(
                MessageChannel.TELEGRAM,
                telegramUserId == null ? null : telegramUserId.toString(),
                chatId,
                telegramUserId,
                telegramMessageId,
                telegramUpdateId,
                text,
                contactPhone,
                firstName,
                lastName,
                username,
                languageCode,
                bot,
                correlationId,
                Instant.now(),
                Map.of()
        );
    }
}
