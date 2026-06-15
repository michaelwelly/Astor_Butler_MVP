package museon_online.astor_butler.domain.content;

import lombok.RequiredArgsConstructor;
import museon_online.astor_butler.domain.media.AerisMediaCatalog;
import museon_online.astor_butler.domain.media.MediaAsset;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AerisContentReadService {

    private final AerisMediaCatalog mediaCatalog;
    private final VenueContentQueryService venueContentQueryService;
    private final VenueContentRepository venueContentRepository;

    @Value("${astor.storage.s3.public-endpoint:http://localhost:9000}")
    private String publicEndpoint;

    public MenuAssetsView menuAssets() {
        return new MenuAssetsView(
                "AERIS",
                mediaCatalog.allMenus().stream().map(this::assetView).toList(),
                mediaCatalog.menuRagSource()
        );
    }

    public QuietGuideView quietGuide(String prompt) {
        List<VenueContentPostView> posts = venueContentQueryService.activeQuietGuidePosts("AERIS", prompt)
                .stream()
                .map(this::postView)
                .toList();
        return new QuietGuideView(
                "AERIS",
                assetView(mediaCatalog.interiorTour()),
                conceptView(),
                posts
        );
    }

    private VenueContentPostView postView(VenueContentPost post) {
        return new VenueContentPostView(
                post.id(),
                post.contentType().name(),
                post.status().name(),
                post.title(),
                post.body(),
                post.sourceUrl(),
                post.sourceChannel(),
                post.sourceMessageId(),
                post.eventStartsAt(),
                post.activeUntil(),
                post.publishedAt(),
                venueContentRepository.findAssetsByPostId(post.id()).stream().map(this::contentAssetView).toList()
        );
    }

    private MediaAssetView assetView(MediaAsset asset) {
        return new MediaAssetView(
                asset.assetCode(),
                asset.venueCode(),
                asset.domain(),
                asset.kind(),
                asset.title(),
                asset.bucket(),
                asset.objectKey(),
                asset.filename(),
                asset.contentType(),
                asset.active(),
                publicUrl(asset.bucket(), asset.objectKey())
        );
    }

    private VenueContentAssetView contentAssetView(VenueContentAsset asset) {
        return new VenueContentAssetView(
                asset.assetKind(),
                asset.sourceUrl(),
                asset.bucket(),
                asset.objectKey(),
                asset.contentType(),
                asset.objectKey() == null || asset.objectKey().isBlank() || asset.bucket() == null || asset.bucket().isBlank()
                        ? null
                        : publicUrl(asset.bucket(), asset.objectKey())
        );
    }

    private ConceptView conceptView() {
        return new ConceptView(
                "Гастрономическая экспедиция Георгия Матвеева в AERIS",
                "Кухня 21 страны Средиземноморья в авторском прочтении Георгия Матвеева: продукт, чистота вкуса, премиальное мясо, свежая рыба, зелень и неклассические соусы."
        );
    }

    private String publicUrl(String bucket, String objectKey) {
        String endpoint = publicEndpoint == null || publicEndpoint.isBlank()
                ? "http://localhost:9000"
                : publicEndpoint;
        return endpoint.replaceAll("/+$", "") + "/" + bucket + "/" + objectKey.replaceAll("^/+", "");
    }

    public record MenuAssetsView(
            String venueCode,
            List<MediaAssetView> menus,
            String ragSource
    ) {
    }

    public record QuietGuideView(
            String venueCode,
            MediaAssetView interiorTour,
            ConceptView concept,
            List<VenueContentPostView> activePosts
    ) {
    }

    public record MediaAssetView(
            String assetCode,
            String venueCode,
            String domain,
            String kind,
            String title,
            String bucket,
            String objectKey,
            String filename,
            String contentType,
            boolean active,
            String publicUrl
    ) {
    }

    public record ConceptView(
            String title,
            String summary
    ) {
    }

    public record VenueContentPostView(
            UUID id,
            String contentType,
            String status,
            String title,
            String body,
            String sourceUrl,
            String sourceChannel,
            String sourceMessageId,
            Instant eventStartsAt,
            Instant activeUntil,
            Instant publishedAt,
            List<VenueContentAssetView> assets
    ) {
    }

    public record VenueContentAssetView(
            String assetKind,
            String sourceUrl,
            String bucket,
            String objectKey,
            String contentType,
            String publicUrl
    ) {
    }
}
