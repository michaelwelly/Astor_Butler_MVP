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
class ImpactMeterScenarioTest {

    @Mock
    private FSMStorage fsmStorage;

    private ImpactMeterScenario scenario;

    @BeforeEach
    void setUp() {
        scenario = new ImpactMeterScenario(fsmStorage);
    }

    @Test
    void returnsAggregatedImpactSummaryWithoutPrivatePaymentData() {
        IncomingMessage incoming = telegram("сколько собрали культурного вклада?");

        OutgoingMessage outgoing = scenario.handle(incoming, BotState.READY_FOR_DIALOG, incoming.text());

        assertThat(outgoing.nextState()).isEqualTo(BotState.READY_FOR_DIALOG.name());
        assertThat(outgoing.text()).contains("агрегированные итоги", "без приватных");
        assertThat(outgoing.actions()).containsExactly("IMPACT_METER", "SHOW_IMPACT_SUMMARY");
        assertThat(outgoing.metadata()).containsEntry("privacy", "AGGREGATED_ONLY");
        assertThat(outgoing.metadata()).containsEntry("containsPrivatePaymentData", false);
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
