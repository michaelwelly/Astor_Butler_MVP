package museon_online.astor_butler.domain.feedback;

public record GuestFeedbackCommand(
        Long chatId,
        Long telegramUserId,
        Long userId,
        String venueCode,
        String guestName,
        String text,
        String previousState,
        String correlationId,
        String adminChatId
) {
}
