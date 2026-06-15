package museon_online.astor_butler.domain.booking;

import java.time.LocalDate;

public record EventBookingCommand(
        Long chatId,
        Long telegramUserId,
        Long userId,
        String venueCode,
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
        String managerChatId
) {
}
