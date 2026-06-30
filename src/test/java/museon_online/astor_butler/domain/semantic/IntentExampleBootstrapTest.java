package museon_online.astor_butler.domain.semantic;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.ResourceAccessException;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IntentExampleBootstrapTest {

    @Test
    void doesNotFailApplicationWhenEmbeddingProviderIsUnavailable() {
        ResourceLoader resourceLoader = mock(ResourceLoader.class);
        IntentExampleCorpusLoader corpusLoader = mock(IntentExampleCorpusLoader.class);
        IntentExampleRepository repository = mock(IntentExampleRepository.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<EmbeddingProvider> providerHolder = mock(ObjectProvider.class);
        Resource resource = mock(Resource.class);
        EmbeddingProvider embeddingProvider = mock(EmbeddingProvider.class);

        IntentExampleSeed first = seed("на троих");
        IntentExampleSeed second = seed("на пятерых");

        when(resourceLoader.getResource("classpath:test-corpus.jsonl")).thenReturn(resource);
        when(corpusLoader.load(resource)).thenReturn(List.of(first, second));
        when(repository.upsert(any())).thenReturn(UUID.randomUUID());
        when(providerHolder.getIfAvailable()).thenReturn(embeddingProvider);
        when(embeddingProvider.model()).thenReturn("nomic-embed-text");
        when(embeddingProvider.embed(any())).thenThrow(new ResourceAccessException("llm-gateway"));

        IntentExampleBootstrap bootstrap = new IntentExampleBootstrap(
                resourceLoader,
                corpusLoader,
                repository,
                providerHolder
        );
        ReflectionTestUtils.setField(bootstrap, "corpusLocation", "classpath:test-corpus.jsonl");

        assertThatCode(() -> bootstrap.run(new DefaultApplicationArguments()))
                .doesNotThrowAnyException();

        verify(repository, times(2)).upsert(any(IntentExampleSeed.class));
        verify(embeddingProvider, times(1)).embed(any());
        verify(repository, never()).upsertEmbedding(any(), any(), any());
    }

    private IntentExampleSeed seed(String phrase) {
        return new IntentExampleSeed(
                "AERIS",
                "TABLE_BOOKING",
                "TABLE_BOOKING_COLLECT_PARTY_SIZE",
                "PARTY_SIZE",
                phrase,
                phrase,
                "{}",
                "TEST",
                "ru",
                1.0
        );
    }
}
