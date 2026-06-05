package museon_online.astor_butler.storage;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.UploadObjectArgs;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Locale;

@Service
@Slf4j
public class ObjectStorageService {

    private final MinioClient minioClient;

    @Value("${astor.storage.s3.media-bucket:astor-media}")
    private String mediaBucket;

    @Value("${astor.storage.s3.public-endpoint:http://localhost:9000}")
    private String publicEndpoint;

    @Value("${astor.storage.s3.ephemeral-prefix:transient}")
    private String ephemeralPrefix;

    @Value("${astor.storage.s3.voice-prefix:telegram-voice}")
    private String voicePrefix;

    @Value("${astor.storage.s3.voice-ttl-days:3}")
    private int voiceTtlDays;

    public ObjectStorageService(
            @Value("${astor.storage.s3.endpoint:http://localhost:9000}") String endpoint,
            @Value("${astor.storage.s3.access-key}") String accessKey,
            @Value("${astor.storage.s3.secret-key}") String secretKey
    ) {
        this.minioClient = MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }

    public ObjectStorageResult uploadTelegramVoice(Path file, Long chatId, Integer messageId, String contentType) {
        if (file == null) {
            throw new IllegalArgumentException("file is required");
        }

        ensureBucket(mediaBucket);
        String objectKey = objectKey(chatId, messageId, file);
        try {
            minioClient.uploadObject(UploadObjectArgs.builder()
                    .bucket(mediaBucket)
                    .object(objectKey)
                    .filename(file.toAbsolutePath().toString())
                    .contentType(contentType == null || contentType.isBlank() ? "application/octet-stream" : contentType)
                    .build());
            log.info("Telegram voice uploaded to object storage: s3://{}/{}", mediaBucket, objectKey);
            return new ObjectStorageResult(mediaBucket, objectKey, publicUrl(mediaBucket, objectKey), voiceTtlDays);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot upload Telegram voice to object storage", e);
        }
    }

    private void ensureBucket(String bucket) {
        try {
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
            }
        } catch (Exception e) {
            throw new IllegalStateException("Cannot ensure object storage bucket " + bucket, e);
        }
    }

    private String objectKey(Long chatId, Integer messageId, Path file) {
        String suffix = suffix(file);
        return "%s/%s/%s/chat-%s/message-%s%s".formatted(
                cleanPrefix(ephemeralPrefix),
                cleanPrefix(voicePrefix),
                LocalDate.now(),
                safe(chatId),
                safe(messageId),
                suffix
        );
    }

    private String publicUrl(String bucket, String objectKey) {
        return publicEndpoint.replaceAll("/+$", "") + "/" + bucket + "/" + objectKey;
    }

    private String suffix(Path file) {
        String name = file.getFileName().toString();
        int dot = name.lastIndexOf('.');
        if (dot < 0) {
            return "";
        }
        return name.substring(dot).toLowerCase(Locale.ROOT);
    }

    private String cleanPrefix(String value) {
        if (value == null || value.isBlank()) {
            return "transient";
        }
        return value.strip().replaceAll("^/+", "").replaceAll("/+$", "");
    }

    private String safe(Object value) {
        if (value == null) {
            return "unknown";
        }
        return value.toString().replaceAll("[^a-zA-Z0-9_-]", "_");
    }
}
