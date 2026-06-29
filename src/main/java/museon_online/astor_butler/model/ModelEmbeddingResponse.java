package museon_online.astor_butler.model;

import java.time.Duration;
import java.util.List;
import java.util.Map;

public record ModelEmbeddingResponse(
        List<Double> embedding,
        String provider,
        String model,
        ModelCapability capability,
        Duration latency,
        boolean fallback,
        Map<String, Object> metadata
) {
    public ModelEmbeddingResponse {
        embedding = embedding == null ? List.of() : List.copyOf(embedding);
        if (metadata == null) {
            metadata = Map.of();
        }
    }

    public static ModelEmbeddingResponse embedding(
            List<Double> embedding,
            String provider,
            String model,
            Duration latency
    ) {
        return new ModelEmbeddingResponse(
                embedding,
                provider,
                model,
                ModelCapability.EMBEDDING,
                latency,
                false,
                Map.of()
        );
    }
}
