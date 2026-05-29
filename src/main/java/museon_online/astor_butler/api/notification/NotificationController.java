package museon_online.astor_butler.api.notification;

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
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
@Tag(name = "Notifications API", description = "Notification read model and delivery commands")
public class NotificationController {

    @GetMapping
    @Operation(summary = "List notifications")
    public ResponseEntity<PageResponse<NotificationResponse>> list() {
        return ResponseEntity.ok(PageResponse.empty(0, 20));
    }

    @PostMapping
    @Operation(summary = "Create notification command")
    public ResponseEntity<NotificationResponse> create(@RequestBody NotificationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(NotificationResponse.from(UUID.randomUUID(), request, false));
    }

    @PostMapping("/test")
    @Operation(summary = "Send test notification")
    public ResponseEntity<ApiCommandResponse> test(@RequestBody NotificationRequest request) {
        return ResponseEntity.accepted().body(ApiCommandResponse.accepted("TEST_NOTIFICATION_ACCEPTED"));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Replace notification read model")
    public ResponseEntity<NotificationResponse> replace(@PathVariable UUID id, @RequestBody NotificationRequest request) {
        return ResponseEntity.ok(NotificationResponse.from(id, request, false));
    }

    @PatchMapping("/{id}/read")
    @Operation(summary = "Mark notification as read")
    public ResponseEntity<NotificationResponse> read(@PathVariable UUID id) {
        return ResponseEntity.ok(new NotificationResponse(id, null, "READ", true, Instant.now()));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete notification")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        return ResponseEntity.noContent().build();
    }

    public record NotificationRequest(UUID userId, String channel, String title, String body) {
    }

    public record NotificationResponse(UUID id, UUID userId, String title, boolean read, Instant updatedAt) {
        static NotificationResponse from(UUID id, NotificationRequest request, boolean read) {
            return new NotificationResponse(id, request.userId(), request.title(), read, Instant.now());
        }
    }
}
