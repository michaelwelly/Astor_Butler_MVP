package museon_online.astor_butler.domain.content;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class VenueContentClassifierTest {

    private final VenueContentClassifier classifier = new VenueContentClassifier();

    @Test
    void classifiesAfishaEvent() {
        NormalizedVenueContentPost post = new NormalizedVenueContentPost(
                "AERIS",
                "TELEGRAM_PUBLIC_HTML",
                "aeris_gastrobar",
                "123",
                "https://t.me/aeris_gastrobar/123",
                "hash",
                Instant.parse("2026-06-15T12:00:00Z"),
                "Сегодня в 21:00 DJ set и гастроужин",
                List.of(),
                "{}"
        );

        ClassifiedVenueContentPost classified = classifier.classify(post);

        assertThat(classified.contentType()).isEqualTo(VenueContentType.AFISHA_EVENT);
        assertThat(classified.status()).isEqualTo(VenueContentStatus.ACTIVE);
        assertThat(classified.classificationConfidence()).isGreaterThan(0.8);
        assertThat(classified.title()).contains("Сегодня");
    }
}
