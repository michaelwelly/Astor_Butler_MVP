package museon_online.astor_butler.domain.semantic;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "astor.semantic-memory.chunks", name = "ingest-on-startup", havingValue = "true")
public class SemanticMemoryBootstrap implements ApplicationRunner {

    private final ResourcePatternResolver resourcePatternResolver;
    private final SemanticMarkdownChunkLoader chunkLoader;
    private final SemanticMemoryRepository repository;
    private final ObjectProvider<EmbeddingProvider> embeddingProvider;

    @Value("${astor.semantic-memory.chunks.seed-location:classpath*:semantic/**/*.md}")
    private String seedLocation;

    @Override
    public void run(ApplicationArguments args) {
        EmbeddingProvider provider = embeddingProvider.getIfAvailable();
        int chunks = 0;
        int embeddings = 0;
        try {
            for (Resource resource : resourcePatternResolver.getResources(seedLocation)) {
                List<SemanticChunkSeed> seeds = chunkLoader.load(resource);
                for (SemanticChunkSeed seed : seeds) {
                    UUID chunkId = repository.upsertChunk(seed);
                    chunks++;
                    if (provider != null) {
                        List<Double> vector = provider.embed(seed.title() + "\n" + seed.content());
                        if (!vector.isEmpty()) {
                            repository.upsertEmbedding(chunkId, provider.model(), vector);
                            embeddings++;
                        }
                    }
                }
            }
            log.info("Semantic RAG chunks bootstrapped: chunks={}, embeddings={}, seedLocation={}",
                    chunks, embeddings, seedLocation);
        } catch (IOException | RuntimeException e) {
            log.warn("Semantic RAG bootstrap skipped: seedLocation={} reason={}", seedLocation, e.toString());
        }
    }
}
