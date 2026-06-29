package museon_online.astor_butler.llm;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import museon_online.astor_butler.model.ModelGateway;
import museon_online.astor_butler.model.ModelTextRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class LlmWarmup {

    private final ModelGateway modelGateway;
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "llm-warmup-thread");
        thread.setDaemon(true);
        return thread;
    });

    @Value("${llm.ollama.warmup-enabled:true}")
    private boolean warmupEnabled;

    @Value("${llm.ollama.warmup-interval-seconds:120}")
    private long warmupIntervalSeconds;

    @PostConstruct
    public void warmup() {
        if (!warmupEnabled) {
            log.info("[LLM] Warm-up disabled");
            return;
        }

        executor.scheduleWithFixedDelay(
                this::ping,
                0,
                Math.max(15, warmupIntervalSeconds),
                TimeUnit.SECONDS
        );
    }

    @PreDestroy
    public void stop() {
        executor.shutdownNow();
    }

    private void ping() {
        try {
            long start = System.currentTimeMillis();
            modelGateway.generateText(ModelTextRequest.of(
                    "Ответь одним словом: готов.",
                    "System",
                    "WARMUP",
                    "local-model-warmup"
            ));
            long duration = System.currentTimeMillis() - start;
            log.info("[LLM] Warm-up ping finished in {} ms", duration);
        } catch (Exception e) {
            log.warn("[LLM] Warm-up ping failed: {}", e.getMessage());
        }
    }
}
