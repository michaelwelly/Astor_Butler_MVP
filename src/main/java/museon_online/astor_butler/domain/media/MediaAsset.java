package museon_online.astor_butler.domain.media;

public record MediaAsset(
        String assetCode,
        String venueCode,
        String domain,
        String kind,
        String title,
        String bucket,
        String objectKey,
        String filename,
        String contentType,
        boolean active
) {
    public MediaAsset {
        if (assetCode == null || assetCode.isBlank()) {
            throw new IllegalArgumentException("assetCode is required");
        }
        if (objectKey == null || objectKey.isBlank()) {
            throw new IllegalArgumentException("objectKey is required");
        }
        if (filename == null || filename.isBlank()) {
            throw new IllegalArgumentException("filename is required");
        }
    }
}
