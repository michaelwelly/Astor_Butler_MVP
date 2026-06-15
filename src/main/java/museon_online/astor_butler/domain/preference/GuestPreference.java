package museon_online.astor_butler.domain.preference;

import java.time.Instant;

public record GuestPreference(
        Long id,
        Long chatId,
        Long telegramUserId,
        Long userId,
        String venueCode,
        GuestPreferenceCategory category,
        String preferenceText,
        String source,
        GuestPreferenceStatus status,
        Double confidence,
        String capturedFromState,
        String correlationId,
        String metadataJson,
        Instant createdAt,
        Instant updatedAt
) {
}
