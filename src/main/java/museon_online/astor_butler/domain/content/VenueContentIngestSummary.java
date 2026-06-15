package museon_online.astor_butler.domain.content;

public record VenueContentIngestSummary(
        String venueCode,
        int discovered,
        int upserted,
        int active,
        int needsReview
) {
}
