package museon_online.astor_butler.domain.ops;

import java.time.Instant;

public record OpsCall(
        Long id,
        Long projectId,
        String title,
        Instant startsAt,
        String owner,
        OpsCallStatus status,
        String notes,
        String metadataJson,
        Instant createdAt,
        Instant updatedAt
) {
}
