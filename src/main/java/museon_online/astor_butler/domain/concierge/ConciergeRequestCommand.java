package museon_online.astor_butler.domain.concierge;

public record ConciergeRequestCommand(
        Long chatId,
        Long telegramUserId,
        Long userId,
        String venueCode,
        ConciergeRequestType requestType,
        String guestName,
        String requestText,
        String managerChatId,
        String capturedFromState,
        String correlationId,
        String metadataJson
) {
}
