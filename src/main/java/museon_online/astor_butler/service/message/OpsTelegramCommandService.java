package museon_online.astor_butler.service.message;

import lombok.RequiredArgsConstructor;
import museon_online.astor_butler.api.common.ApiException;
import museon_online.astor_butler.domain.ops.OpsProject;
import museon_online.astor_butler.domain.ops.OpsProjectDashboard;
import museon_online.astor_butler.domain.ops.OpsProjectService;
import museon_online.astor_butler.domain.ops.OpsProjectStage;
import museon_online.astor_butler.domain.ops.OpsProjectStatus;
import museon_online.astor_butler.domain.ops.OpsStatusDigestFormatter;
import museon_online.astor_butler.domain.ops.OpsTask;
import museon_online.astor_butler.domain.ops.OpsTaskCommand;
import museon_online.astor_butler.fsm.core.BotState;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class OpsTelegramCommandService {

    private static final ZoneId OPS_ZONE = ZoneId.of("Asia/Yekaterinburg");
    private static final Pattern PERCENT = Pattern.compile("^(\\d{1,3})%?$");
    private static final Pattern QUOTED_TITLE = Pattern.compile("\"([^\"]+)\"");
    private static final Pattern DUE_DATE = Pattern.compile("\\b(\\d{1,2})\\.(\\d{1,2})\\b");

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

    private boolean isOpsCommand(String text) {
        String command = command(text);
        return command.equals("/ops")
                || command.equals("/projects")
                || command.equals("/project")
                || command.equals("/tasks")
                || command.equals("/summary")
                || command.equals("/status")
                || command.equals("/task");
    }

    private String route(String text) {
        String command = command(text);
        String args = args(text);
        return switch (command) {
            case "/ops" -> help();
            case "/projects" -> projects();
            case "/project" -> project(args);
            case "/tasks" -> tasks(args);
            case "/summary" -> summary(args);
            case "/status" -> status(args);
            case "/task" -> task(args);
            default -> help();
        };
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
            return "Нужен код проекта: <code>/project AERIS_LAUNCH</code>";
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
            return "Нужен код проекта: <code>/tasks AERIS_LAUNCH</code>";
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
            return "Нужен код проекта: <code>/summary AERIS_LAUNCH</code>";
        }
        OpsProject project = opsProjectService.getProjectByCode(code);
        OpsProjectDashboard dashboard = opsProjectService.dashboard(project.id(), 10);
        return digestFormatter.format(dashboard);
    }

    private String status(String args) {
        String[] parts = args.split("\\s+", 4);
        if (parts.length < 2) {
            return "Формат: <code>/status AERIS_LAUNCH READY_TO_LAUNCH 90% waiting DNS</code>";
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
            return "Формат: <code>/task AERIS_LAUNCH \"Подготовить презентацию\" @owner 25.07</code>";
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

    private String help() {
        return """
                <b>Smart Solution Ops</b>

                Команды:
                <code>/projects</code> — активные проекты
                <code>/project CODE</code> — карточка проекта
                <code>/tasks CODE</code> — открытые задачи
                <code>/summary CODE</code> — статус для команды
                <code>/status CODE STATUS 90% текст</code> — обновить статус
                <code>/task CODE "Задача" @owner 25.07</code> — создать задачу

                Пример:
                <code>/summary AERIS_LAUNCH</code>
                """.strip();
    }

    private String command(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        return text.trim().split("\\s+", 2)[0].toLowerCase(Locale.ROOT);
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
}
