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
class ChangeCancelScenarioTest {

    @Mock
    private FSMStorage fsmStorage;

    private ChangeCancelScenario scenario;

    @BeforeEach
    void setUp() {
        scenario = new ChangeCancelScenario(fsmStorage);
        ReflectionTestUtils.setField(scenario, "adminChatId", "100500");
    }

    @Test
    void asksForReferenceBeforeChangingBooking() {
        IncomingMessage incoming = telegram("отменить бронь");

        OutgoingMessage outgoing = scenario.handle(incoming, BotState.READY_FOR_DIALOG, incoming.text());

        assertThat(outgoing.nextState()).isEqualTo(BotState.TABLE_BOOKING_CHANGE_REQUESTED.name());
        assertThat(outgoing.adminAlert().required()).isFalse();
        assertThat(outgoing.actions()).containsExactly("CHANGE_CANCEL", "ASK_ACTIVE_ORDER_REFERENCE");
        assertThat(outgoing.text()).contains("дату", "время", "номер заявки");
        verify(fsmStorage).setState(incoming.chatId(), BotState.TABLE_BOOKING_CHANGE_REQUESTED);
    }

    @Test
    void sendsAdminAlertWhenReferenceIsProvided() {
        IncomingMessage incoming = telegram("бронь завтра на 20:00, надо перенести");

        OutgoingMessage outgoing = scenario.handle(incoming, BotState.READY_FOR_DIALOG, incoming.text());

        assertThat(outgoing.nextState()).isEqualTo(BotState.READY_FOR_DIALOG.name());
        assertThat(outgoing.adminAlert().required()).isTrue();
        assertThat(outgoing.adminAlert().chatId()).isEqualTo("100500");
        assertThat(outgoing.adminAlert().text()).contains("Astor Butler / change or cancel", "бронь завтра на 20:00");
        assertThat(outgoing.actions()).containsExactly("CHANGE_CANCEL", "ADMIN_ALERT", "RETURN_MAIN_MENU");
        verify(fsmStorage).setState(incoming.chatId(), BotState.READY_FOR_DIALOG);
    }

    @Test
    void supportsChangeCancelIntentAndContinuation() {
        IncomingMessage incoming = telegram("перенести бронь");

        assertThat(scenario.supports(incoming, BotState.READY_FOR_DIALOG, incoming.text())).isTrue();
        assertThat(scenario.supports(incoming, BotState.TABLE_BOOKING_CHANGE_REQUESTED, "завтра 20:00")).isTrue();
        assertThat(scenario.supports(incoming, BotState.TABLE_BOOKING_COLLECT_TIME, "завтра 20:00")).isFalse();
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
