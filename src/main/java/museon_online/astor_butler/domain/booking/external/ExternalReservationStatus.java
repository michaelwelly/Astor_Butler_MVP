package museon_online.astor_butler.domain.booking.external;

import java.util.List;

public record ExternalReservationStatus(
        String providerId,
        boolean enabled,
        boolean configured,
        List<String> missingConfiguration,
        String mode
) {
    public static ExternalReservationStatus disabled(String providerId, List<String> missingConfiguration) {
        return new ExternalReservationStatus(providerId, false, false, List.copyOf(missingConfiguration), "DISABLED");
    }
}
