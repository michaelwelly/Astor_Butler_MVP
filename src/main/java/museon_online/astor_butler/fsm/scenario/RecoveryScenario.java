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
public class RecoveryScenario implements FsmScenario {

    private final FSMStorage fsmStorage;
    private final RecoveryRetryService recoveryRetryService;

    @Value("${telegram.admin.chat-id:}")
    private String adminChatId;

    @Override
    public String id() {
        return "RECOVERY";
    }

    @Override
    public int priority() {
        return 95;
    }

    @Override
    public boolean supports(IncomingMessage incoming, BotState currentState, String text) {
        BotState state = currentState == null ? BotState.UNKNOWN : currentState.canonical();
        String normalized = normalize(text);
        return !normalized.isBlank() && (state == BotState.READY_FOR_DIALOG || state == BotState.AI_FALLBACK);
    }

    @Override
    public OutgoingMessage handle(IncomingMessage incoming, BotState currentState, String text) {
        long attempts = recoveryRetryService.recordUnclear(incoming.chatId());
        if (attempts <= 1) {
            fsmStorage.setState(incoming.chatId(), BotState.READY_FOR_DIALOG);
            return OutgoingMessage.of(
                    incoming,
                    """
                    Я не хочу угадать неправильно. Выберите направление или напишите одной короткой фразой:

                    • стол / бронь
                    • меню / винная карта
                    • афиша / видео-тур / концепция
                    • мероприятие
                    • чаевые / донат / аукцион
                    • менеджер
                    """,
                    BotState.READY_FOR_DIALOG.name(),
                    false,
                    false,
                    true,
                    false,
                    AdminAlert.none(),
                    List.of("RECOVERY", "CLARIFY_INTENT", "SHOW_MAIN_OPTIONS")
            ).withMetadata(Map.of(
                    "scenario", id(),
                    "recoveryAttempts", attempts
            ));
        }

        fsmStorage.setState(incoming.chatId(), BotState.READY_FOR_DIALOG);
        return OutgoingMessage.of(
                incoming,
                "Я передал запрос администратору, чтобы не увести вас не туда. Пока команда смотрит, можно написать проще: бронь, меню, афиша, мероприятие или менеджер.",
                BotState.READY_FOR_DIALOG.name(),
                false,
                false,
                true,
                false,
                adminAlert(incoming, currentState, text, attempts),
                List.of("RECOVERY", "ADMIN_ALERT", "RETURN_MAIN_MENU")
        ).withMetadata(Map.of(
                "scenario", id(),
                "recoveryAttempts", attempts
        ));
    }

    @Override
    public boolean owns(BotState state) {
        BotState canonical = state == null ? BotState.UNKNOWN : state.canonical();
        return canonical == BotState.AI_FALLBACK;
    }

    private AdminAlert adminAlert(IncomingMessage incoming, BotState previousState, String text, long attempts) {
        if (adminChatId == null || adminChatId.isBlank()) {
            return AdminAlert.none();
        }
        String body = """
                <b>Astor Butler / recovery</b>
                Повторно не удалось распознать сценарий

                <b>%s</b>
                chat %s / user %s%s

                <b>Сообщение гостя</b>
                <blockquote>%s</blockquote>

                <b>Контекст</b>
                Previous state: %s
                Recovery attempts: %s

                <b>Действие</b>
                Проверьте диалог. Если нужен человек, подключитесь вручную; если это новый повторяемый intent, добавьте его в FSM.

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
                html(text(attempts)),
                html(text(incoming.channel())),
                html(blankAsEmptyLabel(incoming.correlationId()))
        );
        return new AdminAlert(true, adminChatId, body);
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
