package museon_online.astor_butler.fsm.scenario;

import museon_online.astor_butler.domain.booking.EventBookingCommand;
import museon_online.astor_butler.domain.booking.EventBookingOrder;
import museon_online.astor_butler.domain.booking.EventBookingService;
import museon_online.astor_butler.domain.booking.EventBookingStatus;
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

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventBookingScenarioTest {

    @Mock
    private FSMStorage fsmStorage;

    @Mock
    private EventBookingService eventBookingService;

    private EventBookingScenario scenario;

    @BeforeEach
    void setUp() {
        scenario = new EventBookingScenario(fsmStorage, eventBookingService);
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
        when(eventBookingService.createOrder(any(EventBookingCommand.class))).thenReturn(eventOrder());

        OutgoingMessage outgoing = scenario.handle(incoming, BotState.READY_FOR_DIALOG, incoming.text());

        assertThat(outgoing.nextState()).isEqualTo(BotState.READY_FOR_DIALOG.name());
        assertThat(outgoing.adminAlert().required()).isTrue();
        assertThat(outgoing.adminAlert().chatId()).isEqualTo("100500");
        assertThat(outgoing.adminAlert().text()).contains("Astor Butler / event booking", "Order: #77", "25 гостей", "Автоподтверждения нет");
        assertThat(outgoing.actions()).containsExactly("EVENT_BOOKING", "EVENT_REQUEST_SENT", "ADMIN_ALERT", "RETURN_MAIN_MENU");
        assertThat(outgoing.metadata()).containsEntry("eventOrderId", 77L);
        verify(fsmStorage).setState(incoming.chatId(), BotState.READY_FOR_DIALOG);
    }

    @Test
    void continuationAlwaysSendsRequestAfterDetailsMessage() {
        IncomingMessage incoming = telegram("20 июня в 19:00 на 25 гостей, банкет и винное сопровождение");

        assertThat(scenario.supports(incoming, BotState.EVENT_BOOKING_COLLECT_DETAILS, incoming.text())).isTrue();
        when(eventBookingService.createOrder(any(EventBookingCommand.class))).thenReturn(eventOrder());

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

    private EventBookingOrder eventOrder() {
        return new EventBookingOrder(
                77L,
                1773317437L,
                1773317437L,
                null,
                "AERIS",
                EventBookingStatus.AWAITING_MANAGER_REVIEW,
                "TELEGRAM",
                "BIRTHDAY",
                null,
                "date signal, 19:00",
                25,
                null,
                null,
                null,
                null,
                "Наталья Поединенко",
                null,
                "день рождения 20 июня в 19:00 на 25 гостей, нужен банкет",
                876857557L,
                null,
                "100500",
                null,
                Instant.parse("2026-06-15T10:00:00Z"),
                Instant.parse("2026-06-15T10:00:00Z")
        );
    }
}
