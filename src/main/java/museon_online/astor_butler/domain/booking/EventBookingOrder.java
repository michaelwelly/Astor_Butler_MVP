package museon_online.astor_butler.domain.booking;

import java.time.Instant;
import java.time.LocalDate;

public record EventBookingOrder(
        Long id,
        Long chatId,
        Long telegramUserId,
        Long userId,
        String venueCode,
        EventBookingStatus status,
        String source,
        String eventType,
        LocalDate requestedDate,
        String requestedTimeText,
        Integer guestCount,
        String budgetText,
        String menuPreferences,
        String technicalRequirements,
        String contact,
        String guestName,
        String guestPhone,
        String guestComment,
        Long managerTelegramId,
        Long managerUserId,
        String managerChatId,
        String externalId,
        Instant createdAt,
        Instant updatedAt
) {
}
