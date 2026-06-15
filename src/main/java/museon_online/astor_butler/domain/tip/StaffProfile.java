package museon_online.astor_butler.domain.tip;

import java.time.Instant;

public record StaffProfile(
        Long id,
        String venueCode,
        String displayName,
        String role,
        Long telegramUserId,
        String phone,
        String sbpLink,
        Boolean active,
        String metadataJson,
        Instant createdAt,
        Instant updatedAt
) {
}
