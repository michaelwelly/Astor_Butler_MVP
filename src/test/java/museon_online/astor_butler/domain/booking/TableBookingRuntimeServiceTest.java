package museon_online.astor_butler.domain.booking;

import museon_online.astor_butler.domain.media.AerisMediaCatalog;
import museon_online.astor_butler.domain.media.MediaAsset;
import museon_online.astor_butler.fsm.scenario.TableBookingDraftStorage;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TableBookingRuntimeServiceTest {

    private final TableReservationService tableReservationService = mock(TableReservationService.class);
    private final TableBookingDraftStorage draftStorage = mock(TableBookingDraftStorage.class);
    private final AerisMediaCatalog mediaCatalog = mock(AerisMediaCatalog.class);
    private final TableBookingRuntimeService service = new TableBookingRuntimeService(
            tableReservationService,
            draftStorage,
            mediaCatalog
    );

    @Test
    void returnsRuntimeViewWithDraftAvailabilityAndFloorPlan() {
        Instant start = Instant.parse("2026-06-06T15:00:00Z");
        Instant end = Instant.parse("2026-06-06T17:00:00Z");
        TableBookingDraftStorage.Draft draft = new TableBookingDraftStorage.Draft(
                "AERIS",
                start,
                end,
                LocalDate.of(2026, 6, 6),
                LocalTime.of(20, 0),
                2,
                "Хочу столик"
        );
        VenueTable table = table("17");
        TableReservationOrder order = order(table);
        TableAvailability availability = TableAvailability.available(table);
        when(draftStorage.find(1773317437L)).thenReturn(Optional.of(draft));
        when(mediaCatalog.floorPlan()).thenReturn(floorPlan());
        when(tableReservationService.listActiveReservationsByChatId(1773317437L)).thenReturn(List.of(order));
        when(tableReservationService.listReservationsByChatId(1773317437L, 10)).thenReturn(List.of(order));
        when(tableReservationService.listTables("AERIS")).thenReturn(List.of(table));
        when(tableReservationService.availability("AERIS", start, end, 2)).thenReturn(List.of(availability));

        TableBookingRuntimeService.TableBookingRuntimeView view = service.telegramRuntime(1773317437L, "AERIS");

        assertThat(view.chatId()).isEqualTo(1773317437L);
        assertThat(view.draft().partySize()).isEqualTo(2);
        assertThat(view.floorPlan().objectKey()).isEqualTo("content/aeris/floor-plan/AERIS_PLAN.pdf");
        assertThat(view.activeReservations()).containsExactly(order);
        assertThat(view.availability()).containsExactly(availability);
        verify(tableReservationService).availability("AERIS", start, end, 2);
    }

    @Test
    void returnsRuntimeViewWithoutAvailabilityWhenDraftIsIncomplete() {
        when(draftStorage.find(1773317437L)).thenReturn(Optional.empty());
        when(mediaCatalog.floorPlan()).thenReturn(floorPlan());
        when(tableReservationService.listActiveReservationsByChatId(1773317437L)).thenReturn(List.of());
        when(tableReservationService.listReservationsByChatId(1773317437L, 10)).thenReturn(List.of());
        when(tableReservationService.listTables("AERIS")).thenReturn(List.of());

        TableBookingRuntimeService.TableBookingRuntimeView view = service.telegramRuntime(1773317437L, null);

        assertThat(view.draft()).isNull();
        assertThat(view.availability()).isEmpty();
        assertThat(view.venueCode()).isEqualTo("AERIS");
    }

    private VenueTable table(String code) {
        return new VenueTable(
                17L,
                "AERIS",
                code,
                "Table " + code,
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

    private TableReservationOrder order(VenueTable table) {
        return new TableReservationOrder(
                44L,
                1773317437L,
                1773317437L,
                null,
                table.id(),
                table.tableCode(),
                table.displayName(),
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

    private MediaAsset floorPlan() {
        return new MediaAsset(
                "AERIS_FLOOR_PLAN",
                "AERIS",
                "TABLE_BOOKING",
                "FLOOR_PLAN",
                "План зала AERIS",
                "astor-media",
                "content/aeris/floor-plan/AERIS_PLAN.pdf",
                "AERIS PLAN.pdf",
                "application/pdf",
                true
        );
    }
}
