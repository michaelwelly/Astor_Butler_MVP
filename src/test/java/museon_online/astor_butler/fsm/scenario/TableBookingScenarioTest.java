package museon_online.astor_butler.fsm.scenario;

import museon_online.astor_butler.fsm.core.BotState;
import museon_online.astor_butler.fsm.storage.FSMStorage;
import museon_online.astor_butler.domain.booking.TableReservationCommand;
import museon_online.astor_butler.domain.booking.TableReservationOrder;
import museon_online.astor_butler.domain.booking.TableReservationService;
import museon_online.astor_butler.domain.booking.TableReservationStatus;
import museon_online.astor_butler.service.message.IncomingMessage;
import museon_online.astor_butler.service.message.OutgoingMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TableBookingScenarioTest {

    @Mock
    private FSMStorage fsmStorage;

    @Mock
    private TableBookingDraftStorage draftStorage;

    @Mock
    private TableReservationService tableReservationService;

    private TableBookingScenario scenario;

    @BeforeEach
    void setUp() {
        scenario = new TableBookingScenario(fsmStorage, draftStorage, tableReservationService);
        ReflectionTestUtils.setField(scenario, "planPdfPath", "classpath:booking/aeris-plan.pdf");
        ReflectionTestUtils.setField(scenario, "defaultVenueCode", "AERIS");
        ReflectionTestUtils.setField(scenario, "managerTelegramId", 876857557L);
        ReflectionTestUtils.setField(scenario, "hostessChatId", "-1004291419562");
    }

    @Test
    void sendsHallPlanForCompleteInitialTableBookingRequest() {
        IncomingMessage incoming = telegram("Хочу забронировать столик завтра на 20:00 на двоих");

        OutgoingMessage outgoing = scenario.handle(incoming, BotState.READY_FOR_DIALOG, incoming.text());

        assertThat(outgoing.nextState()).isEqualTo(BotState.TABLE_BOOKING_WAIT_TABLE_SELECTION.name());
        assertThat(outgoing.actions()).contains("SEND_HALL_PLAN", "ASK_TABLE_SELECTION");
        assertThat(outgoing.metadata()).containsEntry("documentResource", "classpath:booking/aeris-plan.pdf");
        verify(fsmStorage).setState(incoming.chatId(), BotState.TABLE_BOOKING_WAIT_TABLE_SELECTION);
        verify(draftStorage).save(any(), any());
    }

    @Test
    void resendsHallPlanWhenGuestRepeatsBookingIntentDuringTableSelection() {
        IncomingMessage incoming = telegram("Хочу забронировать столик завтра на 20:00 на двоих");

        OutgoingMessage outgoing = scenario.handle(incoming, BotState.TABLE_BOOKING_WAIT_TABLE_SELECTION, incoming.text());

        assertThat(outgoing.nextState()).isEqualTo(BotState.TABLE_BOOKING_WAIT_TABLE_SELECTION.name());
        assertThat(outgoing.actions()).contains("SEND_HALL_PLAN", "ASK_TABLE_SELECTION");
        assertThat(outgoing.metadata()).containsEntry("documentResource", "classpath:booking/aeris-plan.pdf");
    }

    @Test
    void acceptsExplicitTableSelectionDuringTableSelection() {
        IncomingMessage incoming = telegram("17");
        when(draftStorage.find(incoming.chatId())).thenReturn(Optional.of(draft()));
        when(tableReservationService.createReservation(any(TableReservationCommand.class))).thenReturn(order(44L));

        OutgoingMessage outgoing = scenario.handle(incoming, BotState.TABLE_BOOKING_WAIT_TABLE_SELECTION, incoming.text());

        assertThat(outgoing.nextState()).isEqualTo(BotState.TABLE_BOOKING_WAIT_HOSTESS_CONFIRMATION.name());
        assertThat(outgoing.actions()).contains("TABLE_SELECTED", "RESERVATION_CREATED", "WAIT_HOSTESS_CONFIRMATION");
        verify(tableReservationService).createReservation(any(TableReservationCommand.class));
        verify(draftStorage).clear(incoming.chatId());
    }

    @Test
    void asksForDetailsWhenTableSelectedWithoutDraft() {
        IncomingMessage incoming = telegram("17");
        when(draftStorage.find(incoming.chatId())).thenReturn(Optional.empty());

        OutgoingMessage outgoing = scenario.handle(incoming, BotState.TABLE_BOOKING_WAIT_TABLE_SELECTION, incoming.text());

        assertThat(outgoing.nextState()).isEqualTo(BotState.TABLE_BOOKING_COLLECT_DATE.name());
        assertThat(outgoing.actions()).contains("ASK_BOOKING_DETAILS");
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

    private TableBookingDraftStorage.Draft draft() {
        Instant start = Instant.parse("2026-06-06T15:00:00Z");
        return new TableBookingDraftStorage.Draft("AERIS", start, start.plusSeconds(7200), 2, "Хочу забронировать столик завтра на 20:00 на двоих");
    }

    private TableReservationOrder order(Long id) {
        return new TableReservationOrder(
                id,
                1773317437L,
                1773317437L,
                null,
                17L,
                "17",
                "Table 17",
                TableReservationStatus.AWAITING_MANAGER_CONFIRMATION,
                "TELEGRAM",
                Instant.parse("2026-06-06T15:00:00Z"),
                Instant.parse("2026-06-06T17:00:00Z"),
                2,
                "Наталья Поединенко",
                null,
                "Хочу забронировать столик завтра на 20:00 на двоих",
                876857557L,
                null,
                "-1004291419562",
                null,
                Instant.parse("2026-06-05T15:00:00Z"),
                Instant.parse("2026-06-05T15:00:00Z")
        );
    }
}
