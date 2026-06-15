package museon_online.astor_butler.domain.content;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class VenueContentIngestService {

    private final PublicTelegramHtmlSource publicTelegramHtmlSource;
    private final VenueContentAssetStorageService assetStorageService;
    private final VenueContentClassifier classifier;
    private final VenueContentRepository repository;

    @Value("${astor.content.telegram.public-channel.venue-code:AERIS}")
    private String venueCode;

    @Value("${astor.content.telegram.public-channel.scheduled-enabled:false}")
    private boolean scheduledEnabled;

    public VenueContentIngestSummary ingestAerisChannel() {
        if (!publicTelegramHtmlSource.enabled()) {
            return new VenueContentIngestSummary(normalizeVenue(venueCode), 0, 0, 0, 0);
        }

        List<NormalizedVenueContentPost> posts = publicTelegramHtmlSource.fetchRecent();
        int active = 0;
        int needsReview = 0;
        int upserted = 0;
        for (NormalizedVenueContentPost post : posts) {
            NormalizedVenueContentPost mirroredPost = assetStorageService.mirrorAssets(post);
            ClassifiedVenueContentPost classified = classifier.classify(mirroredPost);
            repository.upsert(classified);
            upserted++;
            if (classified.status() == VenueContentStatus.ACTIVE) {
                active++;
            } else if (classified.status() == VenueContentStatus.NEEDS_REVIEW) {
                needsReview++;
            }
        }
        VenueContentIngestSummary summary = new VenueContentIngestSummary(normalizeVenue(venueCode), posts.size(), upserted, active, needsReview);
        log.info("AERIS channel ingest finished: {}", summary);
        return summary;
    }

    @Scheduled(fixedDelayString = "${astor.content.telegram.public-channel.fixed-delay-ms:1800000}")
    public void scheduledIngest() {
        if (scheduledEnabled) {
            ingestAerisChannel();
        }
    }

    private String normalizeVenue(String value) {
        return value == null || value.isBlank() ? "AERIS" : value.trim().toUpperCase();
    }
}
