package museon_online.astor_butler.domain.content;

import java.time.Instant;
import java.util.UUID;

public record VenueContentPost(
        UUID id,
        String venueCode,
        String sourceType,
        String sourceChannel,
        String sourceMessageId,
        String sourceUrl,
        VenueContentType contentType,
        VenueContentStatus status,
        String title,
        String body,
        Instant eventStartsAt,
        Instant activeUntil,
        double classificationConfidence,
        Instant publishedAt,
        Instant createdAt,
        Instant updatedAt
) {
}
