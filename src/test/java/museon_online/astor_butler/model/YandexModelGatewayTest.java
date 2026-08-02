package museon_online.astor_butler.model;

import org.junit.jupiter.api.Test;
import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class YandexModelGatewayTest {

    @Test
    void generateTextUsesYandexCompletionContractAndParsesFirstAlternative() {
        AtomicReference<MockRestServiceServer> serverRef = new AtomicReference<>();
        YandexModelGateway gateway = new YandexModelGateway(
                new RestTemplateBuilder(restTemplate ->
                        serverRef.set(MockRestServiceServer.bindTo(restTemplate).build())),
                "https://llm.test",
                "folder-123",
                "api-key-123",
                "",
                "yandexgpt-5-lite",
                "yandexgpt-5.1",
                8000,
                128,
                0.0,
                1,
                250
        );
        MockRestServiceServer server = serverRef.get();

        server.expect(once(), requestTo("https://llm.test/foundationModels/v1/completion"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Api-Key api-key-123"))
                .andExpect(jsonPath("$.modelUri").value("gpt://folder-123/yandexgpt-5-lite"))
                .andExpect(jsonPath("$.completionOptions.stream").value(false))
                .andExpect(jsonPath("$.completionOptions.maxTokens").value("128"))
                .andExpect(jsonPath("$.jsonObject").value(true))
                .andRespond(withSuccess("""
                        {
                          "alternatives": [
                            {
                              "message": {
                                "role": "assistant",
                                "text": "{\\"intent\\":\\"TABLE_BOOKING\\"}"
                              },
                              "status": "ALTERNATIVE_STATUS_FINAL"
                            }
                          ],
                          "usage": {
                            "inputTextTokens": "42",
                            "completionTokens": "8",
                            "totalTokens": "50"
                          },
                          "modelVersion": "test-version"
                        }
                        """, MediaType.APPLICATION_JSON));

        ModelTextResponse response = gateway.generateText(new ModelTextRequest(
                "Верни JSON",
                "LLM_UNDERSTANDING",
                "READY_FOR_DIALOG",
                "intent-slots-json",
                ModelProfile.FRONTLINE,
                Map.of()
        ));

        assertThat(response.text()).isEqualTo("{\"intent\":\"TABLE_BOOKING\"}");
        assertThat(response.provider()).isEqualTo("yandex-ai");
        assertThat(response.model()).isEqualTo("gpt://folder-123/yandexgpt-5-lite");
        assertThat(response.metadata()).containsKey("usage");
        server.verify();
    }

    @Test
    void generateTextParsesNestedResultResponse() {
        AtomicReference<MockRestServiceServer> serverRef = new AtomicReference<>();
        YandexModelGateway gateway = new YandexModelGateway(
                new RestTemplateBuilder(restTemplate ->
                        serverRef.set(MockRestServiceServer.bindTo(restTemplate).build())),
                "https://llm.test",
                "folder-123",
                "api-key-123",
                "",
                "yandexgpt-5-lite",
                "yandexgpt-5.1",
                8000,
                128,
                0.0,
                1,
                250
        );
        MockRestServiceServer server = serverRef.get();

        server.expect(once(), requestTo("https://llm.test/foundationModels/v1/completion"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("""
                        {
                          "result": {
                            "alternatives": [
                              {
                                "message": {
                                  "role": "assistant",
                                  "text": "{\\"intent\\":\\"PRODUCT_INTEREST\\"}"
                                },
                                "status": "ALTERNATIVE_STATUS_FINAL"
                              }
                            ],
                            "usage": {
                              "inputTextTokens": "90",
                              "completionTokens": "12",
                              "totalTokens": "102"
                            },
                            "modelVersion": "nested-version"
                          }
                        }
                        """, MediaType.APPLICATION_JSON));

        ModelTextResponse response = gateway.generateText(new ModelTextRequest(
                "Верни JSON",
                "LLM_UNDERSTANDING",
                "READY_FOR_DIALOG",
                "intent-slots-json",
                ModelProfile.FRONTLINE,
                Map.of()
        ));

        assertThat(response.text()).isEqualTo("{\"intent\":\"PRODUCT_INTEREST\"}");
        assertThat(response.metadata().get("modelVersion")).isEqualTo("nested-version");
        assertThat(response.metadata().get("usage")).isEqualTo(Map.of(
                "inputTextTokens", "90",
                "completionTokens", "12",
                "totalTokens", "102"
        ));
        server.verify();
    }

    @Test
    void generateEmbeddingUsesYandexTextEmbeddingContractAndParsesVector() throws IOException {
        AtomicReference<String> requestBody = new AtomicReference<>();
        AtomicReference<String> authorization = new AtomicReference<>();
        HttpServer server = embeddingServer(requestBody, authorization, """
                {
                  "embedding": [0.11, -0.22, 0.33],
                  "numTokens": "7"
                }
                """);
        server.start();
        YandexModelGateway gateway = new YandexModelGateway(
                new RestTemplateBuilder(),
                "http://127.0.0.1:" + server.getAddress().getPort(),
                "folder-123",
                "api-key-123",
                "",
                "yandexgpt-5-lite",
                "yandexgpt-5.1",
                8000,
                128,
                0.0,
                1,
                250
        );
        try {
            ModelEmbeddingResponse response = gateway.generateEmbedding(ModelEmbeddingRequest.of(
                    "AERIS - гастробар",
                    "text-search-doc/latest",
                    "SemanticMemory",
                    null,
                    "embedding-document"
            ));

            assertThat(authorization.get()).isEqualTo("Api-Key api-key-123");
            assertThat(requestBody.get()).contains("\"modelUri\":\"emb://folder-123/text-search-doc/latest\"");
            assertThat(requestBody.get()).contains("\"text\":\"AERIS - гастробар\"");
            assertThat(response.embedding()).containsExactlyElementsOf(List.of(0.11, -0.22, 0.33));
            assertThat(response.provider()).isEqualTo("yandex-ai");
            assertThat(response.model()).isEqualTo("emb://folder-123/text-search-doc/latest");
            assertThat(response.fallback()).isFalse();
            assertThat(response.metadata().get("dimension")).isEqualTo(3);
        } finally {
            server.stop(0);
        }
    }

    @Test
    void generateEmbeddingUsesQueryModelWhenModelAliasIsBlankAndPurposeIsQuery() throws IOException {
        AtomicReference<String> requestBody = new AtomicReference<>();
        AtomicReference<String> authorization = new AtomicReference<>();
        HttpServer server = embeddingServer(requestBody, authorization, """
                {"result": {"embedding": ["0.1", "0.2"]}}
                """);
        server.start();
        YandexModelGateway gateway = new YandexModelGateway(
                new RestTemplateBuilder(),
                "http://127.0.0.1:" + server.getAddress().getPort(),
                "folder-123",
                "api-key-123",
                "",
                "yandexgpt-5-lite",
                "yandexgpt-5.1",
                8000,
                128,
                0.0,
                1,
                250
        );

        try {
            ModelEmbeddingResponse response = gateway.generateEmbedding(ModelEmbeddingRequest.of(
                    "винная карта",
                    "",
                    "SemanticMemory",
                    null,
                    "embedding-query"
            ));

            assertThat(authorization.get()).isEqualTo("Api-Key api-key-123");
            assertThat(requestBody.get()).contains("\"modelUri\":\"emb://folder-123/text-search-query/latest\"");
            assertThat(response.embedding()).containsExactly(0.1, 0.2);
            assertThat(response.model()).isEqualTo("emb://folder-123/text-search-query/latest");
        } finally {
            server.stop(0);
        }
    }

    private HttpServer embeddingServer(
            AtomicReference<String> requestBody,
            AtomicReference<String> authorization,
            String responseBody
    ) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/foundationModels/v1/textEmbedding", exchange -> {
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] response = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json,application/grpc");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        return server;
    }
}
