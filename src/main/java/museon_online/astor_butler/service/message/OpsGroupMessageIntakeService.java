package museon_online.astor_butler.service.message;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import museon_online.astor_butler.domain.ops.OpsArtifactCommand;
import museon_online.astor_butler.domain.ops.OpsArtifactStatus;
import museon_online.astor_butler.domain.ops.OpsArtifactType;
import museon_online.astor_butler.domain.ops.OpsCallCommand;
import museon_online.astor_butler.domain.ops.OpsGroupMessageClassification;
import museon_online.astor_butler.domain.ops.OpsProject;
import museon_online.astor_butler.domain.ops.OpsProjectMemoryService;
import museon_online.astor_butler.domain.ops.OpsProjectService;
import museon_online.astor_butler.domain.ops.OpsProjectStage;
import museon_online.astor_butler.domain.ops.OpsProjectStatus;
import museon_online.astor_butler.domain.ops.OpsTaskCommand;
import museon_online.astor_butler.domain.ops.OpsTaskPriority;
import museon_online.astor_butler.domain.ops.OpsTaskStatus;
import museon_online.astor_butler.fsm.core.BotState;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class OpsGroupMessageIntakeService {

    private static final ZoneId OPS_ZONE = ZoneId.of("Asia/Yekaterinburg");
    private static final Pattern URL_PATTERN = Pattern.compile("https?://\\S+");
    private static final Pattern PROGRESS_PATTERN = Pattern.compile("(?<!\\d)(100|\\d{1,2})\\s*%");
    private static final Pattern DATE_PATTERN = Pattern.compile("(?<!\\d)(\\d{1,2})[./](\\d{1,2})(?:[./](\\d{2,4}))?(?!\\d)");
    private static final Pattern TIME_PATTERN = Pattern.compile("(?<!\\d)(\\d{1,2})[:.](\\d{2})(?!\\d)|\\bв\\s+(\\d{1,2})\\b");
    private static final Pattern MENTION_PATTERN = Pattern.compile("@[\\p{L}\\p{N}_]+");

    private static final Map<String, List<String>> PROJECT_ALIASES = Map.of(
            "VIDEO", List.of("video", "видео", "егор", "@egor", "продакш"),
            "MED", List.of("med", "мед", "медицин", "медицина", "презентац", "@michael"),
            "IZI", List.of("izi", "изи", "оплат", "платеж"),
            "RESTO", List.of("resto", "ресто", "ресторан", "horeca", "запуск ресторан"),
            "PRINT", List.of("print", "принт", "типограф", "макет", "тираж"),
            "SITE", List.of("site", "сайт", "smart_soultion", "smart solution", "crm", "дашборд"),
            "ADS", List.of(
                    "ads", "adtech", "реклам", "яндекс бизнес", "yandex business", "директ",
                    "direct", "карты", "навигатор", "приоритетное размещ", "рся", "кампани", "ставк"
            )
    );

    private final OpsProjectService opsProjectService;
    private final OpsProjectMemoryService memoryService;

    @Value("${telegram.ops.group-intake.enabled:false}")
    private boolean enabled;

    @Value("${telegram.ops.group-intake.ack-enabled:true}")
    private boolean ackEnabled;

    @Value("${telegram.ops.chat-id:}")
    private String opsChatId;

    public Optional<OutgoingMessage> handle(IncomingMessage incoming, BotState currentState, String text) {
        if (!enabled || incoming == null || incoming.chatId() == null || incoming.chatId() > 0) {
            return Optional.empty();
        }
        if (!isOpsChat(incoming.chatId()) || Boolean.TRUE.equals(incoming.bot())) {
            return Optional.empty();
        }

        String normalized = normalize(text);
        if (normalized.isBlank() || normalized.startsWith("/")) {
            return Optional.empty();
        }
        if (looksLikeQuestion(normalized)) {
            return Optional.empty();
        }

        ClassificationDraft draft = classify(normalized);
        OpsGroupMessageClassification classification = OpsGroupMessageClassification.of(
                draft.scenario,
                draft.intent,
                draft.projectCode,
                draft.resultStatus,
                draft.summary,
                draft.actions
        );
        memoryService.rememberGroupMessage(incoming, classification);

        List<String> crmActions = applyStructuredActions(normalized, draft);
        if (!ackEnabled || crmActions.isEmpty()) {
            return Optional.empty();
        }

        List<String> actions = new ArrayList<>();
        actions.add("OPS_GROUP_MESSAGE_INTAKE");
        actions.addAll(crmActions);
        actions.add("PROJECT_MEMORY_UPDATED");
        actions.add("SKIP_GUEST_FSM");

        return Optional.of(OutgoingMessage.of(
                incoming,
                ackText(classification, crmActions),
                currentState.name(),
                true,
                false,
                false,
                false,
                AdminAlert.none(),
                actions
        ).withMetadata(replyMetadata(incoming)));
    }

    private List<String> applyStructuredActions(String text, ClassificationDraft draft) {
        List<String> actions = new ArrayList<>();
        if (draft.projectCode == null || draft.projectCode.isBlank()) {
            return actions;
        }

        OpsProject project;
        try {
            project = opsProjectService.getProjectByCode(draft.projectCode);
        } catch (RuntimeException ex) {
            log.warn("Ops group intake skipped CRM mutation: projectCode={} reason={}", draft.projectCode, ex.toString());
            return actions;
        }

        if ("STATUS_UPDATE".equals(draft.intent) || "RESULT_UPDATE".equals(draft.intent)) {
            OpsProjectStatus status = statusFromText(text);
            Integer progress = progressFromText(text);
            OpsProjectStage stage = stageFromText(text);
            if (status != null || progress != null || stage != null) {
                opsProjectService.updateProjectStatus(
                        project.id(),
                        status == null ? project.status() : status,
                        stage,
                        progress,
                        text
                );
                actions.add("OPS_PROJECT_STATUS_UPDATED");
            }
        }

        if ("TASK_INTAKE".equals(draft.intent)) {
            opsProjectService.createTask(new OpsTaskCommand(
                    project.id(),
                    taskTitle(text),
                    firstMention(text),
                    OpsTaskStatus.TODO,
                    priorityFromText(text),
                    stageFromText(text) == null ? OpsProjectStage.PLANNING : stageFromText(text),
                    null,
                    firstUrl(text),
                    "Создано автоматически из группового сообщения.",
                    metadataJson("OPS_TASK_INTAKE", text)
            ));
            actions.add("OPS_TASK_CREATED");
        }

        if ("ARTIFACT_INTAKE".equals(draft.intent) && firstUrl(text) != null) {
            opsProjectService.createArtifact(new OpsArtifactCommand(
                    project.id(),
                    artifactTitle(text),
                    artifactTypeFromText(text),
                    artifactStatusFromText(text),
                    firstMention(text),
                    firstUrl(text),
                    "Добавлено автоматически из группового сообщения.",
                    metadataJson("OPS_ARTIFACT_INTAKE", text)
            ));
            actions.add("OPS_ARTIFACT_CREATED");
        }

        if ("CALL_INTAKE".equals(draft.intent)) {
            Instant startsAt = parseCallStartsAt(text);
            if (startsAt != null) {
                opsProjectService.createCall(new OpsCallCommand(
                        project.id(),
                        callTitle(text),
                        startsAt,
                        firstMention(text),
                        null,
                        "Добавлено автоматически из группового сообщения.",
                        metadataJson("OPS_CALL_INTAKE", text)
                ));
                actions.add("OPS_CALL_CREATED");
            }
        }
        return actions;
    }

    private ClassificationDraft classify(String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        String projectCode = detectProjectCode(lower);
        String intent = "RESULT_MEMORY";
        String scenario = "OPS_RESULT_MEMORY";

        if (containsAny(lower, "задача", "таск", "todo", "нужно", "надо сделать", "сделать")) {
            intent = "TASK_INTAKE";
            scenario = "OPS_TASK_INTAKE";
        }
        if (containsAny(lower, "колл", "созвон", "встреча", "call")) {
            intent = "CALL_INTAKE";
            scenario = "OPS_CALL_INTAKE";
        }
        if (firstUrl(text) != null || containsAny(lower, "ссылка", "презентац", "макет", "бриф", "договор", "отчет", "видео", "дизайн")) {
            intent = "ARTIFACT_INTAKE";
            scenario = "OPS_ARTIFACT_INTAKE";
        }
        if (containsAny(lower, "статус", "готов", "ожидает", "ждем", "заблок", "запущ", "результат", "оплат", "дедлайн")) {
            intent = containsAny(lower, "результат", "готово", "готовый") ? "RESULT_UPDATE" : "STATUS_UPDATE";
            scenario = "OPS_STATUS_UPDATE";
        }

        return new ClassificationDraft(
                scenario,
                intent,
                projectCode,
                resultStatusFromText(lower),
                summary(text),
                List.of("OPS_INTENT_" + intent, "OPS_FSM_" + scenario)
        );
    }

    private String detectProjectCode(String lower) {
        for (Map.Entry<String, List<String>> entry : PROJECT_ALIASES.entrySet()) {
            String code = entry.getKey();
            if (Pattern.compile("(?<![\\p{L}\\p{N}_])" + Pattern.quote(code.toLowerCase(Locale.ROOT)) + "(?![\\p{L}\\p{N}_])")
                    .matcher(lower)
                    .find()) {
                return code;
            }
            for (String alias : entry.getValue()) {
                if (lower.contains(alias)) {
                    return code;
                }
            }
        }
        return null;
    }

    private OpsProjectStatus statusFromText(String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        if (containsAny(lower, "заблок", "blocked", "стоп")) {
            return OpsProjectStatus.BLOCKED;
        }
        if (containsAny(lower, "ожидает оплат", "ждем оплат", "ждём оплат", "waiting client", "клиент")) {
            return OpsProjectStatus.WAITING_CLIENT;
        }
        if (containsAny(lower, "ждем команд", "ждём команд", "waiting team")) {
            return OpsProjectStatus.WAITING_TEAM;
        }
        if (containsAny(lower, "готов к запуск", "ready to launch", "можно запускать")) {
            return OpsProjectStatus.READY_TO_LAUNCH;
        }
        if (containsAny(lower, "запущ", "launched", "в проде")) {
            return OpsProjectStatus.LAUNCHED;
        }
        if (containsAny(lower, "драфт", "draft")) {
            return OpsProjectStatus.DRAFT;
        }
        if (containsAny(lower, "в работе", "делаем", "актив", "production")) {
            return OpsProjectStatus.ACTIVE;
        }
        if (containsAny(lower, "готово", "готовый результат", "финал")) {
            return OpsProjectStatus.READY_TO_LAUNCH;
        }
        return null;
    }

    private OpsProjectStage stageFromText(String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        if (containsAny(lower, "бриф", "brief")) {
            return OpsProjectStage.BRIEFING;
        }
        if (containsAny(lower, "план", "pipeline")) {
            return OpsProjectStage.PLANNING;
        }
        if (containsAny(lower, "продакш", "production", "съем", "съём", "делаем")) {
            return OpsProjectStage.PRODUCTION;
        }
        if (containsAny(lower, "ревью", "review", "согласован", "презентац")) {
            return OpsProjectStage.REVIEW;
        }
        if (containsAny(lower, "запуск", "launch", "релиз")) {
            return OpsProjectStage.LAUNCH;
        }
        if (containsAny(lower, "поддерж", "support")) {
            return OpsProjectStage.SUPPORT;
        }
        if (containsAny(lower, "готово", "done", "финал")) {
            return OpsProjectStage.DONE;
        }
        return null;
    }

    private String resultStatusFromText(String lower) {
        Integer progress = progressFromText(lower);
        if (progress != null) {
            return progress + "%";
        }
        OpsProjectStatus status = statusFromText(lower);
        if (status != null) {
            return status.name();
        }
        return containsAny(lower, "результат", "готов", "статус") ? "STATUS_MENTIONED" : "MEMORY_ONLY";
    }

    private Integer progressFromText(String text) {
        Matcher matcher = PROGRESS_PATTERN.matcher(text);
        if (!matcher.find()) {
            return null;
        }
        return Math.max(0, Math.min(100, Integer.parseInt(matcher.group(1))));
    }

    private OpsTaskPriority priorityFromText(String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        if (containsAny(lower, "срочно", "urgent", "горит", "сегодня")) {
            return OpsTaskPriority.URGENT;
        }
        if (containsAny(lower, "низк", "low", "потом")) {
            return OpsTaskPriority.LOW;
        }
        if (containsAny(lower, "важно", "high")) {
            return OpsTaskPriority.HIGH;
        }
        return OpsTaskPriority.NORMAL;
    }

    private OpsArtifactType artifactTypeFromText(String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        if (containsAny(lower, "презентац", "deck", "slides")) {
            return OpsArtifactType.PRESENTATION;
        }
        if (containsAny(lower, "договор", "контракт")) {
            return OpsArtifactType.CONTRACT;
        }
        if (containsAny(lower, "макет", "дизайн")) {
            return OpsArtifactType.DESIGN;
        }
        if (containsAny(lower, "видео", "ролик")) {
            return OpsArtifactType.VIDEO;
        }
        if (containsAny(lower, "отчет", "отчёт", "report")) {
            return OpsArtifactType.REPORT;
        }
        if (containsAny(lower, "бриф", "brief")) {
            return OpsArtifactType.BRIEF;
        }
        return OpsArtifactType.OTHER;
    }

    private OpsArtifactStatus artifactStatusFromText(String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        if (containsAny(lower, "согласован", "approved")) {
            return OpsArtifactStatus.APPROVED;
        }
        if (containsAny(lower, "отправ", "sent")) {
            return OpsArtifactStatus.SENT;
        }
        if (containsAny(lower, "ревью", "review")) {
            return OpsArtifactStatus.IN_REVIEW;
        }
        return OpsArtifactStatus.DRAFT;
    }

    private Instant parseCallStartsAt(String text) {
        LocalDate date = parseDate(text);
        LocalTime time = parseTime(text);
        if (date == null || time == null) {
            return null;
        }
        return LocalDateTime.of(date, time).atZone(OPS_ZONE).toInstant();
    }

    private LocalDate parseDate(String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        LocalDate today = LocalDate.now(OPS_ZONE);
        if (lower.contains("завтра")) {
            return today.plusDays(1);
        }
        if (lower.contains("сегодня")) {
            return today;
        }
        Matcher matcher = DATE_PATTERN.matcher(text);
        if (!matcher.find()) {
            return null;
        }
        int day = Integer.parseInt(matcher.group(1));
        int month = Integer.parseInt(matcher.group(2));
        int year = matcher.group(3) == null
                ? today.getYear()
                : Integer.parseInt(matcher.group(3).length() == 2 ? "20" + matcher.group(3) : matcher.group(3));
        try {
            return LocalDate.of(year, month, day);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private LocalTime parseTime(String text) {
        Matcher matcher = TIME_PATTERN.matcher(text);
        if (!matcher.find()) {
            return null;
        }
        String hour = matcher.group(1) == null ? matcher.group(3) : matcher.group(1);
        String minute = matcher.group(2) == null ? "00" : matcher.group(2);
        try {
            return LocalTime.of(Integer.parseInt(hour), Integer.parseInt(minute));
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private boolean looksLikeQuestion(String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        return lower.contains("?")
                || lower.startsWith("что ")
                || lower.startsWith("кто ")
                || lower.startsWith("когда ")
                || lower.startsWith("как ")
                || lower.startsWith("какой ")
                || lower.startsWith("какая ")
                || lower.startsWith("какие ");
    }

    private String ackText(OpsGroupMessageClassification classification, List<String> crmActions) {
        String project = classification.projectCode() == null ? "без проекта" : classification.projectCode();
        String crm = String.join(", ", crmActions);
        return """
                <b>Smart Solution Ops / intake</b>
                Забрал в память: <b>%s</b> / <code>%s</code>
                Проект: <code>%s</code>
                CRM: %s
                """.formatted(html(classification.scenario()), html(classification.intent()), html(project), html(crm)).strip();
    }

    private String taskTitle(String text) {
        return cleanupTitle(text, "задача");
    }

    private String artifactTitle(String text) {
        String withoutUrl = URL_PATTERN.matcher(text).replaceAll("").trim();
        return cleanupTitle(withoutUrl, "артефакт");
    }

    private String callTitle(String text) {
        return cleanupTitle(text, "созвон");
    }

    private String cleanupTitle(String text, String fallback) {
        String cleaned = text == null ? "" : text
                .replaceAll("(?i)\\b(VIDEO|MED|IZI|RESTO|PRINT|SITE|ADS)\\b", "")
                .replaceAll("https?://\\S+", "")
                .replaceAll("\\s+", " ")
                .trim();
        if (cleaned.length() > 180) {
            cleaned = cleaned.substring(0, 177) + "...";
        }
        return cleaned.isBlank() ? fallback : cleaned;
    }

    private String summary(String text) {
        String cleaned = text == null ? "" : text.replaceAll("\\s+", " ").trim();
        return cleaned.length() <= 180 ? cleaned : cleaned.substring(0, 177) + "...";
    }

    private String firstUrl(String text) {
        Matcher matcher = URL_PATTERN.matcher(text == null ? "" : text);
        return matcher.find() ? matcher.group() : null;
    }

    private String firstMention(String text) {
        Matcher matcher = MENTION_PATTERN.matcher(text == null ? "" : text);
        return matcher.find() ? matcher.group() : null;
    }

    private boolean containsAny(String text, String... needles) {
        for (String needle : needles) {
            if (text.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private boolean isOpsChat(Long chatId) {
        if (opsChatId == null || opsChatId.isBlank()) {
            return true;
        }
        return chatId.toString().equals(opsChatId.trim());
    }

    private String metadataJson(String source, String text) {
        return "{\"source\":\"%s\",\"rawText\":\"%s\"}".formatted(source, escapeJson(text));
    }

    private Map<String, Object> replyMetadata(IncomingMessage incoming) {
        if (incoming == null || incoming.telegramMessageId() == null) {
            return Map.of();
        }
        return Map.of("replyToMessageId", incoming.telegramMessageId());
    }

    private String escapeJson(String value) {
        return value == null ? "" : value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
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

    private record ClassificationDraft(
            String scenario,
            String intent,
            String projectCode,
            String resultStatus,
            String summary,
            List<String> actions
    ) {
    }
}
