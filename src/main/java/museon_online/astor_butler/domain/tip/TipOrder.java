package museon_online.astor_butler.domain.tip;

import java.time.Instant;

public record TipOrder(
        Long id,
        Long chatId,
        Long telegramUserId,
        Long userId,
        String venueCode,
        Long staffId,
        String staffDisplayName,
        TipOrderStatus status,
        String source,
        Long amountMinor,
        String currency,
        String guestName,
        String guestComment,
        String sbpUrl,
        String paymentExternalId,
        Instant createdAt,
        Instant updatedAt
) {
}
