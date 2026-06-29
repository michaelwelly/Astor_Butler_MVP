package museon_online.astor_butler.fsm.understanding;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import museon_online.astor_butler.fsm.core.BotState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

@Component
@ConditionalOnProperty(prefix = "astor.nlu.duckling", name = "enabled", havingValue = "true")
public class DucklingRussianNluAdapter implements RussianNluAdapter {

    private static final Logger log = LoggerFactory.getLogger(DucklingRussianNluAdapter.class);

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String url;
    private final String locale;
    private final String timezone;

    public DucklingRussianNluAdapter(
            RestTemplateBuilder restTemplateBuilder,
            ObjectMapper objectMapper,
            @Value("${astor.nlu.duckling.url:http://localhost:8000/parse}") String url,
            @Value("${astor.nlu.duckling.locale:ru_RU}") String locale,
            @Value("${astor.nlu.duckling.timezone:Asia/Yekaterinburg}") String timezone,
            @Value("${astor.nlu.duckling.timeout-ms:500}") int timeoutMs
    ) {
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofMillis(timeoutMs))
                .setReadTimeout(Duration.ofMillis(timeoutMs))
                .build();
        this.objectMapper = objectMapper;
        this.url = url;
        this.locale = locale;
        this.timezone = timezone;
    }

    @Override
    public RussianNluResult analyze(String text, BotState currentState) {
        if (text == null || text.isBlank()) {
            return RussianNluResult.empty("duckling");
        }
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("text", text);
            body.add("locale", locale);
            body.add("tz", timezone);

            String response = restTemplate.postForObject(url, new HttpEntity<>(body, headers), String.class);
            if (response == null || response.isBlank()) {
                return RussianNluResult.empty("duckling");
            }
            JsonNode root = objectMapper.readTree(response);
            if (!root.isArray()) {
                return RussianNluResult.empty("duckling");
            }
            List<RussianNluSlot> slots = new ArrayList<>();
            for (JsonNode entity : root) {
                mapEntity(entity, slots);
            }
            return new RussianNluResult("duckling", List.copyOf(slots));
        } catch (Exception ex) {
            log.debug("Duckling NLU unavailable, continuing with local understanding: {}", ex.getMessage());
            return RussianNluResult.empty("duckling");
        }
    }

    private void mapEntity(JsonNode entity, List<RussianNluSlot> slots) {
        String dimension = entity.path("dim").asText("");
        JsonNode value = entity.path("value");
        if ("time".equals(dimension)) {
            addTimeSlots(value.path("value").asText(""), slots);
            return;
        }
        if ("number".equals(dimension)) {
            String number = value.path("value").asText("");
            if (!number.isBlank()) {
                slots.add(new RussianNluSlot("number", trimDecimal(number), 0.72, "duckling"));
            }
        }
    }

    private void addTimeSlots(String raw, List<RussianNluSlot> slots) {
        if (raw == null || raw.isBlank()) {
            return;
        }
        try {
            OffsetDateTime parsed = OffsetDateTime.parse(raw);
            slots.add(new RussianNluSlot("time", "%02d:%02d".formatted(parsed.getHour(), parsed.getMinute()), 0.88, "duckling"));
            slots.add(new RussianNluSlot("date", parsed.toLocalDate().toString(), 0.82, "duckling"));
        } catch (DateTimeParseException ignored) {
            if (raw.matches("\\d{1,2}:\\d{2}.*")) {
                slots.add(new RussianNluSlot("time", raw.substring(0, 5), 0.78, "duckling"));
            }
        }
    }

    private String trimDecimal(String number) {
        return number.endsWith(".0") ? number.substring(0, number.length() - 2) : number;
    }
}
