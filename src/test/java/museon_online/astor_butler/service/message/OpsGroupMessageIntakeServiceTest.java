package museon_online.astor_butler.service.message;

import museon_online.astor_butler.domain.ops.OpsArtifactCommand;
import museon_online.astor_butler.domain.ops.OpsArtifactType;
import museon_online.astor_butler.domain.ops.OpsGroupMessageClassification;
import museon_online.astor_butler.domain.ops.OpsProject;
import museon_online.astor_butler.domain.ops.OpsProjectMemoryService;
import museon_online.astor_butler.domain.ops.OpsProjectService;
import museon_online.astor_butler.domain.ops.OpsProjectStage;
import museon_online.astor_butler.domain.ops.OpsProjectStatus;
import museon_online.astor_butler.domain.ops.OpsProjectVertical;
import museon_online.astor_butler.fsm.core.BotState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OpsGroupMessageIntakeServiceTest {

    private final OpsProjectService opsProjectService = mock(OpsProjectService.class);
    private final OpsProjectMemoryService memoryService = mock(OpsProjectMemoryService.class);

    private OpsGroupMessageIntakeService service;

    @BeforeEach
    void setUp() {
        service = new OpsGroupMessageIntakeService(opsProjectService, memoryService);
        ReflectionTestUtils.setField(service, "enabled", true);
        ReflectionTestUtils.setField(service, "ackEnabled", true);
        ReflectionTestUtils.setField(service, "opsChatId", "-1003975140329");
    }

    @Test
    void storesStatusMessageAndUpdatesProjectStatus() {
        IncomingMessage incoming = groupMessage("MED статус 70% презентация у @michael на ревью");
        when(opsProjectService.getProjectByCode("MED")).thenReturn(project());

        Optional<OutgoingMessage> result = service.handle(incoming, BotState.UNKNOWN, incoming.text());

        assertThat(result).isPresent();
        assertThat(result.get().text()).contains("OPS_STATUS_UPDATE", "MED");
        assertThat(result.get().actions()).contains("OPS_PROJECT_STATUS_UPDATED", "PROJECT_MEMORY_UPDATED");
        ArgumentCaptor<OpsGroupMessageClassification> classification = ArgumentCaptor.forClass(OpsGroupMessageClassification.class);
        verify(memoryService).rememberGroupMessage(org.mockito.Mockito.eq(incoming), classification.capture());
        assertThat(classification.getValue().scenario()).isEqualTo("OPS_STATUS_UPDATE");
        assertThat(classification.getValue().intent()).isEqualTo("STATUS_UPDATE");
        assertThat(classification.getValue().projectCode()).isEqualTo("MED");
        assertThat(classification.getValue().resultStatus()).isEqualTo("70%");
        verify(opsProjectService).updateProjectStatus(
                77L,
                OpsProjectStatus.ACTIVE,
                OpsProjectStage.REVIEW,
                70,
                incoming.text()
        );
    }

    @Test
    void storesArtifactMessageAndCreatesArtifact() {
        IncomingMessage incoming = groupMessage("MED презентация v2 https://example.com/deck @michael");
        when(opsProjectService.getProjectByCode("MED")).thenReturn(project());

        Optional<OutgoingMessage> result = service.handle(incoming, BotState.UNKNOWN, incoming.text());

        assertThat(result).isPresent();
        assertThat(result.get().actions()).contains("OPS_ARTIFACT_CREATED", "PROJECT_MEMORY_UPDATED");
        ArgumentCaptor<OpsArtifactCommand> command = ArgumentCaptor.forClass(OpsArtifactCommand.class);
        verify(opsProjectService).createArtifact(command.capture());
        assertThat(command.getValue().projectId()).isEqualTo(77L);
        assertThat(command.getValue().type()).isEqualTo(OpsArtifactType.PRESENTATION);
        assertThat(command.getValue().url()).isEqualTo("https://example.com/deck");
        assertThat(command.getValue().owner()).isEqualTo("@michael");
    }

    @Test
    void storesYandexAdsStatusMessageInAdsProject() {
        IncomingMessage incoming = groupMessage("ADS статус 20% Яндекс Бизнес и Директ подключаем, UTM и KPI в работе");
        when(opsProjectService.getProjectByCode("ADS")).thenReturn(adsProject());

        Optional<OutgoingMessage> result = service.handle(incoming, BotState.UNKNOWN, incoming.text());

        assertThat(result).isPresent();
        assertThat(result.get().text()).contains("OPS_STATUS_UPDATE", "ADS");
        ArgumentCaptor<OpsGroupMessageClassification> classification = ArgumentCaptor.forClass(OpsGroupMessageClassification.class);
        verify(memoryService).rememberGroupMessage(org.mockito.Mockito.eq(incoming), classification.capture());
        assertThat(classification.getValue().projectCode()).isEqualTo("ADS");
        assertThat(classification.getValue().resultStatus()).isEqualTo("20%");
        verify(opsProjectService).updateProjectStatus(
                88L,
                OpsProjectStatus.ACTIVE,
                null,
                20,
                incoming.text()
        );
    }

    @Test
    void leavesQuestionsForQuestionAnswerService() {
        IncomingMessage incoming = groupMessage("что по презентации MED?");

        Optional<OutgoingMessage> result = service.handle(incoming, BotState.UNKNOWN, incoming.text());

        assertThat(result).isEmpty();
        verify(memoryService, never()).rememberGroupMessage(any(), any());
        verify(opsProjectService, never()).getProjectByCode(any());
    }

    private IncomingMessage groupMessage(String text) {
        return IncomingMessage.telegram(
                -1003975140329L,
                1773317437L,
                44,
                144,
                text,
                null,
                "Michael",
                null,
                "michaelwelly",
                "ru",
                false,
                "144"
        );
    }

    private OpsProject project() {
        return new OpsProject(
                77L,
                "MED",
                "Медицина / презентация",
                OpsProjectVertical.MEDICINE,
                OpsProjectStage.REVIEW,
                OpsProjectStatus.ACTIVE,
                "@michael",
                null,
                55,
                Instant.parse("2026-07-29T12:00:00Z"),
                Instant.parse("2026-07-24T08:00:00Z"),
                "Презентация у Майкла.",
                "Готовая медицинская презентация.",
                "medical project",
                "{}",
                Instant.parse("2026-07-23T10:00:00Z"),
                Instant.parse("2026-07-23T10:00:00Z")
        );
    }

    private OpsProject adsProject() {
        return new OpsProject(
                88L,
                "ADS",
                "Яндекс Бизнес / Директ / Карты",
                OpsProjectVertical.MARKETING,
                OpsProjectStage.PLANNING,
                OpsProjectStatus.ACTIVE,
                "@michael",
                null,
                20,
                Instant.parse("2026-08-05T13:00:00Z"),
                Instant.parse("2026-07-25T11:00:00Z"),
                "Яндекс Бизнес и Директ подключаем.",
                "Карточка, кампании, KPI и отчеты.",
                "ads project",
                "{}",
                Instant.parse("2026-07-23T10:00:00Z"),
                Instant.parse("2026-07-23T10:00:00Z")
        );
    }
}
