package museon_online.astor_butler.storage;

public record ObjectStorageResult(
        String bucket,
        String objectKey,
        String publicUrl,
        int ttlDays
) {
}
