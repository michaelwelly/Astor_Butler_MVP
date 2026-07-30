package museon_online.astor_butler.service.message;

import museon_online.astor_butler.domain.ops.OpsGroupQuestion;
import museon_online.astor_butler.domain.ops.OpsGroupQuestionRepository;
import museon_online.astor_butler.domain.ops.OpsGroupQuestionStatus;
import museon_online.astor_butler.domain.ops.OpsProject;
import museon_online.astor_butler.domain.ops.OpsProjectDashboard;
import museon_online.astor_butler.domain.ops.OpsProjectMemoryService;
import museon_online.astor_butler.domain.ops.OpsProjectService;
import museon_online.astor_butler.domain.ops.OpsProjectStage;
import museon_online.astor_butler.domain.ops.OpsProjectStatus;
import museon_online.astor_butler.domain.ops.OpsProjectVertical;
import museon_online.astor_butler.domain.ops.OpsTask;
import museon_online.astor_butler.domain.ops.OpsTaskPriority;
import museon_online.astor_butler.domain.ops.OpsTaskStatus;
import museon_online.astor_butler.domain.semantic.SemanticSearchResult;
import museon_online.astor_butler.fsm.core.BotState;
import museon_online.astor_butler.model.ModelCapability;
import museon_online.astor_butler.model.ModelGateway;
import museon_online.astor_butler.model.ModelTextResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OpsGroupQuestionAnswerServiceTest {

    private final OpsGroupQuestionRepository questionRepository = mock(OpsGroupQuestionRepository.class);
    private final OpsProjectMemoryService memoryService = mock(OpsProjectMemoryService.class);
    private final OpsProjectService opsProjectService = mock(OpsProjectService.class);
    private final ModelGateway modelGateway = mock(ModelGateway.class);

    private OpsGroupQuestionAnswerService service;

    @BeforeEach
    void setUp() {
        service = new OpsGroupQuestionAnswerService(
                questionRepository,
                memoryService,
                opsProjectService,
                modelGateway
        );
        ReflectionTestUtils.setField(service, "enabled", true);
        ReflectionTestUtils.setField(service, "llmEnabled", true);
        ReflectionTestUtils.setField(service, "ownerMention", "@michaelwelly");
        ReflectionTestUtils.setField(service, "ownerUsername", "michaelwelly");
        ReflectionTestUtils.setField(service, "ownerUserId", "");
    }

    @Test
    void answersGroupQuestionFromCrmAndMemory() {
        IncomingMessage incoming = groupMessage(11, "что по презентации MED?", "anna");
        OpsProject project = project();
        when(opsProjectService.listProjects(null, null, 20)).thenReturn(List.of(project));
        when(opsProjectService.dashboard(77L, 5)).thenReturn(new OpsProjectDashboard(project, List.of(task())));
        when(memoryService.search("что по презентации MED?", 4)).thenReturn(List.of(memoryHit()));
        when(modelGateway.generateText(any())).thenReturn(response("ANSWER: MED у Майкла, финальная версия нужна 24.07."));
        when(questionRepository.savePending(incoming, "что по презентации MED?")).thenReturn(question());
        when(questionRepository.markAnsweredByBot(question(), "MED у Майкла, финальная версия нужна 24.07.", "ops_crm+SMART_SOLUTION_GROUP_MEMORY"))
                .thenReturn(question());

        Optional<OutgoingMessage> result = service.handle(incoming, BotState.UNKNOWN, incoming.text());

        assertThat(result).isPresent();
        assertThat(result.get().text()).contains("MED у Майкла");
        assertThat(result.get().metadata()).containsEntry("replyToMessageId", 11);
        assertThat(result.get().actions()).containsExactly("GROUP_QA_RAG_ANSWER", "SKIP_GUEST_FSM");
        verify(questionRepository).markAnsweredByBot(question(), "MED у Майкла, финальная версия нужна 24.07.", "ops_crm+SMART_SOLUTION_GROUP_MEMORY");
    }

    @Test
    void asksOwnerWhenModelDoesNotKnow() {
        IncomingMessage incoming = groupMessage(12, "кто согласовал новый договор?", "anna");
        when(opsProjectService.listProjects(null, null, 20)).thenReturn(List.of(project()));
        when(memoryService.search("кто согласовал новый договор?", 4)).thenReturn(List.of());
        when(modelGateway.generateText(any())).thenReturn(response("UNKNOWN: нет данных в памяти"));
        when(questionRepository.savePending(incoming, "кто согласовал новый договор?")).thenReturn(question());

        Optional<OutgoingMessage> result = service.handle(incoming, BotState.UNKNOWN, incoming.text());

        assertThat(result).isPresent();
        assertThat(result.get().text()).contains("@michaelwelly", "reply на исходный вопрос");
        assertThat(result.get().metadata()).containsEntry("replyToMessageId", 12);
        assertThat(result.get().actions()).containsExactly("GROUP_QA_NEEDS_HUMAN", "GROUP_QA_LLM_UNKNOWN", "SKIP_GUEST_FSM");
    }

    @Test
    void learnsFromOwnerReplyToPendingQuestion() {
        IncomingMessage answer = IncomingMessage.telegram(
                -1003975140329L,
                421441838L,
                22,
                101,
                "Оплату ждем завтра до 15:00, после этого запускаем.",
                null,
                "Michael",
                null,
                "michaelwelly",
                "ru",
                false,
                "101",
                Map.of("replyToMessageId", 12)
        );
        OpsGroupQuestion pending = question();
        when(questionRepository.findPendingByMessage(-1003975140329L, 12)).thenReturn(Optional.of(pending));
        when(questionRepository.markAnsweredByHuman(pending, answer.text(), "@michaelwelly")).thenReturn(pending);

        Optional<OutgoingMessage> result = service.handle(answer, BotState.UNKNOWN, answer.text());

        assertThat(result).isPresent();
        assertThat(result.get().text()).contains("память обновлена");
        assertThat(result.get().metadata()).containsEntry("replyToMessageId", 12);
        assertThat(result.get().actions()).containsExactly("GROUP_QA_HUMAN_REPLY_LEARNED", "PROJECT_MEMORY_UPDATED", "SKIP_GUEST_FSM");
        verify(memoryService).rememberHumanAnswer(pending, answer);
    }

    private IncomingMessage groupMessage(int messageId, String text, String username) {
        return IncomingMessage.telegram(
                -1003975140329L,
                1000L + messageId,
                messageId,
                100 + messageId,
                text,
                null,
                "Team",
                null,
                username,
                "ru",
                false,
                Integer.toString(100 + messageId)
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
                "Презентация у Майкла, нужна финальная версия.",
                "Готовая презентация с AI-решением и сроками запуска.",
                "medical project",
                "{}",
                Instant.parse("2026-07-23T10:00:00Z"),
                Instant.parse("2026-07-23T10:00:00Z")
        );
    }

    private OpsTask task() {
        return new OpsTask(
                88L,
                77L,
                "Довести медицинскую презентацию до версии для показа",
                "@michael",
                OpsTaskStatus.IN_PROGRESS,
                OpsTaskPriority.URGENT,
                OpsProjectStage.REVIEW,
                Instant.parse("2026-07-24T15:00:00Z"),
                null,
                "Нужна финальная версия.",
                "{}",
                Instant.parse("2026-07-23T10:00:00Z"),
                Instant.parse("2026-07-23T10:00:00Z")
        );
    }

    private SemanticSearchResult memoryHit() {
        return new SemanticSearchResult(
                UUID.randomUUID(),
                "SMART_SOLUTION_GROUP_MEMORY",
                "GROUP_QA_MEMORY",
                "MED presentation status",
                "Вопрос: что по презентации MED? Ответ: презентация у Майкла.",
                0.72
        );
    }

    private OpsGroupQuestion question() {
        return new OpsGroupQuestion(
                1L,
                -1003975140329L,
                12,
                1012L,
                "anna",
                "Anna",
                "что по презентации MED?",
                OpsGroupQuestionStatus.PENDING_HUMAN,
                null,
                null,
                null,
                null,
                "{}",
                Instant.parse("2026-07-23T10:00:00Z"),
                Instant.parse("2026-07-23T10:00:00Z")
        );
    }

    private ModelTextResponse response(String text) {
        return new ModelTextResponse(
                text,
                "test",
                "test-model",
                ModelCapability.TEXT_GENERATION,
                Duration.ZERO,
                false,
                Map.of()
        );
    }
}
