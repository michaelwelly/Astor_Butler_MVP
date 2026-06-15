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
import java.util.Locale;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class EventBookingScenario implements FsmScenario {

    private final FSMStorage fsmStorage;

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
        fsmStorage.setState(incoming.chatId(), BotState.READY_FOR_DIALOG);
        return OutgoingMessage.of(
                incoming,
                "Собрал заявку на мероприятие и передал менеджеру. Это не автоматическое подтверждение: команда проверит дату, формат, меню и свяжется с вами.",
                BotState.READY_FOR_DIALOG.name(),
                false,
                false,
                true,
                false,
                adminAlert(incoming, previousState, text),
                List.of("EVENT_BOOKING", "EVENT_REQUEST_SENT", "ADMIN_ALERT", "RETURN_MAIN_MENU")
        ).withMetadata(Map.of(
                "scenario", id(),
                "eventRequest", text == null ? "" : text.trim()
        ));
    }

    private AdminAlert adminAlert(IncomingMessage incoming, BotState previousState, String text) {
        if (adminChatId == null || adminChatId.isBlank()) {
            return AdminAlert.none();
        }
        String body = """
                <b>Astor Butler / event booking</b>
                Новая заявка на мероприятие

                <b>%s</b>
                chat %s / user %s%s

                <b>Сообщение гостя</b>
                <blockquote>%s</blockquote>

                <b>Контекст</b>
                Previous state: %s
                Scenario: EVENT_BOOKING

                <b>Действие</b>
                Проверьте дату, формат, количество гостей, банкетное меню и условия. Автоподтверждения нет.

                <b>Техника</b>
                Channel: %s
                Correlation: %s
                """.formatted(
                html(displayName(incoming)),
                html(text(incoming.chatId())),
                html(text(incoming.telegramUserId())),
                incoming.username() == null || incoming.username().isBlank() ? "" : " / @" + html(incoming.username()),
                html(blankAsEmptyLabel(text)),
                html(text(previousState)),
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
