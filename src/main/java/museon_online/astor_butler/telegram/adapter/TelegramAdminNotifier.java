package museon_online.astor_butler.telegram.adapter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import museon_online.astor_butler.telegram.utils.TelegramBot;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.ResponseParameters;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;

@Service
@RequiredArgsConstructor
@Slf4j
public class TelegramAdminNotifier {

    private final TelegramBot telegramBot;
    private long lastAnalyticsSentAtMillis;

    @Value("${telegram.bot.enabled:false}")
    private boolean telegramEnabled;

    @Value("${telegram.analytics.chat-id:}")
    private String analyticsChatId;

    @Value("${telegram.analytics.min-send-interval-ms:3200}")
    private long analyticsMinSendIntervalMs;

    @Value("${telegram.analytics.max-retries:2}")
    private int analyticsMaxRetries;

    public synchronized boolean sendAnalytics(String text) {
        if (!telegramEnabled || analyticsChatId == null || analyticsChatId.isBlank()) {
            log.debug("Telegram analytics notification skipped: botEnabled={}, chatConfigured={}", telegramEnabled, analyticsChatId != null && !analyticsChatId.isBlank());
            return true;
        }

        SendMessage message = SendMessage.builder()
                .chatId(analyticsChatId)
                .text(text)
                .parseMode("HTML")
                .build();

        int attempts = Math.max(1, analyticsMaxRetries + 1);
        for (int attempt = 1; attempt <= attempts; attempt++) {
            if (executeWithRetryHints(message, attempt, attempts)) {
                return true;
            }
        }
        return false;
    }

    private boolean executeWithRetryHints(SendMessage message, int attempt, int attempts) {
        try {
            throttleAnalyticsDelivery();
            telegramBot.execute(message);
            lastAnalyticsSentAtMillis = System.currentTimeMillis();
            return true;
        } catch (TelegramApiRequestException e) {
            Long migratedChatId = migratedChatId(e);
            if (migratedChatId != null) {
                log.warn(
                        "Telegram analytics chat migrated to supergroup. Set TELEGRAM_ANALYTICS_CHAT_ID={} and restart the app. Retrying once.",
                        migratedChatId
                );
                try {
                    message.setChatId(migratedChatId);
                    telegramBot.execute(message);
                    lastAnalyticsSentAtMillis = System.currentTimeMillis();
                    return true;
                } catch (Exception retryException) {
                    log.warn("Telegram analytics notification retry after migration failed: {}", retryException.getMessage());
                    return true;
                }
            }

            if (e.getApiResponse() != null && e.getApiResponse().contains("migrate_to_chat_id")) {
                log.warn("Telegram analytics chat migrated. API response: {}", e.getApiResponse());
                return true;
            }

            Integer retryAfter = retryAfter(e);
            if (retryAfter != null && attempt < attempts) {
                log.warn(
                        "Telegram analytics notification rate-limited, retrying after {}s: attempt={}/{}",
                        retryAfter,
                        attempt,
                        attempts
                );
                sleep(retryAfter * 1000L + 1000L);
                return false;
            }
            log.warn("Telegram analytics notification failed: attempt={}/{}, reason={}", attempt, attempts, e.getMessage());
        } catch (Exception e) {
            log.warn("Telegram analytics notification failed: attempt={}/{}, reason={}", attempt, attempts, e.getMessage());
        }
        return false;
    }

    private Long migratedChatId(TelegramApiRequestException e) {
        ResponseParameters parameters = e.getParameters();
        if (parameters == null) {
            return null;
        }
        return parameters.getMigrateToChatId();
    }

    private Integer retryAfter(TelegramApiRequestException e) {
        ResponseParameters parameters = e.getParameters();
        if (parameters == null) {
            return null;
        }
        return parameters.getRetryAfter();
    }

    private void throttleAnalyticsDelivery() {
        long waitMs = analyticsMinSendIntervalMs - (System.currentTimeMillis() - lastAnalyticsSentAtMillis);
        if (waitMs > 0) {
            sleep(waitMs);
        }
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
