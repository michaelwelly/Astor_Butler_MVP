package museon_online.astor_butler.api.content;

import lombok.RequiredArgsConstructor;
import museon_online.astor_butler.domain.content.VenueContentIngestService;
import museon_online.astor_butler.domain.content.VenueContentIngestSummary;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/content/ingest")
@RequiredArgsConstructor
public class VenueContentIngestController {

    private final VenueContentIngestService ingestService;

    @PostMapping("/aeris-channel")
    public VenueContentIngestSummary ingestAerisChannel() {
        return ingestService.ingestAerisChannel();
    }
}
