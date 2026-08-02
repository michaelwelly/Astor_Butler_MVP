package museon_online.astor_butler.domain.booking.external;

import java.util.List;
import java.util.Map;

public record ExternalAvailabilityResult(
        boolean available,
        boolean providerConfigured,
        String providerId,
        String status,
        String message,
        List<String> missingConfiguration,
        Map<String, Object> metadata
) {
    public static ExternalAvailabilityResult unavailableBecauseUnconfigured(
            String providerId,
            List<String> missingConfiguration
    ) {
        return new ExternalAvailabilityResult(
                false,
                false,
                providerId,
                "PROVIDER_NOT_CONFIGURED",
                "External reservation provider is not configured; keep local hostess confirmation flow.",
                List.copyOf(missingConfiguration),
                Map.of()
        );
    }

    public static ExternalAvailabilityResult unavailableBecauseNotImplemented(String providerId) {
        return new ExternalAvailabilityResult(
                false,
                true,
                providerId,
                "PROVIDER_CONTRACT_NOT_IMPLEMENTED",
                "External provider contract is not implemented; keep local hostess confirmation flow.",
                List.of("official Saby reservation API contract"),
                Map.of()
        );
    }
}
