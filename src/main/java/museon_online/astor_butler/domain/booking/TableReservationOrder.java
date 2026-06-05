package museon_online.astor_butler.domain.booking;

import java.time.Instant;

public record TableReservationOrder(
        Long id,
        Long chatId,
        Long telegramUserId,
        Long userId,
        Long tableId,
        String tableCode,
        String tableDisplayName,
        TableReservationStatus status,
        String source,
        Instant requestedStartAt,
        Instant requestedEndAt,
        Integer partySize,
        String guestName,
        String guestPhone,
        String guestComment,
        Long managerTelegramId,
        Long managerUserId,
        String hostessChatId,
        String sbisExternalId,
        Instant createdAt,
        Instant updatedAt
) {
}
