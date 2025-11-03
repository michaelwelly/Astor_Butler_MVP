package museon_online.astor_butler.alisa;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import museon_online.astor_butler.alisa.dto.AlisaRequest;
import museon_online.astor_butler.alisa.dto.AlisaResponse;
import museon_online.astor_butler.alisa.exception.AlisaClientException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
@Slf4j
public class AlisaClient {

    private final RestTemplate restTemplate;

    @Value("${yandex.ai.endpoint}")
    private String endpoint;

    @Value("${yandex.ai.api-key}")
    private String apiKey;

    public String ask(String prompt) {
        try {
            // 1) Собираем тело запроса под Яндекс LLM
            var body = new AlisaRequest(prompt);
            // 2) Заголовки: тип и авторизация
            var headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Api-Key " + apiKey);
            // 3) Собираем HTTP-запрос
            var entity = new HttpEntity<>(body, headers);
            log.debug("Sending prompt to Yandex AI: {}", prompt);
            // 4) Шлём POST на endpoint и маппим ответ в AlisaResponse
            ResponseEntity<AlisaResponse> response = restTemplate.exchange(
                    endpoint, HttpMethod.POST, entity, AlisaResponse.class);
            // 5) Проверяем, что тело не пустое
            if (response.getBody() == null || response.getBody().result() == null) {
                throw new AlisaClientException("Empty response from Yandex AI");
            }
            // 6) Достаём первый вариант ответа и возвращаем текст
            var text = response.getBody().result().alternatives().get(0).message().text();
            log.info("Received LLM response: {}", text);
            return text;

        } catch (Exception e) {
            // 7) Любая ошибка — логируем и заворачиваем в своё исключение
            log.error("Failed to call Yandex AI", e);
            throw new AlisaClientException("Yandex AI call failed: " + e.getMessage());
        }
    }
}
