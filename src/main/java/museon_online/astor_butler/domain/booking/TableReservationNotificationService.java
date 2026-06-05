package museon_online.astor_butler.domain.booking;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import museon_online.astor_butler.telegram.utils.TelegramBot;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TableReservationNotificationService {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
            .withZone(ZoneId.of("Asia/Yekaterinburg"));

    private final ObjectProvider<TelegramBot> telegramBotProvider;

    @Value("${telegram.bot.enabled:false}")
    private boolean telegramEnabled;

    @Value("${telegram.booking.notifications-enabled:true}")
    private boolean notificationsEnabled;

    @Value("${telegram.booking.manager-chat-id:876857557}")
    private String defaultManagerChatId;

    @Value("${telegram.booking.hostess-chat-id:}")
    private String defaultHostessChatId;

    public void notifyHostessApprovalRequest(TableReservationOrder order) {
        send(hostessChatId(order), hostessApprovalRequestText(order), approvalKeyboard(order.id()), "hostess-approval-request");
    }

    public void notifyHostessConfirmed(TableReservationOrder order) {
        String chatId = hostessChatId(order);
        send(chatId, hostessText(order), null, "hostess");
    }

    public void notifyHostessAcknowledged(TableReservationOrder order) {
        send(hostessChatId(order), """
                <b>Принял.</b> Бронь #%s подтверждена, гостю отправлен красивый ордер.
                """.formatted(order.id()), null, "hostess-ack");
    }

    public void notifyHostessRejected(TableReservationOrder order) {
        send(hostessChatId(order), """
                <b>Принял.</b> Бронь #%s отменена, гостю отправлен вежливый отказ с предложением выбрать другой вариант.
                """.formatted(order.id()), null, "hostess-reject-ack");
    }

    public void notifyGuestConfirmed(TableReservationOrder order) {
        if (order.chatId() == null) {
            return;
        }
        send(order.chatId().toString(), guestConfirmedText(order), null, "guest-confirmed");
    }

    public void notifyGuestRejected(TableReservationOrder order) {
        if (order.chatId() == null) {
            return;
        }
        send(order.chatId().toString(), guestRejectedText(order), null, "guest-rejected");
    }

    private void send(String chatId, String text, InlineKeyboardMarkup keyboard, String target) {
        if (!telegramEnabled || !notificationsEnabled || chatId == null || chatId.isBlank()) {
            log.debug("Table reservation Telegram notification skipped: target={}, botEnabled={}, notificationsEnabled={}, chatConfigured={}",
                    target,
                    telegramEnabled,
                    notificationsEnabled,
                    chatId != null && !chatId.isBlank());
            return;
        }

        try {
            TelegramBot telegramBot = telegramBotProvider.getIfAvailable();
            if (telegramBot == null) {
                log.debug("Table reservation Telegram notification skipped: bot bean is not available");
                return;
            }
            telegramBot.execute(SendMessage.builder()
                    .chatId(chatId)
                    .text(text)
                    .parseMode("HTML")
                    .replyMarkup(keyboard)
                    .build());
        } catch (Exception e) {
            log.warn("Table reservation Telegram notification failed: target={}, orderId={}, reason={}",
                    target,
                    text.contains("#") ? "included" : "unknown",
                    e.getMessage());
        }
    }

    private String hostessApprovalRequestText(TableReservationOrder order) {
        return """
                <b>Новая заявка на бронь стола</b>

                <b>Заказ:</b> #%s
                <b>Гость:</b> %s
                <b>Стол:</b> %s
                <b>Время:</b> %s - %s
                <b>Гостей:</b> %s
                <b>Телефон:</b> %s
                <b>Комментарий:</b> %s

                Статус: <b>%s</b>
                Выберите действие кнопкой ниже.
                """.formatted(
                order.id(),
                escape(blank(order.guestName(), "гость из Telegram")),
                escape(tableName(order)),
                format(order.requestedStartAt()),
                format(order.requestedEndAt()),
                order.partySize(),
                escape(blank(order.guestPhone(), "не указан")),
                escape(blank(order.guestComment(), "нет")),
                order.status()
        );
    }

    private String hostessText(TableReservationOrder order) {
        return """
                <b>Подтверждена бронь стола</b>

                <b>Заказ:</b> #%s
                <b>Гость:</b> %s
                <b>Стол:</b> %s
                <b>Время:</b> %s - %s
                <b>Гостей:</b> %s
                <b>Телефон:</b> %s
                <b>Комментарий:</b> %s
                """.formatted(
                order.id(),
                escape(blank(order.guestName(), "гость из Telegram")),
                escape(tableName(order)),
                format(order.requestedStartAt()),
                format(order.requestedEndAt()),
                order.partySize(),
                escape(blank(order.guestPhone(), "не указан")),
                escape(blank(order.guestComment(), "нет"))
        );
    }

    private String guestConfirmedText(TableReservationOrder order) {
        return """
                <b>Бронь подтверждена</b>

                Ваш стол ждет вас.

                <b>Заказ:</b> #%s
                <b>Стол:</b> %s
                <b>Время:</b> %s - %s
                <b>Гостей:</b> %s

                Если планы изменятся, просто напишите сюда, и я помогу все поправить.
                """.formatted(
                order.id(),
                escape(tableName(order)),
                format(order.requestedStartAt()),
                format(order.requestedEndAt()),
                order.partySize()
        );
    }

    private String guestRejectedText(TableReservationOrder order) {
        return """
                <b>Пока не получилось подтвердить этот стол</b>

                По выбранному варианту команда хостес не смогла подтвердить бронь.

                <b>Заказ:</b> #%s
                <b>Стол:</b> %s
                <b>Время:</b> %s - %s
                <b>Гостей:</b> %s

                Давайте подберем другой стол или время. Напишите, что удобнее изменить: время, количество гостей или зону посадки.
                """.formatted(
                order.id(),
                escape(tableName(order)),
                format(order.requestedStartAt()),
                format(order.requestedEndAt()),
                order.partySize()
        );
    }

    private InlineKeyboardMarkup approvalKeyboard(Long orderId) {
        return InlineKeyboardMarkup.builder()
                .keyboard(List.of(List.of(
                        InlineKeyboardButton.builder()
                                .text("Да")
                                .callbackData("table_booking:confirm:" + orderId)
                                .build(),
                        InlineKeyboardButton.builder()
                                .text("Нет")
                                .callbackData("table_booking:reject:" + orderId)
                                .build()
                )))
                .build();
    }

    private String hostessChatId(TableReservationOrder order) {
        return order.hostessChatId() == null || order.hostessChatId().isBlank()
                ? defaultHostessChatId
                : order.hostessChatId();
    }

    private String tableName(TableReservationOrder order) {
        if (order.tableCode() == null) {
            return "не выбран";
        }
        if (order.tableDisplayName() == null || order.tableDisplayName().isBlank()) {
            return order.tableCode();
        }
        return order.tableDisplayName() + " (" + order.tableCode() + ")";
    }

    private String format(java.time.Instant instant) {
        return instant == null ? "не указано" : DATE_TIME.format(instant);
    }

    private String blank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String escape(String value) {
        return value == null ? "" : value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}
