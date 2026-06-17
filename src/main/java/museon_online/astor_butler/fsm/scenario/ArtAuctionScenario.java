package museon_online.astor_butler.fsm.scenario;

import lombok.RequiredArgsConstructor;
import museon_online.astor_butler.domain.auction.ArtAuctionBid;
import museon_online.astor_butler.domain.auction.ArtAuctionBidCommand;
import museon_online.astor_butler.domain.auction.ArtAuctionService;
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
    private final ArtAuctionService artAuctionService;

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

        ArtAuctionBid bid = artAuctionService.createBidDraft(auctionBidCommand(incoming, text));

        return OutgoingMessage.of(
                incoming,
                """
                Собрал заявку на ставку #%s.

                Сумма: %s ₽
                Лот: #%s

                Перед финальным приемом ставку проверит event owner: активный лот, минимальный шаг и текущий top-5. Подтверждаете?
                """.formatted(bid.id(), rubles(bid.amountMinor()), bid.lotId()),
                BotState.AUCTION_WAIT_BID.name(),
                false,
                false,
                true,
                false,
                AdminAlert.none(),
                List.of("ART_AUCTION", "VALIDATE_AUCTION_BID", "ASK_EXPLICIT_CONFIRMATION")
        ).withMetadata(Map.of(
                "scenario", id(),
                "auctionBidId", bid.id(),
                "lotId", bid.lotId(),
                "amountMinor", bid.amountMinor(),
                "requiresActiveLot", true,
                "requiresManagerValidation", true
        ));
    }

    private OutgoingMessage continueAuction(IncomingMessage incoming, String text) {
        if (isConfirmIntent(text)) {
            ArtAuctionBid bid = artAuctionService.confirmLatestBidDraft(incoming.chatId());
            fsmStorage.setState(incoming.chatId(), BotState.READY_FOR_DIALOG);
            return OutgoingMessage.of(
                    incoming,
                    """
                            Принял подтверждение ставки #%s как заявку к активному лоту.

                            Сумма: %s ₽
                            Лот: #%s

                            Финальный прием ставки требует проверки лота, минимального шага, текущего top-5 и подтверждения event owner.
                            """.formatted(bid.id(), rubles(bid.amountMinor()), bid.lotId()),
                    BotState.READY_FOR_DIALOG.name(),
                    false,
                    false,
                    true,
                    false,
                    AdminAlert.none(),
                    List.of("ART_AUCTION", "AUCTION_BID_GUEST_CONFIRMED", "MANAGER_CONFIRMATION_REQUIRED", "RETURN_MAIN_MENU")
            ).withMetadata(Map.of(
                    "scenario", id(),
                    "auctionBidId", bid.id(),
                    "auctionBidStatus", bid.status().name(),
                    "lotId", bid.lotId(),
                    "requiresManagerValidation", true
            ));
        }
        if (isRejectIntent(text)) {
            ArtAuctionBid bid = artAuctionService.cancelLatestBidDraft(incoming.chatId());
            fsmStorage.setState(incoming.chatId(), BotState.READY_FOR_DIALOG);
            return OutgoingMessage.of(
                    incoming,
                    "Хорошо, отменил заявку на ставку #%s. Можно остаться наблюдателем или вернуться к другому сценарию.".formatted(bid.id()),
                    BotState.READY_FOR_DIALOG.name(),
                    false,
                    false,
                    true,
                    false,
                    AdminAlert.none(),
                    List.of("ART_AUCTION", "AUCTION_BID_CANCELLED", "RETURN_MAIN_MENU")
            ).withMetadata(Map.of(
                    "scenario", id(),
                    "auctionBidId", bid.id(),
                    "auctionBidStatus", bid.status().name()
            ));
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

    private ArtAuctionBidCommand auctionBidCommand(IncomingMessage incoming, String text) {
        return new ArtAuctionBidCommand(
                incoming.chatId(),
                incoming.telegramUserId(),
                null,
                "AERIS",
                null,
                amountMinor(text),
                "RUB",
                displayName(incoming),
                text
        );
    }

    private Long amountMinor(String text) {
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("\\d{2,8}").matcher(text);
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
