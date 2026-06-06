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
class MainMenuScenarioTest {

    @Mock
    private FSMStorage fsmStorage;

    private MainMenuScenario scenario;

    @BeforeEach
    void setUp() {
        scenario = new MainMenuScenario(fsmStorage);
    }

    @Test
    void showsMainMenuFromReadyState() {
        IncomingMessage incoming = telegram("меню");

        OutgoingMessage outgoing = scenario.handle(incoming, BotState.READY_FOR_DIALOG, incoming.text());

        assertThat(outgoing.nextState()).isEqualTo(BotState.READY_FOR_DIALOG.name());
        assertThat(outgoing.text()).contains("Забронировать стол", "Благотворительность / аукцион");
        assertThat(outgoing.actions()).containsExactly("MAIN_MENU", "SHOW_MENU");
        verify(fsmStorage).setState(incoming.chatId(), BotState.READY_FOR_DIALOG);
    }

    @Test
    void routesSmartTipWithAmountToConfirmation() {
        IncomingMessage incoming = telegram("хочу оставить чаевые 1000");

        OutgoingMessage outgoing = scenario.handle(incoming, BotState.READY_FOR_DIALOG, incoming.text());

        assertThat(outgoing.nextState()).isEqualTo(BotState.TIP_CONFIRMATION.name());
        assertThat(outgoing.text()).contains("сумму чаевых", "Подтверждаете");
        assertThat(outgoing.actions()).containsExactly("SMART_TIP", "TIP_CONFIRMATION");
        verify(fsmStorage).setState(incoming.chatId(), BotState.TIP_CONFIRMATION);
    }

    @Test
    void asksDonationAmountAndKeepsDonationState() {
        IncomingMessage incoming = telegram("хочу поддержать благотворительный проект");

        OutgoingMessage outgoing = scenario.handle(incoming, BotState.READY_FOR_DIALOG, incoming.text());

        assertThat(outgoing.nextState()).isEqualTo(BotState.DONATION_COLLECT_AMOUNT.name());
        assertThat(outgoing.text()).contains("Hidden Heart", "анонимным");
        assertThat(outgoing.actions()).containsExactly("HIDDEN_HEART", "ASK_DONATION_AMOUNT");
        verify(fsmStorage).setState(incoming.chatId(), BotState.DONATION_COLLECT_AMOUNT);
    }

    @Test
    void continuesDonationAmountToConfirmation() {
        IncomingMessage incoming = telegram("5000");

        OutgoingMessage outgoing = scenario.handle(incoming, BotState.DONATION_COLLECT_AMOUNT, incoming.text());

        assertThat(outgoing.nextState()).isEqualTo(BotState.DONATION_CONFIRMATION.name());
        assertThat(outgoing.actions()).contains("DONATION_CONFIRMATION", "IMPACT_EVENT_DRAFT");
        verify(fsmStorage).setState(incoming.chatId(), BotState.DONATION_CONFIRMATION);
    }

    @Test
    void confirmsSmartTipDraftAndReturnsHome() {
        IncomingMessage incoming = telegram("да");

        OutgoingMessage outgoing = scenario.handle(incoming, BotState.TIP_CONFIRMATION, incoming.text());

        assertThat(outgoing.nextState()).isEqualTo(BotState.READY_FOR_DIALOG.name());
        assertThat(outgoing.text()).contains("draft благодарности");
        assertThat(outgoing.actions()).containsExactly("SMART_TIP", "TIP_DRAFT_CONFIRMED", "RETURN_MAIN_MENU");
        verify(fsmStorage).setState(incoming.chatId(), BotState.READY_FOR_DIALOG);
    }

    @Test
    void cancelsSmartTipDraftAndReturnsHome() {
        IncomingMessage incoming = telegram("нет");

        OutgoingMessage outgoing = scenario.handle(incoming, BotState.TIP_CONFIRMATION, incoming.text());

        assertThat(outgoing.nextState()).isEqualTo(BotState.READY_FOR_DIALOG.name());
        assertThat(outgoing.text()).contains("чаевые не фиксирую");
        assertThat(outgoing.actions()).containsExactly("SMART_TIP", "TIP_CANCELLED", "RETURN_MAIN_MENU");
        verify(fsmStorage).setState(incoming.chatId(), BotState.READY_FOR_DIALOG);
    }

    @Test
    void confirmsDonationDraftAndReturnsHome() {
        IncomingMessage incoming = telegram("подтверждаю");

        OutgoingMessage outgoing = scenario.handle(incoming, BotState.DONATION_CONFIRMATION, incoming.text());

        assertThat(outgoing.nextState()).isEqualTo(BotState.READY_FOR_DIALOG.name());
        assertThat(outgoing.text()).contains("анонимный donation draft", "без приватных данных");
        assertThat(outgoing.actions()).containsExactly(
                "HIDDEN_HEART",
                "DONATION_DRAFT_CONFIRMED",
                "IMPACT_EVENT_DRAFT",
                "RETURN_MAIN_MENU"
        );
        verify(fsmStorage).setState(incoming.chatId(), BotState.READY_FOR_DIALOG);
    }

    @Test
    void validatesAuctionBidInsteadOfAcceptingItDirectly() {
        IncomingMessage incoming = telegram("ставлю 20000 за картину");

        OutgoingMessage outgoing = scenario.handle(incoming, BotState.READY_FOR_DIALOG, incoming.text());

        assertThat(outgoing.nextState()).isEqualTo(BotState.AUCTION_WAIT_BID.name());
        assertThat(outgoing.text()).contains("проверю активный лот", "LLM ставку сам не принимает");
        assertThat(outgoing.actions()).containsExactly("ART_AUCTION", "VALIDATE_AUCTION_BID", "ASK_EXPLICIT_CONFIRMATION");
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

    @Test
    void returnsAggregatedImpactSummary() {
        IncomingMessage incoming = telegram("сколько собрали?");

        OutgoingMessage outgoing = scenario.handle(incoming, BotState.READY_FOR_DIALOG, incoming.text());

        assertThat(outgoing.nextState()).isEqualTo(BotState.READY_FOR_DIALOG.name());
        assertThat(outgoing.text()).contains("агрегированные итоги", "без приватных");
        assertThat(outgoing.actions()).containsExactly("IMPACT_METER", "SHOW_IMPACT_SUMMARY");
        verify(fsmStorage).setState(incoming.chatId(), BotState.READY_FOR_DIALOG);
    }

    @Test
    void doesNotStealTableBookingWithTodayWord() {
        IncomingMessage incoming = telegram("хочу столик сегодня на двоих");

        assertThat(scenario.supports(incoming, BotState.READY_FOR_DIALOG, incoming.text())).isFalse();
    }

    @Test
    void safeExitStopsActiveScenario() {
        IncomingMessage incoming = telegram("стоп");

        OutgoingMessage outgoing = scenario.handle(incoming, BotState.AUCTION_WAIT_BID, incoming.text());

        assertThat(outgoing.nextState()).isEqualTo(BotState.READY_FOR_DIALOG.name());
        assertThat(outgoing.text()).contains("Остановил текущий сценарий");
        assertThat(outgoing.actions()).containsExactly("SAFE_EXIT", "OPEN_MAIN_MENU");
        verify(fsmStorage).setState(incoming.chatId(), BotState.READY_FOR_DIALOG);
    }

    private IncomingMessage telegram(String text) {
        return IncomingMessage.telegram(
                1773317437L,
                1773317437L,
                356,
                284069928,
                text,
                null,
                "Наталья",
                "Поединенко",
                "Poedinenko",
                "ru",
                false,
                "284069928"
        );
    }
}
