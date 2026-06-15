package museon_online.astor_butler.domain.content;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import museon_online.astor_butler.storage.ObjectStorageResult;
import museon_online.astor_butler.storage.ObjectStorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class VenueContentAssetStorageService {

    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("yyyy/MM").withZone(ZoneOffset.UTC);

    private final ObjectStorageService objectStorageService;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    @Value("${astor.content.telegram.public-channel.asset-mirroring-enabled:true}")
    private boolean assetMirroringEnabled;

    @Value("${astor.content.telegram.public-channel.asset-timeout-seconds:12}")
    private long timeoutSeconds;

    @Value("${astor.content.telegram.public-channel.asset-max-bytes:25000000}")
    private int maxBytes;

    public NormalizedVenueContentPost mirrorAssets(NormalizedVenueContentPost post) {
        if (!assetMirroringEnabled || post == null || post.assets().isEmpty()) {
            return post;
        }

        List<VenueContentAsset> mirrored = new ArrayList<>();
        for (int i = 0; i < post.assets().size(); i++) {
            VenueContentAsset asset = post.assets().get(i);
            mirrored.add(mirrorAsset(post, asset, i + 1));
        }
        return new NormalizedVenueContentPost(
                post.venueCode(),
                post.sourceType(),
                post.sourceChannel(),
                post.sourceMessageId(),
                post.sourceUrl(),
                post.sourceHash(),
                post.publishedAt(),
                post.text(),
                mirrored,
                post.rawPayloadJson()
        );
    }

    private VenueContentAsset mirrorAsset(NormalizedVenueContentPost post, VenueContentAsset asset, int index) {
        if (asset.sourceUrl() == null || asset.sourceUrl().isBlank()) {
            return asset;
        }
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(asset.sourceUrl()))
                    .GET()
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .header("User-Agent", "AstorButler/1.0 (+local content media ingest)")
                    .build();
            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() < 200 || response.statusCode() >= 300 || response.body() == null || response.body().length == 0) {
                log.debug("Venue content asset mirror skipped: status={}, url={}", response.statusCode(), asset.sourceUrl());
                return asset;
            }
            if (response.body().length > maxBytes) {
                log.warn("Venue content asset mirror skipped: too large bytes={}, url={}", response.body().length, asset.sourceUrl());
                return asset;
            }
            String contentType = response.headers().firstValue("content-type").orElse(asset.contentType());
            String objectKey = objectKey(post, asset, index, contentType);
            ObjectStorageResult result = objectStorageService.uploadMediaObject(objectKey, response.body(), contentType);
            return new VenueContentAsset(
                    asset.assetKind(),
                    asset.sourceUrl(),
                    result.bucket(),
                    result.objectKey(),
                    contentType
            );
        } catch (Exception e) {
            log.debug("Venue content asset mirror skipped: url={}, reason={}", asset.sourceUrl(), e.getMessage());
            return asset;
        }
    }

    private String objectKey(NormalizedVenueContentPost post, VenueContentAsset asset, int index, String contentType) {
        String month = post.publishedAt() == null ? MONTH.format(java.time.Instant.now()) : MONTH.format(post.publishedAt());
        return "content/%s/channel/%s/%s/%02d%s".formatted(
                post.venueCode().toLowerCase(Locale.ROOT),
                month,
                safe(post.sourceMessageId()),
                index,
                extension(asset, contentType)
        );
    }

    private String extension(VenueContentAsset asset, String contentType) {
        String sourceUrl = asset.sourceUrl() == null ? "" : asset.sourceUrl();
        int query = sourceUrl.indexOf('?');
        String cleanUrl = query < 0 ? sourceUrl : sourceUrl.substring(0, query);
        int dot = cleanUrl.lastIndexOf('.');
        if (dot >= 0 && dot + 1 < cleanUrl.length() && cleanUrl.length() - dot <= 6) {
            return cleanUrl.substring(dot).toLowerCase(Locale.ROOT);
        }
        if (contentType != null && contentType.contains("png")) {
            return ".png";
        }
        if (contentType != null && contentType.contains("webp")) {
            return ".webp";
        }
        if (contentType != null && contentType.contains("video")) {
            return ".mp4";
        }
        return "VIDEO".equalsIgnoreCase(asset.assetKind()) ? ".mp4" : ".jpg";
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "unknown" : value.replaceAll("[^a-zA-Z0-9_-]", "_");
    }
}
