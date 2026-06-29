package museon_online.astor_butler.model;

import java.time.Duration;
import java.util.Map;

public record ModelTextResponse(
        String text,
        String provider,
        String model,
        ModelCapability capability,
        Duration latency,
        boolean fallback,
        Map<String, Object> metadata
) {
    public ModelTextResponse {
        if (metadata == null) {
            metadata = Map.of();
        }
    }

    public static ModelTextResponse text(
            String text,
            String provider,
            String model,
            Duration latency
    ) {
        return new ModelTextResponse(
                text,
                provider,
                model,
                ModelCapability.TEXT_GENERATION,
                latency,
                false,
                Map.of()
        );
    }
}
