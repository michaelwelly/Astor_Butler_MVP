package museon_online.astor_butler.domain.content;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;

@Service
public class VenueContentClassifier {

    public ClassifiedVenueContentPost classify(NormalizedVenueContentPost post) {
        String text = normalize(post == null ? "" : post.text());
        VenueContentType type = type(text);
        double confidence = confidence(type, text);
        VenueContentStatus status = confidence < 0.55 ? VenueContentStatus.NEEDS_REVIEW : VenueContentStatus.ACTIVE;
        Instant activeFrom = post == null || post.publishedAt() == null ? Instant.now() : post.publishedAt();
        Instant activeUntil = activeUntil(type, activeFrom);
        return new ClassifiedVenueContentPost(
                post,
                type,
                status,
                title(post == null ? "" : post.text(), type),
                trimBody(post == null ? "" : post.text()),
                null,
                null,
                activeFrom,
                activeUntil,
                confidence
        );
    }

    private VenueContentType type(String text) {
        if (containsAny(text, "афиша", "вечерин", "dj", "диджей", "ужин", "концерт", "live", "сегодня", "завтра", "выходн")) {
            return VenueContentType.AFISHA_EVENT;
        }
        if (containsAny(text, "скид", "акци", "special", "сет", "happy hour", "предложен", "промо")) {
            return VenueContentType.PROMO_OFFER;
        }
        if (containsAny(text, "новое меню", "новое блюдо", "коктейл", "винн", "барн", "десерт", "шеф")) {
            return VenueContentType.MENU_UPDATE;
        }
        if (containsAny(text, "режим", "работаем", "открыт", "команда", "новость")) {
            return VenueContentType.VENUE_NEWS;
        }
        if (text.length() > 40) {
            return VenueContentType.ATMOSPHERE_CONTENT;
        }
        return VenueContentType.UNKNOWN_REVIEW;
    }

    private double confidence(VenueContentType type, String text) {
        if (type == VenueContentType.UNKNOWN_REVIEW) {
            return 0.25;
        }
        if (type == VenueContentType.AFISHA_EVENT && containsAny(text, "сегодня", "завтра", "выходн", "20:", "21:", "22:")) {
            return 0.82;
        }
        if (type == VenueContentType.PROMO_OFFER || type == VenueContentType.MENU_UPDATE) {
            return 0.74;
        }
        return 0.62;
    }

    private Instant activeUntil(VenueContentType type, Instant activeFrom) {
        Duration ttl = switch (type) {
            case AFISHA_EVENT, PROMO_OFFER -> Duration.ofDays(45);
            case MENU_UPDATE, VENUE_NEWS -> Duration.ofDays(90);
            case ATMOSPHERE_CONTENT -> Duration.ofDays(30);
            case UNKNOWN_REVIEW -> Duration.ofDays(14);
        };
        return activeFrom.plus(ttl);
    }

    private String title(String text, VenueContentType type) {
        String trimmed = trimBody(text);
        if (!trimmed.isBlank()) {
            int newline = trimmed.indexOf('\n');
            String firstLine = newline < 0 ? trimmed : trimmed.substring(0, newline);
            return firstLine.length() > 90 ? firstLine.substring(0, 87) + "..." : firstLine;
        }
        return switch (type) {
            case AFISHA_EVENT -> "Афиша AERIS";
            case PROMO_OFFER -> "Предложение AERIS";
            case MENU_UPDATE -> "Обновление меню AERIS";
            case VENUE_NEWS -> "Новость AERIS";
            case ATMOSPHERE_CONTENT -> "AERIS";
            case UNKNOWN_REVIEW -> "Пост AERIS на проверке";
        };
    }

    private String trimBody(String text) {
        if (text == null) {
            return "";
        }
        String trimmed = text.trim();
        return trimmed.length() > 900 ? trimmed.substring(0, 897) + "..." : trimmed;
    }

    private boolean containsAny(String text, String... needles) {
        for (String needle : needles) {
            if (text.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String text) {
        return text == null ? "" : text.toLowerCase(Locale.ROOT);
    }
}
