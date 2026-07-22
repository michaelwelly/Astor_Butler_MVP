package museon_online.astor_butler.domain.ops;

import museon_online.astor_butler.api.common.ApiException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OpsProjectServiceTest {

    private final OpsProjectRepository repository = mock(OpsProjectRepository.class);
    private final OpsProjectService service = new OpsProjectService(repository);

    @Test
    void createsProjectWithDomainDefaults() {
        OpsProject created = project(OpsProjectStatus.ACTIVE);
        when(repository.createProject(org.mockito.ArgumentMatchers.any())).thenReturn(created);

        service.createProject(new OpsProjectCommand(
                "aeris launch",
                "AERIS launch",
                null,
                null,
                null,
                "Michael",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        ));

        var captor = forClass(OpsProjectCommand.class);
        verify(repository).createProject(captor.capture());
        assertThat(captor.getValue().vertical()).isEqualTo(OpsProjectVertical.OTHER);
        assertThat(captor.getValue().stage()).isEqualTo(OpsProjectStage.INTAKE);
        assertThat(captor.getValue().status()).isEqualTo(OpsProjectStatus.ACTIVE);
        assertThat(captor.getValue().progressPercent()).isZero();
    }

    @Test
    void rejectsArchivedProjectStatusUpdate() {
        when(repository.findProjectById(77L)).thenReturn(Optional.of(project(OpsProjectStatus.ARCHIVED)));

        assertThatThrownBy(() -> service.updateProjectStatus(77L, OpsProjectStatus.ACTIVE, null, null, null))
                .isInstanceOf(ApiException.class)
                .hasMessage("Archived ops project cannot be updated");
    }

    @Test
    void rejectsNullTaskBodyBeforeRepositoryCall() {
        assertThatThrownBy(() -> service.createTask(null))
                .isInstanceOf(ApiException.class)
                .hasMessage("Request body is required");
    }

    private OpsProject project(OpsProjectStatus status) {
        return new OpsProject(
                77L,
                "AERIS_LAUNCH",
                "AERIS launch",
                OpsProjectVertical.HORECA,
                OpsProjectStage.LAUNCH,
                status,
                "Michael",
                "-100500",
                90,
                Instant.parse("2026-08-01T12:00:00Z"),
                Instant.parse("2026-07-24T10:00:00Z"),
                "ready",
                "launched",
                "ops project",
                Map.of().toString(),
                Instant.parse("2026-07-23T10:00:00Z"),
                Instant.parse("2026-07-23T10:00:00Z")
        );
    }
}
