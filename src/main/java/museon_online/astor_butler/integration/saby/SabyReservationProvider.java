package museon_online.astor_butler.integration.saby;

import lombok.RequiredArgsConstructor;
import museon_online.astor_butler.domain.booking.TableReservationCommand;
import museon_online.astor_butler.domain.booking.external.ExternalAvailabilityResult;
import museon_online.astor_butler.domain.booking.external.ExternalReservationProvider;
import museon_online.astor_butler.domain.booking.external.ExternalReservationResult;
import museon_online.astor_butler.domain.booking.external.ExternalReservationStatus;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class SabyReservationProvider implements ExternalReservationProvider {

    public static final String PROVIDER_ID = "SABY";

    private final SabyReservationProperties properties;

    @Override
    public String providerId() {
        return PROVIDER_ID;
    }

    @Override
    public ExternalReservationStatus status() {
        List<String> missing = properties.missingConfiguration();
        return new ExternalReservationStatus(
                PROVIDER_ID,
                properties.isEnabled(),
                properties.isEnabled() && missing.isEmpty(),
                missing,
                properties.isEnabled() ? "CONFIG_REQUIRED" : "DISABLED"
        );
    }

    @Override
    public ExternalAvailabilityResult checkAvailability(ExternalAvailabilityRequest request) {
        if (!status().configured()) {
            return ExternalAvailabilityResult.unavailableBecauseUnconfigured(PROVIDER_ID, status().missingConfiguration());
        }
        return ExternalAvailabilityResult.unavailableBecauseNotImplemented(PROVIDER_ID);
    }

    @Override
    public ExternalReservationResult reserve(TableReservationCommand command, String idempotencyKey) {
        if (!status().configured()) {
            return ExternalReservationResult.rejectedBecauseUnconfigured(PROVIDER_ID, status().missingConfiguration());
        }
        return ExternalReservationResult.rejectedBecauseNotImplemented(PROVIDER_ID);
    }
}
