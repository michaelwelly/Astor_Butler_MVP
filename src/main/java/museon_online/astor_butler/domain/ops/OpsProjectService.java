package museon_online.astor_butler.domain.ops;

import lombok.RequiredArgsConstructor;
import museon_online.astor_butler.api.common.ApiException;
import museon_online.astor_butler.api.common.ErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OpsProjectService {

    private final OpsProjectRepository repository;

    @Transactional
    public OpsProject createProject(OpsProjectCommand command) {
        validateProject(command);
        return repository.createProject(defaultProjectValues(command));
    }

    public OpsProject getProject(Long id) {
        if (id == null) {
            throw badRequest("project id is required");
        }
        return repository.findProjectById(id)
                .orElseThrow(() -> notFound("Ops project was not found", Map.of("id", id)));
    }

    public OpsProject getProjectByCode(String code) {
        if (code == null || code.isBlank()) {
            throw badRequest("project code is required");
        }
        return repository.findProjectByCode(code)
                .orElseThrow(() -> notFound("Ops project was not found", Map.of("code", code)));
    }

    public List<OpsProject> listProjects(OpsProjectStatus status, OpsProjectVertical vertical, int limit) {
        return repository.listProjects(status, vertical, normalizeLimit(limit));
    }

    @Transactional
    public OpsProject updateProjectStatus(Long id, OpsProjectStatus status, OpsProjectStage stage, Integer progressPercent, String launchStatus) {
        OpsProject project = getProject(id);
        if (status == null) {
            throw badRequest("status is required");
        }
        if (project.status() == OpsProjectStatus.ARCHIVED) {
            throw new ApiException(HttpStatus.CONFLICT, ErrorCode.CONFLICT, "Archived ops project cannot be updated", Map.of("id", id));
        }
        return repository.updateProjectStatus(id, status, stage, progressPercent, launchStatus);
    }

    @Transactional
    public OpsTask createTask(OpsTaskCommand command) {
        validateTask(command);
        getProject(command.projectId());
        return repository.createTask(defaultTaskValues(command));
    }

    public OpsTask getTask(Long id) {
        if (id == null) {
            throw badRequest("task id is required");
        }
        return repository.findTaskById(id)
                .orElseThrow(() -> notFound("Ops task was not found", Map.of("id", id)));
    }

    public List<OpsTask> listOpenTasks(Long projectId, int limit) {
        getProject(projectId);
        return repository.listOpenTasksByProject(projectId, normalizeLimit(limit));
    }

    @Transactional
    public OpsTask updateTaskStatus(Long id, OpsTaskStatus status) {
        OpsTask task = getTask(id);
        if (status == null) {
            throw badRequest("status is required");
        }
        if (task.status() == OpsTaskStatus.DONE || task.status() == OpsTaskStatus.CANCELLED) {
            return task;
        }
        return repository.updateTaskStatus(id, status);
    }

    public OpsProjectDashboard dashboard(Long projectId, int taskLimit) {
        OpsProject project = getProject(projectId);
        int limit = normalizeLimit(taskLimit);
        return new OpsProjectDashboard(
                project,
                repository.listOpenTasksByProject(project.id(), limit),
                repository.listUpcomingCalls(project.id(), limit),
                repository.listArtifacts(project.id(), limit)
        );
    }

    @Transactional
    public OpsCall createCall(OpsCallCommand command) {
        validateCall(command);
        OpsProject project = getProject(command.projectId());
        OpsCall created = repository.createCall(defaultCallValues(command));
        if (project.nextCallAt() == null || created.startsAt().isBefore(project.nextCallAt())) {
            repository.updateProjectNextCallAt(project.id(), created.startsAt());
        }
        return created;
    }

    public List<OpsCall> listUpcomingCalls(Long projectId, int limit) {
        if (projectId != null) {
            getProject(projectId);
        }
        return repository.listUpcomingCalls(projectId, normalizeLimit(limit));
    }

    @Transactional
    public OpsArtifact createArtifact(OpsArtifactCommand command) {
        validateArtifact(command);
        getProject(command.projectId());
        return repository.createArtifact(defaultArtifactValues(command));
    }

    public List<OpsArtifact> listArtifacts(Long projectId, int limit) {
        getProject(projectId);
        return repository.listArtifacts(projectId, normalizeLimit(limit));
    }

    private OpsProjectCommand defaultProjectValues(OpsProjectCommand command) {
        return new OpsProjectCommand(
                command.code(),
                command.name(),
                command.vertical() == null ? OpsProjectVertical.OTHER : command.vertical(),
                command.stage() == null ? OpsProjectStage.INTAKE : command.stage(),
                command.status() == null ? OpsProjectStatus.ACTIVE : command.status(),
                command.owner(),
                command.teamChatId(),
                command.progressPercent() == null ? 0 : command.progressPercent(),
                command.deadlineAt(),
                command.nextCallAt(),
                command.launchStatus(),
                command.resultDefinition(),
                command.description(),
                command.metadataJson()
        );
    }

    private OpsTaskCommand defaultTaskValues(OpsTaskCommand command) {
        return new OpsTaskCommand(
                command.projectId(),
                command.title(),
                command.owner(),
                command.status() == null ? OpsTaskStatus.TODO : command.status(),
                command.priority() == null ? OpsTaskPriority.NORMAL : command.priority(),
                command.pipelineStage() == null ? OpsProjectStage.PLANNING : command.pipelineStage(),
                command.dueAt(),
                command.deliverableUrl(),
                command.notes(),
                command.metadataJson()
        );
    }

    private OpsCallCommand defaultCallValues(OpsCallCommand command) {
        return new OpsCallCommand(
                command.projectId(),
                command.title(),
                command.startsAt(),
                command.owner(),
                command.status() == null ? OpsCallStatus.SCHEDULED : command.status(),
                command.notes(),
                command.metadataJson()
        );
    }

    private OpsArtifactCommand defaultArtifactValues(OpsArtifactCommand command) {
        return new OpsArtifactCommand(
                command.projectId(),
                command.title(),
                command.type() == null ? OpsArtifactType.OTHER : command.type(),
                command.status() == null ? OpsArtifactStatus.DRAFT : command.status(),
                command.owner(),
                command.url(),
                command.notes(),
                command.metadataJson()
        );
    }

    private void validateProject(OpsProjectCommand command) {
        if (command == null) {
            throw badRequest("Request body is required");
        }
        if (command.code() == null || command.code().isBlank()) {
            throw badRequest("project code is required");
        }
        if (command.code().length() > 80) {
            throw badRequest("project code is too long");
        }
        if (command.name() == null || command.name().isBlank()) {
            throw badRequest("project name is required");
        }
        if (command.deadlineAt() != null && command.deadlineAt().isBefore(Instant.parse("2026-01-01T00:00:00Z"))) {
            throw badRequest("deadlineAt is outside supported range");
        }
    }

    private void validateTask(OpsTaskCommand command) {
        if (command == null) {
            throw badRequest("Request body is required");
        }
        if (command.projectId() == null) {
            throw badRequest("projectId is required");
        }
        if (command.title() == null || command.title().isBlank()) {
            throw badRequest("task title is required");
        }
        if (command.title().length() > 240) {
            throw badRequest("task title is too long");
        }
    }

    private void validateCall(OpsCallCommand command) {
        if (command == null) {
            throw badRequest("Request body is required");
        }
        if (command.projectId() == null) {
            throw badRequest("projectId is required");
        }
        if (command.title() == null || command.title().isBlank()) {
            throw badRequest("call title is required");
        }
        if (command.startsAt() == null) {
            throw badRequest("startsAt is required");
        }
    }

    private void validateArtifact(OpsArtifactCommand command) {
        if (command == null) {
            throw badRequest("Request body is required");
        }
        if (command.projectId() == null) {
            throw badRequest("projectId is required");
        }
        if (command.title() == null || command.title().isBlank()) {
            throw badRequest("artifact title is required");
        }
        if (command.url() == null || command.url().isBlank()) {
            throw badRequest("artifact url is required");
        }
    }

    private int normalizeLimit(int limit) {
        return Math.max(1, Math.min(limit, 100));
    }

    private ApiException badRequest(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, ErrorCode.BAD_REQUEST, message);
    }

    private ApiException notFound(String message, Map<String, Object> details) {
        return new ApiException(HttpStatus.NOT_FOUND, ErrorCode.NOT_FOUND, message, details);
    }
}
