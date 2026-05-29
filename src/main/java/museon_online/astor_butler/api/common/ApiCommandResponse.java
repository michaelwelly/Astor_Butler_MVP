package museon_online.astor_butler.api.common;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Generic command result for async or state-changing operations")
public record ApiCommandResponse(
        UUID operationId,
        String status,
        Instant acceptedAt
) {
    public static ApiCommandResponse accepted(String status) {
        return new ApiCommandResponse(UUID.randomUUID(), status, Instant.now());
    }
}
