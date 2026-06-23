package museon_online.astor_butler.api.auth;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Auth API", description = "OAuth2/OIDC, JWT and current principal boundary")
public class AuthController {

    @PostMapping("/login")
    @Operation(summary = "Start OAuth2/OIDC login flow through Keycloak")
    public ResponseEntity<LoginFlowResponse> login(@RequestBody LoginRequest request) {
        String provider = provider(request.provider());
        String authorizationUrl = "/oauth2/authorization/keycloak";
        if (!provider.isBlank()) {
            authorizationUrl = authorizationUrl + "?kc_idp_hint=" + provider;
        }
        return ResponseEntity.accepted().body(new LoginFlowResponse(
                "keycloak",
                authorizationUrl,
                request.redirectUri(),
                request.returnTo(),
                Instant.now()
        ));
    }

    @PostMapping("/logout")
    @Operation(summary = "Complete client logout and invalidate refresh flow when supported")
    public ResponseEntity<CommandResponse> logout() {
        return ResponseEntity.accepted().body(new CommandResponse("LOGOUT_ACCEPTED", Instant.now()));
    }

    @GetMapping("/me")
    @Operation(summary = "Read current user from JWT claims")
    public ResponseEntity<CurrentPrincipalResponse> me(Authentication authentication) {
        if (authentication == null
                || authentication instanceof AnonymousAuthenticationToken
                || !authentication.isAuthenticated()) {
            return ResponseEntity.ok(new CurrentPrincipalResponse(
                    "anonymous",
                    false,
                    List.of("GUEST"),
                    Map.of("source", "anonymous"),
                    Instant.now()
            ));
        }

        return ResponseEntity.ok(new CurrentPrincipalResponse(
                authentication.getName(),
                true,
                authentication.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .toList(),
                Map.of("source", authentication.getClass().getSimpleName()),
                Instant.now()
        ));
    }

    private String provider(String provider) {
        if (provider == null || provider.isBlank()) {
            return "";
        }
        String normalized = provider.trim().toLowerCase();
        return switch (normalized) {
            case "google", "yandex" -> normalized;
            default -> "";
        };
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record LoginRequest(String provider, String redirectUri, String returnTo) {
    }

    public record LoginFlowResponse(String provider, String authorizationUrl, String redirectUri, String returnTo, Instant issuedAt) {
    }

    public record CurrentPrincipalResponse(String subject, boolean authenticated, List<String> roles, Map<String, String> claims, Instant resolvedAt) {
    }

    public record CommandResponse(String status, Instant acceptedAt) {
    }
}
