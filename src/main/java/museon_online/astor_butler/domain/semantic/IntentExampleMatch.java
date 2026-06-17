package museon_online.astor_butler.domain.semantic;

import java.util.UUID;

public record IntentExampleMatch(
        UUID exampleId,
        String scenarioId,
        String state,
        String intent,
        String phrase,
        String normalizedPhrase,
        String expectedSlotsJson,
        double score
) {
}
