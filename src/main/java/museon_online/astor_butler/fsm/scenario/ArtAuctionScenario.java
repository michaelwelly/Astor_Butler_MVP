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
public class ArtAuctionScenario implements FsmScenario {

    private static final Pattern MONEY = Pattern.compile(".*\\b\\d{2,8}\\b.*");

    private final FSMStorage fsmStorage;

    @Override
    public String id() {
        return "ART_AUCTION";
    }

    @Override
    public int priority() {
        return 64;
    }

    @Override
    public boolean supports(IncomingMessage incoming, BotState currentState, String text) {
        BotState state = currentState == null ? BotState.UNKNOWN : currentState.canonical();
        String normalized = normalize(text);
        if (normalized.isBlank()) {
            return false;
        }
        return owns(state) || isAuctionIntent(normalized);
    }

    @Override
    public OutgoingMessage handle(IncomingMessage incoming, BotState currentState, String text) {
        BotState state = currentState == null ? BotState.UNKNOWN : currentState.canonical();
        String normalized = normalize(text);
        if (state == BotState.AUCTION_WAIT_BID) {
            return continueAuction(incoming, normalized);
        }
        return collectBid(incoming, normalized);
    }

    @Override
    public boolean owns(BotState state) {
        BotState canonical = state == null ? BotState.UNKNOWN : state.canonical();
        return canonical == BotState.AUCTION_RUNNING || canonical == BotState.AUCTION_WAIT_BID;
    }

    @Override
    public boolean sideEffecting() {
        return true;
    }

    private OutgoingMessage collectBid(IncomingMessage incoming, String text) {
        fsmStorage.setState(incoming.chatId(), BotState.AUCTION_WAIT_BID);
        if (!hasMoney(text)) {
            return OutgoingMessage.of(
                    incoming,
                    "Аукцион по картинам работает только при активном лоте события. Напишите сумму ставки или попросите менеджера показать текущий лот.",
                    BotState.AUCTION_WAIT_BID.name(),
                    false,
                    false,
                    true,
                    false,
                    AdminAlert.none(),
                    List.of("ART_AUCTION", "ASK_AUCTION_BID")
            ).withMetadata(Map.of(
                    "scenario", id(),
                    "requiresActiveLot", true
            ));
        }

        return OutgoingMessage.of(
                incoming,
                "Ставку вижу. Перед тем как принять ее, проверю активный лот и минимальный шаг, затем попрошу явное подтверждение. LLM ставку сам не принимает.",
                BotState.AUCTION_WAIT_BID.name(),
                false,
                false,
                true,
                false,
                AdminAlert.none(),
                List.of("ART_AUCTION", "VALIDATE_AUCTION_BID", "ASK_EXPLICIT_CONFIRMATION")
        ).withMetadata(Map.of(
                "scenario", id(),
                "requiresActiveLot", true,
                "requiresManagerValidation", true
        ));
    }

    private OutgoingMessage continueAuction(IncomingMessage incoming, String text) {
        if (isConfirmIntent(text)) {
            fsmStorage.setState(incoming.chatId(), BotState.READY_FOR_DIALOG);
            return OutgoingMessage.of(
                    incoming,
                    "Принял подтверждение ставки как заявку к активному лоту. Финальный прием ставки требует проверки лота, минимального шага и подтверждения event owner.",
                    BotState.READY_FOR_DIALOG.name(),
                    false,
                    false,
                    true,
                    false,
                    AdminAlert.none(),
                    List.of("ART_AUCTION", "AUCTION_BID_GUEST_CONFIRMED", "MANAGER_CONFIRMATION_REQUIRED", "RETURN_MAIN_MENU")
            ).withMetadata(Map.of("scenario", id()));
        }
        if (isRejectIntent(text)) {
            fsmStorage.setState(incoming.chatId(), BotState.READY_FOR_DIALOG);
            return OutgoingMessage.of(
                    incoming,
                    "Хорошо, ставку не фиксирую. Можно остаться наблюдателем или вернуться к другому сценарию.",
                    BotState.READY_FOR_DIALOG.name(),
                    false,
                    false,
                    true,
                    false,
                    AdminAlert.none(),
                    List.of("ART_AUCTION", "AUCTION_BID_CANCELLED", "RETURN_MAIN_MENU")
            ).withMetadata(Map.of("scenario", id()));
        }
        return collectBid(incoming, text);
    }

    private boolean isAuctionIntent(String text) {
        return containsAny(text, "аукцион", "картина", "ставка", "ставлю", "лот");
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
