package museon_online.astor_butler.llm;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import museon_online.astor_butler.model.ModelCapability;
import museon_online.astor_butler.model.ModelEmbeddingResponse;
import museon_online.astor_butler.model.ModelVisionResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
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

    @Value("${llm.ollama.frontline-model:${llm.ollama.model}}")
    private String frontlineModel;

    @Value("${llm.ollama.quality-model:${llm.ollama.model}}")
    private String qualityModel;

    @Value("${llm.ollama.vision-model:qwen2.5vl:3b}")
    private String visionModel;

    @Value("${llm.ollama.keep-alive:30m}")
    private String keepAlive;

    public String ask(String prompt) {
        return ask(prompt, frontlineModel);
    }

    public String ask(String prompt, String modelName) {
        Map<String, Object> body = Map.of(
                "model", modelName,
                "prompt", prompt,
                "stream", false,
                "keep_alive", keepAlive,
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

    public ModelEmbeddingResponse embed(String text, String embeddingModel) {
        String modelName = embeddingModel == null || embeddingModel.isBlank() ? "nomic-embed-text" : embeddingModel;
        long startedAt = System.nanoTime();
        Map<String, Object> body = Map.of(
                "model", modelName,
                "input", text == null ? "" : text,
                "keep_alive", keepAlive
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl + "/api/embed",
                HttpMethod.POST,
                entity,
                Map.class
        );

        Duration latency = Duration.ofNanos(System.nanoTime() - startedAt);
        return new ModelEmbeddingResponse(
                readEmbedding(response.getBody()),
                "ollama-raw",
                modelName,
                ModelCapability.EMBEDDING,
                latency,
                false,
                Map.of()
        );
    }

    public ModelVisionResponse analyzeImage(String prompt, String imageBase64, String requestedModel) {
        String modelName = requestedModel == null || requestedModel.isBlank() ? visionModel : requestedModel;
        long startedAt = System.nanoTime();
        Map<String, Object> message = Map.of(
                "role", "user",
                "content", prompt == null ? "" : prompt,
                "images", List.of(imageBase64 == null ? "" : imageBase64)
        );
        Map<String, Object> body = Map.of(
                "model", modelName,
                "messages", List.of(message),
                "stream", false,
                "keep_alive", keepAlive
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl + "/api/chat",
                HttpMethod.POST,
                entity,
                Map.class
        );

        Duration latency = Duration.ofNanos(System.nanoTime() - startedAt);
        return ModelVisionResponse.vision(readVisionText(response.getBody()), "ollama-raw", modelName, latency);
    }

    private List<Double> readEmbedding(Map<?, ?> body) {
        if (body == null) {
            return List.of();
        }
        Object embeddings = body.get("embeddings");
        if (embeddings instanceof List<?> list && !list.isEmpty() && list.getFirst() instanceof List<?> first) {
            return doubles(first);
        }
        Object embedding = body.get("embedding");
        if (embedding instanceof List<?> list) {
            return doubles(list);
        }
        return List.of();
    }

    private String readVisionText(Map<?, ?> body) {
        if (body == null) {
            return "";
        }
        Object message = body.get("message");
        if (message instanceof Map<?, ?> map) {
            Object content = map.get("content");
            return content != null ? content.toString() : "";
        }
        Object response = body.get("response");
        return response != null ? response.toString() : "";
    }

    private List<Double> doubles(List<?> values) {
        List<Double> result = new ArrayList<>(values.size());
        for (Object value : values) {
            if (value instanceof Number number) {
                result.add(number.doubleValue());
            }
        }
        return result;
    }

    public String modelName() {
        return frontlineModel;
    }

    public String modelNameForQuality() {
        return qualityModel;
    }

    public String modelNameForVision() {
        return visionModel;
    }
}
