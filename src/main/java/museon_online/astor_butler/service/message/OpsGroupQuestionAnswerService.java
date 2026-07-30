package museon_online.astor_butler.service.message;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import museon_online.astor_butler.domain.ops.OpsGroupQuestion;
import museon_online.astor_butler.domain.ops.OpsGroupQuestionRepository;
import museon_online.astor_butler.domain.ops.OpsProject;
import museon_online.astor_butler.domain.ops.OpsProjectDashboard;
import museon_online.astor_butler.domain.ops.OpsProjectMemoryService;
import museon_online.astor_butler.domain.ops.OpsProjectService;
import museon_online.astor_butler.domain.semantic.SemanticSearchResult;
import museon_online.astor_butler.fsm.core.BotState;
import museon_online.astor_butler.model.ModelGateway;
import museon_online.astor_butler.model.ModelTextRequest;
import museon_online.astor_butler.model.ModelTextResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OpsGroupQuestionAnswerService {

    private static final List<String> QUESTION_PREFIXES = List.of(
            "как ", "что ", "кто ", "когда ", "где ", "куда ", "почему ", "зачем ",
            "какой ", "какая ", "какие ", "какое ", "можно ", "нужно ", "сколько "
    );
    private static final List<String> OPS_KEYWORDS = List.of(
            "статус", "срок", "дедлайн", "ответствен", "презентац", "оплат", "проект",
            "задач", "колл", "созвон", "пайплайн", "запуск", "видео", "мед", "типограф",
            "ресторан", "izi", "site", "smart", "solution"
    );

    private final OpsGroupQuestionRepository questionRepository;
    private final OpsProjectMemoryService memoryService;
    private final OpsProjectService opsProjectService;
    private final ModelGateway modelGateway;

    @Value("${telegram.ops.group-qa.enabled:false}")
    private boolean enabled;

    @Value("${telegram.ops.group-qa.llm-enabled:true}")
    private boolean llmEnabled;

    @Value("${telegram.ops.owner-mention:@michaelwelly}")
    private String ownerMention;

    @Value("${telegram.ops.owner-username:michaelwelly}")
    private String ownerUsername;

    @Value("${telegram.ops.owner-user-id:}")
    private String ownerUserId;

    public Optional<OutgoingMessage> handle(IncomingMessage incoming, BotState currentState, String text) {
        if (!enabled || incoming == null || incoming.chatId() == null || incoming.chatId() > 0) {
            return Optional.empty();
        }
        if (Boolean.TRUE.equals(incoming.bot())) {
            return Optional.empty();
        }

        String normalized = normalize(text);
        if (normalized.isBlank() || normalized.startsWith("/")) {
            return Optional.empty();
        }

        Optional<OutgoingMessage> learned = learnFromHumanReply(incoming, currentState, normalized);
        if (learned.isPresent()) {
            return learned;
        }

        if (!looksLikeQuestion(normalized)) {
            return Optional.empty();
        }
        if (!llmEnabled) {
            return Optional.of(askHuman(incoming, currentState, normalized, "LLM disabled"));
        }

        return Optional.of(answerQuestion(incoming, currentState, normalized));
    }

    private Optional<OutgoingMessage> learnFromHumanReply(IncomingMessage incoming, BotState currentState, String answerText) {
        Integer replyToMessageId = intPayload(incoming, "replyToMessageId");
        if (replyToMessageId == null || !isOwner(incoming)) {
            return Optional.empty();
        }

        Optional<OpsGroupQuestion> pending = questionRepository.findPendingByMessage(incoming.chatId(), replyToMessageId);
        if (pending.isEmpty()) {
            return Optional.empty();
        }

        OpsGroupQuestion answered = questionRepository.markAnsweredByHuman(
                pending.get(),
                answerText,
                displayName(incoming)
        );
        memoryService.rememberHumanAnswer(answered, incoming);

        return Optional.of(OutgoingMessage.of(
                incoming,
                """
                <b>Smart Solution Ops / память обновлена</b>
                Запомнил ответ и добавил его в проектную память. В следующий раз попробую ответить сам.
                """.strip(),
                currentState.name(),
                true,
                false,
                false,
                false,
                AdminAlert.none(),
                List.of("GROUP_QA_HUMAN_REPLY_LEARNED", "PROJECT_MEMORY_UPDATED", "SKIP_GUEST_FSM")
        ).withMetadata(Map.of("replyToMessageId", replyToMessageId)));
    }

    private OutgoingMessage answerQuestion(IncomingMessage incoming, BotState currentState, String questionText) {
        List<SemanticSearchResult> memory = memoryService.search(questionText, 4);
        String context = buildContext(questionText, memory);
        String prompt = """
                Ты отвечаешь в Telegram-группе команды Smart_Soultion.com.
                Используй только контекст CRM и проектной памяти ниже.
                Если точного ответа в контексте нет, начни ответ ровно с "UNKNOWN:" и кратко скажи, чего не хватает.
                Если ответ есть, начни ответ ровно с "ANSWER:" и дай короткий полезный ответ на русском.
                Не выдумывай статусы, сроки, ссылки и ответственных.

                Контекст:
                %s

                Вопрос участника:
                %s
                """.formatted(context, questionText);

        try {
            ModelTextResponse response = modelGateway.generateText(ModelTextRequest.quality(
                    prompt,
                    "SmartSolutionGroupQa",
                    currentState.name(),
                    "group-question-answer"
            ));
            ParsedAnswer parsed = parseAnswer(response == null ? "" : response.text());
            if (parsed.answerable()) {
                OpsGroupQuestion saved = questionRepository.savePending(incoming, questionText);
                questionRepository.markAnsweredByBot(saved, parsed.text(), answerSource(memory));
                return OutgoingMessage.of(
                        incoming,
                        "<b>Smart Solution Ops</b>\n" + html(parsed.text()),
                        currentState.name(),
                        true,
                        false,
                        false,
                        false,
                        AdminAlert.none(),
                        List.of("GROUP_QA_RAG_ANSWER", "SKIP_GUEST_FSM")
                ).withMetadata(Map.of("replyToMessageId", incoming.telegramMessageId()));
            }
        } catch (RuntimeException ex) {
            log.warn("Smart Solution group QA fell back to human: chatId={} messageId={} reason={}",
                    incoming.chatId(), incoming.telegramMessageId(), ex.toString());
        }

        return askHuman(incoming, currentState, questionText, "LLM unknown/error");
    }

    private OutgoingMessage askHuman(IncomingMessage incoming, BotState currentState, String questionText, String reason) {
        questionRepository.savePending(incoming, questionText);
        String mention = ownerMention == null || ownerMention.isBlank() ? "@michaelwelly" : ownerMention.trim();
        return OutgoingMessage.of(
                incoming,
                """
                <b>Smart Solution Ops</b>
                Не нашел уверенный ответ в CRM/памяти.

                %s, посмотри, пожалуйста. Ответь reply на исходный вопрос — я запомню ответ в проектную память.
                """.formatted(html(mention)).strip(),
                currentState.name(),
                true,
                false,
                false,
                false,
                AdminAlert.none(),
                List.of("GROUP_QA_NEEDS_HUMAN", reasonAction(reason), "SKIP_GUEST_FSM")
        ).withMetadata(replyMetadata(incoming));
    }

    private String buildContext(String questionText, List<SemanticSearchResult> memory) {
        StringBuilder context = new StringBuilder();
        context.append("<OPS_CRM>\n");
        try {
            List<OpsProject> projects = opsProjectService.listProjects(null, null, 20);
            for (OpsProject project : projects) {
                context.append("- ")
                        .append(project.code()).append(": ")
                        .append(project.name())
                        .append(" | vertical=").append(project.vertical())
                        .append(" | stage=").append(project.stage())
                        .append(" | status=").append(project.status())
                        .append(" | progress=").append(project.progressPercent()).append("%")
                        .append(" | owner=").append(blank(project.owner()))
                        .append(" | deadline=").append(project.deadlineAt())
                        .append(" | nextCall=").append(project.nextCallAt())
                        .append("\n  launchStatus=").append(blank(project.launchStatus()))
                        .append("\n  doneMeans=").append(blank(project.resultDefinition()))
                        .append("\n");
                if (mentionsProject(questionText, project)) {
                    OpsProjectDashboard dashboard = opsProjectService.dashboard(project.id(), 5);
                    dashboard.openTasks().forEach(task -> context.append("  task: ")
                            .append(task.title())
                            .append(" | status=").append(task.status())
                            .append(" | owner=").append(blank(task.owner()))
                            .append(" | due=").append(task.dueAt())
                            .append("\n"));
                    dashboard.upcomingCalls().forEach(call -> context.append("  call: ")
                            .append(call.title())
                            .append(" | startsAt=").append(call.startsAt())
                            .append(" | owner=").append(blank(call.owner()))
                            .append("\n"));
                    dashboard.artifacts().forEach(artifact -> context.append("  artifact: ")
                            .append(artifact.title())
                            .append(" | type=").append(artifact.type())
                            .append(" | status=").append(artifact.status())
                            .append(" | url=").append(artifact.url())
                            .append("\n"));
                }
            }
        } catch (RuntimeException ex) {
            context.append("CRM unavailable: ").append(ex.getClass().getSimpleName()).append("\n");
        }
        context.append("</OPS_CRM>\n\n<PROJECT_MEMORY>\n");
        if (memory == null || memory.isEmpty()) {
            context.append("No project memory hits.\n");
        } else {
            for (SemanticSearchResult result : memory) {
                context.append("- ")
                        .append(result.title())
                        .append(" | score=").append(String.format(Locale.ROOT, "%.2f", result.score()))
                        .append("\n  ")
                        .append(result.shortContent(700))
                        .append("\n");
            }
        }
        context.append("</PROJECT_MEMORY>");
        return context.toString();
    }

    private boolean mentionsProject(String questionText, OpsProject project) {
        String text = normalize(questionText).toLowerCase(Locale.ROOT);
        return text.contains(project.code().toLowerCase(Locale.ROOT))
                || text.contains(project.name().toLowerCase(Locale.ROOT))
                || (project.owner() != null && text.contains(project.owner().toLowerCase(Locale.ROOT)));
    }

    private boolean looksLikeQuestion(String text) {
        String normalized = normalize(text).toLowerCase(Locale.ROOT);
        if (normalized.contains("?")) {
            return true;
        }
        return QUESTION_PREFIXES.stream().anyMatch(normalized::startsWith);
    }

    private boolean isOwner(IncomingMessage incoming) {
        if (ownerUserId != null && !ownerUserId.isBlank() && incoming.telegramUserId() != null) {
            return ownerUserId.trim().equals(incoming.telegramUserId().toString());
        }
        if (ownerUsername == null || ownerUsername.isBlank()) {
            return true;
        }
        return incoming.username() != null && ownerUsername.trim().equalsIgnoreCase(incoming.username().trim());
    }

    private ParsedAnswer parseAnswer(String rawText) {
        String text = normalize(rawText);
        if (text.isBlank()) {
            return ParsedAnswer.unknown();
        }
        if (text.regionMatches(true, 0, "ANSWER:", 0, "ANSWER:".length())) {
            return new ParsedAnswer(true, text.substring("ANSWER:".length()).trim());
        }
        if (text.regionMatches(true, 0, "UNKNOWN:", 0, "UNKNOWN:".length())) {
            return ParsedAnswer.unknown();
        }
        String lower = text.toLowerCase(Locale.ROOT);
        if (lower.contains("не знаю") || lower.contains("нет данных") || lower.contains("не найден")) {
            return ParsedAnswer.unknown();
        }
        return new ParsedAnswer(true, text);
    }

    private String answerSource(List<SemanticSearchResult> memory) {
        if (memory == null || memory.isEmpty()) {
            return "ops_crm";
        }
        return "ops_crm+" + memory.stream()
                .map(SemanticSearchResult::sourceCode)
                .distinct()
                .reduce((left, right) -> left + "," + right)
                .orElse("project_memory");
    }

    private Map<String, Object> replyMetadata(IncomingMessage incoming) {
        if (incoming == null || incoming.telegramMessageId() == null) {
            return Map.of();
        }
        return Map.of("replyToMessageId", incoming.telegramMessageId());
    }

    private String reasonAction(String reason) {
        return reason != null && reason.toLowerCase(Locale.ROOT).contains("disabled")
                ? "GROUP_QA_LLM_DISABLED"
                : "GROUP_QA_LLM_UNKNOWN";
    }

    private Integer intPayload(IncomingMessage incoming, String key) {
        if (incoming.payload() == null) {
            return null;
        }
        Object value = incoming.payload().get(key);
        if (value instanceof Integer integer) {
            return integer;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String string && !string.isBlank()) {
            try {
                return Integer.parseInt(string.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private String displayName(IncomingMessage incoming) {
        if (incoming.username() != null && !incoming.username().isBlank()) {
            return "@" + incoming.username().trim();
        }
        String firstName = incoming.firstName() == null ? "" : incoming.firstName().trim();
        String lastName = incoming.lastName() == null ? "" : incoming.lastName().trim();
        String fullName = (firstName + " " + lastName).trim();
        return fullName.isBlank() ? "unknown" : fullName;
    }

    private String blank(String value) {
        return value == null || value.isBlank() ? "not set" : value;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private String html(String value) {
        return normalize(value)
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    private record ParsedAnswer(boolean answerable, String text) {
        static ParsedAnswer unknown() {
            return new ParsedAnswer(false, "");
        }
    }
}
