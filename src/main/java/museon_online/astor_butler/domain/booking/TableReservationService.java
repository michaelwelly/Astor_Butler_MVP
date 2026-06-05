package museon_online.astor_butler.domain.booking;

import lombok.RequiredArgsConstructor;
import museon_online.astor_butler.api.common.ApiException;
import museon_online.astor_butler.api.common.ErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TableReservationService {

    private final TableReservationRepository repository;
    private final TableReservationNotificationService notificationService;

    public List<VenueTable> listTables(String venueCode) {
        return repository.findTables(venueCode);
    }

    public List<TableAvailability> availability(String venueCode, Instant startAt, Instant endAt, int partySize) {
        validateWindow(startAt, endAt);
        validatePartySize(partySize);
        List<VenueTable> available = repository.findAvailableTables(venueCode, startAt, endAt, partySize);
        return available.stream()
                .map(TableAvailability::available)
                .toList();
    }

    @Transactional
    public TableReservationOrder createReservation(TableReservationCommand command) {
        validateCommand(command);

        VenueTable table = resolveTable(command);
        if (Boolean.FALSE.equals(table.active()) || Boolean.FALSE.equals(table.bookable())) {
            throw conflict("Table is not bookable", table.tableCode());
        }
        if (table.capacityMax() < command.partySize()) {
            throw conflict("Table capacity is lower than requested party size", table.tableCode());
        }
        if (repository.hasActiveConflict(table.id(), command.requestedStartAt(), command.requestedEndAt())) {
            throw conflict("Table already has an active hold for this time window", table.tableCode());
        }

        TableReservationOrder order = repository.createAwaitingManagerOrder(command, table);
        notificationService.notifyHostessApprovalRequest(order);
        return order;
    }

    @Transactional
    public TableReservationOrder confirm(Long id) {
        TableReservationOrder current = requireOrder(id);
        if (current.status() != TableReservationStatus.AWAITING_MANAGER_CONFIRMATION) {
            throw conflict("Only awaiting manager confirmation reservations can be confirmed", current.tableCode());
        }

        TableReservationOrder confirmed = repository.confirm(id);
        notificationService.notifyHostessConfirmed(confirmed);
        notificationService.notifyGuestConfirmed(confirmed);
        return confirmed;
    }

    @Transactional
    public TableReservationOrder reject(Long id) {
        TableReservationOrder current = requireOrder(id);
        if (current.status() != TableReservationStatus.AWAITING_MANAGER_CONFIRMATION) {
            throw conflict("Only awaiting manager confirmation reservations can be rejected", current.tableCode());
        }

        TableReservationOrder rejected = repository.reject(id);
        notificationService.notifyGuestRejected(rejected);
        return rejected;
    }

    private TableReservationOrder requireOrder(Long id) {
        if (id == null) {
            throw badRequest("reservation id is required");
        }
        return repository.findOrder(id)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        ErrorCode.NOT_FOUND,
                        "Table reservation was not found",
                        Map.of("id", id)
                ));
    }

    private VenueTable resolveTable(TableReservationCommand command) {
        if (command.tableCode() != null && !command.tableCode().isBlank()) {
            return repository.findTableByCode(command.venueCode(), command.tableCode())
                    .orElseThrow(() -> new ApiException(
                            HttpStatus.NOT_FOUND,
                            ErrorCode.NOT_FOUND,
                            "Requested table was not found",
                            Map.of("tableCode", command.tableCode())
                    ));
        }

        return repository.findAvailableTables(
                        command.venueCode(),
                        command.requestedStartAt(),
                        command.requestedEndAt(),
                        command.partySize()
                )
                .stream()
                .findFirst()
                .orElseThrow(() -> new ApiException(
                        HttpStatus.CONFLICT,
                        ErrorCode.CONFLICT,
                        "No available table for requested time window and party size"
                ));
    }

    private void validateCommand(TableReservationCommand command) {
        if (command == null) {
            throw badRequest("Request body is required");
        }
        if (command.chatId() == null) {
            throw badRequest("chatId is required");
        }
        validateWindow(command.requestedStartAt(), command.requestedEndAt());
        validatePartySize(command.partySize());
    }

    private void validateWindow(Instant startAt, Instant endAt) {
        if (startAt == null || endAt == null) {
            throw badRequest("requestedStartAt and requestedEndAt are required");
        }
        if (!endAt.isAfter(startAt)) {
            throw badRequest("requestedEndAt must be after requestedStartAt");
        }
    }

    private void validatePartySize(Integer partySize) {
        if (partySize == null || partySize < 1) {
            throw badRequest("partySize must be positive");
        }
    }

    private ApiException badRequest(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, ErrorCode.BAD_REQUEST, message);
    }

    private ApiException conflict(String message, String tableCode) {
        return new ApiException(
                HttpStatus.CONFLICT,
                ErrorCode.CONFLICT,
                message,
                Map.of("tableCode", tableCode)
        );
    }
}
