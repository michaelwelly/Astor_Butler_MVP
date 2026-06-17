package museon_online.astor_butler.domain.semantic;

import java.math.BigDecimal;
import java.util.UUID;

public record IntentExample(
        UUID exampleId,
        String venueCode,
        String scenarioId,
        String state,
        String intent,
        String phrase,
        String normalizedPhrase,
        String expectedSlotsJson,
        String source,
        String locale,
        String status,
        BigDecimal weight
) {
}
