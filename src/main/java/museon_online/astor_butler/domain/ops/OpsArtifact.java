package museon_online.astor_butler.domain.ops;

import java.time.Instant;

public record OpsArtifact(
        Long id,
        Long projectId,
        String title,
        OpsArtifactType type,
        OpsArtifactStatus status,
        String owner,
        String url,
        String notes,
        String metadataJson,
        Instant createdAt,
        Instant updatedAt
) {
}
