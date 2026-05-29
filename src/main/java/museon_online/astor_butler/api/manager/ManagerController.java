package museon_online.astor_butler.api.manager;

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
@RequestMapping("/api/manager")
@Tag(name = "Manager API", description = "Manager dashboard, tasks and escalation workflow")
public class ManagerController {

    @GetMapping("/dashboard")
    @Operation(summary = "Get manager dashboard")
    public ResponseEntity<DashboardResponse> dashboard() {
        return ResponseEntity.ok(new DashboardResponse(0, 0, 0, Map.of(), Instant.now()));
    }

    @GetMapping("/tasks")
    @Operation(summary = "List manager tasks")
    public ResponseEntity<PageResponse<ManagerTaskResponse>> tasks() {
        return ResponseEntity.ok(PageResponse.empty(0, 20));
    }

    @PostMapping("/tasks")
    @Operation(summary = "Create manager task")
    public ResponseEntity<ManagerTaskResponse> createTask(@RequestBody ManagerTaskRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ManagerTaskResponse.from(UUID.randomUUID(), request, "OPEN"));
    }

    @PutMapping("/tasks/{id}")
    @Operation(summary = "Replace manager task")
    public ResponseEntity<ManagerTaskResponse> replaceTask(@PathVariable UUID id, @RequestBody ManagerTaskRequest request) {
        return ResponseEntity.ok(ManagerTaskResponse.from(id, request, "OPEN"));
    }

    @PatchMapping("/tasks/{id}")
    @Operation(summary = "Patch manager task")
    public ResponseEntity<ManagerTaskResponse> patchTask(@PathVariable UUID id, @RequestBody ManagerTaskRequest request) {
        return ResponseEntity.ok(ManagerTaskResponse.from(id, request, "OPEN"));
    }

    @PostMapping("/tasks/{id}/complete")
    @Operation(summary = "Complete manager task")
    public ResponseEntity<ApiCommandResponse> completeTask(@PathVariable UUID id) {
        return ResponseEntity.accepted().body(ApiCommandResponse.accepted("MANAGER_TASK_COMPLETE_ACCEPTED"));
    }

    @DeleteMapping("/tasks/{id}")
    @Operation(summary = "Delete manager task")
    public ResponseEntity<Void> deleteTask(@PathVariable UUID id) {
        return ResponseEntity.noContent().build();
    }

    public record DashboardResponse(long openBookings, long overdueTasks, long unreadNotifications, Map<String, Object> metrics, Instant updatedAt) {
    }

    public record ManagerTaskRequest(String title, String description, UUID assigneeId, UUID bookingId) {
    }

    public record ManagerTaskResponse(UUID id, String title, String description, UUID assigneeId, UUID bookingId, String status, Instant updatedAt) {
        static ManagerTaskResponse from(UUID id, ManagerTaskRequest request, String status) {
            return new ManagerTaskResponse(id, request.title(), request.description(), request.assigneeId(), request.bookingId(), status, Instant.now());
        }
    }
}
