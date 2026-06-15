package museon_online.astor_butler.domain.content;

import java.util.List;

public interface VenueContentSource {

    boolean enabled();

    List<NormalizedVenueContentPost> fetchRecent();
}
