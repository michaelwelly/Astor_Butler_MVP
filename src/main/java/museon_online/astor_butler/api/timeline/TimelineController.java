package museon_online.astor_butler.api.timeline;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@RequestMapping("/api/timelines")
@Tag(name = "Timeline API", description = "User, booking, manager and system timelines")
public class TimelineController {

    @GetMapping("/users/{userId}")
    @Operation(summary = "Get user timeline")
    public ResponseEntity<PageResponse<TimelineEventResponse>> userTimeline(@PathVariable UUID userId) {
        return ResponseEntity.ok(PageResponse.empty(0, 20));
    }

    @GetMapping("/bookings/{bookingId}")
    @Operation(summary = "Get booking timeline")
    public ResponseEntity<PageResponse<TimelineEventResponse>> bookingTimeline(@PathVariable UUID bookingId) {
        return ResponseEntity.ok(PageResponse.empty(0, 20));
    }

    @PostMapping("/events")
    @Operation(summary = "Create timeline event")
    public ResponseEntity<TimelineEventResponse> create(@RequestBody TimelineEventRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(TimelineEventResponse.created(UUID.randomUUID(), request));
    }

    @PutMapping("/events/{id}")
    @Operation(summary = "Replace timeline event read-model metadata")
    public ResponseEntity<TimelineEventResponse> replace(@PathVariable UUID id, @RequestBody TimelineEventRequest request) {
        return ResponseEntity.ok(TimelineEventResponse.created(id, request));
    }

    @PatchMapping("/events/{id}")
    @Operation(summary = "Patch timeline event metadata")
    public ResponseEntity<TimelineEventResponse> patch(@PathVariable UUID id, @RequestBody Map<String, Object> patch) {
        return ResponseEntity.ok(new TimelineEventResponse(id, null, "PATCHED", patch, Instant.now()));
    }

    @DeleteMapping("/events/{id}")
    @Operation(summary = "Soft-delete timeline event from read model")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        return ResponseEntity.noContent().build();
    }

    public record TimelineEventRequest(UUID ownerId, String type, Map<String, Object> payload) {
    }

    public record TimelineEventResponse(UUID id, UUID ownerId, String type, Map<String, Object> payload, Instant createdAt) {
        static TimelineEventResponse created(UUID id, TimelineEventRequest request) {
            return new TimelineEventResponse(id, request.ownerId(), request.type(), request.payload(), Instant.now());
        }
    }
}
