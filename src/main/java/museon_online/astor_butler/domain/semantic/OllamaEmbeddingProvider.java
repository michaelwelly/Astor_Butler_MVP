package museon_online.astor_butler.domain.semantic;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(prefix = "astor.semantic-memory.embeddings", name = "provider", havingValue = "ollama")
@Slf4j
public class OllamaEmbeddingProvider implements EmbeddingProvider {

    private final RestClient restClient;
    private final String model;

    public OllamaEmbeddingProvider(
            @Value("${astor.semantic-memory.embeddings.ollama-base-url:${llm.ollama.base-url:http://localhost:11434}}") String baseUrl,
            @Value("${astor.semantic-memory.embeddings.model:nomic-embed-text}") String model
    ) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl == null || baseUrl.isBlank() ? "http://localhost:11434" : baseUrl)
                .build();
        this.model = model == null || model.isBlank() ? "nomic-embed-text" : model;
    }

    @Override
    public String model() {
        return model;
    }

    @Override
    public List<Double> embed(String text) {
        String input = text == null ? "" : text;
        JsonNode response = restClient.post()
                .uri("/api/embed")
                .body(Map.of("model", model, "input", input))
                .retrieve()
                .body(JsonNode.class);

        List<Double> embedding = readEmbedResponse(response);
        if (embedding.isEmpty()) {
            log.warn("Ollama embedding response was empty for model={}", model);
        }
        return embedding;
    }

    private List<Double> readEmbedResponse(JsonNode response) {
        if (response == null) {
            return List.of();
        }
        JsonNode embeddings = response.path("embeddings");
        if (embeddings.isArray() && embeddings.size() > 0) {
            return doubles(embeddings.get(0));
        }
        JsonNode embedding = response.path("embedding");
        if (embedding.isArray()) {
            return doubles(embedding);
        }
        return List.of();
    }

    private List<Double> doubles(JsonNode node) {
        List<Double> result = new ArrayList<>();
        if (node == null || !node.isArray()) {
            return result;
        }
        for (JsonNode value : node) {
            result.add(value.asDouble());
        }
        return result;
    }
}
