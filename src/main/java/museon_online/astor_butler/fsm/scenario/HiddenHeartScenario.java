package museon_online.astor_butler.fsm.scenario;

import lombok.RequiredArgsConstructor;
import museon_online.astor_butler.fsm.core.BotState;
import museon_online.astor_butler.fsm.storage.FSMStorage;
import museon_online.astor_butler.service.message.AdminAlert;
import museon_online.astor_butler.service.message.IncomingMessage;
import museon_online.astor_butler.service.message.OutgoingMessage;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class HiddenHeartScenario implements FsmScenario {

    private static final Pattern MONEY = Pattern.compile(".*\\b\\d{2,7}\\b.*");

    private final FSMStorage fsmStorage;

    @Override
    public String id() {
        return "HIDDEN_HEART";
    }

    @Override
    public int priority() {
        return 62;
    }

    @Override
    public boolean supports(IncomingMessage incoming, BotState currentState, String text) {
        BotState state = currentState == null ? BotState.UNKNOWN : currentState.canonical();
        String normalized = normalize(text);
        if (normalized.isBlank()) {
            return false;
        }
        return owns(state) || isDonationIntent(normalized);
    }

    @Override
    public OutgoingMessage handle(IncomingMessage incoming, BotState currentState, String text) {
        BotState state = currentState == null ? BotState.UNKNOWN : currentState.canonical();
        String normalized = normalize(text);
        if (state == BotState.DONATION_CONFIRMATION) {
            return confirmDonation(incoming, normalized);
        }
        return collectAmount(incoming, normalized);
    }

    @Override
    public boolean owns(BotState state) {
        BotState canonical = state == null ? BotState.UNKNOWN : state.canonical();
        return canonical == BotState.DONATION_COLLECT_AMOUNT || canonical == BotState.DONATION_CONFIRMATION;
    }

    @Override
    public boolean sideEffecting() {
        return true;
    }

    private OutgoingMessage collectAmount(IncomingMessage incoming, String text) {
        if (!hasMoney(text)) {
            fsmStorage.setState(incoming.chatId(), BotState.DONATION_COLLECT_AMOUNT);
            return OutgoingMessage.of(
                    incoming,
                    "Hidden Heart включен. Какую сумму хотите направить на благотворительный проект? По умолчанию вклад будет анонимным.",
                    BotState.DONATION_COLLECT_AMOUNT.name(),
                    false,
                    false,
                    true,
                    false,
                    AdminAlert.none(),
                    List.of("HIDDEN_HEART", "ASK_DONATION_AMOUNT")
            ).withMetadata(Map.of("scenario", id()));
        }

        fsmStorage.setState(incoming.chatId(), BotState.DONATION_CONFIRMATION);
        return OutgoingMessage.of(
                incoming,
                "Собрал donation draft: анонимный вклад, сумма из сообщения. Перед платежным контуром я покажу подтверждение и не раскрою личные данные в impact-отчете.",
                BotState.DONATION_CONFIRMATION.name(),
                false,
                false,
                true,
                false,
                AdminAlert.none(),
                List.of("HIDDEN_HEART", "DONATION_CONFIRMATION", "IMPACT_EVENT_DRAFT")
        ).withMetadata(Map.of(
                "scenario", id(),
                "privacy", "ANONYMOUS_BY_DEFAULT",
                "paymentBoundary", "SBP_FUTURE_INTEGRATION"
        ));
    }

    private OutgoingMessage confirmDonation(IncomingMessage incoming, String text) {
        if (isConfirmIntent(text)) {
            fsmStorage.setState(incoming.chatId(), BotState.READY_FOR_DIALOG);
            return OutgoingMessage.of(
                    incoming,
                    "Зафиксировал анонимный donation draft. В impact попадет только агрегированный вклад, без приватных данных.",
                    BotState.READY_FOR_DIALOG.name(),
                    false,
                    false,
                    true,
                    false,
                    AdminAlert.none(),
                    List.of("HIDDEN_HEART", "DONATION_DRAFT_CONFIRMED", "IMPACT_EVENT_DRAFT", "RETURN_MAIN_MENU")
            ).withMetadata(Map.of("scenario", id()));
        }
        if (isRejectIntent(text)) {
            fsmStorage.setState(incoming.chatId(), BotState.READY_FOR_DIALOG);
            return OutgoingMessage.of(
                    incoming,
                    "Хорошо, донат не фиксирую. Возвращаюсь в главное меню.",
                    BotState.READY_FOR_DIALOG.name(),
                    false,
                    false,
                    true,
                    false,
                    AdminAlert.none(),
                    List.of("HIDDEN_HEART", "DONATION_CANCELLED", "RETURN_MAIN_MENU")
            ).withMetadata(Map.of("scenario", id()));
        }
        fsmStorage.setState(incoming.chatId(), BotState.DONATION_CONFIRMATION);
        return OutgoingMessage.of(
                incoming,
                "Подтвердите, пожалуйста: да — зафиксировать анонимный donation draft, нет — отменить.",
                BotState.DONATION_CONFIRMATION.name(),
                false,
                false,
                true,
                false,
                AdminAlert.none(),
                List.of("HIDDEN_HEART", "ASK_CONFIRMATION")
        ).withMetadata(Map.of("scenario", id()));
    }

    private boolean isDonationIntent(String text) {
        return containsAny(text, "донат", "благотвор", "поддержать проект", "hidden heart", "пожертв", "помочь проекту", "социальный вклад");
    }

    private boolean hasMoney(String text) {
        return MONEY.matcher(text).matches()
                || text.contains("тысяч")
                || text.contains("тысячи")
                || text.contains("руб");
    }

    private boolean isConfirmIntent(String text) {
        return text.equals("да")
                || text.equals("ок")
                || text.equals("okay")
                || text.equals("ok")
                || text.equals("подтверждаю")
                || text.equals("подтвердить")
                || text.equals("согласен")
                || text.equals("согласна");
    }

    private boolean isRejectIntent(String text) {
        return text.equals("нет")
                || text.equals("не надо")
                || text.equals("отмена")
                || text.equals("отмени")
                || text.equals("не подтверждаю");
    }

    private boolean containsAny(String text, String... variants) {
        for (String variant : variants) {
            if (text.contains(variant)) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String text) {
        return text == null ? "" : text.trim().toLowerCase(Locale.ROOT);
    }
}
