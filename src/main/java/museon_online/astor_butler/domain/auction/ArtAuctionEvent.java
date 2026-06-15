package museon_online.astor_butler.domain.auction;

import java.time.Instant;

public record ArtAuctionEvent(
        Long id,
        String venueCode,
        String eventCode,
        String title,
        String description,
        ArtAuctionEventStatus status,
        Instant startsAt,
        Instant endsAt,
        Long donationInitiativeId,
        String metadataJson,
        Instant createdAt,
        Instant updatedAt
) {
}
