package museon_online.astor_butler.domain.booking;

import museon_online.astor_butler.api.common.ApiException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentCaptor.forClass;

class TableReservationServiceTest {

    private final TableReservationRepository repository = mock(TableReservationRepository.class);
    private final TableReservationNotificationService notificationService = mock(TableReservationNotificationService.class);
    private final TableReservationService service = new TableReservationService(repository, notificationService);

    @Test
    void createsReservationWhenTableIsAvailable() {
        Instant start = Instant.parse("2026-06-06T17:00:00Z");
        Instant end = Instant.parse("2026-06-06T19:00:00Z");
        VenueTable table = table(1L, "5", 4, true, true);
        TableReservationCommand command = command("5", start, end, 3);
        TableReservationOrder expected = order(10L, table);

        when(repository.findTableByCode("AERIS", "5")).thenReturn(Optional.of(table));
        when(repository.hasActiveConflict(1L, start, end)).thenReturn(false);
        when(repository.createAwaitingManagerOrder(command, table)).thenReturn(expected);

        TableReservationOrder result = service.createReservation(command);

        assertThat(result).isEqualTo(expected);
        verify(repository).createAwaitingManagerOrder(command, table);
        verify(notificationService).notifyHostessApprovalRequest(expected);
    }

    @Test
    void rejectsReservationWhenTimeWindowConflicts() {
        Instant start = Instant.parse("2026-06-06T17:00:00Z");
        Instant end = Instant.parse("2026-06-06T19:00:00Z");
        VenueTable table = table(1L, "5", 4, true, true);

        when(repository.findTableByCode("AERIS", "5")).thenReturn(Optional.of(table));
        when(repository.hasActiveConflict(1L, start, end)).thenReturn(true);

        assertThatThrownBy(() -> service.createReservation(command("5", start, end, 3)))
                .isInstanceOf(ApiException.class)
                .hasMessage("Table already has an active hold for this time window");
    }

    @Test
    void autoSelectsSmallestAvailableTableWhenTableCodeIsBlank() {
        Instant start = Instant.parse("2026-06-06T17:00:00Z");
        Instant end = Instant.parse("2026-06-06T19:00:00Z");
        VenueTable table = table(17L, "17", 2, true, true);
        TableReservationCommand command = command(null, start, end, 2);
        TableReservationOrder expected = order(11L, table);

        when(repository.findAvailableTables("AERIS", start, end, 2)).thenReturn(List.of(table));
        when(repository.hasActiveConflict(17L, start, end)).thenReturn(false);
        when(repository.createAwaitingManagerOrder(command, table)).thenReturn(expected);

        TableReservationOrder result = service.createReservation(command);

        assertThat(result.tableCode()).isEqualTo("17");
    }

    @Test
    void returnsReservationById() {
        VenueTable table = table(5L, "5", 4, true, true);
        TableReservationOrder expected = order(12L, table);

        when(repository.findOrder(12L)).thenReturn(Optional.of(expected));

        TableReservationOrder result = service.getReservation(12L);

        assertThat(result).isEqualTo(expected);
    }

    @Test
    void listsReservationsByChatIdWithBoundedLimit() {
        VenueTable table = table(5L, "5", 4, true, true);
        TableReservationOrder expected = order(12L, table);
        when(repository.findOrdersByChatId(1773317437L, 100)).thenReturn(List.of(expected));

        List<TableReservationOrder> result = service.listReservationsByChatId(1773317437L, 500);

        assertThat(result).containsExactly(expected);
        verify(repository).findOrdersByChatId(1773317437L, 100);
    }

    @Test
    void listsActiveReservationsByChatId() {
        VenueTable table = table(5L, "5", 4, true, true);
        TableReservationOrder expected = order(12L, table, TableReservationStatus.CONFIRMED);
        when(repository.findActiveOrdersByChatId(1773317437L)).thenReturn(List.of(expected));

        List<TableReservationOrder> result = service.listActiveReservationsByChatId(1773317437L);

        assertThat(result).containsExactly(expected);
    }

    @Test
    void confirmsReservationAndNotifiesHostess() {
        VenueTable table = table(5L, "5", 4, true, true);
        TableReservationOrder awaiting = order(12L, table, TableReservationStatus.AWAITING_MANAGER_CONFIRMATION);
        TableReservationOrder confirmed = order(12L, table, TableReservationStatus.CONFIRMED);

        when(repository.findOrder(12L)).thenReturn(Optional.of(awaiting));
        when(repository.confirm(12L)).thenReturn(confirmed);

        TableReservationOrder result = service.confirm(12L);

        assertThat(result.status()).isEqualTo(TableReservationStatus.CONFIRMED);
        verify(notificationService).notifyHostessConfirmed(confirmed);
    }

