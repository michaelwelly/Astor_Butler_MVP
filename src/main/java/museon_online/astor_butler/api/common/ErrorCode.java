package museon_online.astor_butler.api.common;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Stable machine-readable API error codes")
public enum ErrorCode {
    BAD_REQUEST,
    UNAUTHORIZED,
    FORBIDDEN,
    NOT_FOUND,
    CONFLICT,
    VALIDATION_FAILED,
    RATE_LIMITED,
    INTERNAL_ERROR,
    BAD_GATEWAY,
    SERVICE_UNAVAILABLE
}
