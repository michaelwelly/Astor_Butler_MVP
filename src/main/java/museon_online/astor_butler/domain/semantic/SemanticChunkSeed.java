package museon_online.astor_butler.domain.semantic;

import java.util.Map;

public record SemanticChunkSeed(
        String sourceCode,
        String chunkKey,
        int chunkIndex,
        String languageCode,
        String title,
        String content,
        Map<String, ?> metadata
) {
}
