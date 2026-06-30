package museon_online.astor_butler.model;

import java.time.Duration;
import java.util.Map;

public record ModelInteractionAuditRecord(
        String venueCode,
        String channel,
        Long chatId,
        Long telegramUserId,
        String correlationId,
        String scenario,
        String state,
        String purpose,
        String provider,
        String model,
        String profile,
        String prompt,
        String guestText,
        String approvedFallback,
        String responseText,
        boolean generated,
        boolean fallbackUsed,
        boolean success,
        String errorType,
        String errorMessage,
        Duration latency,
        Map<String, Object> metadata
) {
    public ModelInteractionAuditRecord {
        if (metadata == null) {
            metadata = Map.of();
        }
    }
}
