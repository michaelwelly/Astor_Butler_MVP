package museon_online.astor_butler.domain.preference;

public record GuestPreferenceCommand(
        Long chatId,
        Long telegramUserId,
        Long userId,
        String venueCode,
        GuestPreferenceCategory category,
        String preferenceText,
        String capturedFromState,
        String correlationId,
        String metadataJson
) {
}
