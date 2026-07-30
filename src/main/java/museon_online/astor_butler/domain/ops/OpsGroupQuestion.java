package museon_online.astor_butler.domain.ops;

import java.time.Instant;

public record OpsGroupQuestion(
        Long id,
        Long chatId,
        Integer messageId,
        Long askerTelegramUserId,
        String askerUsername,
        String askerDisplayName,
        String questionText,
        OpsGroupQuestionStatus status,
        String answerText,
        String answerSource,
        String answeredBy,
        Instant answeredAt,
        String metadataJson,
        Instant createdAt,
        Instant updatedAt
) {
}
