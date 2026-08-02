package museon_online.astor_butler.domain.booking.external;

import museon_online.astor_butler.domain.booking.TableReservationCommand;

import java.time.Instant;

public interface ExternalReservationProvider {

    String providerId();

    ExternalReservationStatus status();

    ExternalAvailabilityResult checkAvailability(ExternalAvailabilityRequest request);

    ExternalReservationResult reserve(TableReservationCommand command, String idempotencyKey);

    record ExternalAvailabilityRequest(
            String venueCode,
            Instant requestedStartAt,
            Instant requestedEndAt,
            Integer partySize,
            String tableCode,
            String preferredZone
    ) {
    }
}
