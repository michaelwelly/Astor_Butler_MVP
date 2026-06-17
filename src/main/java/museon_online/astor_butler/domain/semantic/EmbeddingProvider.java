package museon_online.astor_butler.domain.semantic;

import java.util.List;

public interface EmbeddingProvider {

    String model();

    List<Double> embed(String text);
}
