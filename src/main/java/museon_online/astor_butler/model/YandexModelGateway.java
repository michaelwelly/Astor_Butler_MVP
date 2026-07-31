package museon_online.astor_butler.model;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@ConditionalOnProperty(prefix = "astor.model", name = "provider", havingValue = "yandex", matchIfMissing = false)
public class YandexModelGateway implements ModelGateway {

    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final String folderId;
    private final String apiKey;
    private final String iamToken;
    private final String frontlineModel;
    private final String qualityModel;
    private final int maxTokens;
    private final double temperature;

    public YandexModelGateway(
            RestTemplateBuilder restTemplateBuilder,
            @Value("${yandex.ai.base-url:https://llm.api.cloud.yandex.net}") String baseUrl,
            @Value("${yandex.ai.folder-id:}") String folderId,
            @Value("${yandex.ai.api-key:}") String apiKey,
            @Value("${yandex.ai.iam-token:}") String iamToken,
            @Value("${yandex.ai.model:yandexgpt-5-lite}") String frontlineModel,
            @Value("${yandex.ai.quality-model:yandexgpt-5.1}") String qualityModel,
            @Value("${yandex.ai.timeout-ms:8000}") int timeoutMs,
            @Value("${yandex.ai.max-tokens:256}") int maxTokens,
            @Value("${yandex.ai.temperature:0.1}") double temperature
    ) {
        this.restTemplate = restTemplateBuilder
                .connectTimeout(Duration.ofMillis(Math.max(1, timeoutMs)))
                .readTimeout(Duration.ofMillis(Math.max(1, timeoutMs)))
                .build();
        this.baseUrl = trimTrailingSlash(baseUrl);
        this.folderId = blankToNull(folderId);
        this.apiKey = blankToNull(apiKey);
        this.iamToken = blankToNull(iamToken);
        this.frontlineModel = blankToNull(frontlineModel) == null ? "yandexgpt-5-lite" : frontlineModel.trim();
        this.qualityModel = blankToNull(qualityModel) == null ? "yandexgpt-5.1" : qualityModel.trim();
        this.maxTokens = Math.max(1, maxTokens);
        this.temperature = Math.max(0.0, Math.min(1.0, temperature));
    }

    @Override
    public ModelTextResponse generateText(ModelTextRequest request) {
        String model = request.profile() == ModelProfile.QUALITY ? qualityModel : frontlineModel;
        String modelUri = modelUri(model);
        long startedAt = System.nanoTime();

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl + "/foundationModels/v1/completion",
                HttpMethod.POST,
                new HttpEntity<>(requestBody(request, modelUri), headers()),
                Map.class
        );

        Duration latency = Duration.ofNanos(System.nanoTime() - startedAt);
        Map<?, ?> body = response.getBody() == null ? Map.of() : response.getBody();
        Map<?, ?> result = resultBody(body);
        String text = readText(result);
        log.debug(
                "ModelGateway text generation provider=yandex-ai profile={} model={} scenario={} state={} purpose={} latencyMs={}",
                request.profile(),
                modelUri,
                request.scenario(),
                request.state(),
                request.purpose(),
                latency.toMillis()
        );
        return new ModelTextResponse(
                text,
                "yandex-ai",
                modelUri,
                ModelCapability.TEXT_GENERATION,
                latency,
                false,
                Map.of(
                        "usage", result.containsKey("usage") ? result.get("usage") : Map.of(),
                        "modelVersion", String.valueOf(result.containsKey("modelVersion") ? result.get("modelVersion") : "")
                )
        );
    }

    @Override
    public ModelEmbeddingResponse generateEmbedding(ModelEmbeddingRequest request) {
        return new ModelEmbeddingResponse(
                List.of(),
                "yandex-ai",
                request.model() == null ? "" : request.model(),
                ModelCapability.EMBEDDING,
                Duration.ZERO,
                true,
                Map.of("reason", "Yandex embedding provider is not wired through ModelGateway yet")
        );
    }

    @Override
    public ModelVisionResponse analyzeImage(ModelVisionRequest request) {
        return new ModelVisionResponse(
                "",
                "yandex-ai",
                request.model() == null ? "" : request.model(),
                ModelCapability.IMAGE_UNDERSTANDING,
                Duration.ZERO,
                true,
                Map.of("reason", "Yandex vision provider is not wired through ModelGateway yet")
        );
    }

    private Map<String, Object> requestBody(ModelTextRequest request, String modelUri) {
        Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("modelUri", modelUri);
        body.put("completionOptions", Map.of(
                "stream", false,
                "temperature", temperature,
                "maxTokens", String.valueOf(maxTokens),
                "reasoningOptions", Map.of("mode", "DISABLED")
        ));
        body.put("messages", List.of(Map.of(
                "role", "user",
                "text", request.prompt() == null ? "" : request.prompt()
        )));
        if (expectsJson(request)) {
            body.put("jsonObject", true);
        }
        return body;
    }

    private HttpHeaders headers() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (apiKey != null) {
            headers.set(HttpHeaders.AUTHORIZATION, "Api-Key " + apiKey);
            return headers;
        }
        if (iamToken != null) {
            headers.setBearerAuth(iamToken);
            return headers;
        }
        throw new IllegalStateException("Yandex AI credentials are not configured: set YANDEX_API_KEY or YANDEX_IAM_TOKEN");
    }

    private String modelUri(String model) {
        if (model.startsWith("gpt://")) {
            return model;
        }
        if (folderId == null) {
            throw new IllegalStateException("Yandex folder ID is required for model alias: set YANDEX_FOLDER_ID");
        }
        return "gpt://" + folderId + "/" + model;
    }

    private boolean expectsJson(ModelTextRequest request) {
        Object metadataFlag = request.metadata().get("jsonObject");
        if (metadataFlag instanceof Boolean flag) {
            return flag;
        }
        String purpose = request.purpose() == null ? "" : request.purpose().toLowerCase(java.util.Locale.ROOT);
        return purpose.contains("json");
    }

    private String readText(Map<?, ?> body) {
        Object alternatives = body.get("alternatives");
        if (!(alternatives instanceof List<?> list) || list.isEmpty()) {
            return "";
        }
        Object first = list.getFirst();
        if (!(first instanceof Map<?, ?> alternative)) {
            return "";
        }
        Object message = alternative.get("message");
        if (!(message instanceof Map<?, ?> messageMap)) {
            return "";
        }
        Object text = messageMap.get("text");
        return text == null ? "" : text.toString();
    }

    private Map<?, ?> resultBody(Map<?, ?> body) {
        Object result = body.get("result");
        if (result instanceof Map<?, ?> resultMap) {
            return resultMap;
        }
        return body;
    }

    private String trimTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            return "https://llm.api.cloud.yandex.net";
        }
        return value.trim().replaceFirst("/+$", "");
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
