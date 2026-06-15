package museon_online.astor_butler.fsm.scenario;

import museon_online.astor_butler.fsm.core.BotState;
import museon_online.astor_butler.fsm.storage.FSMStorage;
import museon_online.astor_butler.service.message.IncomingMessage;
import museon_online.astor_butler.service.message.OutgoingMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ArtAuctionScenarioTest {

    @Mock
    private FSMStorage fsmStorage;

    private ArtAuctionScenario scenario;

    @BeforeEach
    void setUp() {
        scenario = new ArtAuctionScenario(fsmStorage);
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

        OutgoingMessage outgoing = scenario.handle(incoming, BotState.READY_FOR_DIALOG, incoming.text());

        assertThat(outgoing.nextState()).isEqualTo(BotState.AUCTION_WAIT_BID.name());
        assertThat(outgoing.text()).contains("проверю активный лот", "LLM ставку сам не принимает");
        assertThat(outgoing.actions()).containsExactly("ART_AUCTION", "VALIDATE_AUCTION_BID", "ASK_EXPLICIT_CONFIRMATION");
        assertThat(outgoing.metadata()).containsEntry("requiresManagerValidation", true);
        verify(fsmStorage).setState(incoming.chatId(), BotState.AUCTION_WAIT_BID);
    }

    @Test
    void confirmsAuctionBidAsManagerCheckedRequest() {
        IncomingMessage incoming = telegram("ok");

        OutgoingMessage outgoing = scenario.handle(incoming, BotState.AUCTION_WAIT_BID, incoming.text());

        assertThat(outgoing.nextState()).isEqualTo(BotState.READY_FOR_DIALOG.name());
        assertThat(outgoing.text()).contains("Финальный прием ставки требует проверки");
        assertThat(outgoing.actions()).containsExactly(
                "ART_AUCTION",
                "AUCTION_BID_GUEST_CONFIRMED",
                "MANAGER_CONFIRMATION_REQUIRED",
                "RETURN_MAIN_MENU"
        );
        verify(fsmStorage).setState(incoming.chatId(), BotState.READY_FOR_DIALOG);
    }

    @Test
    void cancelsAuctionBidAndReturnsHome() {
        IncomingMessage incoming = telegram("нет");

        OutgoingMessage outgoing = scenario.handle(incoming, BotState.AUCTION_WAIT_BID, incoming.text());

        assertThat(outgoing.nextState()).isEqualTo(BotState.READY_FOR_DIALOG.name());
        assertThat(outgoing.text()).contains("ставку не фиксирую");
        assertThat(outgoing.actions()).containsExactly("ART_AUCTION", "AUCTION_BID_CANCELLED", "RETURN_MAIN_MENU");
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
}
