package museon_online.astor_butler.llm;

import museon_online.astor_butler.model.ModelCapability;
import museon_online.astor_butler.model.ModelVisionResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OllamaClientTest {

    @Test
    void analyzeImageUsesOllamaChatImagesContract() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        OllamaClient client = new OllamaClient(restTemplate);
        ReflectionTestUtils.setField(client, "baseUrl", "http://llm-gateway:11434");
        ReflectionTestUtils.setField(client, "visionModel", "qwen2.5vl:3b");
        ReflectionTestUtils.setField(client, "keepAlive", "30m");

        when(restTemplate.exchange(
                eq("http://llm-gateway:11434/api/chat"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(Map.class)
        )).thenReturn(new ResponseEntity<>(
                Map.of("message", Map.of("content", "table 10 is marked")),
                HttpStatus.OK
        ));

        ModelVisionResponse response = client.analyzeImage("Что отмечено на плане?", "base64-image", null);

        assertThat(response.text()).isEqualTo("table 10 is marked");
        assertThat(response.model()).isEqualTo("qwen2.5vl:3b");
        assertThat(response.capability()).isEqualTo(ModelCapability.IMAGE_UNDERSTANDING);

        @SuppressWarnings("unchecked")
        var entityCaptor = org.mockito.ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).exchange(
                eq("http://llm-gateway:11434/api/chat"),
                eq(HttpMethod.POST),
                entityCaptor.capture(),
                eq(Map.class)
        );

        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) entityCaptor.getValue().getBody();
        assertThat(body).containsEntry("model", "qwen2.5vl:3b");
        assertThat(body).containsEntry("stream", false);
        assertThat(body).containsEntry("keep_alive", "30m");
        assertThat(body).containsKey("messages");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> messages = (List<Map<String, Object>>) body.get("messages");
        assertThat(messages).hasSize(1);
        assertThat(messages.getFirst()).containsEntry("role", "user");
        assertThat(messages.getFirst()).containsEntry("content", "Что отмечено на плане?");
        assertThat(messages.getFirst()).containsEntry("images", List.of("base64-image"));
    }
}
