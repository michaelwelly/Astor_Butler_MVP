package museon_online.astor_butler.domain.semantic;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class IntentExampleCorpusLoader {

    private final ObjectMapper objectMapper;

    public List<IntentExampleSeed> load(Resource resource) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            return reader.lines()
                    .filter(line -> !line.isBlank())
                    .map(this::parseLine)
                    .toList();
        } catch (Exception e) {
            throw new IllegalStateException("Cannot load intent example corpus from " + resource, e);
        }
    }

    private IntentExampleSeed parseLine(String line) {
        try {
            JsonNode json = objectMapper.readTree(line);
            String phrase = text(json, "text");
            String state = text(json, "state");
            String intent = text(json, "intent");
            Map<String, Object> slots = new LinkedHashMap<>();
            if (json.hasNonNull("slot")) {
                slots.put(json.get("slot").asText(), json.hasNonNull("slotValue") ? json.get("slotValue").asText() : true);
            }
            return new IntentExampleSeed(
                    textOr(json, "venueCode", "AERIS"),
                    scenarioFor(intent),
                    state,
                    intent,
                    phrase,
                    json.hasNonNull("normalized") ? SemanticTextNormalizer.normalize(json.get("normalized").asText()) : SemanticTextNormalizer.normalize(phrase),
                    objectMapper.writeValueAsString(slots),
                    "GOLDEN_CORPUS",
                    "ru",
                    1.0
            );
        } catch (Exception e) {
            throw new IllegalArgumentException("Cannot parse intent example line: " + line, e);
        }
    }

    private String scenarioFor(String intent) {
        if (intent == null) {
            return "UNKNOWN";
        }
        return switch (intent) {
            case "TABLE_BOOKING", "PROVIDE_DATE", "PROVIDE_TIME", "PROVIDE_PARTY_SIZE", "PROVIDE_TABLE_SELECTION" -> "TABLE_BOOKING";
            case "MENU_ASSETS" -> "MENU_ASSETS";
            case "QUIET_GUIDE" -> "QUIET_GUIDE";
            case "SAFE_PLAY" -> "SAFE_PLAY";
            case "EVENT_BOOKING" -> "EVENT_BOOKING";
            case "MANAGER_HELP" -> "MANAGER_HELP";
            case "FEEDBACK" -> "FEEDBACK";
            case "SMART_TIP" -> "SMART_TIP";
            case "HIDDEN_HEART" -> "HIDDEN_HEART";
            case "ART_AUCTION" -> "ART_AUCTION";
            case "MERCH" -> "MERCH";
            default -> "GENERAL";
        };
    }

    private String text(JsonNode json, String field) {
        if (!json.hasNonNull(field)) {
            throw new IllegalArgumentException("Missing field " + field);
        }
        return json.get(field).asText();
    }

    private String textOr(JsonNode json, String field, String fallback) {
        return json.hasNonNull(field) ? json.get(field).asText() : fallback;
    }
}
