package museon_online.astor_butler.domain.auction;

import java.time.Instant;

public record ArtAuctionBid(
        Long id,
        Long lotId,
        Long chatId,
        Long telegramUserId,
        Long userId,
        ArtAuctionBidStatus status,
        String source,
        Long amountMinor,
        String currency,
        String bidderName,
        String guestComment,
        String paymentExternalId,
        Instant createdAt,
        Instant updatedAt
) {
}
