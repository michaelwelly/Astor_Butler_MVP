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
class EventBookingScenarioTest {

    @Mock
    private FSMStorage fsmStorage;

    private EventBookingScenario scenario;

    @BeforeEach
    void setUp() {
        scenario = new EventBookingScenario(fsmStorage);
        ReflectionTestUtils.setField(scenario, "adminChatId", "100500");
    }

    @Test
    void asksForStructuredDetailsWhenEventRequestIsTooShort() {
        IncomingMessage incoming = telegram("хочу банкет");

        OutgoingMessage outgoing = scenario.handle(incoming, BotState.READY_FOR_DIALOG, incoming.text());

        assertThat(outgoing.nextState()).isEqualTo(BotState.EVENT_BOOKING_COLLECT_DETAILS.name());
        assertThat(outgoing.adminAlert().required()).isFalse();
        assertThat(outgoing.actions()).containsExactly("EVENT_BOOKING", "ASK_EVENT_DETAILS");
        assertThat(outgoing.text()).contains("дата", "количество гостей", "пожелания");
        verify(fsmStorage).setState(incoming.chatId(), BotState.EVENT_BOOKING_COLLECT_DETAILS);
    }

    @Test
    void sendsStructuredEventRequestToAdminWhenDetailsAreEnough() {
        IncomingMessage incoming = telegram("день рождения 20 июня в 19:00 на 25 гостей, нужен банкет");

        OutgoingMessage outgoing = scenario.handle(incoming, BotState.READY_FOR_DIALOG, incoming.text());

        assertThat(outgoing.nextState()).isEqualTo(BotState.READY_FOR_DIALOG.name());
        assertThat(outgoing.adminAlert().required()).isTrue();
        assertThat(outgoing.adminAlert().chatId()).isEqualTo("100500");
        assertThat(outgoing.adminAlert().text()).contains("Astor Butler / event booking", "25 гостей", "Автоподтверждения нет");
        assertThat(outgoing.actions()).containsExactly("EVENT_BOOKING", "EVENT_REQUEST_SENT", "ADMIN_ALERT", "RETURN_MAIN_MENU");
        verify(fsmStorage).setState(incoming.chatId(), BotState.READY_FOR_DIALOG);
    }

    @Test
    void continuationAlwaysSendsRequestAfterDetailsMessage() {
        IncomingMessage incoming = telegram("20 июня в 19:00 на 25 гостей, банкет и винное сопровождение");

        assertThat(scenario.supports(incoming, BotState.EVENT_BOOKING_COLLECT_DETAILS, incoming.text())).isTrue();

        OutgoingMessage outgoing = scenario.handle(incoming, BotState.EVENT_BOOKING_COLLECT_DETAILS, incoming.text());

        assertThat(outgoing.nextState()).isEqualTo(BotState.READY_FOR_DIALOG.name());
        assertThat(outgoing.actions()).containsExactly("EVENT_BOOKING", "EVENT_REQUEST_SENT", "ADMIN_ALERT", "RETURN_MAIN_MENU");
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
