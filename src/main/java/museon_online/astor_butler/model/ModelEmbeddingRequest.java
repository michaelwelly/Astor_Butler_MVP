package museon_online.astor_butler.model;

import java.util.Map;

public record ModelEmbeddingRequest(
        String text,
        String model,
        String scenario,
        String state,
        String purpose,
        Map<String, Object> metadata
) {
    public ModelEmbeddingRequest {
        if (metadata == null) {
            metadata = Map.of();
        }
    }

    public static ModelEmbeddingRequest of(String text, String model, String scenario, String state, String purpose) {
        return new ModelEmbeddingRequest(text, model, scenario, state, purpose, Map.of());
    }
}