    @Test
    void changesReservationAndReopensHostessConfirmation() {
        VenueTable currentTable = table(5L, "5", 4, true, true);
        VenueTable betterFit = table(11L, "11", 6, true, true);
        TableReservationOrder current = order(12L, currentTable, TableReservationStatus.CONFIRMED);
        TableReservationOrder changed = new TableReservationOrder(
                12L,
                1773317437L,
                1773317437L,
                null,
                betterFit.id(),
                betterFit.tableCode(),
                betterFit.displayName(),
                null,
                "Хочу спокойный стол",
                TableReservationStatus.AWAITING_MANAGER_CONFIRMATION,
                "TELEGRAM",
                Instant.parse("2026-06-06T17:00:00Z"),
                Instant.parse("2026-06-06T19:00:00Z"),
                5,
                "Наталья",
                "+79990000000",
                "Хочу спокойный стол | Количество гостей изменено: 5",
                876857557L,
                null,
                null,
                null,
                Instant.parse("2026-06-05T00:00:00Z"),
                Instant.parse("2026-06-05T00:00:00Z")
        );
        TableReservationChangeCommand command = new TableReservationChangeCommand(
                "AERIS",
                null,
                null,
                null,
                current.requestedStartAt(),
                current.requestedEndAt(),
                5,
                "Хочу спокойный стол | Количество гостей изменено: 5"
        );

        when(repository.findOrder(12L)).thenReturn(Optional.of(current));
        when(repository.findTableByCode("AERIS", "5")).thenReturn(Optional.of(currentTable));
        when(repository.findAvailableTables("AERIS", current.requestedStartAt(), current.requestedEndAt(), 5, null))
                .thenReturn(List.of(betterFit));
        when(repository.hasActiveConflict(11L, current.requestedStartAt(), current.requestedEndAt(), 12L)).thenReturn(false);
        when(repository.changeReservation(eq(12L), any(TableReservationChangeCommand.class), eq(betterFit))).thenReturn(changed);

        TableReservationOrder result = service.changeByGuest(12L, command);

        var captor = forClass(TableReservationChangeCommand.class);
        assertThat(result.status()).isEqualTo(TableReservationStatus.AWAITING_MANAGER_CONFIRMATION);
        assertThat(result.tableCode()).isEqualTo("11");
        assertThat(result.partySize()).isEqualTo(5);
        verify(repository).changeReservation(eq(12L), captor.capture(), eq(betterFit));
        assertThat(captor.getValue().partySize()).isEqualTo(5);
        assertThat(captor.getValue().seatingPreference()).isEqualTo("Хочу спокойный стол");
        verify(notificationService).notifyHostessApprovalRequest(changed);
    }

    @Test
    void rejectsReservationAndDoesNotNotifyHostess() {
        VenueTable table = table(5L, "5", 4, true, true);
        TableReservationOrder awaiting = order(12L, table, TableReservationStatus.AWAITING_MANAGER_CONFIRMATION);
        TableReservationOrder rejected = order(12L, table, TableReservationStatus.REJECTED);

        when(repository.findOrder(12L)).thenReturn(Optional.of(awaiting));
        when(repository.reject(12L)).thenReturn(rejected);

        TableReservationOrder result = service.reject(12L);

        assertThat(result.status()).isEqualTo(TableReservationStatus.REJECTED);
    }

    @Test
    void validatesTimeWindow() {
        Instant start = Instant.parse("2026-06-06T19:00:00Z");
        Instant end = Instant.parse("2026-06-06T17:00:00Z");

        assertThatThrownBy(() -> service.createReservation(command("5", start, end, 3)))
                .isInstanceOf(ApiException.class)
                .hasMessage("requestedEndAt must be after requestedStartAt");
    }

    private TableReservationCommand command(String tableCode, Instant start, Instant end, int partySize) {
        return new TableReservationCommand(
                1773317437L,
                1773317437L,
                null,
                "AERIS",
                tableCode,
                null,
                "Хочу спокойный стол",
                start,
                end,
                partySize,
                "Наталья",
                "+79990000000",
                "Хочу спокойный стол",
                876857557L,
                null
        );
    }

    private VenueTable table(Long id, String code, int capacity, boolean bookable, boolean active) {
        return new VenueTable(
                id,
                "AERIS",
                code,
                "Table " + code,
                "MAIN_HALL",
                1,
                capacity,
                null,
                bookable,
                active,
                2,
                "AERIS PLAN",
                Integer.parseInt(code),
                Instant.parse("2026-06-05T00:00:00Z"),
                Instant.parse("2026-06-05T00:00:00Z")
        );
    }

    private TableReservationOrder order(Long id, VenueTable table) {
        return order(id, table, TableReservationStatus.AWAITING_MANAGER_CONFIRMATION);
    }

    private TableReservationOrder order(Long id, VenueTable table, TableReservationStatus status) {
        return new TableReservationOrder(
                id,
                1773317437L,
                1773317437L,
                null,
                table.id(),
                table.tableCode(),
                table.displayName(),
                null,
                "Хочу спокойный стол",
                status,
                "TELEGRAM",
                Instant.parse("2026-06-06T17:00:00Z"),
                Instant.parse("2026-06-06T19:00:00Z"),
                3,
                "Наталья",
                "+79990000000",
                "Хочу спокойный стол",
                876857557L,
                null,
                null,
                null,
                Instant.parse("2026-06-05T00:00:00Z"),
                Instant.parse("2026-06-05T00:00:00Z")
        );
    }
}
