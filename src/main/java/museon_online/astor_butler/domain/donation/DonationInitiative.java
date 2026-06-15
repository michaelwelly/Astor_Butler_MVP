package museon_online.astor_butler.domain.donation;

import java.time.Instant;

public record DonationInitiative(
        Long id,
        String venueCode,
        String initiativeCode,
        String title,
        String description,
        String sbpLink,
        Boolean active,
        String metadataJson,
        Instant createdAt,
        Instant updatedAt
) {
}
