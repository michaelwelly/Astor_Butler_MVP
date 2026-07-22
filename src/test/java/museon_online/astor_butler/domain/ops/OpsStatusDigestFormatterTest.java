package museon_online.astor_butler.domain.ops;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OpsStatusDigestFormatterTest {

    private final OpsStatusDigestFormatter formatter = new OpsStatusDigestFormatter();

    @Test
    void formatsTelegramReadyProjectDashboard() {
        OpsProject project = new OpsProject(
                77L,
                "AERIS_LAUNCH",
                "AERIS restaurant launch",
                OpsProjectVertical.HORECA,
                OpsProjectStage.LAUNCH,
                OpsProjectStatus.READY_TO_LAUNCH,
                "Michael",
                "-100500",
                90,
                Instant.parse("2026-08-01T12:00:00Z"),
                Instant.parse("2026-07-24T10:00:00Z"),
                "hostess smoke test remains",
                "bot accepts bookings and team sees cards",
                "restaurant launch pipeline",
                "{}",
                Instant.parse("2026-07-23T10:00:00Z"),
                Instant.parse("2026-07-23T10:00:00Z")
        );
        OpsTask task = new OpsTask(
                88L,
                77L,
                "Run Telegram smoke test",
                "Anna",
                OpsTaskStatus.IN_PROGRESS,
                OpsTaskPriority.URGENT,
                OpsProjectStage.LAUNCH,
                Instant.parse("2026-07-25T12:00:00Z"),
                null,
                "manual test with team chat",
                "{}",
                Instant.parse("2026-07-23T10:00:00Z"),
                Instant.parse("2026-07-23T10:00:00Z")
        );

        String text = formatter.format(new OpsProjectDashboard(project, List.of(task)));

        assertThat(text).contains("<b>Smart Solution / project status</b>");
        assertThat(text).contains("AERIS restaurant launch");
        assertThat(text).contains("READY_TO_LAUNCH");
        assertThat(text).contains("Run Telegram smoke test");
        assertThat(text).contains("owner: Anna");
    }
}
