package museon_online.astor_butler.domain.semantic;

import lombok.RequiredArgsConstructor;
import museon_online.astor_butler.model.ModelEmbeddingRequest;
import museon_online.astor_butler.model.ModelGateway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConditionalOnProperty(prefix = "astor.semantic-memory.embeddings", name = "provider", havingValue = "model-gateway")
@RequiredArgsConstructor
public class ModelGatewayEmbeddingProvider implements EmbeddingProvider {

    private final ModelGateway modelGateway;

    @Value("${astor.semantic-memory.embeddings.model:nomic-embed-text}")
    private String model;

    @Value("${astor.semantic-memory.embeddings.query-model:}")
    private String queryModel;

    @Override
    public String model() {
        return model == null || model.isBlank() ? "nomic-embed-text" : model;
    }

    @Override
    public List<Double> embed(String text) {
        return modelGateway.generateEmbedding(ModelEmbeddingRequest.of(
                text,
                model(),
                "SemanticMemory",
                null,
                "embedding-document"
        )).embedding();
    }

    @Override
    public List<Double> embedQuery(String text) {
        return modelGateway.generateEmbedding(ModelEmbeddingRequest.of(
                text,
                queryModel(),
                "SemanticMemory",
                null,
                "embedding-query"
        )).embedding();
    }

    private String queryModel() {
        return queryModel == null || queryModel.isBlank() ? model() : queryModel.trim();
    }
}
