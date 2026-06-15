package museon_online.astor_butler.domain.feedback;

import java.time.Instant;

public record GuestFeedback(
        Long id,
        Long chatId,
        Long telegramUserId,
        Long userId,
        String venueCode,
        FeedbackStatus status,
        String source,
        FeedbackType feedbackType,
        FeedbackSentiment sentiment,
        FeedbackPriority priority,
        String guestName,
        String text,
        String previousState,
        String correlationId,
        String adminChatId,
        Long adminMessageId,
        String metadataJson,
        Instant createdAt,
        Instant updatedAt
) {
}
