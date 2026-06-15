package museon_online.astor_butler.domain.content;

import java.time.Instant;
import java.util.List;

public record NormalizedVenueContentPost(
        String venueCode,
        String sourceType,
        String sourceChannel,
        String sourceMessageId,
        String sourceUrl,
        String sourceHash,
        Instant publishedAt,
        String text,
        List<VenueContentAsset> assets,
        String rawPayloadJson
) {
    public NormalizedVenueContentPost {
        assets = assets == null ? List.of() : List.copyOf(assets);
    }
}
