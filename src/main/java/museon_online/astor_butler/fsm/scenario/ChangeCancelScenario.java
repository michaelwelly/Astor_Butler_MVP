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
public class ChangeCancelScenario implements FsmScenario {

    private final FSMStorage fsmStorage;

    @Value("${telegram.admin.chat-id:}")
    private String adminChatId;

    @Override
    public String id() {
        return "CHANGE_CANCEL";
    }

    @Override
    public int priority() {
        return 34;
    }

    @Override
    public boolean supports(IncomingMessage incoming, BotState currentState, String text) {
        BotState state = currentState == null ? BotState.UNKNOWN : currentState.canonical();
        String normalized = normalize(text);
        if (normalized.isBlank()) {
            return false;
        }
        return owns(state) || isChangeCancelIntent(normalized);
    }

    @Override
    public OutgoingMessage handle(IncomingMessage incoming, BotState currentState, String text) {
        BotState state = currentState == null ? BotState.UNKNOWN : currentState.canonical();
        String normalized = normalize(text);
        if (state == BotState.TABLE_BOOKING_CHANGE_REQUESTED || containsReference(normalized)) {
            return sendChangeCancelRequest(incoming, currentState, text);
        }

        fsmStorage.setState(incoming.chatId(), BotState.TABLE_BOOKING_CHANGE_REQUESTED);
        return OutgoingMessage.of(
                incoming,
                "Понял, нужно изменить или отменить бронь. Напишите, пожалуйста, дату, время или номер заявки — я передам команде точный запрос и не сниму бронь без подтверждения.",
                BotState.TABLE_BOOKING_CHANGE_REQUESTED.name(),
                false,
                false,
                true,
                false,
                AdminAlert.none(),
                List.of("CHANGE_CANCEL", "ASK_ACTIVE_ORDER_REFERENCE")
        ).withMetadata(Map.of("scenario", id()));
    }

    @Override
    public boolean owns(BotState state) {
        BotState canonical = state == null ? BotState.UNKNOWN : state.canonical();
        return canonical == BotState.TABLE_BOOKING_CHANGE_REQUESTED;
    }

    @Override
    public boolean sideEffecting() {
        return true;
    }

    private OutgoingMessage sendChangeCancelRequest(IncomingMessage incoming, BotState previousState, String text) {
        fsmStorage.setState(incoming.chatId(), BotState.READY_FOR_DIALOG);
        return OutgoingMessage.of(
                incoming,
                "Передал запрос команде. Бронь не меняю и не отменяю автоматически: менеджер проверит активную заявку и подтвердит следующий шаг.",
                BotState.READY_FOR_DIALOG.name(),
                false,
                false,
                true,
                false,
                adminAlert(incoming, previousState, text),
                List.of("CHANGE_CANCEL", "ADMIN_ALERT", "RETURN_MAIN_MENU")
        ).withMetadata(Map.of(
                "scenario", id(),
                "changeCancelRequest", text == null ? "" : text.trim()
        ));
    }

    private AdminAlert adminAlert(IncomingMessage incoming, BotState previousState, String text) {
        if (adminChatId == null || adminChatId.isBlank()) {
            return AdminAlert.none();
        }
        String body = """
                <b>Astor Butler / change or cancel</b>
                Гость просит изменить или отменить бронь

                <b>%s</b>
                chat %s / user %s%s

                <b>Сообщение гостя</b>
                <blockquote>%s</blockquote>

                <b>Контекст</b>
                Previous state: %s
                Scenario: CHANGE_CANCEL

                <b>Действие</b>
                Найдите активную бронь/заявку. Не отменяйте без явного подтверждения гостя и команды.

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

    private boolean isChangeCancelIntent(String text) {
        return containsAny(
                text,
                "отменить брон",
                "отмена брон",
                "отменить стол",
                "отменить заявку",
                "перенести брон",
                "изменить брон",
                "поменять брон",
                "изменить время",
                "поменять время",
                "перенести время",
                "не придем",
                "не придём",
                "не сможем",
                "опоздаем",
                "задержимся"
        );
    }

    private boolean containsReference(String text) {
        return text.matches(".*\\b\\d{1,2}[:.]\\d{2}\\b.*")
                || text.matches(".*\\b\\d{1,2}[./-]\\d{1,2}.*")
                || text.matches(".*\\b\\d{3,}\\b.*")
                || containsAny(text, "сегодня", "завтра", "послезавтра", "вечером", "обед", "ужин");
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
