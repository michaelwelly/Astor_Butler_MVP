package museon_online.astor_butler.model;

import lombok.extern.slf4j.Slf4j;
import museon_online.astor_butler.llm.OllamaClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@ConditionalOnProperty(prefix = "astor.model", name = "provider", havingValue = "spring-ai", matchIfMissing = true)
public class SpringAiOllamaModelGateway implements ModelGateway {

    private final OllamaChatModel chatModel;
    private final OllamaEmbeddingModel embeddingModel;
    private final OllamaClient fallbackClient;
    private final String keepAlive;
    private final String defaultEmbeddingModel;

    public SpringAiOllamaModelGateway(
            @Value("${llm.ollama.base-url}") String baseUrl,
            @Value("${llm.ollama.model}") String defaultModel,
            @Value("${astor.semantic-memory.embeddings.model:nomic-embed-text}") String defaultEmbeddingModel,
            @Value("${llm.ollama.keep-alive:30m}") String keepAlive,
            OllamaClient fallbackClient
    ) {
        this.keepAlive = keepAlive;
        this.fallbackClient = fallbackClient;
        this.defaultEmbeddingModel = defaultEmbeddingModel == null || defaultEmbeddingModel.isBlank()
                ? "nomic-embed-text"
                : defaultEmbeddingModel;
        OllamaApi ollamaApi = new OllamaApi(baseUrl);
        this.chatModel = OllamaChatModel.builder()
                .ollamaApi(ollamaApi)
                .defaultOptions(OllamaOptions.builder()
                        .model(defaultModel)
                        .keepAlive(keepAlive)
                        .numPredict(50)
                        .temperature(0.2)
                        .topP(0.9)
                        .build())
                .build();
        this.embeddingModel = OllamaEmbeddingModel.builder()
                .ollamaApi(ollamaApi)
                .defaultOptions(OllamaOptions.builder()
                        .model(this.defaultEmbeddingModel)
                        .keepAlive(keepAlive)
                        .build())
                .build();
    }

    @Override
    public ModelTextResponse generateText(ModelTextRequest request) {
        String modelName = switch (request.profile()) {
            case QUALITY -> fallbackClient.modelNameForQuality();
            case FRONTLINE -> fallbackClient.modelName();
        };

        long startedAt = System.nanoTime();
        try {
            ChatResponse response = chatModel.call(new Prompt(
                    request.prompt(),
                    OllamaOptions.builder()
                            .model(modelName)
                            .keepAlive(keepAlive)
                            .numPredict(50)
                            .temperature(0.2)
                            .topP(0.9)
                            .build()
            ));
            Duration latency = Duration.ofNanos(System.nanoTime() - startedAt);
            String text = response != null && response.getResult() != null
                    ? response.getResult().getOutput().getText()
                    : "";
            log.debug(
                    "ModelGateway text generation provider=spring-ai-ollama profile={} model={} scenario={} state={} purpose={} latencyMs={}",
                    request.profile(),
                    modelName,
                    request.scenario(),
                    request.state(),
                    request.purpose(),
                    latency.toMillis()
            );
            return ModelTextResponse.text(text, "spring-ai-ollama", modelName, latency);
        } catch (RuntimeException ex) {
            log.warn(
                    "Spring AI Ollama generation failed; falling back to raw Ollama provider. profile={} model={} scenario={} state={} purpose={} reason={}",
                    request.profile(),
                    modelName,
                    request.scenario(),
                    request.state(),
                    request.purpose(),
                    ex.toString()
            );
            String text = fallbackClient.ask(request.prompt(), modelName);
            Duration latency = Duration.ofNanos(System.nanoTime() - startedAt);
            return new ModelTextResponse(
                    text,
                    "ollama-raw-fallback",
                    modelName,
                    ModelCapability.TEXT_GENERATION,
                    latency,
                    true,
                    request.metadata()
            );
        }
    }

    @Override
    public ModelEmbeddingResponse generateEmbedding(ModelEmbeddingRequest request) {
        String modelName = request.model() == null || request.model().isBlank()
                ? defaultEmbeddingModel
                : request.model();
        long startedAt = System.nanoTime();
        try {
            float[] raw = embeddingModel.embed(request.text() == null ? "" : request.text());
            Duration latency = Duration.ofNanos(System.nanoTime() - startedAt);
            List<Double> embedding = toDoubles(raw);
            log.debug(
                    "ModelGateway embedding provider=spring-ai-ollama model={} scenario={} state={} purpose={} dimension={} latencyMs={}",
                    modelName,
                    request.scenario(),
                    request.state(),
                    request.purpose(),
                    embedding.size(),
                    latency.toMillis()
            );
            return ModelEmbeddingResponse.embedding(embedding, "spring-ai-ollama", modelName, latency);
        } catch (RuntimeException ex) {
            log.warn(
                    "Spring AI Ollama embedding failed; falling back to raw Ollama embedding provider. model={} scenario={} state={} purpose={} reason={}",
                    modelName,
                    request.scenario(),
                    request.state(),
                    request.purpose(),
                    ex.toString()
            );
            ModelEmbeddingResponse fallback = fallbackClient.embed(request.text(), modelName);
            Duration latency = Duration.ofNanos(System.nanoTime() - startedAt);
            return new ModelEmbeddingResponse(
                    fallback.embedding(),
                    "ollama-raw-fallback",
                    modelName,
                    ModelCapability.EMBEDDING,
                    latency,
                    true,
                    request.metadata()
            );
        }
    }

    @Override
    public ModelVisionResponse analyzeImage(ModelVisionRequest request) {
        String modelName = request.model() == null || request.model().isBlank()
                ? fallbackClient.modelNameForVision()
                : request.model();
        long startedAt = System.nanoTime();
        try {
            ModelVisionResponse response = fallbackClient.analyzeImage(
                    request.prompt(),
                    request.imageBase64(),
                    modelName
            );
            Duration latency = Duration.ofNanos(System.nanoTime() - startedAt);
            log.debug(
                    "ModelGateway vision provider=ollama-raw-via-spring-ai-gateway model={} scenario={} state={} purpose={} latencyMs={}",
                    modelName,
                    request.scenario(),
                    request.state(),
                    request.purpose(),
                    latency.toMillis()
            );
            return new ModelVisionResponse(
                    response.text(),
                    "ollama-raw-via-spring-ai-gateway",
                    modelName,
                    ModelCapability.IMAGE_UNDERSTANDING,
                    latency,
                    false,
                    request.metadata()
            );
        } catch (RuntimeException ex) {
            Duration latency = Duration.ofNanos(System.nanoTime() - startedAt);
            log.warn(
                    "ModelGateway vision failed. model={} scenario={} state={} purpose={} reason={}",
                    modelName,
                    request.scenario(),
                    request.state(),
                    request.purpose(),
                    ex.toString()
            );
            return new ModelVisionResponse(
                    "",
                    "ollama-raw-via-spring-ai-gateway",
                    modelName,
                    ModelCapability.IMAGE_UNDERSTANDING,
                    latency,
                    true,
                    request.metadata()
            );
        }
    }

    private List<Double> toDoubles(float[] values) {
        if (values == null || values.length == 0) {
            return List.of();
        }
        List<Double> result = new ArrayList<>(values.length);
        for (float value : values) {
            result.add((double) value);
        }
        return result;
    }
}
