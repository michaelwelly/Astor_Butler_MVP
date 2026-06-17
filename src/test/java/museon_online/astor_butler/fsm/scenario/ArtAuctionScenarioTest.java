package museon_online.astor_butler.fsm.scenario;

import museon_online.astor_butler.domain.auction.ArtAuctionBid;
import museon_online.astor_butler.domain.auction.ArtAuctionBidCommand;
import museon_online.astor_butler.domain.auction.ArtAuctionBidStatus;
import museon_online.astor_butler.domain.auction.ArtAuctionService;
import museon_online.astor_butler.fsm.core.BotState;
import museon_online.astor_butler.fsm.storage.FSMStorage;
import museon_online.astor_butler.service.message.IncomingMessage;
import museon_online.astor_butler.service.message.OutgoingMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ArtAuctionScenarioTest {

    @Mock
    private FSMStorage fsmStorage;

    @Mock
    private ArtAuctionService artAuctionService;

    private ArtAuctionScenario scenario;

    @BeforeEach
    void setUp() {
        scenario = new ArtAuctionScenario(fsmStorage, artAuctionService);
    }

    @Test
    void asksForBidWhenAuctionIntentHasNoAmount() {
        IncomingMessage incoming = telegram("покажи лот аукциона");

        OutgoingMessage outgoing = scenario.handle(incoming, BotState.READY_FOR_DIALOG, incoming.text());

        assertThat(outgoing.nextState()).isEqualTo(BotState.AUCTION_WAIT_BID.name());
        assertThat(outgoing.actions()).containsExactly("ART_AUCTION", "ASK_AUCTION_BID");
        assertThat(outgoing.metadata()).containsEntry("requiresActiveLot", true);
        verify(fsmStorage).setState(incoming.chatId(), BotState.AUCTION_WAIT_BID);
    }

    @Test
    void validatesBidInsteadOfAcceptingItDirectly() {
        IncomingMessage incoming = telegram("ставлю 20000 за картину");
        when(artAuctionService.createBidDraft(any(ArtAuctionBidCommand.class))).thenReturn(auctionBid());

        OutgoingMessage outgoing = scenario.handle(incoming, BotState.READY_FOR_DIALOG, incoming.text());

        assertThat(outgoing.nextState()).isEqualTo(BotState.AUCTION_WAIT_BID.name());
        assertThat(outgoing.text()).contains("заявку на ставку #77", "20000 ₽", "Лот: #12", "Подтверждаете");
        assertThat(outgoing.actions()).containsExactly("ART_AUCTION", "VALIDATE_AUCTION_BID", "ASK_EXPLICIT_CONFIRMATION");
        assertThat(outgoing.metadata()).containsEntry("auctionBidId", 77L);
        assertThat(outgoing.metadata()).containsEntry("lotId", 12L);
        assertThat(outgoing.metadata()).containsEntry("amountMinor", 2_000_000L);
        assertThat(outgoing.metadata()).containsEntry("requiresManagerValidation", true);
        verify(fsmStorage).setState(incoming.chatId(), BotState.AUCTION_WAIT_BID);
    }

    @Test
    void confirmsAuctionBidAsManagerCheckedRequest() {
        IncomingMessage incoming = telegram("ok");
        when(artAuctionService.confirmLatestBidDraft(incoming.chatId())).thenReturn(confirmedAuctionBid());

        OutgoingMessage outgoing = scenario.handle(incoming, BotState.AUCTION_WAIT_BID, incoming.text());

        assertThat(outgoing.nextState()).isEqualTo(BotState.READY_FOR_DIALOG.name());
        assertThat(outgoing.text()).contains("ставки #77", "20000 ₽", "Финальный прием ставки требует проверки");
        assertThat(outgoing.actions()).containsExactly(
                "ART_AUCTION",
                "AUCTION_BID_GUEST_CONFIRMED",
                "MANAGER_CONFIRMATION_REQUIRED",
                "RETURN_MAIN_MENU"
        );
        assertThat(outgoing.metadata()).containsEntry("auctionBidId", 77L);
        assertThat(outgoing.metadata()).containsEntry("auctionBidStatus", "AWAITING_MANAGER_VALIDATION");
        verify(artAuctionService).confirmLatestBidDraft(incoming.chatId());
        verify(fsmStorage).setState(incoming.chatId(), BotState.READY_FOR_DIALOG);
    }

    @Test
    void cancelsAuctionBidAndReturnsHome() {
        IncomingMessage incoming = telegram("нет");
        when(artAuctionService.cancelLatestBidDraft(incoming.chatId())).thenReturn(cancelledAuctionBid());

        OutgoingMessage outgoing = scenario.handle(incoming, BotState.AUCTION_WAIT_BID, incoming.text());

        assertThat(outgoing.nextState()).isEqualTo(BotState.READY_FOR_DIALOG.name());
        assertThat(outgoing.text()).contains("отменил заявку на ставку #77");
        assertThat(outgoing.actions()).containsExactly("ART_AUCTION", "AUCTION_BID_CANCELLED", "RETURN_MAIN_MENU");
        assertThat(outgoing.metadata()).containsEntry("auctionBidId", 77L);
        assertThat(outgoing.metadata()).containsEntry("auctionBidStatus", "CANCELLED");
        verify(artAuctionService).cancelLatestBidDraft(incoming.chatId());
        verify(fsmStorage).setState(incoming.chatId(), BotState.READY_FOR_DIALOG);
    }

    private IncomingMessage telegram(String text) {
        return IncomingMessage.telegram(
                1773317437L,
                1773317437L,
                351,
                284069875,
                text,
                null,
                "Наталья",
                "Поединенко",
                "Poedinenko",
                "ru",
                false,
                "284069875"
        );
    }

    private ArtAuctionBid auctionBid() {
        return new ArtAuctionBid(
                77L,
                12L,
                1773317437L,
                1773317437L,
                null,
                ArtAuctionBidStatus.AWAITING_GUEST_CONFIRMATION,
                "TELEGRAM",
                2_000_000L,
                "RUB",
                "Наталья Поединенко",
                "ставлю 20000 за картину",
                null,
                Instant.parse("2026-06-15T10:00:00Z"),
                Instant.parse("2026-06-15T10:00:00Z")
        );
    }

    private ArtAuctionBid confirmedAuctionBid() {
        return auctionBid(ArtAuctionBidStatus.AWAITING_MANAGER_VALIDATION);
    }

    private ArtAuctionBid cancelledAuctionBid() {
        return auctionBid(ArtAuctionBidStatus.CANCELLED);
    }

    private ArtAuctionBid auctionBid(ArtAuctionBidStatus status) {
        return new ArtAuctionBid(
                77L,
                12L,
                1773317437L,
                1773317437L,
                null,
                status,
                "TELEGRAM",
                2_000_000L,
                "RUB",
                "Наталья Поединенко",
                "ставлю 20000 за картину",
                null,
                Instant.parse("2026-06-15T10:00:00Z"),
                Instant.parse("2026-06-15T10:00:00Z")
        );
    }
}
