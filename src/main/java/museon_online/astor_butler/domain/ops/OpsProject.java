package museon_online.astor_butler.domain.ops;

import java.time.Instant;

public record OpsProject(
        Long id,
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
        String metadataJson,
        Instant createdAt,
        Instant updatedAt
) {
}
