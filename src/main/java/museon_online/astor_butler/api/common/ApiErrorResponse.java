package museon_online.astor_butler.api.common;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.Map;

@Schema(description = "Standard API error response")
public record ApiErrorResponse(
        String code,
        String message,
        String traceId,
        Map<String, Object> details,
        Instant timestamp
) {
}
