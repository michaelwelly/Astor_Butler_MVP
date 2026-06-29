package museon_online.astor_butler.model;

import museon_online.astor_butler.llm.OllamaClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class ModelGatewayProviderTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withBean(OllamaClient.class, () -> {
                OllamaClient client = mock(OllamaClient.class);
                return client;
            })
            .withUserConfiguration(SpringAiOllamaModelGateway.class, OllamaModelGateway.class)
            .withPropertyValues(
                    "llm.ollama.base-url=http://localhost:11434",
                    "llm.ollama.model=qwen2.5:1.5b",
                    "llm.ollama.frontline-model=qwen2.5:1.5b",
                    "llm.ollama.quality-model=qwen2.5:3b",
                    "llm.ollama.keep-alive=30m"
            );

    @Test
    void springAiProviderIsDefaultModelGateway() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(ModelGateway.class);
            assertThat(context.getBean(ModelGateway.class)).isInstanceOf(SpringAiOllamaModelGateway.class);
        });
    }

    @Test
    void rawOllamaProviderCanBeSelectedExplicitly() {
        contextRunner
                .withPropertyValues("astor.model.provider=ollama-raw")
                .run(context -> {
                    assertThat(context).hasSingleBean(ModelGateway.class);
                    assertThat(context.getBean(ModelGateway.class)).isInstanceOf(OllamaModelGateway.class);
                });
    }
}
