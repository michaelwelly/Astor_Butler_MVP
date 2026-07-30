package museon_online.astor_butler.api.ops;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import museon_online.astor_butler.domain.ops.OpsArtifact;
import museon_online.astor_butler.domain.ops.OpsArtifactCommand;
import museon_online.astor_butler.domain.ops.OpsArtifactStatus;
import museon_online.astor_butler.domain.ops.OpsArtifactType;
import museon_online.astor_butler.domain.ops.OpsCall;
import museon_online.astor_butler.domain.ops.OpsCallCommand;
import museon_online.astor_butler.domain.ops.OpsCallStatus;
import museon_online.astor_butler.domain.ops.OpsProject;
import museon_online.astor_butler.domain.ops.OpsProjectCommand;
import museon_online.astor_butler.domain.ops.OpsProjectDashboard;
import museon_online.astor_butler.domain.ops.OpsProjectService;
import museon_online.astor_butler.domain.ops.OpsProjectStage;
import museon_online.astor_butler.domain.ops.OpsProjectStatus;
import museon_online.astor_butler.domain.ops.OpsProjectVertical;
import museon_online.astor_butler.domain.ops.OpsStatusDigestFormatter;
import museon_online.astor_butler.domain.ops.OpsTask;
import museon_online.astor_butler.domain.ops.OpsTaskCommand;
import museon_online.astor_butler.domain.ops.OpsTaskPriority;
import museon_online.astor_butler.domain.ops.OpsTaskStatus;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/ops")
@Tag(name = "Smart Solution Ops API", description = "Internal project CRM for team status, deadlines, calls and launch pipeline")
@RequiredArgsConstructor
public class OpsProjectController {

    private final OpsProjectService opsProjectService;
    private final OpsStatusDigestFormatter digestFormatter;

    @PostMapping("/projects")
    @Operation(summary = "Create ops project")
    public ResponseEntity<OpsProjectResponse> createProject(@RequestBody OpsProjectCreateRequest request) {
        OpsProject created = opsProjectService.createProject(request == null ? null : request.toCommand());
        return ResponseEntity.status(HttpStatus.CREATED).body(OpsProjectResponse.from(created));
    }

    @GetMapping("/projects")
    @Operation(summary = "List ops projects")
    public ResponseEntity<List<OpsProjectResponse>> listProjects(
            @RequestParam(name = "status", required = false) OpsProjectStatus status,
            @RequestParam(name = "vertical", required = false) OpsProjectVertical vertical,
            @RequestParam(name = "limit", defaultValue = "30") Integer limit
    ) {
        return ResponseEntity.ok(opsProjectService.listProjects(status, vertical, limit).stream()
                .map(OpsProjectResponse::from)
                .toList());
    }

    @GetMapping("/projects/{id}")
    @Operation(summary = "Get ops project")
    public ResponseEntity<OpsProjectResponse> getProject(@PathVariable("id") Long id) {
        return ResponseEntity.ok(OpsProjectResponse.from(opsProjectService.getProject(id)));
    }

    @PostMapping("/projects/{id}/status")
    @Operation(summary = "Update ops project status")
    public ResponseEntity<OpsProjectResponse> updateProjectStatus(
            @PathVariable("id") Long id,
            @RequestBody OpsProjectStatusRequest request
    ) {
        OpsProject updated = opsProjectService.updateProjectStatus(
                id,
                request == null ? null : request.status(),
                request == null ? null : request.stage(),
                request == null ? null : request.progressPercent(),
                request == null ? null : request.launchStatus()
        );
        return ResponseEntity.ok(OpsProjectResponse.from(updated));
    }

    @PostMapping("/projects/{id}/tasks")
    @Operation(summary = "Create ops task")
    public ResponseEntity<OpsTaskResponse> createTask(
            @PathVariable("id") Long id,
            @RequestBody OpsTaskCreateRequest request
    ) {
        OpsTask created = opsProjectService.createTask(request == null ? null : request.toCommand(id));
        return ResponseEntity.status(HttpStatus.CREATED).body(OpsTaskResponse.from(created));
    }

    @GetMapping("/projects/{id}/tasks/open")
    @Operation(summary = "List open ops tasks for project")
    public ResponseEntity<List<OpsTaskResponse>> listOpenTasks(
            @PathVariable("id") Long id,
            @RequestParam(name = "limit", defaultValue = "30") Integer limit
    ) {
        return ResponseEntity.ok(opsProjectService.listOpenTasks(id, limit).stream()
                .map(OpsTaskResponse::from)
                .toList());
    }

    @GetMapping("/projects/{id}/dashboard")
    @Operation(summary = "Get project dashboard data")
    public ResponseEntity<OpsProjectDashboardResponse> dashboard(
            @PathVariable("id") Long id,
            @RequestParam(name = "taskLimit", defaultValue = "10") Integer taskLimit
    ) {
        return ResponseEntity.ok(OpsProjectDashboardResponse.from(opsProjectService.dashboard(id, taskLimit)));
    }

