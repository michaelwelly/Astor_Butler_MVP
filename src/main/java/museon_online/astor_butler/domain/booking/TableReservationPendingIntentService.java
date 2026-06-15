package museon_online.astor_butler.domain.booking;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import museon_online.astor_butler.domain.media.AerisMediaCatalog;
import museon_online.astor_butler.domain.media.MediaAsset;
import museon_online.astor_butler.fsm.storage.FSMStorage;
import museon_online.astor_butler.telegram.adapter.TelegramMediaSender;
import museon_online.astor_butler.telegram.utils.TelegramBot;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class TableReservationPendingIntentService {

    private final FSMStorage fsmStorage;
    private final AerisMediaCatalog mediaCatalog;
    private final ObjectProvider<TelegramBot> telegramBotProvider;
    private final TelegramMediaSender telegramMediaSender;

    @Value("${telegram.bot.enabled:false}")
    private boolean telegramEnabled;

    @Value("${telegram.booking.notifications-enabled:true}")
    private boolean notificationsEnabled;

    public void deliverAfterConfirmation(TableReservationOrder order) {
        if (order == null || order.chatId() == null) {
            return;
        }
        List<String> pendingIntents = fsmStorage.getPendingIntents(order.chatId());
        if (pendingIntents.isEmpty()) {
            return;
        }
        TelegramBot telegramBot = telegramBotProvider.getIfAvailable();
        if (!telegramEnabled || !notificationsEnabled || telegramBot == null) {
            log.debug("Pending table booking intents kept for later: chatId={}, pending={}", order.chatId(), pendingIntents);
            return;
        }

        boolean deliveredAny = false;
        for (String pendingIntent : pendingIntents) {
            deliveredAny = deliver(order.chatId(), pendingIntent, telegramBot) || deliveredAny;
        }
        if (deliveredAny) {
            fsmStorage.clearPendingIntents(order.chatId());
        }
    }

    private boolean deliver(Long chatId, String pendingIntent, TelegramBot telegramBot) {
        String code = pendingIntentCode(pendingIntent);
        String prompt = pendingIntentPrompt(pendingIntent);
        if ("MENU_ASSETS".equals(code)) {
            return deliverMenu(chatId, prompt, telegramBot);
        }
        if ("QUIET_GUIDE".equals(code)) {
            return deliverQuietGuide(chatId, prompt, telegramBot);
        }
        return false;
    }

    private boolean deliverMenu(Long chatId, String prompt, TelegramBot telegramBot) {
        List<MediaAsset> assets = menuAssets(prompt);
        sendText(chatId, "И как обещал, отправляю материалы после подтверждения брони.", telegramBot);
        for (MediaAsset asset : assets) {
            telegramMediaSender.sendDocumentIfPresent(chatId, Map.of(
                    "documents", List.of(Map.of(
                            "objectKey", asset.objectKey(),
                            "filename", asset.filename(),
                            "caption", asset.title()
                    ))
            ), telegramBot);
        }
        return true;
    }

    private boolean deliverQuietGuide(Long chatId, String prompt, TelegramBot telegramBot) {
        if (containsAny(prompt, "концепц", "шеф", "георг", "матве")) {
            sendText(chatId, """
                    <b>Гастрономическая экспедиция Георгия Матвеева в AERIS</b>

                    AERIS - кухня 21 страны Средиземноморья в прочтении Георгия Матвеева. В фокусе продукт, чистый вкус, премиальное мясо, свежая рыба, зелень и авторские соусы.
                    """, telegramBot);
            return true;
        }
        if (containsAny(prompt, "афиша", "событ", "расписание")) {
            sendText(chatId, "Афишу держу как тихую справку. Сейчас актуальные события лучше уточнить у команды, я передам запрос при необходимости.", telegramBot);
            return true;
        }
        MediaAsset video = mediaCatalog.interiorTour();
        sendText(chatId, "И как обещал, отправляю видео-тур по AERIS после подтверждения брони.", telegramBot);
        telegramMediaSender.sendVideoIfPresent(chatId, Map.of(
                "videoObjectKey", video.objectKey(),
                "videoFilename", video.filename(),
                "videoCaption", video.title(),
                "videoSendMode", "DOCUMENT"
        ), telegramBot);
        return true;
    }

    private List<MediaAsset> menuAssets(String prompt) {
        String normalized = normalize(prompt);
        if (containsAny(normalized, "вино", "вин", "игрист", "шампан")) {
            return List.of(mediaCatalog.wineMenu());
        }
        if (containsAny(normalized, "коктей", "elements", "элемент")) {
            return List.of(mediaCatalog.elementsMenu());
        }
        if (containsAny(normalized, "бар", "напит")) {
            return List.of(mediaCatalog.barMenu());
        }
        if (containsAny(normalized, "кух", "еда", "поесть")) {
            return List.of(mediaCatalog.kitchenMenu());
        }
        return mediaCatalog.allMenus();
    }

    private void sendText(Long chatId, String text, TelegramBot telegramBot) {
        try {
            telegramBot.execute(SendMessage.builder()
                    .chatId(chatId.toString())
                    .text(text)
                    .parseMode("HTML")
                    .build());
        } catch (Exception e) {
            log.warn("Pending intent text was not sent: chatId={}, reason={}", chatId, e.getMessage());
        }
    }

    private String pendingIntentCode(String pendingIntent) {
        if (pendingIntent == null || pendingIntent.isBlank()) {
            return "";
        }
        int separator = pendingIntent.indexOf("::");
        return separator < 0 ? pendingIntent.trim() : pendingIntent.substring(0, separator).trim();
    }

    private String pendingIntentPrompt(String pendingIntent) {
        if (pendingIntent == null || pendingIntent.isBlank()) {
            return "";
        }
        int separator = pendingIntent.indexOf("::");
        return separator < 0 || separator + 2 >= pendingIntent.length()
                ? ""
                : pendingIntent.substring(separator + 2).trim();
    }

    private boolean containsAny(String text, String... needles) {
        String normalized = normalize(text);
        for (String needle : needles) {
            if (normalized.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String text) {
        return text == null ? "" : text.trim().toLowerCase();
    }
}
