package museon_online.astor_butler.api.ops;

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
}
