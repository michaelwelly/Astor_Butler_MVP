package museon_online.astor_butler.domain.content;

public record VenueContentAsset(
        String assetKind,
        String sourceUrl,
        String bucket,
        String objectKey,
        String contentType
) {
}
