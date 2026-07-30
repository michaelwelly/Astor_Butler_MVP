package museon_online.astor_butler.domain.ops;

import java.time.Instant;

public record OpsTask(
        Long id,
        Long projectId,
        String title,
        String owner,
        OpsTaskStatus status,
        OpsTaskPriority priority,
        OpsProjectStage pipelineStage,
        Instant dueAt,
        String deliverableUrl,
        String notes,
        String metadataJson,
        Instant createdAt,
        Instant updatedAt
) {
}
