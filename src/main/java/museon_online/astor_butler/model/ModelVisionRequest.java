package museon_online.astor_butler.model;

import java.util.Map;

public record ModelVisionRequest(
        String prompt,
        String imageBase64,
        String mimeType,
        String model,
        String scenario,
        String state,
        String purpose,
        Map<String, Object> metadata
) {
    public ModelVisionRequest {
        if (metadata == null) {
            metadata = Map.of();
        }
    }

    public static ModelVisionRequest of(
            String prompt,
            String imageBase64,
            String mimeType,
            String model,
            String scenario,
            String state,
            String purpose
    ) {
        return new ModelVisionRequest(prompt, imageBase64, mimeType, model, scenario, state, purpose, Map.of());
    }
}
