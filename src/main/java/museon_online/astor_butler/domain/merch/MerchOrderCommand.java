package museon_online.astor_butler.domain.merch;

public record MerchOrderCommand(
        Long chatId,
        Long telegramUserId,
        Long userId,
        String venueCode,
        Long itemId,
        String itemTitle,
        Integer quantity,
        String guestName,
        String guestComment,
        String paymentMethodHint
) {
}
