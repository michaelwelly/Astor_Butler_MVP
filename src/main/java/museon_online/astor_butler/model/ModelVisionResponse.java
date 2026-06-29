package museon_online.astor_butler.model;

import java.time.Duration;
import java.util.Map;

public record ModelVisionResponse(
        String text,
        String provider,
        String model,
        ModelCapability capability,
        Duration latency,
        boolean fallback,
        Map<String, Object> metadata
) {
    public ModelVisionResponse {
        if (metadata == null) {
            metadata = Map.of();
        }
    }

    public static ModelVisionResponse vision(
            String text,
            String provider,
            String model,
            Duration latency
    ) {
        return new ModelVisionResponse(
                text,
                provider,
                model,
                ModelCapability.IMAGE_UNDERSTANDING,
                latency,
                false,
                Map.of()
        );
    }
}
