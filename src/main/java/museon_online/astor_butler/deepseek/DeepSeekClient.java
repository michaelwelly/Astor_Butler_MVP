package museon_online.astor_butler.deepseek;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;


@Component
@RequiredArgsConstructor
public class DeepSeekClient {

    private final WebClient webClient = WebClient.builder()
            .baseUrl("https://api.deepseek.com/v1/chat/completions")
            .build();

    @Value("${deepseek.api-key}")
    private String apiKey;

//    public String ask(String prompt) {
//        try {
//            var response = webClient.post().header("Authorization", "Bearer " + apiKey)
//                    .contentType(MediaType.APPLICATION_JSON)
//                    .bodyValue(Map.of(
//                            "model", "deepseek-chat", "messages",
//                            List.of(Map.of(
//                                    "role", "user", "content", prompt))
//                    )).retrieve().bodyToMono(JsonNode.class).block();
//
//            return response.path("choices").get(0).path("message").path("content").asText();
//        } catch (Exception e) {
//            return "⚠\uFE0F DeepSeek is unavailable right now. Please try again later.";
//        }
//    }

    public Mono<String> askAsync(String prompt) {
        return webClient.post()
                .header("Authorization", "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of(
                        "model", "deepseek-chat",
                        "messages", List.of(Map.of("role", "user", "content", prompt))
                ))
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(json -> json.path("choices").get(0).path("message").path("content").asText())
                .onErrorReturn("⚠️ DeepSeek is unavailable right now.");
    }


}
