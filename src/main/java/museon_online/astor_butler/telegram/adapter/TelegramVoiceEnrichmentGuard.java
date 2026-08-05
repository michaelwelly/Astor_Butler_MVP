package museon_online.astor_butler.telegram.adapter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import museon_online.astor_butler.service.message.IncomingMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.bots.AbsSender;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class TelegramVoiceEnrichmentGuard {

    private final TelegramVoiceTranscriptionService voiceTranscriptionService;

    @Value("${telegram.voice.enrichment-timeout-ms:20000}")
    private long enrichmentTimeoutMs;

    public IncomingMessage enrich(IncomingMessage incoming, AbsSender sender) {
        if (!isVoiceMessage(incoming)) {
            return incoming;
        }

        try {
            return CompletableFuture.supplyAsync(() -> voiceTranscriptionService.enrich(incoming, sender))
                    .orTimeout(timeoutMs(), TimeUnit.MILLISECONDS)
                    .join();
        } catch (RuntimeException e) {
            Throwable root = rootCause(e);
            log.warn(
                    "Telegram voice enrichment failed or timed out; continuing with voice fallback: chatId={}, messageId={}, updateId={}, reason={}",
                    incoming.chatId(),
                    incoming.telegramMessageId(),
                    incoming.telegramUpdateId(),
                    root.getClass().getSimpleName() + ": " + root.getMessage()
            );
            return withFailedTranscription(incoming, root);
        }
    }

    private boolean isVoiceMessage(IncomingMessage incoming) {
        if (incoming == null || incoming.payload() == null) {
            return false;
        }
        Object mediaKind = incoming.payload().get("mediaKind");
        return "VOICE".equals(mediaKind) || "AUDIO".equals(mediaKind);
    }

    private IncomingMessage withFailedTranscription(IncomingMessage incoming, Throwable throwable) {
        Map<String, Object> payload = new LinkedHashMap<>(incoming.payload());
        payload.put("transcriptionAvailable", false);
        payload.put("transcriptionStatus", "FAILED");
        payload.put("transcriptionReason", failureReason(throwable));
        return incoming.withTextAndPayload(incoming.text(), payload);
    }

    private String failureReason(Throwable throwable) {
        if (throwable == null) {
            return "VOICE_ENRICHMENT_FAILED";
        }
        String message = throwable.getMessage();
        if (message == null || message.isBlank()) {
            return throwable.getClass().getSimpleName();
        }
        return throwable.getClass().getSimpleName() + ": " + message;
    }

    private Throwable rootCause(Throwable throwable) {
        Throwable current = throwable;
        while ((current instanceof CompletionException || current instanceof ExecutionException)
                && current.getCause() != null) {
            current = current.getCause();
        }
        return current == null ? new IllegalStateException("unknown voice enrichment failure") : current;
    }

    private long timeoutMs() {
        return Math.max(250L, enrichmentTimeoutMs);
    }
}
