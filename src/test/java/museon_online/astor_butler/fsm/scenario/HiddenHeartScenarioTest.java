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
class HiddenHeartScenarioTest {

    @Mock
    private FSMStorage fsmStorage;

    private HiddenHeartScenario scenario;

    @BeforeEach
    void setUp() {
        scenario = new HiddenHeartScenario(fsmStorage);
    }

    @Test
    void asksForAmountWhenDonationIntentHasNoMoney() {
        IncomingMessage incoming = telegram("хочу помочь проекту");

        OutgoingMessage outgoing = scenario.handle(incoming, BotState.READY_FOR_DIALOG, incoming.text());

        assertThat(outgoing.nextState()).isEqualTo(BotState.DONATION_COLLECT_AMOUNT.name());
        assertThat(outgoing.actions()).containsExactly("HIDDEN_HEART", "ASK_DONATION_AMOUNT");
        assertThat(outgoing.text()).contains("Какую сумму", "анонимным");
        verify(fsmStorage).setState(incoming.chatId(), BotState.DONATION_COLLECT_AMOUNT);
    }

    @Test
    void movesToConfirmationWhenAmountIsPresent() {
        IncomingMessage incoming = telegram("донат 1000 рублей");

        OutgoingMessage outgoing = scenario.handle(incoming, BotState.READY_FOR_DIALOG, incoming.text());

        assertThat(outgoing.nextState()).isEqualTo(BotState.DONATION_CONFIRMATION.name());
        assertThat(outgoing.actions()).containsExactly("HIDDEN_HEART", "DONATION_CONFIRMATION", "IMPACT_EVENT_DRAFT");
        assertThat(outgoing.metadata()).containsEntry("privacy", "ANONYMOUS_BY_DEFAULT");
        assertThat(outgoing.metadata()).containsEntry("paymentBoundary", "SBP_FUTURE_INTEGRATION");
        verify(fsmStorage).setState(incoming.chatId(), BotState.DONATION_CONFIRMATION);
    }

    @Test
    void confirmsDonationDraftAndReturnsToMainMenu() {
        IncomingMessage incoming = telegram("да");

        OutgoingMessage outgoing = scenario.handle(incoming, BotState.DONATION_CONFIRMATION, incoming.text());

        assertThat(outgoing.nextState()).isEqualTo(BotState.READY_FOR_DIALOG.name());
        assertThat(outgoing.actions()).containsExactly(
                "HIDDEN_HEART",
                "DONATION_DRAFT_CONFIRMED",
                "IMPACT_EVENT_DRAFT",
                "RETURN_MAIN_MENU"
        );
        verify(fsmStorage).setState(incoming.chatId(), BotState.READY_FOR_DIALOG);
    }

    @Test
    void cancelsDonationDraftAndReturnsToMainMenu() {
        IncomingMessage incoming = telegram("нет");

        OutgoingMessage outgoing = scenario.handle(incoming, BotState.DONATION_CONFIRMATION, incoming.text());

        assertThat(outgoing.nextState()).isEqualTo(BotState.READY_FOR_DIALOG.name());
        assertThat(outgoing.actions()).containsExactly("HIDDEN_HEART", "DONATION_CANCELLED", "RETURN_MAIN_MENU");
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
