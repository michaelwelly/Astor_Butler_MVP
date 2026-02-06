package museon_online.astor_butler.llm;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class OllamaClient {

    private final RestTemplate restTemplate;

    @Value("${llm.ollama.base-url}")
    private String baseUrl;

    @Value("${llm.ollama.model}")
    private String model;

    public String ask(String prompt) {
        Map<String, Object> body = Map.of(
                "model", model,
                "prompt", prompt,
                "stream", false,
                "keep_alive", "5m",
                "options", Map.of(
                        "num_predict", 50,
                        "temperature", 0.2,
                        "top_p", 0.9
                )
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity =
                new HttpEntity<>(body, headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl + "/api/generate",
                HttpMethod.POST,
                entity,
                Map.class
        );

        Object text = response.getBody().get("response");
        return text != null ? text.toString() : "";
    }
}