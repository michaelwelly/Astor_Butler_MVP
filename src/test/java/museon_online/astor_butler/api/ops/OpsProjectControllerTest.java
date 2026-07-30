package museon_online.astor_butler.api.ops;

import museon_online.astor_butler.domain.ops.OpsArtifact;
import museon_online.astor_butler.domain.ops.OpsArtifactStatus;
import museon_online.astor_butler.domain.ops.OpsArtifactType;
import museon_online.astor_butler.domain.ops.OpsCall;
import museon_online.astor_butler.domain.ops.OpsCallStatus;
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
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OpsProjectControllerTest {

    private final OpsProjectService opsProjectService = mock(OpsProjectService.class);
    private final OpsStatusDigestFormatter digestFormatter = mock(OpsStatusDigestFormatter.class);
    private final OpsProjectController controller = new OpsProjectController(opsProjectService, digestFormatter);

    @Test
    void createsOpsProject() {
        when(opsProjectService.createProject(any())).thenReturn(project(OpsProjectStatus.ACTIVE));

        ResponseEntity<OpsProjectController.OpsProjectResponse> response = controller.createProject(
                new OpsProjectController.OpsProjectCreateRequest(
                        "smart_site",
                        "Smart Solution site",
                        OpsProjectVertical.WEBSITE,
                        OpsProjectStage.PRODUCTION,
                        OpsProjectStatus.ACTIVE,
                        "Michael",
                        "-100500",
                        45,
                        Instant.parse("2026-08-01T12:00:00Z"),
                        Instant.parse("2026-07-24T10:00:00Z"),
                        "landing in progress",
                        "published website",
                        "Public website for Smart Solution",
                        "{}"
                )
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("SMART_SITE");
        assertThat(response.getBody().vertical()).isEqualTo(OpsProjectVertical.WEBSITE);
        verify(opsProjectService).createProject(any());
    }

    @Test
    void delegatesNullProjectBodyToServiceValidation() {
        when(opsProjectService.createProject(null)).thenThrow(new IllegalArgumentException("Request body is required"));

        try {
            controller.createProject(null);
        } catch (IllegalArgumentException ignored) {
            // Expected from mocked service; controller must not throw NullPointerException first.
        }

        verify(opsProjectService).createProject(isNull());
    }

    @Test
    void updatesProjectStatus() {
        when(opsProjectService.updateProjectStatus(77L, OpsProjectStatus.READY_TO_LAUNCH, OpsProjectStage.LAUNCH, 90, "waiting DNS"))
                .thenReturn(project(OpsProjectStatus.READY_TO_LAUNCH));

        ResponseEntity<OpsProjectController.OpsProjectResponse> response = controller.updateProjectStatus(
                77L,
                new OpsProjectController.OpsProjectStatusRequest(
                        OpsProjectStatus.READY_TO_LAUNCH,
                        OpsProjectStage.LAUNCH,
                        90,
                        "waiting DNS"
                )
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(OpsProjectStatus.READY_TO_LAUNCH);
        verify(opsProjectService).updateProjectStatus(77L, OpsProjectStatus.READY_TO_LAUNCH, OpsProjectStage.LAUNCH, 90, "waiting DNS");
    }

    @Test
    void delegatesNullStatusBodyToServiceValidation() {
        when(opsProjectService.updateProjectStatus(77L, null, null, null, null))
                .thenThrow(new IllegalArgumentException("status is required"));

        try {
            controller.updateProjectStatus(77L, null);
        } catch (IllegalArgumentException ignored) {
            // Expected from mocked service; controller must not throw NullPointerException first.
        }

        verify(opsProjectService).updateProjectStatus(77L, null, null, null, null);
    }

    @Test
    void createsTaskInsideProject() {
        OpsTask task = task(OpsTaskStatus.TODO);
        when(opsProjectService.createTask(any())).thenReturn(task);

        ResponseEntity<OpsProjectController.OpsTaskResponse> response = controller.createTask(
                77L,
                new OpsProjectController.OpsTaskCreateRequest(
                        "Prepare launch presentation",
                        "Anna",
                        OpsTaskStatus.TODO,
                        OpsTaskPriority.HIGH,
                        OpsProjectStage.REVIEW,
                        Instant.parse("2026-07-25T12:00:00Z"),
                        "https://example.com/deck",
                        "first version",
                        "{}"
                )
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().projectId()).isEqualTo(77L);
        assertThat(response.getBody().priority()).isEqualTo(OpsTaskPriority.HIGH);
        verify(opsProjectService).createTask(any());
    }

    @Test
    void returnsTelegramReadyDigest() {
        OpsProjectDashboard dashboard = new OpsProjectDashboard(project(OpsProjectStatus.ACTIVE), List.of(task(OpsTaskStatus.IN_PROGRESS)));
        when(opsProjectService.dashboard(77L, 10)).thenReturn(dashboard);
        when(digestFormatter.format(dashboard)).thenReturn("<b>Smart Solution / project status</b>");

        ResponseEntity<OpsProjectController.OpsProjectDigestResponse> response = controller.digest(77L, 10);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().text()).contains("Smart Solution");
        verify(opsProjectService).dashboard(77L, 10);
        verify(digestFormatter).format(dashboard);
    }

    @Test
    void schedulesProjectCall() {
        when(opsProjectService.createCall(any())).thenReturn(call());

        ResponseEntity<OpsProjectController.OpsCallResponse> response = controller.createCall(
                77L,
                new OpsProjectController.OpsCallCreateRequest(
                        "Launch sync",
                        Instant.parse("2026-07-24T10:00:00Z"),
                        "Anna",
                        OpsCallStatus.SCHEDULED,
                        "weekly launch sync",
                        "{}"
                )
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().title()).isEqualTo("Launch sync");
        verify(opsProjectService).createCall(any());
    }

    @Test
    void listsProjectCalls() {
        when(opsProjectService.listUpcomingCalls(77L, 20)).thenReturn(List.of(call()));

        ResponseEntity<List<OpsProjectController.OpsCallResponse>> response = controller.listProjectCalls(77L, 20);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().getFirst().status()).isEqualTo(OpsCallStatus.SCHEDULED);
        verify(opsProjectService).listUpcomingCalls(77L, 20);
    }

    @Test
    void createsProjectArtifact() {
        when(opsProjectService.createArtifact(any())).thenReturn(artifact());

        ResponseEntity<OpsProjectController.OpsArtifactResponse> response = controller.createArtifact(
                77L,
                new OpsProjectController.OpsArtifactCreateRequest(
                        "Launch deck",
                        OpsArtifactType.PRESENTATION,
                        OpsArtifactStatus.DRAFT,
                        "Anna",
                        "https://example.com/deck",
                        "presentation for team",
                        "{}"
                )
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().type()).isEqualTo(OpsArtifactType.PRESENTATION);
        verify(opsProjectService).createArtifact(any());
    }

    @Test
    void listsProjectArtifacts() {
        when(opsProjectService.listArtifacts(77L, 20)).thenReturn(List.of(artifact()));

        ResponseEntity<List<OpsProjectController.OpsArtifactResponse>> response = controller.listArtifacts(77L, 20);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().getFirst().url()).isEqualTo("https://example.com/deck");
        verify(opsProjectService).listArtifacts(77L, 20);
    }

    private OpsProject project(OpsProjectStatus status) {
        return new OpsProject(
                77L,
                "SMART_SITE",
                "Smart Solution site",
                OpsProjectVertical.WEBSITE,
                OpsProjectStage.PRODUCTION,
                status,
                "Michael",
                "-100500",
                45,
                Instant.parse("2026-08-01T12:00:00Z"),
                Instant.parse("2026-07-24T10:00:00Z"),
                "landing in progress",
                "published website",
                "Public website for Smart Solution",
                "{}",
                Instant.parse("2026-07-23T10:00:00Z"),
                Instant.parse("2026-07-23T10:00:00Z")
        );
    }

    private OpsTask task(OpsTaskStatus status) {
        return new OpsTask(
                88L,
                77L,
                "Prepare launch presentation",
                "Anna",
                status,
                OpsTaskPriority.HIGH,
                OpsProjectStage.REVIEW,
                Instant.parse("2026-07-25T12:00:00Z"),
                "https://example.com/deck",
                "first version",
                "{}",
                Instant.parse("2026-07-23T10:00:00Z"),
                Instant.parse("2026-07-23T10:00:00Z")
        );
    }

    private OpsCall call() {
        return new OpsCall(
                99L,
                77L,
                "Launch sync",
                Instant.parse("2026-07-24T10:00:00Z"),
                "Anna",
                OpsCallStatus.SCHEDULED,
                "weekly launch sync",
                "{}",
                Instant.parse("2026-07-23T10:00:00Z"),
                Instant.parse("2026-07-23T10:00:00Z")
        );
    }

    private OpsArtifact artifact() {
        return new OpsArtifact(
                101L,
                77L,
                "Launch deck",
                OpsArtifactType.PRESENTATION,
                OpsArtifactStatus.DRAFT,
                "Anna",
                "https://example.com/deck",
                "presentation for team",
                "{}",
                Instant.parse("2026-07-23T10:00:00Z"),
                Instant.parse("2026-07-23T10:00:00Z")
        );
    }
}
