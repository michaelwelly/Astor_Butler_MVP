package museon_online.astor_butler.domain.merch;

import java.time.Instant;

public record MerchOrder(
        Long id,
        Long chatId,
        Long telegramUserId,
        Long userId,
        String venueCode,
        Long itemId,
        String itemTitle,
        MerchOrderStatus status,
        String source,
        Integer quantity,
        Long priceMinor,
        String currency,
        String guestName,
        String guestComment,
        String paymentMethodHint,
        String paymentExternalId,
        String metadataJson,
        Instant createdAt,
        Instant updatedAt
) {
}
