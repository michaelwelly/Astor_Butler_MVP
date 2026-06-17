package museon_online.astor_butler.fsm.scenario;

import lombok.RequiredArgsConstructor;
import museon_online.astor_butler.fsm.core.BotState;
import museon_online.astor_butler.fsm.storage.FSMStorage;
import museon_online.astor_butler.service.message.AdminAlert;
import museon_online.astor_butler.service.message.IncomingMessage;
import museon_online.astor_butler.service.message.OutgoingMessage;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class MainMenuScenario implements FsmScenario {

    private final FSMStorage fsmStorage;

    public String id() {
        return "MAIN_MENU";
    }

    public int priority() {
        return 90;
    }

    public boolean supports(IncomingMessage incoming, BotState currentState, String text) {
        BotState state = currentState == null ? BotState.UNKNOWN : currentState.canonical();
        String normalized = normalize(text);
        if (normalized.isBlank()) {
            return false;
        }
        if (isSafeExitIntent(normalized) && isActiveScenarioState(state)) {
            return true;
        }
        return (state == BotState.READY_FOR_DIALOG || state == BotState.AI_FALLBACK) && isMainMenuIntent(normalized);
    }

    public OutgoingMessage handle(IncomingMessage incoming, BotState currentState, String text) {
        BotState state = currentState == null ? BotState.UNKNOWN : currentState.canonical();
        String normalized = normalize(text);
        if (isSafeExitIntent(normalized) && isActiveScenarioState(state)) {
            return ready(
                    incoming,
                    "Остановил текущий сценарий. Я на связи: стол, меню, афиша, мероприятие, чаевые, благотворительность, мерч или менеджер?",
                    "SAFE_EXIT",
                    "OPEN_MAIN_MENU"
            );
        }
        if (isMainMenuIntent(normalized)) {
            return menu(incoming);
        }
        return clarification(incoming);
    }

    private OutgoingMessage menu(IncomingMessage incoming) {
        return ready(
                incoming,
                """
                Я на связи. Выберите действие кнопкой ниже или напишите свободно.

                Быстрые сценарии:
                • забронировать стол
                • посмотреть меню и карты
                • сабраж
                • видео-тур, афиша или концепция AERIS
                • мероприятие
                • помощь команды
                • изменить или отменить бронь
                • отзыв
                • чаевые, донат, аукцион или мерч

                Если у вас есть личное пожелание, напишите: запомни, я люблю тихий стол.
                """,
                "MAIN_MENU",
                "SHOW_MENU"
        );
    }

    private OutgoingMessage clarification(IncomingMessage incoming) {
        return ready(
                incoming,
                "Я пока не уверен, какой сценарий нужен. Нажмите кнопку ниже или напишите проще: стол, меню, сабраж, афиша, мероприятие, чаевые, донат, мерч, отзыв или команда.",
                "MAIN_MENU_CLARIFY"
        );
    }

    private OutgoingMessage ready(IncomingMessage incoming, String text, String... actions) {
        fsmStorage.setState(incoming.chatId(), BotState.READY_FOR_DIALOG);
        return message(incoming, text, BotState.READY_FOR_DIALOG, actions);
    }

    private OutgoingMessage message(IncomingMessage incoming, String text, BotState nextState, String... actions) {
        return OutgoingMessage.of(
                incoming,
                text,
                nextState.name(),
                false,
                false,
                true,
                false,
                AdminAlert.none(),
                List.of(actions)
        );
    }

    private boolean isMainMenuIntent(String text) {
        return text.equals("/menu")
                || text.equals("меню")
                || text.equals("главное меню")
                || text.equals("главная")
                || text.equals("main menu");
    }

    private boolean isActiveScenarioState(BotState state) {
        return state != BotState.UNKNOWN
                && state != BotState.CONSENT_REQUIRED
                && state != BotState.READY_FOR_DIALOG
                && state != BotState.AI_FALLBACK;
    }

    public boolean owns(BotState state) {
        BotState canonical = state == null ? BotState.UNKNOWN : state.canonical();
        return canonical == BotState.READY_FOR_DIALOG;
    }

    private boolean isSafeExitIntent(String text) {
        return text.equals("стоп")
                || text.equals("назад")
                || text.equals("выйти")
                || text.equals("отмена")
                || text.equals("отмени")
                || text.equals("/cancel");
    }

    private String normalize(String text) {
        return text == null ? "" : text.trim().toLowerCase();
    }
}
