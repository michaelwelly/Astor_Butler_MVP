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
public class ManagerHelpScenario implements FsmScenario {

    private final FSMStorage fsmStorage;

    @Value("${telegram.admin.chat-id:}")
    private String adminChatId;

    @Override
    public String id() {
        return "MANAGER_HELP";
    }

    @Override
    public int priority() {
        return 35;
    }

    @Override
    public boolean supports(IncomingMessage incoming, BotState currentState, String text) {
        BotState state = currentState == null ? BotState.UNKNOWN : currentState.canonical();
        String normalized = normalize(text);
        if (normalized.isBlank()) {
            return false;
        }
        return owns(state) || isManagerHelpIntent(normalized);
    }

    @Override
    public OutgoingMessage handle(IncomingMessage incoming, BotState currentState, String text) {
        BotState state = currentState == null ? BotState.UNKNOWN : currentState.canonical();
        String normalized = normalize(text);
        if (state == BotState.MANAGER_HELP_COLLECT_REASON) {
            return sendHandoff(incoming, currentState, text, "MANAGER_HELP_REASON_RECEIVED");
        }
        if (isShortManagerCall(normalized)) {
            fsmStorage.setState(incoming.chatId(), BotState.MANAGER_HELP_COLLECT_REASON);
            return OutgoingMessage.of(
                    incoming,
                    "Позову менеджера. Напишите, пожалуйста, одним сообщением, что именно передать команде.",
                    BotState.MANAGER_HELP_COLLECT_REASON.name(),
                    false,
                    false,
                    true,
                    false,
                    AdminAlert.none(),
                    List.of("MANAGER_HELP", "ASK_MANAGER_REASON")
            ).withMetadata(Map.of("scenario", id()));
        }
        return sendHandoff(incoming, currentState, text, "MANAGER_HELP_DIRECT_REQUEST");
    }

    @Override
    public boolean owns(BotState state) {
        BotState canonical = state == null ? BotState.UNKNOWN : state.canonical();
        return canonical == BotState.MANAGER_HELP_COLLECT_REASON || canonical == BotState.MANAGER_HELP_SENT;
    }

    @Override
    public boolean sideEffecting() {
        return true;
    }

    private OutgoingMessage sendHandoff(
            IncomingMessage incoming,
            BotState previousState,
            String text,
            String reasonAction
    ) {
        fsmStorage.setState(incoming.chatId(), BotState.READY_FOR_DIALOG);
        return OutgoingMessage.of(
                incoming,
                "Передал запрос команде. Менеджер увидит контекст и подключится, если вопрос требует ручного решения. Я остаюсь на связи: можно параллельно попросить меню, бронь или видео-тур.",
                BotState.READY_FOR_DIALOG.name(),
                false,
                false,
                true,
                false,
                adminAlert(incoming, previousState, text),
                List.of("MANAGER_HELP", reasonAction, "ADMIN_ALERT", "RETURN_MAIN_MENU")
        ).withMetadata(Map.of(
                "scenario", id(),
                "handoffReason", reasonAction
        ));
    }

    private AdminAlert adminAlert(IncomingMessage incoming, BotState previousState, String text) {
        if (adminChatId == null || adminChatId.isBlank()) {
            return AdminAlert.none();
        }
        String guestLink = incoming.telegramUserId() == null
                ? html(text(incoming.chatId()))
                : "<a href=\"tg://user?id=%s\">%s</a>".formatted(
                html(text(incoming.telegramUserId())),
                html(displayName(incoming))
        );
        String body = """
                <b>Astor Butler / manager help</b>
                Гость просит ручную помощь

                <b>Гость</b>
                %s
                chat %s / user %s%s

                <b>Сообщение гостя</b>
                <blockquote>%s</blockquote>

                <b>Контекст</b>
                Previous state: %s
                Scenario: MANAGER_HELP

                <b>Действие</b>
                Проверьте диалог и подключитесь вручную, если вопрос нельзя закрыть FSM-сценарием.

                <b>Техника</b>
                Channel: %s
                Correlation: %s
                """.formatted(
                guestLink,
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

    private boolean isManagerHelpIntent(String text) {
        return containsAny(text, "менеджер", "администратор", "человек", "оператор", "сотрудник", "позови", "позвать", "ручная помощь", "жалоба");
    }

    private boolean isShortManagerCall(String text) {
        return text.equals("менеджер")
                || text.equals("администратор")
                || text.equals("позови менеджера")
                || text.equals("позвать менеджера")
                || text.equals("оператор")
                || text.equals("человек");
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
