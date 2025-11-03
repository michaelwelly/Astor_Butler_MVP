package museon_online.astor_butler.alisa.dto;

import java.util.List;
import java.util.Map;

public record AlisaRequest(String modelUri, Map<String, Object> completionOptions, List<Message> messages) {

    public AlisaRequest(String prompt) {
        this("gpt://b1glau5tifq7ttbeb2fa/yandexgpt/latest",
                Map.of("stream", false, "temperature", 0.9),
                List.of(new Message("user", prompt)));
    }

    public record Message(String role, String text) {}
}