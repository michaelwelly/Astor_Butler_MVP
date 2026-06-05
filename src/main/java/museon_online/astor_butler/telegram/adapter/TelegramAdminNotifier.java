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

    @Value("${telegram.bot.enabled:false}")
    private boolean telegramEnabled;

    @Value("${telegram.analytics.chat-id:}")
    private String analyticsChatId;

    public void sendAnalytics(String text) {
        if (!telegramEnabled || analyticsChatId == null || analyticsChatId.isBlank()) {
            log.debug("Telegram analytics notification skipped: botEnabled={}, chatConfigured={}", telegramEnabled, analyticsChatId != null && !analyticsChatId.isBlank());
            return;
        }

        SendMessage message = SendMessage.builder()
                .chatId(analyticsChatId)
                .text(text)
                .parseMode("HTML")
                .build();

        try {
            telegramBot.execute(message);
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
                    return;
                } catch (Exception retryException) {
                    log.warn("Telegram analytics notification retry after migration failed: {}", retryException.getMessage());
                    return;
                }
            }

            if (e.getApiResponse() != null && e.getApiResponse().contains("migrate_to_chat_id")) {
                log.warn("Telegram analytics chat migrated. API response: {}", e.getApiResponse());
                return;
            }
            log.warn("Telegram analytics notification failed: {}", e.getMessage());
        } catch (Exception e) {
            log.warn("Telegram analytics notification failed: {}", e.getMessage());
        }
    }

    private Long migratedChatId(TelegramApiRequestException e) {
        ResponseParameters parameters = e.getParameters();
        if (parameters == null) {
            return null;
        }
        return parameters.getMigrateToChatId();
    }
}
