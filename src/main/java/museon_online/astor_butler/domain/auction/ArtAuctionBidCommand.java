package museon_online.astor_butler.domain.auction;

public record ArtAuctionBidCommand(
        Long chatId,
        Long telegramUserId,
        Long userId,
        String venueCode,
        Long lotId,
        Long amountMinor,
        String currency,
        String bidderName,
        String guestComment
) {
}
