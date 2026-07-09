package museon_online.astor_butler.model;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.client.MockServerRestTemplateCustomizer;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;

import java.util.Map;

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
        MockServerRestTemplateCustomizer customizer = new MockServerRestTemplateCustomizer();
        YandexModelGateway gateway = new YandexModelGateway(
                new RestTemplateBuilder(customizer),
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
        MockRestServiceServer server = customizer.getServer();

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
}
