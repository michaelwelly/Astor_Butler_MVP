package museon_online.astor_butler.model;

import java.util.Map;

public record ModelTextRequest(
        String prompt,
        String scenario,
        String state,
        String purpose,
        ModelProfile profile,
        Map<String, Object> metadata
) {
    public ModelTextRequest {
        if (profile == null) {
            profile = ModelProfile.FRONTLINE;
        }
        if (metadata == null) {
            metadata = Map.of();
        }
    }

    public static ModelTextRequest of(String prompt, String scenario, String state, String purpose) {
        return new ModelTextRequest(prompt, scenario, state, purpose, ModelProfile.FRONTLINE, Map.of());
    }

    public static ModelTextRequest quality(String prompt, String scenario, String state, String purpose) {
        return new ModelTextRequest(prompt, scenario, state, purpose, ModelProfile.QUALITY, Map.of());
    }
}
