package museon_online.astor_butler.model;

import org.junit.jupiter.api.Test;
import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;

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
                0.0
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
                0.0
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
    void generateEmbeddingUsesYandexTextEmbeddingContractAndParsesVector() {
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
                0.0
        );
        MockRestServiceServer server = serverRef.get();

        server.expect(once(), requestTo("https://llm.test/foundationModels/v1/textEmbedding"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Api-Key api-key-123"))
                .andExpect(jsonPath("$.modelUri").value("emb://folder-123/text-search-doc/latest"))
                .andExpect(jsonPath("$.text").value("AERIS - гастробар"))
                .andRespond(withSuccess("""
                        {
                          "embedding": [0.11, -0.22, 0.33],
                          "numTokens": "7"
                        }
                        """, MediaType.APPLICATION_JSON));

        ModelEmbeddingResponse response = gateway.generateEmbedding(ModelEmbeddingRequest.of(
                "AERIS - гастробар",
                "text-search-doc/latest",
                "SemanticMemory",
                null,
                "embedding-document"
        ));

        assertThat(response.embedding()).containsExactlyElementsOf(List.of(0.11, -0.22, 0.33));
        assertThat(response.provider()).isEqualTo("yandex-ai");
        assertThat(response.model()).isEqualTo("emb://folder-123/text-search-doc/latest");
        assertThat(response.fallback()).isFalse();
        assertThat(response.metadata().get("dimension")).isEqualTo(3);
        server.verify();
    }

    @Test
    void generateEmbeddingUsesQueryModelWhenModelAliasIsBlankAndPurposeIsQuery() {
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
                0.0
        );
        MockRestServiceServer server = serverRef.get();

        server.expect(once(), requestTo("https://llm.test/foundationModels/v1/textEmbedding"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.modelUri").value("emb://folder-123/text-search-query/latest"))
                .andRespond(withSuccess("""
                        {"result": {"embedding": ["0.1", "0.2"]}}
                        """, MediaType.APPLICATION_JSON));

        ModelEmbeddingResponse response = gateway.generateEmbedding(ModelEmbeddingRequest.of(
                "винная карта",
                "",
                "SemanticMemory",
                null,
                "embedding-query"
        ));

        assertThat(response.embedding()).containsExactly(0.1, 0.2);
        assertThat(response.model()).isEqualTo("emb://folder-123/text-search-query/latest");
        server.verify();
    }
}
