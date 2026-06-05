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
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TableBookingScenarioTest {

    @Mock
    private FSMStorage fsmStorage;

    private TableBookingScenario scenario;

    @BeforeEach
    void setUp() {
        scenario = new TableBookingScenario(fsmStorage);
        ReflectionTestUtils.setField(scenario, "planPdfPath", "classpath:booking/aeris-plan.pdf");
    }

    @Test
    void sendsHallPlanForCompleteInitialTableBookingRequest() {
        IncomingMessage incoming = telegram("Хочу забронировать столик завтра на 20:00 на двоих");

        OutgoingMessage outgoing = scenario.handle(incoming, BotState.READY_FOR_DIALOG, incoming.text());

        assertThat(outgoing.nextState()).isEqualTo(BotState.TABLE_BOOKING_WAIT_TABLE_SELECTION.name());
        assertThat(outgoing.actions()).contains("SEND_HALL_PLAN", "ASK_TABLE_SELECTION");
        assertThat(outgoing.metadata()).containsEntry("documentResource", "classpath:booking/aeris-plan.pdf");
        verify(fsmStorage).setState(incoming.chatId(), BotState.TABLE_BOOKING_WAIT_TABLE_SELECTION);
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
