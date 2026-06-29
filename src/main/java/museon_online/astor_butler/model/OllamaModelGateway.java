package museon_online.astor_butler.model;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import museon_online.astor_butler.llm.OllamaClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
@ConditionalOnProperty(prefix = "astor.model", name = "provider", havingValue = "ollama-raw", matchIfMissing = false)
@RequiredArgsConstructor
public class OllamaModelGateway implements ModelGateway {

    private final OllamaClient ollamaClient;

    @Override
    public ModelTextResponse generateText(ModelTextRequest request) {
        long startedAt = System.nanoTime();
        String modelName = switch (request.profile()) {
            case QUALITY -> ollamaClient.modelNameForQuality();
            case FRONTLINE -> ollamaClient.modelName();
        };
        String text = ollamaClient.ask(request.prompt(), modelName);
        Duration latency = Duration.ofNanos(System.nanoTime() - startedAt);
        log.debug(
                "ModelGateway text generation provider=ollama profile={} model={} scenario={} state={} purpose={} latencyMs={}",
                request.profile(),
                modelName,
                request.scenario(),
                request.state(),
                request.purpose(),
                latency.toMillis()
        );
        return ModelTextResponse.text(text, "ollama", modelName, latency);
    }

    @Override
    public ModelEmbeddingResponse generateEmbedding(ModelEmbeddingRequest request) {
        String modelName = request.model() == null || request.model().isBlank()
                ? "nomic-embed-text"
                : request.model();
        return ollamaClient.embed(request.text(), modelName);
    }

    @Override
    public ModelVisionResponse analyzeImage(ModelVisionRequest request) {
        long startedAt = System.nanoTime();
        String modelName = request.model() == null || request.model().isBlank()
                ? ollamaClient.modelNameForVision()
                : request.model();
        ModelVisionResponse response = ollamaClient.analyzeImage(request.prompt(), request.imageBase64(), modelName);
        Duration latency = Duration.ofNanos(System.nanoTime() - startedAt);
        log.debug(
                "ModelGateway vision provider=ollama model={} scenario={} state={} purpose={} latencyMs={}",
                modelName,
                request.scenario(),
                request.state(),
                request.purpose(),
                latency.toMillis()
        );
        return response;
    }
}
