package museon_online.astor_butler.domain.content;

import java.time.Instant;

public record ClassifiedVenueContentPost(
        NormalizedVenueContentPost source,
        VenueContentType contentType,
        VenueContentStatus status,
        String title,
        String body,
        Instant eventStartsAt,
        Instant eventEndsAt,
        Instant activeFrom,
        Instant activeUntil,
        double classificationConfidence
) {
}
