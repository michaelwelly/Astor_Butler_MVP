package museon_online.astor_butler.service.message;

import lombok.RequiredArgsConstructor;
import museon_online.astor_butler.api.common.ApiException;
import museon_online.astor_butler.domain.ops.OpsArtifact;
import museon_online.astor_butler.domain.ops.OpsArtifactCommand;
import museon_online.astor_butler.domain.ops.OpsArtifactType;
import museon_online.astor_butler.domain.ops.OpsCall;
import museon_online.astor_butler.domain.ops.OpsCallCommand;
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
import museon_online.astor_butler.fsm.core.BotState;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class OpsTelegramCommandService {

    private static final ZoneId OPS_ZONE = ZoneId.of("Asia/Yekaterinburg");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd.MM HH:mm").withZone(OPS_ZONE);
    private static final Pattern PERCENT = Pattern.compile("^(\\d{1,3})%?$");
    private static final Pattern QUOTED_TITLE = Pattern.compile("\"([^\"]+)\"");
    private static final Pattern DUE_DATE = Pattern.compile("\\b(\\d{1,2})\\.(\\d{1,2})\\b");
    private static final Pattern TIME = Pattern.compile("\\b([01]?\\d|2[0-3]):([0-5]\\d)\\b");
    private static final Pattern URL = Pattern.compile("https?://\\S+");

    private final OpsProjectService opsProjectService;
    private final OpsStatusDigestFormatter digestFormatter;

    public Optional<OutgoingMessage> handle(IncomingMessage incoming, BotState currentState, String text) {
        String normalized = text == null ? "" : text.trim();
        if (!isOpsCommand(normalized)) {
            return Optional.empty();
        }

        String reply;
        try {
            reply = route(normalized);
        } catch (ApiException e) {
            reply = """
                    <b>Smart Solution Ops</b>
                    Не нашел данные для команды.

                    Проверь код проекта или напиши /ops.
                    """.strip();
        } catch (Exception e) {
            reply = """
                    <b>Smart Solution Ops</b>
                    Команда не выполнена. Проверь формат или напиши /ops.
                    """.strip();
        }

        return Optional.of(OutgoingMessage.of(
                incoming,
                reply,
                currentState.name(),
                true,
                false,
                false,
                false,
                AdminAlert.none(),
                List.of("OPS_TELEGRAM_COMMAND", "SKIP_GUEST_FSM")
        ));
    }

    public boolean supports(String text) {
        return isOpsCommand(text == null ? "" : text.trim());
    }

    private boolean isOpsCommand(String text) {
        String command = command(text);
        return command.equals("/ops")
                || command.equals("/newproject")
                || command.equals("/projects")
                || command.equals("/project")
                || command.equals("/tasks")
                || command.equals("/summary")
                || command.equals("/status")
                || command.equals("/task")
                || command.equals("/call")
                || command.equals("/calls")
                || command.equals("/artifact")
                || command.equals("/artifacts");
    }

    private String route(String text) {
        String command = command(text);
        String args = args(text);
        return switch (command) {
            case "/ops" -> help();
            case "/newproject" -> newProject(args);
            case "/projects" -> projects();
            case "/project" -> project(args);
            case "/tasks" -> tasks(args);
            case "/summary" -> summary(args);
            case "/status" -> status(args);
            case "/task" -> task(args);
            case "/call" -> call(args);
            case "/calls" -> calls(args);
            case "/artifact" -> artifact(args);
            case "/artifacts" -> artifacts(args);
            default -> help();
        };
    }

    private String newProject(String args) {
        ParsedProject parsed = parseProject(args);
        if (parsed == null) {
            return "Формат: <code>/newproject SITE \"Smart_Soultion.com\" WEBSITE @owner</code>";
        }
        OpsProject created = opsProjectService.createProject(new OpsProjectCommand(
                parsed.code(),
                parsed.name(),
                parsed.vertical(),
                OpsProjectStage.INTAKE,
                OpsProjectStatus.ACTIVE,
                parsed.owner(),
                null,
                0,
                null,
                null,
                "project created from Telegram",
                null,
                args,
                "{}"
        ));
        return """
                <b>Smart Solution Ops / project created</b>
                %s / <code>%s</code>

                Vertical: %s
                Owner: %s
                Next: <code>/summary %s</code>
                """.formatted(
                html(created.name()),
                html(created.code()),
                created.vertical(),
                html(blank(created.owner())),
                html(created.code())
        );
    }

    private String projects() {
        List<OpsProject> projects = opsProjectService.listProjects(OpsProjectStatus.ACTIVE, null, 20);
        if (projects.isEmpty()) {
            return """
                    <b>Smart Solution Ops / projects</b>
                    Активных проектов пока нет.
                    """.strip();
        }

        StringBuilder text = new StringBuilder("<b>Smart Solution Ops / active projects</b>\n\n");
        for (OpsProject project : projects) {
            text.append("• ")
                    .append(html(project.code()))
                    .append(" — ")
                    .append(html(project.name()))
                    .append("\n")
                    .append("  ")
                    .append(project.vertical())
                    .append(" / ")
                    .append(project.stage())
                    .append(" / ")
                    .append(project.progressPercent())
                    .append("%")
                    .append(project.owner() == null || project.owner().isBlank() ? "" : " / " + html(project.owner()))
                    .append("\n");
        }
        return text.toString();
    }

    private String project(String code) {
        if (code.isBlank()) {
            return "Нужен код проекта: <code>/project MED</code>";
        }
        OpsProject project = opsProjectService.getProjectByCode(code);
        return """
                <b>Smart Solution Ops / project</b>
                %s / <code>%s</code>

                <b>Pipeline</b>
                Vertical: %s
                Stage: %s
                Status: %s
                Progress: %s%%
                Owner: %s

                <b>Launch</b>
                %s

                <b>Done means</b>
                %s
                """.formatted(
                html(project.name()),
                html(project.code()),
                project.vertical(),
                project.stage(),
                project.status(),
                project.progressPercent(),
                html(blank(project.owner())),
                html(blank(project.launchStatus())),
                html(blank(project.resultDefinition()))
        );
    }

    private String tasks(String code) {
        if (code.isBlank()) {
            return "Нужен код проекта: <code>/tasks VIDEO</code>";
        }
        OpsProject project = opsProjectService.getProjectByCode(code);
        List<OpsTask> tasks = opsProjectService.listOpenTasks(project.id(), 20);
        if (tasks.isEmpty()) {
            return "<b>Smart Solution Ops / tasks</b>\nОткрытых задач по проекту <code>%s</code> нет.".formatted(html(project.code()));
        }

        StringBuilder text = new StringBuilder()
                .append("<b>Smart Solution Ops / tasks</b>\n")
                .append(html(project.name()))
                .append(" / <code>")
                .append(html(project.code()))
                .append("</code>\n\n");
        for (OpsTask task : tasks) {
            text.append("• #")
                    .append(task.id())
                    .append(" ")
                    .append(html(task.title()))
                    .append(" — ")
                    .append(task.status())
                    .append(", ")
                    .append(task.priority())
                    .append(", owner: ")
                    .append(html(blank(task.owner())))
                    .append("\n");
        }
        return text.toString();
    }

    private String summary(String code) {
        if (code.isBlank()) {
            return "Нужен код проекта: <code>/summary MED</code>";
        }
        OpsProject project = opsProjectService.getProjectByCode(code);
        OpsProjectDashboard dashboard = opsProjectService.dashboard(project.id(), 10);
        return digestFormatter.format(dashboard);
    }

    private String status(String args) {
        String[] parts = args.split("\\s+", 4);
        if (parts.length < 2) {
            return "Формат: <code>/status IZI WAITING_CLIENT 70% ожидает оплату</code>";
        }

        OpsProject project = opsProjectService.getProjectByCode(parts[0]);
        OpsProjectStatus status = OpsProjectStatus.valueOf(parts[1].toUpperCase(Locale.ROOT));
        Integer progress = null;
        String launchStatus = null;
        if (parts.length >= 3) {
            Matcher matcher = PERCENT.matcher(parts[2]);
            if (matcher.matches()) {
                progress = Integer.parseInt(matcher.group(1));
                launchStatus = parts.length >= 4 ? parts[3] : null;
            } else {
                launchStatus = args.substring(args.indexOf(parts[2]));
            }
        }

        OpsProject updated = opsProjectService.updateProjectStatus(project.id(), status, null, progress, launchStatus);
        return """
                <b>Smart Solution Ops / status updated</b>
                %s / <code>%s</code>

                Status: %s
                Stage: %s
                Progress: %s%%
                Launch: %s
                """.formatted(
                html(updated.name()),
                html(updated.code()),
                updated.status(),
                updated.stage(),
                updated.progressPercent(),
                html(blank(updated.launchStatus()))
        );
    }

    private String task(String args) {
        String[] parts = args.split("\\s+", 2);
        if (parts.length < 2) {
            return "Формат: <code>/task MED \"Подготовить презентацию\" @owner 25.07</code>";
        }

        OpsProject project = opsProjectService.getProjectByCode(parts[0]);
        ParsedTask parsed = parseTask(parts[1]);
        OpsTask created = opsProjectService.createTask(new OpsTaskCommand(
                project.id(),
                parsed.title(),
                parsed.owner(),
                null,
                null,
                OpsProjectStage.PLANNING,
                parsed.dueAt(),
                null,
                parsed.notes(),
                "{}"
        ));

        return """
                <b>Smart Solution Ops / task created</b>
                #%s / <code>%s</code>
                %s

                Owner: %s
                Status: %s
                Priority: %s
                """.formatted(
                created.id(),
                html(project.code()),
                html(created.title()),
                html(blank(created.owner())),
                created.status(),
                created.priority()
        );
    }

    private String call(String args) {
        String[] parts = args.split("\\s+", 2);
        if (parts.length < 2) {
            return "Формат: <code>/call VIDEO \"Созвон по запуску\" 24.07 15:00 @owner</code>";
        }

        OpsProject project = opsProjectService.getProjectByCode(parts[0]);
        ParsedCall parsed = parseCall(parts[1]);
        if (parsed == null) {
            return "Формат: <code>/call VIDEO \"Созвон по запуску\" 24.07 15:00 @owner</code>";
        }
        OpsCall created = opsProjectService.createCall(new OpsCallCommand(
                project.id(),
                parsed.title(),
                parsed.startsAt(),
                parsed.owner(),
                null,
                parts[1],
                "{}"
        ));

        return """
                <b>Smart Solution Ops / call scheduled</b>
                #%s / <code>%s</code>
                %s

                When: %s
                Owner: %s
                """.formatted(
                created.id(),
                html(project.code()),
                html(created.title()),
                DATE_TIME.format(created.startsAt()),
                html(blank(created.owner()))
        );
    }

    private String calls(String code) {
        Long projectId = null;
        String label = "all projects";
        if (!code.isBlank()) {
            OpsProject project = opsProjectService.getProjectByCode(code);
            projectId = project.id();
            label = project.code();
        }

        List<OpsCall> calls = opsProjectService.listUpcomingCalls(projectId, 20);
        if (calls.isEmpty()) {
            return "<b>Smart Solution Ops / calls</b>\nБлижайших коллов нет: <code>%s</code>.".formatted(html(label));
        }

        StringBuilder text = new StringBuilder()
                .append("<b>Smart Solution Ops / calls</b>\n")
                .append(html(label))
                .append("\n\n");
        for (OpsCall call : calls) {
            text.append("• #")
                    .append(call.id())
                    .append(" ")
                    .append(html(call.title()))
                    .append(" — ")
                    .append(DATE_TIME.format(call.startsAt()))
                    .append(", owner: ")
                    .append(html(blank(call.owner())))
                    .append("\n");
        }
        return text.toString();
    }

    private String artifact(String args) {
        String[] parts = args.split("\\s+", 2);
        if (parts.length < 2) {
            return "Формат: <code>/artifact MED \"Презентация\" https://... PRESENTATION @owner</code>";
        }

        OpsProject project = opsProjectService.getProjectByCode(parts[0]);
        ParsedArtifact parsed = parseArtifact(parts[1]);
        if (parsed == null) {
            return "Формат: <code>/artifact MED \"Презентация\" https://... PRESENTATION @owner</code>";
        }
        OpsArtifact created = opsProjectService.createArtifact(new OpsArtifactCommand(
                project.id(),
                parsed.title(),
                parsed.type(),
                null,
                parsed.owner(),
                parsed.url(),
                parts[1],
                "{}"
        ));

        return """
                <b>Smart Solution Ops / artifact saved</b>
                #%s / <code>%s</code>
                %s

                Type: %s
                Owner: %s
                %s
                """.formatted(
                created.id(),
                html(project.code()),
                html(created.title()),
                created.type(),
                html(blank(created.owner())),
                html(created.url())
        );
    }

    private String artifacts(String code) {
        if (code.isBlank()) {
            return "Нужен код проекта: <code>/artifacts MED</code>";
        }
        OpsProject project = opsProjectService.getProjectByCode(code);
        List<OpsArtifact> artifacts = opsProjectService.listArtifacts(project.id(), 20);
        if (artifacts.isEmpty()) {
            return "<b>Smart Solution Ops / artifacts</b>\nАртефактов по проекту <code>%s</code> пока нет.".formatted(html(project.code()));
        }

        StringBuilder text = new StringBuilder()
                .append("<b>Smart Solution Ops / artifacts</b>\n")
                .append(html(project.name()))
                .append(" / <code>")
                .append(html(project.code()))
                .append("</code>\n\n");
        for (OpsArtifact artifact : artifacts) {
            text.append("• #")
                    .append(artifact.id())
                    .append(" ")
                    .append(html(artifact.title()))
                    .append(" — ")
                    .append(artifact.type())
                    .append(", ")
                    .append(artifact.status())
                    .append("\n  ")
                    .append(html(artifact.url()))
                    .append("\n");
        }
        return text.toString();
    }

    private String help() {
        return """
                <b>Smart Solution Ops</b>

                Стартовые проекты:
                <code>VIDEO</code> — видео-продакшен / @egor
                <code>MED</code> — медицина и презентация / @michael
                <code>IZI</code> — ожидает оплату
                <code>RESTO</code> — статусы запусков ресторанов
                <code>PRINT</code> — типография и сроки
                <code>SITE</code> — Smart_Soultion.com и CRM
                <code>ADS</code> — Яндекс Бизнес / Директ / Карты

                Команды:
                <code>/newproject CODE "Название" VERTICAL @owner</code> — создать проект
                <code>/projects</code> — активные проекты
                <code>/project CODE</code> — карточка проекта
                <code>/tasks CODE</code> — открытые задачи
                <code>/summary CODE</code> — статус для команды
                <code>/status CODE STATUS 90% текст</code> — обновить статус
                <code>/task CODE "Задача" @owner 25.07</code> — создать задачу
                <code>/call CODE "Созвон" 24.07 15:00 @owner</code> — поставить call
                <code>/calls CODE</code> — расписание коллов
                <code>/artifact CODE "Презентация" https://... PRESENTATION @owner</code> — сохранить материал
                <code>/artifacts CODE</code> — материалы проекта

                Пример:
                <code>/summary MED</code>
                """.strip();
    }

    private String command(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        String rawCommand = text.trim().split("\\s+", 2)[0].toLowerCase(Locale.ROOT);
        int botMentionIndex = rawCommand.indexOf('@');
        return botMentionIndex < 0 ? rawCommand : rawCommand.substring(0, botMentionIndex);
    }

    private String args(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        String[] parts = text.trim().split("\\s+", 2);
        return parts.length < 2 ? "" : parts[1].trim();
    }

    private ParsedTask parseTask(String raw) {
        String source = raw == null ? "" : raw.trim();
        Matcher titleMatcher = QUOTED_TITLE.matcher(source);
        String title;
        String remainder;
        if (titleMatcher.find()) {
            title = titleMatcher.group(1).trim();
            remainder = source.substring(0, titleMatcher.start()) + " " + source.substring(titleMatcher.end());
        } else {
            title = source.replaceAll("@\\S+", "").replaceAll("\\b\\d{1,2}\\.\\d{1,2}\\b", "").trim();
            remainder = source;
        }

        String owner = null;
        for (String token : remainder.split("\\s+")) {
            if (token.startsWith("@") && token.length() > 1) {
                owner = token.substring(1);
                break;
            }
        }

        Instant dueAt = null;
        Matcher dueMatcher = DUE_DATE.matcher(remainder);
        if (dueMatcher.find()) {
            int day = Integer.parseInt(dueMatcher.group(1));
            int month = Integer.parseInt(dueMatcher.group(2));
            int year = LocalDate.now(OPS_ZONE).getYear();
            dueAt = LocalDate.of(year, month, day)
                    .atTime(LocalTime.NOON)
                    .atZone(OPS_ZONE)
                    .toInstant();
        }

        if (title.isBlank()) {
            title = source;
        }
        return new ParsedTask(title, owner, dueAt, source);
    }

    private ParsedProject parseProject(String raw) {
        String source = raw == null ? "" : raw.trim();
        String[] parts = source.split("\\s+", 2);
        if (parts.length < 2) {
            return null;
        }
        Matcher titleMatcher = QUOTED_TITLE.matcher(parts[1]);
        if (!titleMatcher.find()) {
            return null;
        }
        String remainder = parts[1].substring(0, titleMatcher.start()) + " " + parts[1].substring(titleMatcher.end());
        return new ParsedProject(
                parts[0],
                titleMatcher.group(1).trim(),
                parseVertical(remainder),
                parseOwner(remainder)
        );
    }

    private ParsedCall parseCall(String raw) {
        String source = raw == null ? "" : raw.trim();
        Matcher titleMatcher = QUOTED_TITLE.matcher(source);
        Matcher dateMatcher = DUE_DATE.matcher(source);
        Matcher timeMatcher = TIME.matcher(source);
        if (!titleMatcher.find() || !dateMatcher.find() || !timeMatcher.find()) {
            return null;
        }
        int day = Integer.parseInt(dateMatcher.group(1));
        int month = Integer.parseInt(dateMatcher.group(2));
        int hour = Integer.parseInt(timeMatcher.group(1));
        int minute = Integer.parseInt(timeMatcher.group(2));
        int year = LocalDate.now(OPS_ZONE).getYear();
        Instant startsAt = LocalDate.of(year, month, day)
                .atTime(hour, minute)
                .atZone(OPS_ZONE)
                .toInstant();
        String remainder = source.substring(0, titleMatcher.start()) + " " + source.substring(titleMatcher.end());
        return new ParsedCall(titleMatcher.group(1).trim(), startsAt, parseOwner(remainder));
    }

    private ParsedArtifact parseArtifact(String raw) {
        String source = raw == null ? "" : raw.trim();
        Matcher titleMatcher = QUOTED_TITLE.matcher(source);
        Matcher urlMatcher = URL.matcher(source);
        if (!titleMatcher.find() || !urlMatcher.find()) {
            return null;
        }
        String remainder = source.substring(0, titleMatcher.start()) + " " + source.substring(titleMatcher.end());
        return new ParsedArtifact(
                titleMatcher.group(1).trim(),
                urlMatcher.group(),
                parseArtifactType(remainder),
                parseOwner(remainder)
        );
    }

    private String parseOwner(String source) {
        for (String token : source.split("\\s+")) {
            if (token.startsWith("@") && token.length() > 1) {
                return token.substring(1);
            }
        }
        return null;
    }

    private OpsProjectVertical parseVertical(String source) {
        for (String token : source.split("\\s+")) {
            try {
                return OpsProjectVertical.valueOf(token.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
            }
        }
        return OpsProjectVertical.OTHER;
    }

    private OpsArtifactType parseArtifactType(String source) {
        for (String token : source.split("\\s+")) {
            try {
                return OpsArtifactType.valueOf(token.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
            }
        }
        return OpsArtifactType.PRESENTATION;
    }

    private String blank(String value) {
        return value == null || value.isBlank() ? "not set" : value;
    }

    private String html(String value) {
        return blank(value)
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    private record ParsedTask(String title, String owner, Instant dueAt, String notes) {
    }

    private record ParsedProject(String code, String name, OpsProjectVertical vertical, String owner) {
    }

    private record ParsedCall(String title, Instant startsAt, String owner) {
    }

    private record ParsedArtifact(String title, String url, OpsArtifactType type, String owner) {
    }
}
