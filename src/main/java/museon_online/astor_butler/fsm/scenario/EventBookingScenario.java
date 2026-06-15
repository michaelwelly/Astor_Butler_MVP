package museon_online.astor_butler.fsm.scenario;

import lombok.RequiredArgsConstructor;
import museon_online.astor_butler.domain.booking.EventBookingCommand;
import museon_online.astor_butler.domain.booking.EventBookingOrder;
import museon_online.astor_butler.domain.booking.EventBookingService;
import museon_online.astor_butler.fsm.core.BotState;
import museon_online.astor_butler.fsm.storage.FSMStorage;
import museon_online.astor_butler.service.message.AdminAlert;
import museon_online.astor_butler.service.message.IncomingMessage;
import museon_online.astor_butler.service.message.OutgoingMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class EventBookingScenario implements FsmScenario {

    private final FSMStorage fsmStorage;
    private final EventBookingService eventBookingService;

    @Value("${telegram.admin.chat-id:}")
    private String adminChatId;

    @Override
    public String id() {
        return "EVENT_BOOKING";
    }

    @Override
    public int priority() {
        return 33;
    }

    @Override
    public boolean supports(IncomingMessage incoming, BotState currentState, String text) {
        BotState state = currentState == null ? BotState.UNKNOWN : currentState.canonical();
        String normalized = normalize(text);
        if (normalized.isBlank()) {
            return false;
        }
        return owns(state) || isEventIntent(normalized);
    }

    @Override
    public OutgoingMessage handle(IncomingMessage incoming, BotState currentState, String text) {
        String normalized = normalize(text);
        if (hasEnoughDetails(normalized) || owns(currentState)) {
            return sendEventRequest(incoming, currentState, text);
        }

        fsmStorage.setState(incoming.chatId(), BotState.EVENT_BOOKING_COLLECT_DETAILS);
        return OutgoingMessage.of(
                incoming,
                """
                Для мероприятия соберу заявку менеджеру.

                Напишите одной фразой: дата, время, формат, количество гостей и пожелания. Например:
                "день рождения 20 июня в 19:00 на 25 гостей, нужен банкет".
                """,
                BotState.EVENT_BOOKING_COLLECT_DETAILS.name(),
                false,
                false,
                true,
                false,
                AdminAlert.none(),
                List.of("EVENT_BOOKING", "ASK_EVENT_DETAILS")
        ).withMetadata(Map.of("scenario", id()));
    }

    @Override
    public boolean owns(BotState state) {
        BotState canonical = state == null ? BotState.UNKNOWN : state.canonical();
        return canonical == BotState.EVENT_BOOKING_COLLECT_DETAILS || canonical == BotState.EVENT_BOOKING_SENT;
    }

    @Override
    public boolean sideEffecting() {
        return true;
    }

    private OutgoingMessage sendEventRequest(IncomingMessage incoming, BotState previousState, String text) {
        EventBookingOrder order = eventBookingService.createOrder(eventBookingCommand(incoming, text));
        fsmStorage.setState(incoming.chatId(), BotState.READY_FOR_DIALOG);
        return OutgoingMessage.of(
                incoming,
                """
                Собрал заявку на мероприятие #%s и передал менеджеру.

                Это не автоматическое подтверждение: команда проверит дату, формат, меню и свяжется с вами.
                """.formatted(order.id()),
                BotState.READY_FOR_DIALOG.name(),
                false,
                false,
                true,
                false,
                adminAlert(incoming, previousState, text, order),
                List.of("EVENT_BOOKING", "EVENT_REQUEST_SENT", "ADMIN_ALERT", "RETURN_MAIN_MENU")
        ).withMetadata(Map.of(
                "scenario", id(),
                "eventOrderId", order.id(),
                "eventRequest", text == null ? "" : text.trim()
        ));
    }

    private EventBookingCommand eventBookingCommand(IncomingMessage incoming, String text) {
        String normalized = normalize(text);
        return new EventBookingCommand(
                incoming.chatId(),
                incoming.telegramUserId(),
                null,
                "AERIS",
                detectEventType(normalized),
                null,
                detectTimeText(normalized),
                detectGuestCount(normalized),
                detectBudgetText(normalized),
                null,
                null,
                null,
                displayName(incoming),
                null,
                text == null ? "" : text.trim(),
                null,
                adminChatId
        );
    }

    private AdminAlert adminAlert(IncomingMessage incoming, BotState previousState, String text, EventBookingOrder order) {
        if (adminChatId == null || adminChatId.isBlank()) {
            return AdminAlert.none();
        }
        String body = """
                <b>Astor Butler / event booking</b>
                Новая заявка на мероприятие
                Order: #%s

                <b>%s</b>
                chat %s / user %s%s

                <b>Сообщение гостя</b>
                <blockquote>%s</blockquote>

                <b>Контекст</b>
                Previous state: %s
                Scenario: EVENT_BOOKING
                Event type: %s
                Guests: %s
                Time: %s

                <b>Действие</b>
                Проверьте дату, формат, количество гостей, банкетное меню и условия. Автоподтверждения нет.

                <b>Техника</b>
                Channel: %s
                Correlation: %s
                """.formatted(
                html(text(order.id())),
                html(displayName(incoming)),
                html(text(incoming.chatId())),
                html(text(incoming.telegramUserId())),
                incoming.username() == null || incoming.username().isBlank() ? "" : " / @" + html(incoming.username()),
                html(blankAsEmptyLabel(text)),
                html(text(previousState)),
                html(blankAsEmptyLabel(order.eventType())),
                html(text(order.guestCount())),
                html(blankAsEmptyLabel(order.requestedTimeText())),
                html(text(incoming.channel())),
                html(blankAsEmptyLabel(incoming.correlationId()))
        );
        return new AdminAlert(true, adminChatId, body);
    }

    private boolean isEventIntent(String text) {
        return containsAny(
                text,
                "банкет",
                "день рождения",
                "др ",
                "корпоратив",
                "свадьба",
                "мероприятие",
                "выкуп зала",
                "закрыть зал",
                "закрытое мероприятие",
                "вечеринка",
                "фуршет",
                "презентация",
                "юбилей",
                "ивент",
                "event"
        );
    }

    private boolean hasEnoughDetails(String text) {
        return hasDateSignal(text) && hasGuestCount(text);
    }

    private boolean hasDateSignal(String text) {
        return text.matches(".*\\b\\d{1,2}[./-]\\d{1,2}.*")
                || text.matches(".*\\d{1,2}\\s+(января|февраля|марта|апреля|мая|июня|июля|августа|сентября|октября|ноября|декабря).*")
                || containsAny(text, "сегодня", "завтра", "послезавтра", "на выходных", "в пятницу", "в субботу", "в воскресенье");
    }

    private boolean hasGuestCount(String text) {
        return text.matches(".*\\d{1,4}\\s*(гостей|гостя|человек|персон|чел).*");
    }

    private String detectEventType(String text) {
        if (text.contains("свадьб")) {
            return "WEDDING";
        }
        if (text.contains("корпоратив")) {
            return "CORPORATE";
        }
        if (text.contains("день рождения") || text.contains("др ") || text.contains("юбилей")) {
            return "BIRTHDAY";
        }
        if (text.contains("банкет")) {
            return "BANQUET";
        }
        if (text.contains("фуршет")) {
            return "BUFFET";
        }
        if (text.contains("презентац")) {
            return "PRESENTATION";
        }
        if (text.contains("выкуп") || text.contains("закрыть зал") || text.contains("закрытое")) {
            return "HALL_BUYOUT";
        }
        return "PRIVATE_EVENT";
    }

    private Integer detectGuestCount(String text) {
        List<String> markers = List.of("гостей", "гостя", "человек", "персон", "чел");
        for (String marker : markers) {
            int markerIndex = text.indexOf(marker);
            if (markerIndex > 0) {
                String prefix = text.substring(0, markerIndex);
                String number = lastNumber(prefix);
                if (!number.isBlank()) {
                    return Integer.parseInt(number);
                }
            }
        }
        return null;
    }

    private String detectTimeText(String text) {
        List<String> parts = new ArrayList<>();
        if (hasDateSignal(text)) {
            parts.add("date signal");
        }
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("\\b\\d{1,2}[:.]\\d{2}\\b").matcher(text);
        if (matcher.find()) {
            parts.add(matcher.group());
        }
        return parts.isEmpty() ? null : String.join(", ", parts);
    }

    private String detectBudgetText(String text) {
        if (!containsAny(text, "бюджет", "депозит", "₽", "руб", "тыс")) {
            return null;
        }
        return text.length() > 160 ? text.substring(0, 160) : text;
    }

    private String lastNumber(String text) {
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("\\d{1,4}").matcher(text);
        String result = "";
        while (matcher.find()) {
            result = matcher.group();
        }
        return result;
    }

    private boolean containsAny(String text, String... variants) {
        for (String variant : variants) {
            if (text.contains(variant)) {
                return true;
            }
        }
        return false;
    }

    private String displayName(IncomingMessage incoming) {
        String firstName = normalizeDisplay(incoming.firstName());
        String lastName = normalizeDisplay(incoming.lastName());
        String username = normalizeDisplay(incoming.username());
        String fullName = (firstName + " " + lastName).trim();
        if (!fullName.isBlank()) {
            return fullName;
        }
        if (!username.isBlank()) {
            return "@" + username;
        }
        return "unknown";
    }

    private String normalize(String text) {
        return text == null ? "" : text.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeDisplay(String text) {
        return text == null ? "" : text.trim();
    }

    private String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String blankAsEmptyLabel(String value) {
        String normalized = normalizeDisplay(value);
        return normalized.isBlank() ? "(empty)" : normalized;
    }

    private String html(String value) {
        return text(value)
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}
