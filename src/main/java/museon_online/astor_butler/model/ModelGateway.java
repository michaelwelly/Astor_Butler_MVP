package museon_online.astor_butler.model;

public interface ModelGateway {

    ModelTextResponse generateText(ModelTextRequest request);

    ModelEmbeddingResponse generateEmbedding(ModelEmbeddingRequest request);

    ModelVisionResponse analyzeImage(ModelVisionRequest request);
}
