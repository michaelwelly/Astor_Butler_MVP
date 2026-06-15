package museon_online.astor_butler.domain.payment;

public record TelegramStarPaymentCommand(
        Long chatId,
        Long telegramUserId,
        Long userId,
        String venueCode,
        TelegramStarPaymentPurpose purpose,
        String relatedEntityType,
        Long relatedEntityId,
        String title,
        String description,
        Long starAmount
) {
}
