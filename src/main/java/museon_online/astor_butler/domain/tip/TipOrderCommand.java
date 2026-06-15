package museon_online.astor_butler.domain.tip;

public record TipOrderCommand(
        Long chatId,
        Long telegramUserId,
        Long userId,
        String venueCode,
        Long staffId,
        String staffDisplayName,
        Long amountMinor,
        String currency,
        String guestName,
        String guestComment
) {
}
