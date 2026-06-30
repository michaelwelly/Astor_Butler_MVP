package museon_online.astor_butler.api.observability;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import museon_online.astor_butler.domain.semantic.SemanticRetrievalService;
import museon_online.astor_butler.domain.semantic.SemanticSearchResult;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/internal")
@Tag(name = "Observability API", description = "Internal health, readiness, liveness and metrics boundary")
@RequiredArgsConstructor
public class ObservabilityController {

    private final SemanticRetrievalService semanticRetrievalService;

    @GetMapping("/metrics")
    @Operation(summary = "Prometheus metrics endpoint placeholder")
    public ResponseEntity<InternalStatusResponse> metrics() {
        return ResponseEntity.ok(new InternalStatusResponse("PROMETHEUS_ENDPOINT_CONFIGURED", Map.of("delegate", "/actuator/prometheus"), Instant.now()));
    }

    @GetMapping("/health/readiness")
    @Operation(summary = "Readiness probe")
    public ResponseEntity<InternalStatusResponse> readiness() {
        return ResponseEntity.ok(new InternalStatusResponse("READY", Map.of(), Instant.now()));
    }

    @GetMapping("/health/liveness")
    @Operation(summary = "Liveness probe")
    public ResponseEntity<InternalStatusResponse> liveness() {
        return ResponseEntity.ok(new InternalStatusResponse("ALIVE", Map.of(), Instant.now()));
    }

    @GetMapping("/semantic/search")
    @Operation(summary = "Internal semantic/RAG search smoke check")
    public ResponseEntity<SemanticSearchResponse> semanticSearch(
            @RequestParam(name = "venueCode", defaultValue = "AERIS") String venueCode,
            @RequestParam("q") String query,
            @RequestParam(name = "sourceCode", required = false) List<String> sourceCodes,
            @RequestParam(name = "limit", defaultValue = "3") int limit
    ) {
        int safeLimit = Math.max(1, Math.min(limit, 10));
        List<SemanticSearchResult> results = semanticRetrievalService.search(
                venueCode,
                query,
                sourceCodes == null ? List.of() : sourceCodes,
                safeLimit
        );
        return ResponseEntity.ok(new SemanticSearchResponse(
                venueCode,
                query,
                sourceCodes == null ? List.of() : sourceCodes,
                results.stream().map(SemanticSearchHit::from).toList(),
                Instant.now()
        ));
    }

    public record InternalStatusResponse(String status, Map<String, Object> details, Instant checkedAt) {
    }

    public record SemanticSearchResponse(
            String venueCode,
            String query,
            List<String> sourceCodes,
            List<SemanticSearchHit> hits,
            Instant checkedAt
    ) {
    }

    public record SemanticSearchHit(
            UUID chunkId,
            String sourceCode,
            String sourceType,
            String title,
            double score,
            String content
    ) {
        static SemanticSearchHit from(SemanticSearchResult result) {
            return new SemanticSearchHit(
                    result.chunkId(),
                    result.sourceCode(),
                    result.sourceType(),
                    result.title(),
                    result.score(),
                    result.shortContent(480)
            );
        }
    }
}
