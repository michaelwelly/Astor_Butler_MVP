package museon_online.astor_butler.api.booking;

import museon_online.astor_butler.domain.booking.TableAvailability;
import museon_online.astor_butler.domain.booking.EventBookingService;
import museon_online.astor_butler.domain.booking.TableBookingRuntimeService;
import museon_online.astor_butler.domain.booking.TableReservationOrder;
import museon_online.astor_butler.domain.booking.TableReservationService;
import museon_online.astor_butler.domain.booking.TableReservationStatus;
import museon_online.astor_butler.domain.booking.VenueTable;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BookingControllerTest {

    private final TableReservationService tableReservationService = mock(TableReservationService.class);
    private final TableBookingRuntimeService tableBookingRuntimeService = mock(TableBookingRuntimeService.class);
    private final EventBookingService eventBookingService = mock(EventBookingService.class);
    private final BookingController controller = new BookingController(
            tableReservationService,
            tableBookingRuntimeService,
            eventBookingService
    );

    @Test
    void returnsTableReservationById() {
        TableReservationOrder order = order();
        when(tableReservationService.getReservation(44L)).thenReturn(order);

        ResponseEntity<BookingController.TableReservationResponse> response = controller.getTableReservation(44L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isEqualTo(44L);
        assertThat(response.getBody().status()).isEqualTo(TableReservationStatus.AWAITING_MANAGER_CONFIRMATION);
    }

    @Test
    void listsTelegramTableReservations() {
        TableReservationOrder order = order();
        when(tableReservationService.listReservationsByChatId(1773317437L, 25)).thenReturn(List.of(order));

        ResponseEntity<List<BookingController.TableReservationResponse>> response =
                controller.listTelegramTableReservations(1773317437L, 25);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        verify(tableReservationService).listReservationsByChatId(1773317437L, 25);
    }

    @Test
    void returnsTelegramTableBookingRuntime() {
        TableBookingRuntimeService.TableBookingRuntimeView view = new TableBookingRuntimeService.TableBookingRuntimeView(
                1773317437L,
                "AERIS",
                null,
                null,
                List.of(table()),
                List.of(TableAvailability.available(table())),
                List.of(order()),
                List.of(order())
        );
        when(tableBookingRuntimeService.telegramRuntime(1773317437L, "AERIS")).thenReturn(view);

        ResponseEntity<TableBookingRuntimeService.TableBookingRuntimeView> response =
                controller.telegramTableBookingRuntime(1773317437L, "AERIS");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(view);
        verify(tableBookingRuntimeService).telegramRuntime(1773317437L, "AERIS");
    }

    private VenueTable table() {
        return new VenueTable(
                17L,
                "AERIS",
                "17",
                "Table 17",
                "MAIN_HALL",
                1,
                2,
                null,
                true,
                true,
                1,
                "AERIS PLAN",
                17,
                Instant.parse("2026-06-05T00:00:00Z"),
                Instant.parse("2026-06-05T00:00:00Z")
        );
    }

    private TableReservationOrder order() {
        return new TableReservationOrder(
                44L,
                1773317437L,
                1773317437L,
                null,
                17L,
                "17",
                "Table 17",
                null,
                null,
                TableReservationStatus.AWAITING_MANAGER_CONFIRMATION,
                "TELEGRAM",
                Instant.parse("2026-06-06T15:00:00Z"),
                Instant.parse("2026-06-06T17:00:00Z"),
                2,
                "Наталья Поединенко",
                null,
                "Хочу столик",
                876857557L,
                null,
                "-1004291419562",
                null,
                Instant.parse("2026-06-05T00:00:00Z"),
                Instant.parse("2026-06-05T00:00:00Z")
        );
    }
}
