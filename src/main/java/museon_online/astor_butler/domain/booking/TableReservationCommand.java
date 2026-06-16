package museon_online.astor_butler.domain.booking;

import java.time.Instant;

public record TableReservationCommand(
        Long chatId,
        Long telegramUserId,
        Long userId,
        String venueCode,
        String tableCode,
        String preferredZone,
        String seatingPreference,
        Instant requestedStartAt,
        Instant requestedEndAt,
        Integer partySize,
        String guestName,
        String guestPhone,
        String guestComment,
        Long managerTelegramId,
        String hostessChatId
) {
}
