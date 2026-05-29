package museon_online.astor_butler.api.auth;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
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
        return ResponseEntity.accepted().body(new LoginFlowResponse(
                "keycloak",
                "/oauth2/authorization/keycloak",
                request.redirectUri(),
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
    public ResponseEntity<CurrentPrincipalResponse> me() {
        return ResponseEntity.ok(new CurrentPrincipalResponse(
                "anonymous",
                List.of("GUEST"),
                Map.of("source", "stub"),
                Instant.now()
        ));
    }

    public record LoginRequest(String redirectUri) {
    }

    public record LoginFlowResponse(String provider, String authorizationUrl, String redirectUri, Instant issuedAt) {
    }

    public record CurrentPrincipalResponse(String subject, List<String> roles, Map<String, String> claims, Instant resolvedAt) {
    }

    public record CommandResponse(String status, Instant acceptedAt) {
    }
}
