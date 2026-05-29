package museon_online.astor_butler.api.common;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Stable status values for accepted commands and async operations")
public enum OperationStatus {
    ACCEPTED,
    CREATED,
    UPDATED,
    DELETED,
    QUEUED,
    REJECTED,
    FAILED
}
