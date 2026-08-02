package museon_online.astor_butler.domain.booking.external;

import java.util.List;
import java.util.Map;

public record ExternalReservationResult(
        boolean created,
        boolean providerConfigured,
        String providerId,
        String status,
        String externalReservationId,
        String message,
        List<String> missingConfiguration,
        Map<String, Object> metadata
) {
    public static ExternalReservationResult rejectedBecauseUnconfigured(
            String providerId,
            List<String> missingConfiguration
    ) {
        return new ExternalReservationResult(
                false,
                false,
                providerId,
                "PROVIDER_NOT_CONFIGURED",
                "",
                "External reservation provider is not configured; booking write is blocked.",
                List.copyOf(missingConfiguration),
                Map.of()
        );
    }

    public static ExternalReservationResult rejectedBecauseNotImplemented(String providerId) {
        return new ExternalReservationResult(
                false,
                true,
                providerId,
                "PROVIDER_CONTRACT_NOT_IMPLEMENTED",
                "",
                "External provider write is blocked until the official API contract and read-only smoke are approved.",
                List.of("official Saby reservation API contract", "hostess confirmation workflow approval"),
                Map.of()
        );
    }
}
