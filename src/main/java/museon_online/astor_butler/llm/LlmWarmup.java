package museon_online.astor_butler.llm;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class LlmWarmup {

    private final OllamaClient ollamaClient;

    @PostConstruct
    public void warmup() {
        new Thread(() -> {
            try {
                log.info("🔥 [LLM] Warm-up started");

                long start = System.currentTimeMillis();

                ollamaClient.ask(
                        "Ответь одним словом: готов."
                );

                long duration = System.currentTimeMillis() - start;

                log.info("🔥 [LLM] Warm-up finished in {} ms", duration);

            } catch (Exception e) {
                log.warn("⚠️ [LLM] Warm-up failed", e);
            }
        }, "llm-warmup-thread").start();
    }
}