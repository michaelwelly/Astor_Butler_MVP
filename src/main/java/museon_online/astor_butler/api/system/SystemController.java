package museon_online.astor_butler.api.system;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/system")
@Tag(name = "System API", description = "Read-only endpoints for local smoke and load checks")
public class SystemController {

    private final String activeProfile;

    public SystemController(@Value("${spring.profiles.active:default}") String activeProfile) {
        this.activeProfile = activeProfile;
    }

    @GetMapping("/ping")
    @Operation(summary = "Проверить доступность REST API")
    public ResponseEntity<Map<String, Object>> ping() {
        return ResponseEntity.ok(Map.of(
                "status", "ok",
                "service", "astor-butler",
                "profile", activeProfile,
                "timestamp", Instant.now().toString()
        ));
    }

    @GetMapping("/readiness")
    @Operation(summary = "Проверить готовность приложения к локальному smoke-тесту")
    public ResponseEntity<Map<String, Object>> readiness() {
        return ResponseEntity.ok(Map.of(
                "ready", true,
                "swagger", "/swagger-ui/index.html",
                "openapi", "/v3/api-docs",
                "health", "/actuator/health"
        ));
    }
}
