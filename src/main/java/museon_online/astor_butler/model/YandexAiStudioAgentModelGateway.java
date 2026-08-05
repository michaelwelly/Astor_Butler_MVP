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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@ConditionalOnProperty(prefix = "astor.model", name = "provider", havingValue = "yandex-agent", matchIfMissing = false)
public class YandexAiStudioAgentModelGateway implements ModelGateway {

    private final RestTemplate restTemplate;
    private final String completionBaseUrl;
    private final String responsesBaseUrl;
    private final String folderId;
    private final String apiKey;
    private final String iamToken;
    private final String frontlineModel;
    private final String qualityModel;
    private final String agentId;
    private final String agentModel;
    private final int maxTokens;
    private final double temperature;

    public YandexAiStudioAgentModelGateway(
            RestTemplateBuilder restTemplateBuilder,
            @Value("${yandex.ai.base-url:https://llm.api.cloud.yandex.net}") String completionBaseUrl,
            @Value("${yandex.ai.responses-base-url:https://ai.api.cloud.yandex.net/v1}") String responsesBaseUrl,
            @Value("${yandex.ai.folder-id:}") String folderId,
            @Value("${yandex.ai.api-key:}") String apiKey,
            @Value("${yandex.ai.iam-token:}") String iamToken,
            @Value("${yandex.ai.model:yandexgpt-5-lite}") String frontlineModel,
            @Value("${yandex.ai.quality-model:yandexgpt-5.1}") String qualityModel,
            @Value("${yandex.ai.agent-id:fvt18kmmnas336paia3g}") String agentId,
            @Value("${yandex.ai.agent-model:${yandex.ai.model:yandexgpt-5-lite}}") String agentModel,
            @Value("${yandex.ai.timeout-ms:8000}") int timeoutMs,
            @Value("${yandex.ai.max-tokens:256}") int maxTokens,
            @Value("${yandex.ai.temperature:0.1}") double temperature
    ) {
        Duration requestTimeout = Duration.ofMillis(Math.max(1, timeoutMs));
        this.restTemplate = restTemplateBuilder
                .connectTimeout(requestTimeout)
                .readTimeout(requestTimeout)
                .build();
        this.completionBaseUrl = trimTrailingSlash(completionBaseUrl, "https://llm.api.cloud.yandex.net");
        this.responsesBaseUrl = trimTrailingSlash(responsesBaseUrl, "https://ai.api.cloud.yandex.net/v1");
        this.folderId = blankToNull(folderId);
        this.apiKey = blankToNull(apiKey);
        this.iamToken = blankToNull(iamToken);
        this.frontlineModel = blankToNull(frontlineModel) == null ? "yandexgpt-5-lite" : frontlineModel.trim();
        this.qualityModel = blankToNull(qualityModel) == null ? "yandexgpt-5.1" : qualityModel.trim();
        this.agentId = blankToNull(agentId);
        this.agentModel = blankToNull(agentModel) == null ? this.frontlineModel : agentModel.trim();
        this.maxTokens = Math.max(1, maxTokens);
        this.temperature = Math.max(0.0, Math.min(1.0, temperature));
    }

    @Override
    public ModelTextResponse generateText(ModelTextRequest request) {
        if (expectsJson(request)) {
            return generateStructuredText(request);
        }
        return generateAgentText(request);
    }

    @Override
    public ModelEmbeddingResponse generateEmbedding(ModelEmbeddingRequest request) {
        return new ModelEmbeddingResponse(
                List.of(),
                "yandex-ai-studio-agent",
                request.model() == null ? "" : request.model(),
                ModelCapability.EMBEDDING,
                Duration.ZERO,
                true,
                Map.of("reason", "AI Studio agent adapter is text-only; use ASTOR_MODEL_PROVIDER=yandex for Yandex embeddings")
        );
    }

    @Override
    public ModelVisionResponse analyzeImage(ModelVisionRequest request) {
        return new ModelVisionResponse(
                "",
                "yandex-ai-studio-agent",
                request.model() == null ? "" : request.model(),
                ModelCapability.IMAGE_UNDERSTANDING,
                Duration.ZERO,
                true,
                Map.of("reason", "AI Studio agent adapter is text-only")
        );
    }

    private ModelTextResponse generateAgentText(ModelTextRequest request) {
        if (agentId == null) {
            throw new IllegalStateException("Yandex AI Studio agent ID is not configured: set YANDEX_AGENT_ID");
        }
        String modelUri = modelUri(agentModel);
        long startedAt = System.nanoTime();
        ResponseEntity<Map> response = restTemplate.exchange(
                responsesBaseUrl + "/responses",
                HttpMethod.POST,
                new HttpEntity<>(agentRequestBody(request, modelUri), responsesHeaders()),
                Map.class
        );

        Duration latency = Duration.ofNanos(System.nanoTime() - startedAt);
        Map<?, ?> body = response.getBody() == null ? Map.of() : response.getBody();
        String text = readResponsesText(body);
        String status = stringValue(body.get("status"));
        log.debug(
                "ModelGateway text generation provider=yandex-ai-studio-agent agent={} model={} scenario={} state={} purpose={} status={} latencyMs={}",
                agentId,
                modelUri,
                request.scenario(),
                request.state(),
                request.purpose(),
                status,
                latency.toMillis()
        );
        return new ModelTextResponse(
                text,
                "yandex-ai-studio-agent",
                modelUri,
                ModelCapability.TEXT_GENERATION,
                latency,
                false,
                agentMetadata(body, status)
        );
    }

