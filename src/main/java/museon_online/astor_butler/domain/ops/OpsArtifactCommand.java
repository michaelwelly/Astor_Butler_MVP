package museon_online.astor_butler.domain.ops;

public record OpsArtifactCommand(
        Long projectId,
        String title,
        OpsArtifactType type,
        OpsArtifactStatus status,
        String owner,
        String url,
        String notes,
        String metadataJson
) {
}
