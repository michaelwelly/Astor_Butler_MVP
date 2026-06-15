package museon_online.astor_butler.fsm.scenario;

import lombok.RequiredArgsConstructor;
import museon_online.astor_butler.domain.merch.MerchOrder;
import museon_online.astor_butler.domain.merch.MerchOrderCommand;
import museon_online.astor_butler.domain.merch.MerchService;
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
public class MerchScenario implements FsmScenario {

    private final FSMStorage fsmStorage;
    private final MerchService merchService;

    @Value("${telegram.admin.chat-id:}")
    private String adminChatId;

    @Override
    public String id() {
        return "MERCH";
    }

    @Override
    public int priority() {
        return 38;
    }

    @Override
    public boolean supports(IncomingMessage incoming, BotState currentState, String text) {
        BotState state = currentState == null ? BotState.UNKNOWN : currentState.canonical();
        String normalized = normalize(text);
        if (normalized.isBlank()) {
            return false;
        }
        return owns(state) || isMerchIntent(normalized);
    }

    @Override
    public OutgoingMessage handle(IncomingMessage incoming, BotState currentState, String text) {
        BotState state = currentState == null ? BotState.UNKNOWN : currentState.canonical();
        String normalized = normalize(text);
        if (state == BotState.MERCH_COLLECT_REQUEST) {
            return sendMerchRequest(incoming, currentState, text, "MERCH_DETAILS_RECEIVED");
        }
        if (isShortMerchCall(normalized)) {
            fsmStorage.setState(incoming.chatId(), BotState.MERCH_COLLECT_REQUEST);
            return OutgoingMessage.of(
                    incoming,
                    "Могу передать запрос по мерчу команде. Напишите, что интересно: сабражная цепь, сувенир, подарок или другой предмет.",
                    BotState.MERCH_COLLECT_REQUEST.name(),
                    false,
                    false,
                    true,
                    false,
                    AdminAlert.none(),
                    List.of("MERCH", "ASK_MERCH_DETAILS")
            ).withMetadata(Map.of("scenario", id()));
        }
        return sendMerchRequest(incoming, currentState, text, "MERCH_DIRECT_REQUEST");
    }

    @Override
    public boolean owns(BotState state) {
        BotState canonical = state == null ? BotState.UNKNOWN : state.canonical();
        return canonical == BotState.MERCH_COLLECT_REQUEST || canonical == BotState.MERCH_SENT;
    }

    @Override
    public boolean sideEffecting() {
        return true;
    }

    private OutgoingMessage sendMerchRequest(
            IncomingMessage incoming,
            BotState previousState,
            String text,
            String reasonAction
    ) {
        MerchOrder order = merchService.createOrder(merchOrderCommand(incoming, text));
        fsmStorage.setState(incoming.chatId(), BotState.READY_FOR_DIALOG);
        return OutgoingMessage.of(
                incoming,
                "Создал заявку по мерчу #%s и передал команде. Пока это заявка без оплаты и без обещания наличия; менеджер подтвердит детали вручную."
                        .formatted(order.id()),
                BotState.READY_FOR_DIALOG.name(),
                false,
                false,
                true,
                false,
                adminAlert(incoming, previousState, text),
                List.of("MERCH", reasonAction, "ADMIN_ALERT", "RETURN_MAIN_MENU")
        ).withMetadata(Map.of(
                "scenario", id(),
                "merchOrderId", order.id(),
                "itemId", order.itemId() == null ? "" : order.itemId(),
                "itemTitle", order.itemTitle() == null ? "" : order.itemTitle(),
                "orderBoundary", "MANUAL_CONFIRMATION_REQUIRED"
        ));
    }

    private MerchOrderCommand merchOrderCommand(IncomingMessage incoming, String text) {
        return new MerchOrderCommand(
                incoming.chatId(),
                incoming.telegramUserId(),
                null,
                "AERIS",
                null,
                null,
                1,
                displayName(incoming),
                text,
                "TEAM_CONFIRMATION_REQUIRED"
        );
    }

    private AdminAlert adminAlert(IncomingMessage incoming, BotState previousState, String text) {
        if (adminChatId == null || adminChatId.isBlank()) {
            return AdminAlert.none();
        }
        String body = """
                <b>Astor Butler / merch</b>
                Гость интересуется мерчом

                <b>Гость</b>
                %s
                chat %s / user %s%s

                <b>Запрос</b>
                <blockquote>%s</blockquote>

                <b>Контекст</b>
                Previous state: %s
                Scenario: MERCH

                <b>Действие</b>
                Проверьте наличие/цену и ответьте гостю вручную или через будущий merch order-flow.

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

    private boolean isMerchIntent(String text) {
        return containsAny(text, "мерч", "сувенир", "подарок", "сабраж", "цепь", "цепочку", "купить цеп");
    }

    private boolean isShortMerchCall(String text) {
        return text.equals("мерч")
                || text.equals("сувенир")
                || text.equals("сувениры")
                || text.equals("/merch")
                || text.equals("купить мерч");
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
