package museon_online.astor_butler.domain.ops;

import java.util.List;

public record OpsProjectDashboard(
        OpsProject project,
        List<OpsTask> openTasks
) {
}
