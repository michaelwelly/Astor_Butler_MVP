package museon_online.astor_butler.api.integration;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import museon_online.astor_butler.api.common.ApiCommandResponse;
import museon_online.astor_butler.api.common.PageResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/integrations")
@Tag(name = "Integrations API", description = "Gmail, CRM, analytics and external integration boundary")
public class IntegrationController {

    @GetMapping
    @Operation(summary = "List integrations")
    public ResponseEntity<PageResponse<IntegrationResponse>> list() {
        return ResponseEntity.ok(PageResponse.empty(0, 20));
    }

    @PostMapping
    @Operation(summary = "Create integration config")
    public ResponseEntity<IntegrationResponse> create(@RequestBody IntegrationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(IntegrationResponse.from(UUID.randomUUID(), request, "DISCONNECTED"));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Replace integration config")
    public ResponseEntity<IntegrationResponse> replace(@PathVariable("id") UUID id, @RequestBody IntegrationRequest request) {
        return ResponseEntity.ok(IntegrationResponse.from(id, request, "DISCONNECTED"));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Patch integration config")
    public ResponseEntity<IntegrationResponse> patch(@PathVariable("id") UUID id, @RequestBody IntegrationRequest request) {
        return ResponseEntity.ok(IntegrationResponse.from(id, request, "DISCONNECTED"));
    }

    @PostMapping("/gmail/connect")
    @Operation(summary = "Start Gmail account connection")
    public ResponseEntity<ApiCommandResponse> connectGmail() {
        return ResponseEntity.accepted().body(ApiCommandResponse.accepted("GMAIL_CONNECT_ACCEPTED"));
    }

    @DeleteMapping("/gmail")
    @Operation(summary = "Disconnect Gmail account")
    public ResponseEntity<Void> disconnectGmail() {
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete integration config")
    public ResponseEntity<Void> delete(@PathVariable("id") UUID id) {
        return ResponseEntity.noContent().build();
    }

    public record IntegrationRequest(String type, String displayName, Map<String, Object> settings) {
    }

    public record IntegrationResponse(UUID id, String type, String displayName, String status, Instant updatedAt) {
        static IntegrationResponse from(UUID id, IntegrationRequest request, String status) {
            return new IntegrationResponse(id, request.type(), request.displayName(), status, Instant.now());
        }
    }
}
