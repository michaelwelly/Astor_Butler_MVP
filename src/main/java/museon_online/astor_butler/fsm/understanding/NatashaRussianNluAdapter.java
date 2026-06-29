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
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(prefix = "astor.nlu.natasha", name = "enabled", havingValue = "true")
public class NatashaRussianNluAdapter implements RussianNluAdapter {

    private static final Logger log = LoggerFactory.getLogger(NatashaRussianNluAdapter.class);

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String url;

    public NatashaRussianNluAdapter(
            RestTemplateBuilder restTemplateBuilder,
            ObjectMapper objectMapper,
            @Value("${astor.nlu.natasha.url:http://localhost:8011/analyze}") String url,
            @Value("${astor.nlu.natasha.timeout-ms:500}") int timeoutMs
    ) {
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofMillis(timeoutMs))
                .setReadTimeout(Duration.ofMillis(timeoutMs))
                .build();
        this.objectMapper = objectMapper;
        this.url = url;
    }

    @Override
    public RussianNluResult analyze(String text, BotState currentState) {
        if (text == null || text.isBlank()) {
            return RussianNluResult.empty("natasha");
        }
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            String response = restTemplate.postForObject(url, new HttpEntity<>(Map.of("text", text), headers), String.class);
            if (response == null || response.isBlank()) {
                return RussianNluResult.empty("natasha");
            }
            JsonNode slotsNode = objectMapper.readTree(response).path("slots");
            if (!slotsNode.isArray()) {
                return RussianNluResult.empty("natasha");
            }
            List<RussianNluSlot> slots = new ArrayList<>();
            for (JsonNode slot : slotsNode) {
                String name = slot.path("name").asText("");
                String value = slot.path("value").asText("");
                if (!name.isBlank() && !value.isBlank()) {
                    slots.add(new RussianNluSlot(name, value, slot.path("confidence").asDouble(0.7), "natasha"));
                }
            }
            return new RussianNluResult("natasha", List.copyOf(slots));
        } catch (Exception ex) {
            log.debug("Natasha NLU unavailable, continuing with local understanding: {}", ex.getMessage());
            return RussianNluResult.empty("natasha");
        }
    }
}
