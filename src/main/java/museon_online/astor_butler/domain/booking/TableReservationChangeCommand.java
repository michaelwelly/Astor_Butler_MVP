package museon_online.astor_butler.domain.booking;

import java.time.Instant;

public record TableReservationChangeCommand(
        String venueCode,
        String tableCode,
        String preferredZone,
        String seatingPreference,
        Instant requestedStartAt,
        Instant requestedEndAt,
        Integer partySize,
        String guestComment
) {
}
