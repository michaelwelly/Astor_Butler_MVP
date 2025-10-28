package museon_online.astor_butler.deepseek;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.UUID;

@Service
public class GigaChatClient {

    @Value("${gigachat.auth-key}")
    private String authKey;

    private final RestTemplate restTemplate;
    private final ObjectMapper mapper = new ObjectMapper();

    public GigaChatClient() {
        this.restTemplate = new RestTemplate();
    }

    private String getAccessToken() {
        String url = "https://ngw.devices.sberbank.ru:9443/api/v2/oauth";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.add("Authorization", "Basic " + authKey);
        headers.add("RqUID", UUID.randomUUID().toString());

        HttpEntity<String> request = new HttpEntity<>("scope=GIGACHAT_API_PERS", headers);

        ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

        if (response.getStatusCode() == HttpStatus.OK) {
            try {
                JsonNode json = mapper.readTree(response.getBody());
                return json.get("access_token").asText();
            } catch (Exception e) {
                throw new RuntimeException("Ошибка парсинга токена GigaChat", e);
            }
        } else {
            throw new RuntimeException("Ошибка авторизации GigaChat: " + response.getStatusCode());
        }
    }

    public String generateText(String prompt) {
        String token = getAccessToken();
        String url = "https://gigachat.devices.sberbank.ru/api/v1/chat/completions";

        String body = """
        {
          "model": "GigaChat",
          "messages": [
            {"role": "system", "content": "Ты — дружелюбный AI-дворецкий, приветствуешь гостей по-разному, с теплом, лёгкостью и стилем."},
            {"role": "user", "content": "%s"}
          ]
        }
        """.formatted(prompt);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.add("Authorization", "Bearer " + token);

        HttpEntity<String> request = new HttpEntity<>(body, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

        if (response.getStatusCode() == HttpStatus.OK) {
            try {
                JsonNode json = mapper.readTree(response.getBody());
                return json.get("choices").get(0).get("message").get("content").asText();
            } catch (Exception e) {
                throw new RuntimeException("Ошибка парсинга ответа GigaChat", e);
            }
        } else {
            throw new RuntimeException("Ошибка при обращении к GigaChat API: " + response.getStatusCode());
        }
    }
}