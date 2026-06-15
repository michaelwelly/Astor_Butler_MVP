package museon_online.astor_butler.domain.content;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class VenueContentQueryService {

    private final VenueContentRepository repository;

    public List<VenueContentPost> activeQuietGuidePosts(String venueCode, String prompt) {
        String normalized = prompt == null ? "" : prompt.toLowerCase(Locale.ROOT);
        boolean includePromos = normalized.contains("акци")
                || normalized.contains("скид")
                || normalized.contains("предлож")
                || normalized.contains("промо")
                || normalized.contains("что нового");
        return repository.findActiveForQuietGuide(venueCode, includePromos, Instant.now(), 3);
    }
}