    private ModelTextResponse generateStructuredText(ModelTextRequest request) {
        String model = request.profile() == ModelProfile.QUALITY ? qualityModel : frontlineModel;
        String modelUri = modelUri(model);
        long startedAt = System.nanoTime();
        ResponseEntity<Map> response = restTemplate.exchange(
                completionBaseUrl + "/foundationModels/v1/completion",
                HttpMethod.POST,
                new HttpEntity<>(completionRequestBody(request, modelUri), completionHeaders()),
                Map.class
        );

        Duration latency = Duration.ofNanos(System.nanoTime() - startedAt);
        Map<?, ?> body = response.getBody() == null ? Map.of() : response.getBody();
        Map<?, ?> result = resultBody(body);
        return new ModelTextResponse(
                readCompletionText(result),
                "yandex-ai",
                modelUri,
                ModelCapability.TEXT_GENERATION,
                latency,
                false,
                structuredMetadata(result)
        );
    }

    private Map<String, Object> agentMetadata(Map<?, ?> body, String status) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("agentId", agentId);
        metadata.put("status", status);
        metadata.put("responseId", stringValue(body.get("id")));
        metadata.put("usage", nonNullOrEmptyMap(body.get("usage")));
        metadata.put("incompleteDetails", nonNullOrEmptyMap(body.get("incomplete_details")));
        return metadata;
    }

    private Map<String, Object> structuredMetadata(Map<?, ?> result) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("routedVia", "foundationModels-json");
        metadata.put("usage", nonNullOrEmptyMap(result.get("usage")));
        metadata.put("modelVersion", stringValue(result.get("modelVersion")));
        return metadata;
    }

    private Map<String, Object> agentRequestBody(ModelTextRequest request, String modelUri) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", modelUri);
        body.put("input", request.prompt() == null ? "" : request.prompt());
        body.put("max_output_tokens", maxTokens);
        body.put("temperature", temperature);
        Map<String, Object> prompt = new LinkedHashMap<>();
        prompt.put("id", agentId);
        Object variables = request.metadata().get("agentVariables");
        if (variables instanceof Map<?, ?> variableMap && !variableMap.isEmpty()) {
            prompt.put("variables", variableMap);
        }
        body.put("prompt", prompt);
        return body;
    }

    private Map<String, Object> completionRequestBody(ModelTextRequest request, String modelUri) {
        Map<String, Object> body = new LinkedHashMap<>();
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
        body.put("jsonObject", true);
        return body;
    }

    private HttpHeaders responsesHeaders() {
        HttpHeaders headers = completionHeaders();
        if (folderId != null) {
            headers.set("OpenAI-Project", folderId);
        }
        return headers;
    }

    private HttpHeaders completionHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(HttpHeaders.AUTHORIZATION, authorizationHeaderValue());
        return headers;
    }

    private String authorizationHeaderValue() {
        if (apiKey != null) {
            return "Api-Key " + apiKey;
        }
        if (iamToken != null) {
            return "Bearer " + iamToken;
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
        return "gpt://" + folderId + "/" + model.replaceFirst("^/+", "");
    }

    private boolean expectsJson(ModelTextRequest request) {
        Object metadataFlag = request.metadata().get("jsonObject");
        if (metadataFlag instanceof Boolean flag) {
            return flag;
        }
        String purpose = request.purpose() == null ? "" : request.purpose().toLowerCase(java.util.Locale.ROOT);
        return purpose.contains("json");
    }

    private String readResponsesText(Map<?, ?> body) {
        Object outputText = body.get("output_text");
        if (outputText instanceof String text && !text.isBlank()) {
            return text;
        }
        Object output = body.get("output");
        if (!(output instanceof List<?> items)) {
            return "";
        }
        List<String> chunks = new ArrayList<>();
        for (Object item : items) {
            if (!(item instanceof Map<?, ?> outputItem)) {
                continue;
            }
            Object content = outputItem.get("content");
            if (!(content instanceof List<?> contentItems)) {
                continue;
            }
            for (Object contentItem : contentItems) {
                if (!(contentItem instanceof Map<?, ?> contentMap)) {
                    continue;
                }
                Object text = contentMap.get("text");
                if (text != null && !text.toString().isBlank()) {
                    chunks.add(text.toString());
                }
            }
        }
        return String.join("\n", chunks).trim();
    }

    private String readCompletionText(Map<?, ?> body) {
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
        return stringValue(messageMap.get("text"));
    }

    private Map<?, ?> resultBody(Map<?, ?> body) {
        Object result = body.get("result");
        if (result instanceof Map<?, ?> resultMap) {
            return resultMap;
        }
        return body;
    }

    private String trimTrailingSlash(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim().replaceFirst("/+$", "");
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String stringValue(Object value) {
        return value == null ? "" : value.toString();
    }

    private Object nonNullOrEmptyMap(Object value) {
        return value == null ? Map.of() : value;
    }
}
