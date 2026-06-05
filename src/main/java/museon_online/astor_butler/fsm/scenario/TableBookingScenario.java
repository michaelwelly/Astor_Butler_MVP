package museon_online.astor_butler.fsm.scenario;

import lombok.RequiredArgsConstructor;
import museon_online.astor_butler.fsm.core.BotState;
import museon_online.astor_butler.fsm.storage.FSMStorage;
import museon_online.astor_butler.service.message.AdminAlert;
import museon_online.astor_butler.service.message.IncomingMessage;
import museon_online.astor_butler.service.message.OutgoingMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class TableBookingScenario {

    private static final Pattern TIME = Pattern.compile("\\b([01]?\\d|2[0-3])[:.]?([0-5]\\d)?\\b");
    private static final Pattern NUMBER = Pattern.compile("\\b\\d{1,2}\\b");

    private final FSMStorage fsmStorage;

    @Value("${telegram.booking.plan-pdf-path:classpath:booking/aeris-plan.pdf}")
    private String planPdfPath;

    public boolean supports(IncomingMessage incoming, BotState currentState, String text) {
        BotState state = currentState == null ? BotState.UNKNOWN : currentState.canonical();
        return isTableBookingState(state) || isTableBookingIntent(text);
    }

    public OutgoingMessage handle(IncomingMessage incoming, BotState currentState, String text) {
        String normalized = normalize(text);
        BotState state = currentState == null ? BotState.UNKNOWN : currentState.canonical();
        if (state == BotState.TABLE_BOOKING_WAIT_TABLE_SELECTION) {
            return tableSelected(incoming);
        }
        if (!hasDate(normalized)) {
            fsmStorage.setState(incoming.chatId(), BotState.TABLE_BOOKING_COLLECT_DATE);
            return message(incoming, "Конечно. На какую дату бронируем стол?", BotState.TABLE_BOOKING_COLLECT_DATE, "ASK_DATE");
        }
        if (!hasTime(normalized)) {
            fsmStorage.setState(incoming.chatId(), BotState.TABLE_BOOKING_COLLECT_TIME);
            return message(incoming, "Принял дату. На какое время поставить бронь?", BotState.TABLE_BOOKING_COLLECT_TIME, "ASK_TIME");
        }
        if (!hasPartySize(normalized)) {
            fsmStorage.setState(incoming.chatId(), BotState.TABLE_BOOKING_COLLECT_PARTY_SIZE);
            return message(incoming, "На сколько гостей бронируем?", BotState.TABLE_BOOKING_COLLECT_PARTY_SIZE, "ASK_PARTY_SIZE");
        }
        fsmStorage.setState(incoming.chatId(), BotState.TABLE_BOOKING_WAIT_TABLE_SELECTION);
        return message(
                incoming,
                "Отправляю план зала AERIS. Выберите, пожалуйста, номер стола или зону. Если хотите, напишите \"выбери сам\" — подберу подходящий вариант.",
                BotState.TABLE_BOOKING_WAIT_TABLE_SELECTION,
                "SEND_HALL_PLAN",
                "ASK_TABLE_SELECTION"
        ).withMetadata(Map.of(
                "documentResource", planPdfPath,
                "documentFilename", "AERIS PLAN.pdf",
                "documentCaption", "План зала AERIS"
        ));
    }

    private OutgoingMessage tableSelected(IncomingMessage incoming) {
        fsmStorage.setState(incoming.chatId(), BotState.TABLE_BOOKING_WAIT_HOSTESS_CONFIRMATION);
        return message(
                incoming,
                "Выбор принял. Сейчас проверю доступность и отправлю заявку хостес на подтверждение.",
                BotState.TABLE_BOOKING_WAIT_HOSTESS_CONFIRMATION,
                "TABLE_SELECTED",
                "WAIT_HOSTESS_CONFIRMATION"
        );
    }

    private OutgoingMessage message(IncomingMessage incoming, String text, BotState nextState, String... actions) {
        return OutgoingMessage.of(
                incoming,
                text,
                nextState.name(),
                false,
                false,
                false,
                false,
                AdminAlert.none(),
                List.of(actions)
        );
    }

    private boolean isTableBookingIntent(String text) {
        String value = normalize(text);
        return value.contains("забронировать стол")
                || value.contains("забронировать столик")
                || value.contains("бронь стол")
                || value.contains("бронь столик")
                || value.contains("столик")
                || value.contains("стол на")
                || value.contains("есть места");
    }

    private boolean isTableBookingState(BotState state) {
        return switch (state) {
            case TABLE_BOOKING_INTENT,
                 TABLE_BOOKING_COLLECT_DATE,
                 TABLE_BOOKING_COLLECT_TIME,
                 TABLE_BOOKING_COLLECT_PARTY_SIZE,
                 TABLE_BOOKING_SHOW_PLAN,
                 TABLE_BOOKING_WAIT_TABLE_SELECTION,
                 TABLE_BOOKING_CHANGE_REQUESTED -> true;
            default -> false;
        };
    }

    private boolean hasDate(String text) {
        return text.contains("сегодня")
                || text.contains("завтра")
                || text.matches(".*\\b\\d{1,2}[./-]\\d{1,2}([./-]\\d{2,4})?\\b.*");
    }

    private boolean hasTime(String text) {
        return TIME.matcher(text).find();
    }

    private boolean hasPartySize(String text) {
        return text.contains("двоих")
                || text.contains("двоем")
                || text.contains("троих")
                || text.contains("четверых")
                || NUMBER.matcher(text).find();
    }

    private String normalize(String text) {
        return text == null ? "" : text.trim().toLowerCase();
    }
}
