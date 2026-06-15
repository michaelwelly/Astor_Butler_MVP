package museon_online.astor_butler.domain.auction;

import java.time.Instant;

public record ArtAuctionLot(
        Long id,
        Long auctionEventId,
        String lotCode,
        String title,
        String artistName,
        String description,
        ArtAuctionLotStatus status,
        Long startingPriceMinor,
        Long minStepMinor,
        String currency,
        String mediaAssetCode,
        String metadataJson,
        Instant createdAt,
        Instant updatedAt
) {
}