    @GetMapping("/projects/{id}/digest")
    @Operation(summary = "Get Telegram-ready project status digest")
    public ResponseEntity<OpsProjectDigestResponse> digest(
            @PathVariable("id") Long id,
            @RequestParam(name = "taskLimit", defaultValue = "10") Integer taskLimit
    ) {
        return ResponseEntity.ok(new OpsProjectDigestResponse(digestFormatter.format(opsProjectService.dashboard(id, taskLimit))));
    }

    @PostMapping("/tasks/{id}/status")
    @Operation(summary = "Update ops task status")
    public ResponseEntity<OpsTaskResponse> updateTaskStatus(
            @PathVariable("id") Long id,
            @RequestBody OpsTaskStatusRequest request
    ) {
        return ResponseEntity.ok(OpsTaskResponse.from(opsProjectService.updateTaskStatus(id, request == null ? null : request.status())));
    }

    @PostMapping("/projects/{id}/calls")
    @Operation(summary = "Schedule ops project call")
    public ResponseEntity<OpsCallResponse> createCall(
            @PathVariable("id") Long id,
            @RequestBody OpsCallCreateRequest request
    ) {
        OpsCall created = opsProjectService.createCall(request == null ? null : request.toCommand(id));
        return ResponseEntity.status(HttpStatus.CREATED).body(OpsCallResponse.from(created));
    }

    @GetMapping("/projects/{id}/calls/upcoming")
    @Operation(summary = "List upcoming calls for project")
    public ResponseEntity<List<OpsCallResponse>> listProjectCalls(
            @PathVariable("id") Long id,
            @RequestParam(name = "limit", defaultValue = "20") Integer limit
    ) {
        return ResponseEntity.ok(opsProjectService.listUpcomingCalls(id, limit).stream()
                .map(OpsCallResponse::from)
                .toList());
    }

    @GetMapping("/calls/upcoming")
    @Operation(summary = "List upcoming ops calls")
    public ResponseEntity<List<OpsCallResponse>> listAllUpcomingCalls(
            @RequestParam(name = "limit", defaultValue = "20") Integer limit
    ) {
        return ResponseEntity.ok(opsProjectService.listUpcomingCalls(null, limit).stream()
                .map(OpsCallResponse::from)
                .toList());
    }

    @PostMapping("/projects/{id}/artifacts")
    @Operation(summary = "Create ops project artifact")
    public ResponseEntity<OpsArtifactResponse> createArtifact(
            @PathVariable("id") Long id,
            @RequestBody OpsArtifactCreateRequest request
    ) {
        OpsArtifact created = opsProjectService.createArtifact(request == null ? null : request.toCommand(id));
        return ResponseEntity.status(HttpStatus.CREATED).body(OpsArtifactResponse.from(created));
    }

    @GetMapping("/projects/{id}/artifacts")
    @Operation(summary = "List ops project artifacts")
    public ResponseEntity<List<OpsArtifactResponse>> listArtifacts(
            @PathVariable("id") Long id,
            @RequestParam(name = "limit", defaultValue = "20") Integer limit
    ) {
        return ResponseEntity.ok(opsProjectService.listArtifacts(id, limit).stream()
                .map(OpsArtifactResponse::from)
                .toList());
    }

    public record OpsProjectCreateRequest(
            String code,
            String name,
            OpsProjectVertical vertical,
            OpsProjectStage stage,
            OpsProjectStatus status,
            String owner,
            String teamChatId,
            Integer progressPercent,
            Instant deadlineAt,
            Instant nextCallAt,
            String launchStatus,
            String resultDefinition,
            String description,
            String metadataJson
    ) {
        OpsProjectCommand toCommand() {
            return new OpsProjectCommand(
                    code,
                    name,
                    vertical,
                    stage,
                    status,
                    owner,
                    teamChatId,
                    progressPercent,
                    deadlineAt,
                    nextCallAt,
                    launchStatus,
                    resultDefinition,
                    description,
                    metadataJson
            );
        }
    }

    public record OpsProjectStatusRequest(
            OpsProjectStatus status,
            OpsProjectStage stage,
            Integer progressPercent,
            String launchStatus
    ) {
    }

    public record OpsTaskCreateRequest(
            String title,
            String owner,
            OpsTaskStatus status,
            OpsTaskPriority priority,
            OpsProjectStage pipelineStage,
            Instant dueAt,
            String deliverableUrl,
            String notes,
            String metadataJson
    ) {
        OpsTaskCommand toCommand(Long projectId) {
            return new OpsTaskCommand(
                    projectId,
                    title,
                    owner,
                    status,
                    priority,
                    pipelineStage,
                    dueAt,
                    deliverableUrl,
                    notes,
                    metadataJson
            );
        }
    }

