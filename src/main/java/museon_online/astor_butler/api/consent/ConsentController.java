package museon_online.astor_butler.api.consent;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import museon_online.astor_butler.domain.web.WebSessionMessageService;
import museon_online.astor_butler.domain.web.WebSessionResolution;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/consents")
@RequiredArgsConstructor
@Tag(name = "Consent Vault API", description = "Consent capture, privacy policy acceptance and export boundary")
public class ConsentController {

    private final WebSessionMessageService webSessionMessageService;

    @PostMapping
    @Operation(summary = "Grant user consent")
    public ResponseEntity<ConsentResponse> grant(@RequestBody ConsentGrantRequest request) {
        WebSessionResolution webSession = null;
        if (request.userId() == null && "WEB".equalsIgnoreCase(request.source())) {
            webSession = webSessionMessageService.grantAnonymousConsent(
                    request.source(),
                    request.policyVersion(),
                    request.evidence()
            );
        }

        Map<String, Object> metadata = webSession == null
                ? Map.of("stub", true)
                : Map.of(
                        "anonymous", true,
                        "sessionId", webSession.sessionId(),
                        "chatId", webSession.chatId(),
                        "externalUserId", webSession.externalUserId()
                );

        return ResponseEntity.status(HttpStatus.CREATED).body(new ConsentResponse(
                UUID.randomUUID(),
                request.userId(),
                request.consentType(),
                request.policyVersion(),
                "GRANTED",
                request.source(),
                Instant.now(),
                metadata
        ));
    }

    @GetMapping("/users/{userId}")
    @Operation(summary = "List user consents")
    public ResponseEntity<ConsentListResponse> list(@PathVariable("userId") UUID userId) {
        return ResponseEntity.ok(new ConsentListResponse(userId, List.of()));
    }

    @PostMapping("/users/{userId}/export")
    @Operation(summary = "Request consent and personal data export")
    public ResponseEntity<ConsentExportResponse> export(@PathVariable("userId") UUID userId) {
        return ResponseEntity.accepted().body(new ConsentExportResponse(
                UUID.randomUUID(),
                userId,
                "ACCEPTED",
                Instant.now()
        ));
    }

    @DeleteMapping("/users/{userId}/{consentType}")
    @Operation(summary = "Revoke user consent")
    public ResponseEntity<Void> revoke(
            @PathVariable("userId") UUID userId,
            @PathVariable("consentType") String consentType
    ) {
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/policy/current")
    @Operation(summary = "Get current privacy policy version")
    public ResponseEntity<PolicyResponse> currentPolicy() {
        return ResponseEntity.ok(new PolicyResponse(
                "2026-06-02-local",
                "Astor Butler local MVP privacy policy placeholder",
                "/docs/policy.html",
                Instant.parse("2026-06-02T00:00:00Z")
        ));
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ConsentGrantRequest(UUID userId, String consentType, String policyVersion, String source, Map<String, Object> evidence) {
    }

    public record ConsentResponse(UUID id, UUID userId, String consentType, String policyVersion, String status, String source, Instant updatedAt, Map<String, Object> metadata) {
    }

    public record ConsentListResponse(UUID userId, List<ConsentResponse> items) {
    }

    public record ConsentExportResponse(UUID requestId, UUID userId, String status, Instant acceptedAt) {
    }

    public record PolicyResponse(String version, String title, String url, Instant effectiveFrom) {
    }
}
