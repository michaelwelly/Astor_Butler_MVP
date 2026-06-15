package museon_online.astor_butler.domain.donation;

public record DonationOrderCommand(
        Long chatId,
        Long telegramUserId,
        Long userId,
        String venueCode,
        Long initiativeId,
        String initiativeTitle,
        Long amountMinor,
        String currency,
        Boolean anonymous,
        String guestName,
        String guestComment
) {
}