    public record OpsTaskStatusRequest(OpsTaskStatus status) {
    }

    public record OpsCallCreateRequest(
            String title,
            Instant startsAt,
            String owner,
            OpsCallStatus status,
            String notes,
            String metadataJson
    ) {
        OpsCallCommand toCommand(Long projectId) {
            return new OpsCallCommand(
                    projectId,
                    title,
                    startsAt,
                    owner,
                    status,
                    notes,
                    metadataJson
            );
        }
    }

    public record OpsArtifactCreateRequest(
            String title,
            OpsArtifactType type,
            OpsArtifactStatus status,
            String owner,
            String url,
            String notes,
            String metadataJson
    ) {
        OpsArtifactCommand toCommand(Long projectId) {
            return new OpsArtifactCommand(
                    projectId,
                    title,
                    type,
                    status,
                    owner,
                    url,
                    notes,
                    metadataJson
            );
        }
    }

    public record OpsProjectResponse(
            Long id,
            String code,
            String name,
            OpsProjectVertical vertical,
            OpsProjectStage stage,
            OpsProjectStatus status,
            String owner,
            String teamChatId,
            Integer progressPercent,
            Instant deadlineAt,
            Instant nextCallAt,
            String launchStatus,
            String resultDefinition,
            String description,
            Instant createdAt,
            Instant updatedAt
    ) {
        static OpsProjectResponse from(OpsProject project) {
            return new OpsProjectResponse(
                    project.id(),
                    project.code(),
                    project.name(),
                    project.vertical(),
                    project.stage(),
                    project.status(),
                    project.owner(),
                    project.teamChatId(),
                    project.progressPercent(),
                    project.deadlineAt(),
                    project.nextCallAt(),
                    project.launchStatus(),
                    project.resultDefinition(),
                    project.description(),
                    project.createdAt(),
                    project.updatedAt()
            );
        }
    }

    public record OpsTaskResponse(
            Long id,
            Long projectId,
            String title,
            String owner,
            OpsTaskStatus status,
            OpsTaskPriority priority,
            OpsProjectStage pipelineStage,
            Instant dueAt,
            String deliverableUrl,
            String notes,
            Instant createdAt,
            Instant updatedAt
    ) {
        static OpsTaskResponse from(OpsTask task) {
            return new OpsTaskResponse(
                    task.id(),
                    task.projectId(),
                    task.title(),
                    task.owner(),
                    task.status(),
                    task.priority(),
                    task.pipelineStage(),
                    task.dueAt(),
                    task.deliverableUrl(),
                    task.notes(),
                    task.createdAt(),
                    task.updatedAt()
            );
        }
    }

    public record OpsProjectDashboardResponse(
            OpsProjectResponse project,
            List<OpsTaskResponse> openTasks,
            List<OpsCallResponse> upcomingCalls,
            List<OpsArtifactResponse> artifacts
    ) {
        static OpsProjectDashboardResponse from(OpsProjectDashboard dashboard) {
            return new OpsProjectDashboardResponse(
                    OpsProjectResponse.from(dashboard.project()),
                    dashboard.openTasks().stream().map(OpsTaskResponse::from).toList(),
                    dashboard.upcomingCalls().stream().map(OpsCallResponse::from).toList(),
                    dashboard.artifacts().stream().map(OpsArtifactResponse::from).toList()
            );
        }
    }

    public record OpsProjectDigestResponse(String text) {
    }

    public record OpsCallResponse(
            Long id,
            Long projectId,
            String title,
            Instant startsAt,
            String owner,
            OpsCallStatus status,
            String notes,
            Instant createdAt,
            Instant updatedAt
    ) {
        static OpsCallResponse from(OpsCall call) {
            return new OpsCallResponse(
                    call.id(),
                    call.projectId(),
                    call.title(),
                    call.startsAt(),
                    call.owner(),
                    call.status(),
                    call.notes(),
                    call.createdAt(),
                    call.updatedAt()
            );
        }
    }

    public record OpsArtifactResponse(
            Long id,
            Long projectId,
            String title,
            OpsArtifactType type,
            OpsArtifactStatus status,
            String owner,
            String url,
            String notes,
            Instant createdAt,
            Instant updatedAt
    ) {
        static OpsArtifactResponse from(OpsArtifact artifact) {
            return new OpsArtifactResponse(
                    artifact.id(),
                    artifact.projectId(),
                    artifact.title(),
                    artifact.type(),
                    artifact.status(),
                    artifact.owner(),
                    artifact.url(),
                    artifact.notes(),
                    artifact.createdAt(),
                    artifact.updatedAt()
            );
        }
    }
}
