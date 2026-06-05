package museon_online.astor_butler.service.message;

import java.time.Instant;
import java.util.HashMap;
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
        return telegram(
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
                Map.of()
        );
    }

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
            String correlationId,
            Map<String, Object> payload
    ) {
        Map<String, Object> safePayload = payload == null ? Map.of() : Map.copyOf(new HashMap<>(payload));
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
                safePayload
        );
    }

    public IncomingMessage withTextAndPayload(String newText, Map<String, Object> newPayload) {
        return new IncomingMessage(
                channel,
                externalUserId,
                chatId,
                telegramUserId,
                telegramMessageId,
                telegramUpdateId,
                newText,
                contactPhone,
                firstName,
                lastName,
                username,
                languageCode,
                bot,
                correlationId,
                receivedAt,
                newPayload == null ? Map.of() : Map.copyOf(new HashMap<>(newPayload))
        );
    }
}
