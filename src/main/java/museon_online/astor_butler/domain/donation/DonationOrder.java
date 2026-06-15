package museon_online.astor_butler.domain.donation;

import java.time.Instant;

public record DonationOrder(
        Long id,
        Long chatId,
        Long telegramUserId,
        Long userId,
        String venueCode,
        Long initiativeId,
        String initiativeTitle,
        DonationOrderStatus status,
        String source,
        Long amountMinor,
        String currency,
        Boolean anonymous,
        String guestName,
        String guestComment,
        String sbpUrl,
        String paymentExternalId,
        Instant createdAt,
        Instant updatedAt
) {
}
