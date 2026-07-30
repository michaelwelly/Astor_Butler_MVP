package museon_online.astor_butler.domain.ops;

import java.util.List;

public record OpsProjectDashboard(
        OpsProject project,
        List<OpsTask> openTasks,
        List<OpsCall> upcomingCalls,
        List<OpsArtifact> artifacts
) {
    public OpsProjectDashboard(OpsProject project, List<OpsTask> openTasks) {
        this(project, openTasks, List.of(), List.of());
    }

    public OpsProjectDashboard {
        openTasks = openTasks == null ? List.of() : List.copyOf(openTasks);
        upcomingCalls = upcomingCalls == null ? List.of() : List.copyOf(upcomingCalls);
        artifacts = artifacts == null ? List.of() : List.copyOf(artifacts);
    }
}
