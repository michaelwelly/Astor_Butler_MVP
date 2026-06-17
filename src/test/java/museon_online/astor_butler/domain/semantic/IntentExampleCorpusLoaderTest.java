package museon_online.astor_butler.domain.semantic;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class IntentExampleCorpusLoaderTest {

    private final IntentExampleCorpusLoader loader = new IntentExampleCorpusLoader(new ObjectMapper());

    @Test
    void loadsGoldenCorpusAsDatabaseSeeds() {
        List<IntentExampleSeed> seeds = loader.load(new ClassPathResource("understanding/guest-input-golden-corpus.jsonl"));

        assertThat(seeds).isNotEmpty();
        assertThat(seeds)
                .anySatisfy(seed -> {
                    assertThat(seed.intent()).isEqualTo("TABLE_BOOKING");
                    assertThat(seed.scenarioId()).isEqualTo("TABLE_BOOKING");
                    assertThat(seed.normalizedPhrase()).contains("забронировать");
                })
                .anySatisfy(seed -> {
                    assertThat(seed.intent()).isEqualTo("PROVIDE_TIME");
                    assertThat(seed.expectedSlotsJson()).contains("time");
                });
    }
}
