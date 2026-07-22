package museon_online.astor_butler.service.message;

import museon_online.astor_butler.domain.ops.OpsProject;
import museon_online.astor_butler.domain.ops.OpsProjectDashboard;
import museon_online.astor_butler.domain.ops.OpsProjectService;
import museon_online.astor_butler.domain.ops.OpsProjectStage;
import museon_online.astor_butler.domain.ops.OpsProjectStatus;
import museon_online.astor_butler.domain.ops.OpsProjectVertical;
import museon_online.astor_butler.domain.ops.OpsStatusDigestFormatter;
import museon_online.astor_butler.domain.ops.OpsTask;
import museon_online.astor_butler.domain.ops.OpsTaskPriority;
import museon_online.astor_butler.domain.ops.OpsTaskStatus;
import museon_online.astor_butler.fsm.core.BotState;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OpsTelegramCommandServiceTest {

    private final OpsProjectService projectService = mock(OpsProjectService.class);
    private final OpsStatusDigestFormatter digestFormatter = mock(OpsStatusDigestFormatter.class);
    private final OpsTelegramCommandService commandService = new OpsTelegramCommandService(projectService, digestFormatter);

    @Test
    void listsActiveProjects() {
        when(projectService.listProjects(OpsProjectStatus.ACTIVE, null, 20))
                .thenReturn(List.of(project()));

        Optional<OutgoingMessage> result = commandService.handle(incoming("/projects"), BotState.READY_FOR_DIALOG, "/projects");

        assertThat(result).isPresent();
        assertThat(result.get().html()).isTrue();
        assertThat(result.get().text()).contains("Smart Solution Ops / active projects");
        assertThat(result.get().text()).contains("AERIS_LAUNCH");
        verify(projectService).listProjects(OpsProjectStatus.ACTIVE, null, 20);
    }

    @Test
    void returnsProjectCardByCode() {
        when(projectService.getProjectByCode("AERIS_LAUNCH")).thenReturn(project());

        Optional<OutgoingMessage> result = commandService.handle(incoming("/project AERIS_LAUNCH"), BotState.READY_FOR_DIALOG, "/project AERIS_LAUNCH");

        assertThat(result).isPresent();
        assertThat(result.get().text()).contains("Smart Solution Ops / project");
        assertThat(result.get().text()).contains("READY_TO_LAUNCH");
        verify(projectService).getProjectByCode("AERIS_LAUNCH");
    }

    @Test
    void returnsOpenTasksByProjectCode() {
        when(projectService.getProjectByCode("AERIS_LAUNCH")).thenReturn(project());
        when(projectService.listOpenTasks(77L, 20)).thenReturn(List.of(task()));

        Optional<OutgoingMessage> result = commandService.handle(incoming("/tasks AERIS_LAUNCH"), BotState.READY_FOR_DIALOG, "/tasks AERIS_LAUNCH");

        assertThat(result).isPresent();
        assertThat(result.get().text()).contains("Run Telegram smoke test");
        verify(projectService).getProjectByCode("AERIS_LAUNCH");
        verify(projectService).listOpenTasks(77L, 20);
    }

    @Test
    void returnsDigestByProjectCode() {
        OpsProjectDashboard dashboard = new OpsProjectDashboard(project(), List.of(task()));
        when(projectService.getProjectByCode("AERIS_LAUNCH")).thenReturn(project());
        when(projectService.dashboard(77L, 10)).thenReturn(dashboard);
        when(digestFormatter.format(dashboard)).thenReturn("<b>Smart Solution / project status</b>");

        Optional<OutgoingMessage> result = commandService.handle(incoming("/summary AERIS_LAUNCH"), BotState.READY_FOR_DIALOG, "/summary AERIS_LAUNCH");

        assertThat(result).isPresent();
        assertThat(result.get().text()).contains("Smart Solution / project status");
        verify(projectService).getProjectByCode("AERIS_LAUNCH");
        verify(projectService).dashboard(77L, 10);
        verify(digestFormatter).format(dashboard);
    }

    @Test
    void updatesProjectStatusFromTelegramCommand() {
        when(projectService.getProjectByCode("AERIS_LAUNCH")).thenReturn(project());
        when(projectService.updateProjectStatus(77L, OpsProjectStatus.READY_TO_LAUNCH, null, 95, "waiting final smoke"))
                .thenReturn(project());

        Optional<OutgoingMessage> result = commandService.handle(
                incoming("/status AERIS_LAUNCH READY_TO_LAUNCH 95% waiting final smoke"),
                BotState.READY_FOR_DIALOG,
                "/status AERIS_LAUNCH READY_TO_LAUNCH 95% waiting final smoke"
        );

        assertThat(result).isPresent();
        assertThat(result.get().text()).contains("status updated");
        verify(projectService).updateProjectStatus(77L, OpsProjectStatus.READY_TO_LAUNCH, null, 95, "waiting final smoke");
    }

    @Test
    void createsTaskFromTelegramCommand() {
        when(projectService.getProjectByCode("AERIS_LAUNCH")).thenReturn(project());
        when(projectService.createTask(argThat(command ->
                command.projectId().equals(77L)
                        && command.title().equals("Prepare launch presentation")
                        && command.owner().equals("anna")
                        && command.dueAt() != null
        ))).thenReturn(task());

        Optional<OutgoingMessage> result = commandService.handle(
                incoming("/task AERIS_LAUNCH \"Prepare launch presentation\" @anna 25.07"),
                BotState.READY_FOR_DIALOG,
                "/task AERIS_LAUNCH \"Prepare launch presentation\" @anna 25.07"
        );

        assertThat(result).isPresent();
        assertThat(result.get().text()).contains("task created");
        assertThat(result.get().text()).contains("Run Telegram smoke test");
        verify(projectService).createTask(argThat(command ->
                command.projectId().equals(77L)
                        && command.title().equals("Prepare launch presentation")
                        && command.owner().equals("anna")
                        && command.dueAt() != null
        ));
    }

    @Test
    void ignoresNonOpsText() {
        Optional<OutgoingMessage> result = commandService.handle(incoming("привет"), BotState.READY_FOR_DIALOG, "привет");

        assertThat(result).isEmpty();
    }

    private IncomingMessage incoming(String text) {
        return IncomingMessage.telegram(
                -100900L,
                1773317437L,
                1,
                100,
                text,
                null,
                "Michael",
                null,
                "michaelwelly",
                "ru",
                false,
                "100"
        );
    }

    private OpsProject project() {
        return new OpsProject(
                77L,
                "AERIS_LAUNCH",
                "AERIS restaurant launch",
                OpsProjectVertical.HORECA,
                OpsProjectStage.LAUNCH,
                OpsProjectStatus.READY_TO_LAUNCH,
                "Michael",
                "-100900",
                90,
                Instant.parse("2026-08-01T12:00:00Z"),
                Instant.parse("2026-07-24T10:00:00Z"),
                "hostess smoke test remains",
                "bot accepts bookings and team sees cards",
                "restaurant launch pipeline",
                "{}",
                Instant.parse("2026-07-23T10:00:00Z"),
                Instant.parse("2026-07-23T10:00:00Z")
        );
    }

    private OpsTask task() {
        return new OpsTask(
                88L,
                77L,
                "Run Telegram smoke test",
                "Anna",
                OpsTaskStatus.IN_PROGRESS,
                OpsTaskPriority.URGENT,
                OpsProjectStage.LAUNCH,
                Instant.parse("2026-07-25T12:00:00Z"),
                null,
                "manual test with team chat",
                "{}",
                Instant.parse("2026-07-23T10:00:00Z"),
                Instant.parse("2026-07-23T10:00:00Z")
        );
    }
}
