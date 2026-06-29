package museon_online.astor_butler.domain.semantic;

import museon_online.astor_butler.model.ModelEmbeddingRequest;
import museon_online.astor_butler.model.ModelEmbeddingResponse;
import museon_online.astor_butler.model.ModelGateway;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ModelGatewayEmbeddingProviderTest {

    @Test
    void delegatesEmbeddingGenerationToModelGateway() {
        ModelGateway modelGateway = mock(ModelGateway.class);
        when(modelGateway.generateEmbedding(any(ModelEmbeddingRequest.class)))
                .thenReturn(ModelEmbeddingResponse.embedding(
                        List.of(0.1, 0.2, 0.3),
                        "test-provider",
                        "nomic-embed-text",
                        Duration.ofMillis(12)
                ));

        ModelGatewayEmbeddingProvider provider = new ModelGatewayEmbeddingProvider(modelGateway);
        ReflectionTestUtils.setField(provider, "model", "nomic-embed-text");

        List<Double> embedding = provider.embed("стол на завтра на двоих");

        assertThat(embedding).containsExactly(0.1, 0.2, 0.3);
        ArgumentCaptor<ModelEmbeddingRequest> request = ArgumentCaptor.forClass(ModelEmbeddingRequest.class);
        verify(modelGateway).generateEmbedding(request.capture());
        assertThat(request.getValue().text()).isEqualTo("стол на завтра на двоих");
        assertThat(request.getValue().model()).isEqualTo("nomic-embed-text");
        assertThat(request.getValue().scenario()).isEqualTo("SemanticMemory");
        assertThat(request.getValue().purpose()).isEqualTo("embedding");
    }
}
