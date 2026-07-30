package museon_online.astor_butler.domain.ops;

import java.time.Instant;

public record OpsProjectCommand(
        String code,
        String name,
        OpsProjectVertical vertical,
        OpsProjectStage stage,
        OpsProjectStatus status,
        String owner,
        String teamChatId,
        Integer progressPercent,
        Instant deadlineAt,
        Instant nextCallAt,
        String launchStatus,
        String resultDefinition,
        String description,
        String metadataJson
) {
}
