package museon_online.astor_butler.model;

import org.junit.jupiter.api.Test;
import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class YandexAiStudioAgentModelGatewayTest {

    @Test
    void generateTextUsesResponsesApiPromptIdForFreeFormReplies() {
        AtomicReference<MockRestServiceServer> serverRef = new AtomicReference<>();
        YandexAiStudioAgentModelGateway gateway = new YandexAiStudioAgentModelGateway(
                new RestTemplateBuilder(restTemplate ->
                        serverRef.set(MockRestServiceServer.bindTo(restTemplate).build())),
                "https://llm.test",
                "https://ai.test/v1",
                "folder-123",
                "api-key-123",
                "",
                "yandexgpt-5-lite",
                "yandexgpt-5.1",
                "fvt18kmmnas336paia3g",
                "yandexgpt/rc",
                8000,
                128,
                0.1
        );
        MockRestServiceServer server = serverRef.get();

        server.expect(once(), requestTo("https://ai.test/v1/responses"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Api-Key api-key-123"))
                .andExpect(header("OpenAI-Project", "folder-123"))
                .andExpect(jsonPath("$.model").value("gpt://folder-123/yandexgpt/rc"))
                .andExpect(jsonPath("$.prompt.id").value("fvt18kmmnas336paia3g"))
                .andExpect(jsonPath("$.input").value("Скажи привет"))
                .andExpect(jsonPath("$.max_output_tokens").value(128))
                .andRespond(withSuccess("""
                        {
                          "id": "resp-123",
                          "status": "completed",
                          "model": "gpt://folder-123/yandexgpt/rc",
                          "output": [
                            {
                              "type": "message",
                              "role": "assistant",
                              "content": [
                                {
                                  "type": "output_text",
                                  "text": "Привет. Я Astor Butler."
                                }
                              ]
                            }
                          ],
                          "usage": {
                            "input_tokens": 11,
                            "output_tokens": 7,
                            "total_tokens": 18
                          }
                        }
                        """, MediaType.APPLICATION_JSON));

        ModelTextResponse response = gateway.generateText(ModelTextRequest.of(
                "Скажи привет",
                "SCENARIO_REPLY",
                "READY_FOR_DIALOG",
                "guest-reply"
        ));

        assertThat(response.text()).isEqualTo("Привет. Я Astor Butler.");
        assertThat(response.provider()).isEqualTo("yandex-ai-studio-agent");
        assertThat(response.model()).isEqualTo("gpt://folder-123/yandexgpt/rc");
        assertThat(response.metadata()).containsEntry("agentId", "fvt18kmmnas336paia3g");
        assertThat(response.metadata()).containsEntry("responseId", "resp-123");
        server.verify();
    }

    @Test
    void generateTextKeepsJsonUnderstandingOnCompletionApi() {
        AtomicReference<MockRestServiceServer> serverRef = new AtomicReference<>();
        YandexAiStudioAgentModelGateway gateway = new YandexAiStudioAgentModelGateway(
                new RestTemplateBuilder(restTemplate ->
                        serverRef.set(MockRestServiceServer.bindTo(restTemplate).build())),
                "https://llm.test",
                "https://ai.test/v1",
                "folder-123",
                "api-key-123",
                "",
                "yandexgpt-5-lite",
                "yandexgpt-5.1",
                "fvt18kmmnas336paia3g",
                "yandexgpt/rc",
                8000,
                128,
                0.1
        );
        MockRestServiceServer server = serverRef.get();

        server.expect(once(), requestTo("https://llm.test/foundationModels/v1/completion"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Api-Key api-key-123"))
                .andExpect(jsonPath("$.modelUri").value("gpt://folder-123/yandexgpt-5-lite"))
                .andExpect(jsonPath("$.jsonObject").value(true))
                .andRespond(withSuccess("""
                        {
                          "alternatives": [
                            {
                              "message": {
                                "role": "assistant",
                                "text": "{\\"intent\\":\\"TABLE_BOOKING\\",\\"confidence\\":0.91}"
                              }
                            }
                          ],
                          "usage": {
                            "inputTextTokens": "30",
                            "completionTokens": "10",
                            "totalTokens": "40"
                          },
                          "modelVersion": "direct-json"
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

        assertThat(response.text()).isEqualTo("{\"intent\":\"TABLE_BOOKING\",\"confidence\":0.91}");
        assertThat(response.provider()).isEqualTo("yandex-ai");
        assertThat(response.metadata()).containsEntry("routedVia", "foundationModels-json");
        server.verify();
    }
}
