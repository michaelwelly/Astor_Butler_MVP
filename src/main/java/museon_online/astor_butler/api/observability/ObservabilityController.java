package museon_online.astor_butler.api.observability;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/internal")
@Tag(name = "Observability API", description = "Internal health, readiness, liveness and metrics boundary")
public class ObservabilityController {

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

    public record InternalStatusResponse(String status, Map<String, Object> details, Instant checkedAt) {
    }
}
