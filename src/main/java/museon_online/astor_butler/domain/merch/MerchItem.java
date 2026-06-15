package museon_online.astor_butler.domain.merch;

import java.time.Instant;

public record MerchItem(
        Long id,
        String venueCode,
        String itemCode,
        String title,
        String description,
        MerchItemStatus status,
        Long priceMinor,
        String currency,
        String stockHint,
        String mediaAssetCode,
        String metadataJson,
        Instant createdAt,
        Instant updatedAt
) {
}
