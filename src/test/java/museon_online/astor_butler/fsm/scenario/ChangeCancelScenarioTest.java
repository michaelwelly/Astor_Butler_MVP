package museon_online.astor_butler.fsm.scenario;

import museon_online.astor_butler.domain.booking.EventBookingOrder;
import museon_online.astor_butler.domain.booking.EventBookingService;
import museon_online.astor_butler.domain.booking.EventBookingStatus;
import museon_online.astor_butler.domain.booking.TableReservationOrder;
import museon_online.astor_butler.domain.booking.TableReservationService;
import museon_online.astor_butler.domain.booking.TableReservationStatus;
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
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChangeCancelScenarioTest {

    @Mock
    private FSMStorage fsmStorage;

    @Mock
    private TableReservationService tableReservationService;

    @Mock
    private EventBookingService eventBookingService;

    private ChangeCancelScenario scenario;

    @BeforeEach
    void setUp() {
        scenario = new ChangeCancelScenario(fsmStorage, tableReservationService, eventBookingService);
        ReflectionTestUtils.setField(scenario, "adminChatId", "100500");
    }

    @Test
    void asksForReferenceBeforeChangingBooking() {
        IncomingMessage incoming = telegram("отменить бронь");
        when(tableReservationService.listActiveReservationsByChatId(incoming.chatId())).thenReturn(List.of());
        when(eventBookingService.listActiveOrdersByChatId(incoming.chatId())).thenReturn(List.of());

        OutgoingMessage outgoing = scenario.handle(incoming, BotState.READY_FOR_DIALOG, incoming.text());

        assertThat(outgoing.nextState()).isEqualTo(BotState.TABLE_BOOKING_CHANGE_REQUESTED.name());
        assertThat(outgoing.adminAlert().required()).isFalse();
        assertThat(outgoing.actions()).containsExactly("CHANGE_CANCEL", "ASK_ACTIVE_ORDER_REFERENCE");
        assertThat(outgoing.text()).contains("дату", "время", "номер заявки");
        verify(fsmStorage).setState(incoming.chatId(), BotState.TABLE_BOOKING_CHANGE_REQUESTED);
    }

    @Test
    void showsActiveReservationsBeforeAskingReference() {
        IncomingMessage incoming = telegram("отменить бронь");
        when(tableReservationService.listActiveReservationsByChatId(incoming.chatId()))
                .thenReturn(List.of(activeReservation()));
        when(eventBookingService.listActiveOrdersByChatId(incoming.chatId())).thenReturn(List.of());

        OutgoingMessage outgoing = scenario.handle(incoming, BotState.READY_FOR_DIALOG, incoming.text());

        assertThat(outgoing.nextState()).isEqualTo(BotState.TABLE_BOOKING_CHANGE_REQUESTED.name());
        assertThat(outgoing.adminAlert().required()).isFalse();
        assertThat(outgoing.actions()).containsExactly(
                "CHANGE_CANCEL",
                "ACTIVE_RESERVATIONS_FOUND",
                "ASK_ACTIVE_ORDER_REFERENCE"
        );
        assertThat(outgoing.text()).contains("стол #44", "Окно у бара (A7)", "18.06 20:00", "Я не сниму бронь");
        assertThat(outgoing.metadata()).containsEntry("activeReservationIds", List.of(44L));
        verify(fsmStorage).setState(incoming.chatId(), BotState.TABLE_BOOKING_CHANGE_REQUESTED);
    }

    @Test
    void showsActiveEventOrdersBeforeAskingReference() {
        IncomingMessage incoming = telegram("перенести мероприятие");
        when(tableReservationService.listActiveReservationsByChatId(incoming.chatId())).thenReturn(List.of());
        when(eventBookingService.listActiveOrdersByChatId(incoming.chatId()))
                .thenReturn(List.of(activeEvent()));

        OutgoingMessage outgoing = scenario.handle(incoming, BotState.READY_FOR_DIALOG, incoming.text());

        assertThat(outgoing.nextState()).isEqualTo(BotState.TABLE_BOOKING_CHANGE_REQUESTED.name());
        assertThat(outgoing.adminAlert().required()).isFalse();
        assertThat(outgoing.actions()).containsExactly(
                "CHANGE_CANCEL",
                "ACTIVE_RESERVATIONS_FOUND",
                "ASK_ACTIVE_ORDER_REFERENCE"
        );
        assertThat(outgoing.text()).contains("мероприятие #88", "CORPORATE", "2026-06-21", "brief мероприятия");
        assertThat(outgoing.metadata()).containsEntry("activeEventOrderIds", List.of(88L));
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

    private TableReservationOrder activeReservation() {
        return new TableReservationOrder(
                44L,
                1773317437L,
                1773317437L,
                777L,
                7L,
                "A7",
                "Окно у бара",
                TableReservationStatus.CONFIRMED,
                "TELEGRAM",
                Instant.parse("2026-06-18T15:00:00Z"),
                Instant.parse("2026-06-18T17:00:00Z"),
                2,
                "Наталья Поединенко",
                null,
                null,
                876857557L,
                null,
                "-1004291419562",
                null,
                Instant.parse("2026-06-15T10:00:00Z"),
                Instant.parse("2026-06-15T10:00:00Z")
        );
    }

    private EventBookingOrder activeEvent() {
        return new EventBookingOrder(
                88L,
                1773317437L,
                1773317437L,
                null,
                "AERIS",
                EventBookingStatus.AWAITING_MANAGER_REVIEW,
                "TELEGRAM",
                "CORPORATE",
                LocalDate.parse("2026-06-21"),
                "19:00",
                30,
                null,
                null,
                null,
                null,
                "Наталья Поединенко",
                null,
                "корпоратив на 30 гостей",
                876857557L,
                null,
                "100500",
                null,
                Instant.parse("2026-06-15T10:00:00Z"),
                Instant.parse("2026-06-15T10:00:00Z")
        );
    }
}
