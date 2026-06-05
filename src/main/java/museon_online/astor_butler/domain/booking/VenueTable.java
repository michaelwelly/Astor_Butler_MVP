package museon_online.astor_butler.domain.booking;

import java.time.Instant;

public record VenueTable(
        Long id,
        String venueCode,
        String tableCode,
        String displayName,
        String zone,
        Integer capacityMin,
        Integer capacityMax,
        String combinableGroup,
        Boolean bookable,
        Boolean active,
        Integer planPage,
        String planRef,
        Integer sortOrder,
        Instant createdAt,
        Instant updatedAt
) {
}
