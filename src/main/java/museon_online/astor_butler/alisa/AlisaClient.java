package museon_online.astor_butler.alisa;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import museon_online.astor_butler.alisa.dto.AgentResponse;
import museon_online.astor_butler.alisa.exception.AlisaClientException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AlisaClient {

    private final RestTemplate restTemplate;

    @Value("${yandex.ai.agent-endpoint}")
    private String agentEndpoint;

    @Value("${yandex.ai.iam-token}")
    private String iamToken;

    @Value("${yandex.ai.folder-id}")
    private String folderId;

    public AgentResponse ask(String userText) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(iamToken);

            Map<String, Object> body = Map.of(
                    "input", Map.of(
                            "messages", java.util.List.of(
                                    Map.of(
                                            "role", "user",
                                            "text", userText
                                    )
                            )
                    )
            );

            log.info("🤖 [AI] Request body = {}", body);
            log.info("🤖 [AI] Endpoint = {}", agentEndpoint);

            HttpEntity<Map<String, Object>> entity =
                    new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    agentEndpoint,
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            log.info("🤖 [AI] Raw response = {}", response.getBody());

            if (response.getBody() == null) {
                throw new AlisaClientException("Empty response from agent");
            }

            Object result = response.getBody().get("result");
            if (!(result instanceof Map<?, ?> resultMap)) {
                throw new AlisaClientException("Invalid agent response structure: no result");
            }

            Object message = resultMap.get("message");
            if (!(message instanceof Map<?, ?> messageMap)) {
                throw new AlisaClientException("Invalid agent response structure: no message");
            }

            Object text = messageMap.get("text");
            if (!(text instanceof String textValue)) {
                throw new AlisaClientException("Invalid agent response structure: no text");
            }

            return new AgentResponse(textValue, null, null);

        } catch (Exception e) {
            log.error("Failed to call Astor Butler Agent", e);
            throw new AlisaClientException(e.getMessage());
        }
    }
}