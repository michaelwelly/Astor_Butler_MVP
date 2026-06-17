package museon_online.astor_butler.domain.semantic;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

@Component
@ConditionalOnBean(EmbeddingModel.class)
@ConditionalOnProperty(prefix = "astor.semantic-memory.embeddings", name = "provider", havingValue = "spring-ai")
@Slf4j
public class SpringAiEmbeddingProvider implements EmbeddingProvider {

    private final EmbeddingModel embeddingModel;
    private final String model;

    public SpringAiEmbeddingProvider(
            EmbeddingModel embeddingModel,
            @Value("${astor.semantic-memory.embeddings.model:spring-ai-default}") String model
    ) {
        this.embeddingModel = embeddingModel;
        this.model = model == null || model.isBlank() ? "spring-ai-default" : model;
    }

    @Override
    public String model() {
        return model;
    }

    @Override
    public List<Double> embed(String text) {
        try {
            Method embed = embeddingModel.getClass().getMethod("embed", String.class);
            Object result = embed.invoke(embeddingModel, text == null ? "" : text);
            return toDoubles(result);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Spring AI EmbeddingModel does not expose embed(String)", e);
        }
    }

    private List<Double> toDoubles(Object value) {
        if (value instanceof float[] floats) {
            List<Double> result = new ArrayList<>(floats.length);
            for (float item : floats) {
                result.add((double) item);
            }
            return result;
        }
        if (value instanceof double[] doubles) {
            List<Double> result = new ArrayList<>(doubles.length);
            for (double item : doubles) {
                result.add(item);
            }
            return result;
        }
        if (value instanceof List<?> list) {
            List<Double> result = new ArrayList<>(list.size());
            for (Object item : list) {
                if (item instanceof Number number) {
                    result.add(number.doubleValue());
                }
            }
            return result;
        }
        log.warn("Unsupported Spring AI embedding result type: {}", value == null ? "null" : value.getClass().getName());
        return List.of();
    }
}
