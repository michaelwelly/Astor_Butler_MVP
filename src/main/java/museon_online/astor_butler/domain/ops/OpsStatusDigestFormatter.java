package museon_online.astor_butler.domain.ops;

import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;

@Component
public class OpsStatusDigestFormatter {

    private static final ZoneId DEFAULT_ZONE = ZoneId.of("Asia/Yekaterinburg");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd.MM HH:mm").withZone(DEFAULT_ZONE);

    public String format(OpsProjectDashboard dashboard) {
        OpsProject project = dashboard.project();
        StringBuilder text = new StringBuilder();
        text.append("<b>Smart Solution / project status</b>\n");
        text.append(html(project.name())).append(" / ").append(html(project.code())).append("\n\n");
        text.append("<b>Pipeline</b>\n");
        text.append("Vertical: ").append(project.vertical()).append("\n");
        text.append("Stage: ").append(project.stage()).append("\n");
        text.append("Status: ").append(project.status()).append("\n");
        text.append("Progress: ").append(project.progressPercent()).append("%\n");
        text.append("Owner: ").append(html(blank(project.owner()))).append("\n");
        text.append("Deadline: ").append(project.deadlineAt() == null ? "not set" : DATE_TIME.format(project.deadlineAt())).append("\n");
        text.append("Next call: ").append(project.nextCallAt() == null ? "not set" : DATE_TIME.format(project.nextCallAt())).append("\n\n");
        text.append("<b>Launch status</b>\n");
        text.append(html(blank(project.launchStatus()))).append("\n\n");
        text.append("<b>Definition of done</b>\n");
        text.append(html(blank(project.resultDefinition()))).append("\n\n");
        text.append("<b>Open tasks</b>\n");

        if (dashboard.openTasks().isEmpty()) {
            text.append("No open tasks.");
            return text.toString();
        }

        dashboard.openTasks().stream()
                .sorted(Comparator.comparing(OpsTask::dueAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .limit(10)
                .forEach(task -> text.append("• ")
                        .append(html(task.title()))
                        .append(" — ")
                        .append(task.status())
                        .append(", ")
                        .append(task.priority())
                        .append(", owner: ")
                        .append(html(blank(task.owner())))
                        .append(task.dueAt() == null ? "" : ", due: " + DATE_TIME.format(task.dueAt()))
                        .append("\n"));
        return text.toString();
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
}
