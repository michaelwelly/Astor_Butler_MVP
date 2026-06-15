package museon_online.astor_butler.domain.payment;

import java.time.Instant;

public record TelegramStarPayment(
        Long id,
        Long chatId,
        Long telegramUserId,
        Long userId,
        String venueCode,
        TelegramStarPaymentStatus status,
        String source,
        TelegramStarPaymentPurpose purpose,
        String relatedEntityType,
        Long relatedEntityId,
        String title,
        String description,
        String payload,
        String currency,
        Long starAmount,
        String providerToken,
        Long invoiceMessageId,
        String preCheckoutQueryId,
        String telegramPaymentChargeId,
        String providerPaymentChargeId,
        String failureReason,
        String metadataJson,
        Instant createdAt,
        Instant updatedAt
) {
}
