package museon_online.astor_butler.domain.semantic;

public record IntentExampleSeed(
        String venueCode,
        String scenarioId,
        String state,
        String intent,
        String phrase,
        String normalizedPhrase,
        String expectedSlotsJson,
        String source,
        String locale,
        double weight
) {
    public IntentExampleSeed {
        venueCode = blankToDefault(venueCode, "AERIS");
        scenarioId = blankToDefault(scenarioId, "UNSPECIFIED");
        normalizedPhrase = blankToDefault(normalizedPhrase, SemanticTextNormalizer.normalize(phrase));
        expectedSlotsJson = blankToDefault(expectedSlotsJson, "{}");
        source = blankToDefault(source, "GOLDEN_CORPUS");
        locale = blankToDefault(locale, "ru");
        weight = weight <= 0 ? 1.0 : weight;
    }

    private static String blankToDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
