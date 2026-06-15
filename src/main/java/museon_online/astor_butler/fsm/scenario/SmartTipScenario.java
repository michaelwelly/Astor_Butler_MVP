package museon_online.astor_butler.fsm.scenario;

import lombok.RequiredArgsConstructor;
import museon_online.astor_butler.domain.tip.TipOrder;
import museon_online.astor_butler.domain.tip.TipOrderCommand;
import museon_online.astor_butler.domain.tip.TipService;
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
public class SmartTipScenario implements FsmScenario {

    private static final Pattern MONEY = Pattern.compile(".*\\b\\d{2,7}\\b.*");

    private final FSMStorage fsmStorage;
    private final TipService tipService;

    @Override
    public String id() {
        return "SMART_TIP";
    }

    @Override
    public int priority() {
        return 60;
    }

    @Override
    public boolean supports(IncomingMessage incoming, BotState currentState, String text) {
        BotState state = currentState == null ? BotState.UNKNOWN : currentState.canonical();
        String normalized = normalize(text);
        if (normalized.isBlank()) {
            return false;
        }
        return owns(state) || isSmartTipIntent(normalized);
    }

    @Override
    public OutgoingMessage handle(IncomingMessage incoming, BotState currentState, String text) {
        BotState state = currentState == null ? BotState.UNKNOWN : currentState.canonical();
        String normalized = normalize(text);
        if (state == BotState.TIP_CONFIRMATION) {
            return confirmTip(incoming, normalized);
        }
        return collectAmount(incoming, normalized);
    }

    @Override
    public boolean owns(BotState state) {
        BotState canonical = state == null ? BotState.UNKNOWN : state.canonical();
        return canonical == BotState.TIP_COLLECT_AMOUNT || canonical == BotState.TIP_CONFIRMATION;
    }

    @Override
    public boolean sideEffecting() {
        return true;
    }

    private OutgoingMessage collectAmount(IncomingMessage incoming, String text) {
        if (!hasMoney(text)) {
            fsmStorage.setState(incoming.chatId(), BotState.TIP_COLLECT_AMOUNT);
            return OutgoingMessage.of(
                    incoming,
                    "Красивая благодарность. Какую сумму чаевых хотите оставить?",
                    BotState.TIP_COLLECT_AMOUNT.name(),
                    false,
                    false,
                    true,
                    false,
                    AdminAlert.none(),
                    List.of("SMART_TIP", "ASK_TIP_AMOUNT")
            ).withMetadata(Map.of("scenario", id()));
        }

        TipOrder order = tipService.createDraft(tipOrderCommand(incoming, text));
        fsmStorage.setState(incoming.chatId(), BotState.TIP_CONFIRMATION);
        return OutgoingMessage.of(
                incoming,
                """
                Принял сумму чаевых и собрал draft благодарности #%s.

                Получатель: %s
                Сумма: %s ₽

                Подтверждаете?
                """.formatted(order.id(), staffName(order), rubles(order.amountMinor())),
                BotState.TIP_CONFIRMATION.name(),
                false,
                false,
                true,
                false,
                AdminAlert.none(),
                List.of("SMART_TIP", "TIP_CONFIRMATION")
        ).withMetadata(Map.of(
                "scenario", id(),
                "paymentBoundary", "SBP_FUTURE_INTEGRATION",
                "tipOrderId", order.id()
        ));
    }

    private OutgoingMessage confirmTip(IncomingMessage incoming, String text) {
        if (isConfirmIntent(text)) {
            fsmStorage.setState(incoming.chatId(), BotState.READY_FOR_DIALOG);
            return OutgoingMessage.of(
                    incoming,
                    "Зафиксировал draft благодарности. Следующий слой подключит СБП-ссылку напрямую к сотруднику, а сейчас я вернул вас в главное меню.",
                    BotState.READY_FOR_DIALOG.name(),
                    false,
                    false,
                    true,
                    false,
                    AdminAlert.none(),
                    List.of("SMART_TIP", "TIP_DRAFT_CONFIRMED", "RETURN_MAIN_MENU")
            ).withMetadata(Map.of("scenario", id()));
        }
        if (isRejectIntent(text)) {
            fsmStorage.setState(incoming.chatId(), BotState.READY_FOR_DIALOG);
            return OutgoingMessage.of(
                    incoming,
                    "Хорошо, чаевые не фиксирую. Возвращаюсь в главное меню.",
                    BotState.READY_FOR_DIALOG.name(),
                    false,
                    false,
                    true,
                    false,
                    AdminAlert.none(),
                    List.of("SMART_TIP", "TIP_CANCELLED", "RETURN_MAIN_MENU")
            ).withMetadata(Map.of("scenario", id()));
        }
        fsmStorage.setState(incoming.chatId(), BotState.TIP_CONFIRMATION);
        return OutgoingMessage.of(
                incoming,
                "Подтвердите, пожалуйста: да — зафиксировать draft чаевых, нет — отменить.",
                BotState.TIP_CONFIRMATION.name(),
                false,
                false,
                true,
                false,
                AdminAlert.none(),
                List.of("SMART_TIP", "ASK_CONFIRMATION")
        ).withMetadata(Map.of("scenario", id()));
    }

    private boolean isSmartTipIntent(String text) {
        return containsAny(text, "чаевые", "поблагодарить", "спасибо официанту", "благодарность", "на чай", "накинуть", "тип", "официанту", "бармену");
    }

    private boolean hasMoney(String text) {
        return MONEY.matcher(text).matches()
                || text.contains("тысяч")
                || text.contains("тысячи")
                || text.contains("руб");
    }

    private TipOrderCommand tipOrderCommand(IncomingMessage incoming, String text) {
        return new TipOrderCommand(
                incoming.chatId(),
                incoming.telegramUserId(),
                null,
                "AERIS",
                null,
                null,
                amountMinor(text),
                "RUB",
                displayName(incoming),
                text
        );
    }

    private Long amountMinor(String text) {
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("\\d{2,7}").matcher(text);
        if (matcher.find()) {
            return Long.parseLong(matcher.group()) * 100L;
        }
        if (text.contains("тысяч") || text.contains("тысячи")) {
            return 100_000L;
        }
        return null;
    }

    private String rubles(Long amountMinor) {
        if (amountMinor == null) {
            return "уточняется";
        }
        return String.valueOf(amountMinor / 100L);
    }

    private String staffName(TipOrder order) {
        if (order.staffDisplayName() == null || order.staffDisplayName().isBlank()) {
            return "команда AERIS";
        }
        return order.staffDisplayName();
    }

    private String displayName(IncomingMessage incoming) {
        String firstName = incoming.firstName() == null ? "" : incoming.firstName().trim();
        String lastName = incoming.lastName() == null ? "" : incoming.lastName().trim();
        String username = incoming.username() == null ? "" : incoming.username().trim();
        String fullName = (firstName + " " + lastName).trim();
        if (!fullName.isBlank()) {
            return fullName;
        }
        if (!username.isBlank()) {
            return "@" + username;
        }
        return "unknown";
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
