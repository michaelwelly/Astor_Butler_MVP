package museon_online.astor_butler.domain.concierge;

import java.time.Instant;

public record ConciergeRequest(
        Long id,
        Long chatId,
        Long telegramUserId,
        Long userId,
        String venueCode,
        ConciergeRequestType requestType,
        ConciergeRequestStatus status,
        String source,
        String guestName,
        String requestText,
        String managerChatId,
        String capturedFromState,
        String correlationId,
        String metadataJson,
        Instant createdAt,
        Instant updatedAt
) {
}
