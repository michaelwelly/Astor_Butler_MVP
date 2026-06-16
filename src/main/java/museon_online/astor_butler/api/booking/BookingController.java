package museon_online.astor_butler.api.booking;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import museon_online.astor_butler.domain.booking.EventBookingCommand;
import museon_online.astor_butler.domain.booking.EventBookingOrder;
import museon_online.astor_butler.domain.booking.EventBookingService;
import museon_online.astor_butler.domain.booking.EventBookingStatus;
import museon_online.astor_butler.domain.booking.TableBookingRuntimeService;
import museon_online.astor_butler.domain.booking.TableAvailability;
import museon_online.astor_butler.domain.booking.TableReservationCommand;
import museon_online.astor_butler.domain.booking.TableReservationOrder;
import museon_online.astor_butler.domain.booking.TableReservationService;
import museon_online.astor_butler.domain.booking.TableReservationStatus;
import museon_online.astor_butler.domain.booking.VenueTable;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/bookings")
@Tag(name = "Booking API", description = "Booking requests, statuses and manager notes")
@RequiredArgsConstructor
public class BookingController {

    private final TableReservationService tableReservationService;
    private final TableBookingRuntimeService tableBookingRuntimeService;
    private final EventBookingService eventBookingService;

    @PostMapping
    @Operation(summary = "Create booking request")
    public ResponseEntity<BookingResponse> create(@RequestBody BookingCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(BookingResponse.created(UUID.randomUUID(), request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get booking request")
    public ResponseEntity<BookingResponse> get(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(BookingResponse.stub(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Replace booking request")
    public ResponseEntity<BookingResponse> replace(@PathVariable("id") UUID id, @RequestBody BookingCreateRequest request) {
        return ResponseEntity.ok(BookingResponse.created(id, request));
    }

    @GetMapping
    @Operation(summary = "Search booking requests")
    public ResponseEntity<BookingSearchResponse> search(
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "query", required = false) String query,
            @RequestParam(name = "from", required = false) LocalDate from,
            @RequestParam(name = "to", required = false) LocalDate to
    ) {
        return ResponseEntity.ok(new BookingSearchResponse(status, query, from, to, List.of()));
    }

    @GetMapping("/tables")
    @Operation(summary = "List venue tables")
    public ResponseEntity<List<VenueTableResponse>> listTables(
            @RequestParam(name = "venueCode", defaultValue = "AERIS") String venueCode
    ) {
        return ResponseEntity.ok(tableReservationService.listTables(venueCode).stream()
                .map(VenueTableResponse::from)
                .toList());
    }

    @GetMapping("/tables/availability")
    @Operation(summary = "Find available tables for a requested time window")
    public ResponseEntity<List<TableAvailabilityResponse>> availability(
            @RequestParam(name = "venueCode", defaultValue = "AERIS") String venueCode,
            @RequestParam(name = "from") Instant from,
            @RequestParam(name = "to") Instant to,
            @RequestParam(name = "partySize") Integer partySize
    ) {
        return ResponseEntity.ok(tableReservationService.availability(venueCode, from, to, partySize).stream()
                .map(TableAvailabilityResponse::from)
                .toList());
    }

    @PostMapping("/table-reservations")
    @Operation(summary = "Create table reservation request and temporary hold")
    public ResponseEntity<TableReservationResponse> createTableReservation(@RequestBody TableReservationCreateRequest request) {
        TableReservationOrder order = tableReservationService.createReservation(request.toCommand());
        return ResponseEntity.status(HttpStatus.CREATED).body(TableReservationResponse.from(order));
    }

    @GetMapping("/table-reservations/{id}")
    @Operation(summary = "Get table reservation request")
    public ResponseEntity<TableReservationResponse> getTableReservation(@PathVariable("id") Long id) {
        return ResponseEntity.ok(TableReservationResponse.from(tableReservationService.getReservation(id)));
    }

    @GetMapping("/table-reservations/telegram/{chatId}")
    @Operation(summary = "List recent table reservations for Telegram chat")
    public ResponseEntity<List<TableReservationResponse>> listTelegramTableReservations(
            @PathVariable("chatId") Long chatId,
            @RequestParam(name = "limit", defaultValue = "10") Integer limit
    ) {
        return ResponseEntity.ok(tableReservationService.listReservationsByChatId(chatId, limit).stream()
                .map(TableReservationResponse::from)
                .toList());
    }

    @GetMapping("/table-reservations/telegram/{chatId}/runtime")
    @Operation(summary = "Get table booking runtime view for Telegram chat")
    public ResponseEntity<TableBookingRuntimeService.TableBookingRuntimeView> telegramTableBookingRuntime(
            @PathVariable("chatId") Long chatId,
            @RequestParam(name = "venueCode", defaultValue = "AERIS") String venueCode
    ) {
        return ResponseEntity.ok(tableBookingRuntimeService.telegramRuntime(chatId, venueCode));
    }

    @PostMapping("/event-orders")
    @Operation(summary = "Create event/private booking request for manager review")
    public ResponseEntity<EventBookingResponse> createEventBooking(@RequestBody EventBookingCreateRequest request) {
        EventBookingOrder order = eventBookingService.createOrder(request.toCommand());
        return ResponseEntity.status(HttpStatus.CREATED).body(EventBookingResponse.from(order));
    }

    @GetMapping("/event-orders/{id}")
    @Operation(summary = "Get event/private booking request")
    public ResponseEntity<EventBookingResponse> getEventBooking(@PathVariable("id") Long id) {
        return ResponseEntity.ok(EventBookingResponse.from(eventBookingService.getOrder(id)));
    }

    @GetMapping("/event-orders/telegram/{chatId}")
    @Operation(summary = "List recent event/private booking requests for Telegram chat")
    public ResponseEntity<List<EventBookingResponse>> listTelegramEventBookings(
            @PathVariable("chatId") Long chatId,
            @RequestParam(name = "limit", defaultValue = "10") Integer limit
    ) {
        return ResponseEntity.ok(eventBookingService.listOrdersByChatId(chatId, limit).stream()
                .map(EventBookingResponse::from)
                .toList());
    }

    @GetMapping("/event-orders/telegram/{chatId}/active")
    @Operation(summary = "List active event/private booking requests for Telegram chat")
    public ResponseEntity<List<EventBookingResponse>> listActiveTelegramEventBookings(
            @PathVariable("chatId") Long chatId
    ) {
        return ResponseEntity.ok(eventBookingService.listActiveOrdersByChatId(chatId).stream()
                .map(EventBookingResponse::from)
                .toList());
    }

    @PostMapping("/table-reservations/{id}/confirm")
    @Operation(summary = "Confirm table reservation and notify hostess chat")
    public ResponseEntity<TableReservationResponse> confirmTableReservation(@PathVariable("id") Long id) {
        return ResponseEntity.ok(TableReservationResponse.from(tableReservationService.confirm(id)));
    }

    @PostMapping("/table-reservations/{id}/reject")
    @Operation(summary = "Reject table reservation and release temporary hold")
    public ResponseEntity<TableReservationResponse> rejectTableReservation(@PathVariable("id") Long id) {
        return ResponseEntity.ok(TableReservationResponse.from(tableReservationService.reject(id)));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Change booking status")
    public ResponseEntity<BookingResponse> changeStatus(@PathVariable("id") UUID id, @RequestBody ChangeBookingStatusRequest request) {
        return ResponseEntity.ok(BookingResponse.stub(id).withStatus(request.status()));
    }

    @PostMapping("/{id}/manager-notes")
    @Operation(summary = "Add manager note")
    public ResponseEntity<ManagerNoteResponse> addManagerNote(@PathVariable("id") UUID id, @RequestBody ManagerNoteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(new ManagerNoteResponse(UUID.randomUUID(), id, request.note(), Instant.now()));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Cancel or soft-delete booking request")
    public ResponseEntity<Void> delete(@PathVariable("id") UUID id) {
        return ResponseEntity.noContent().build();
    }

    public record BookingCreateRequest(
            UUID userId,
            LocalDate date,
            String time,
            String eventFormat,
            Integer guestCount,
            BigDecimal budget,
            String menu,
            String technicalRequirements,
            String contact
    ) {
    }

    public record ChangeBookingStatusRequest(String status) {
    }

    public record ManagerNoteRequest(String note) {
    }

    public record ManagerNoteResponse(UUID id, UUID bookingId, String note, Instant createdAt) {
    }

    public record BookingSearchResponse(String status, String query, LocalDate from, LocalDate to, List<BookingResponse> items) {
    }

    public record BookingResponse(
            UUID id,
            UUID userId,
            String status,
            LocalDate date,
            String time,
            String eventFormat,
            Integer guestCount,
            BigDecimal budget,
            Instant updatedAt
    ) {
        static BookingResponse created(UUID id, BookingCreateRequest request) {
            return new BookingResponse(id, request.userId(), "DRAFT", request.date(), request.time(), request.eventFormat(), request.guestCount(), request.budget(), Instant.now());
        }

        static BookingResponse stub(UUID id) {
            return new BookingResponse(id, null, "DRAFT", null, null, null, null, null, Instant.now());
        }

        BookingResponse withStatus(String status) {
            return new BookingResponse(id, userId, status, date, time, eventFormat, guestCount, budget, Instant.now());
        }
    }

    public record TableReservationCreateRequest(
            Long chatId,
            Long telegramUserId,
            Long userId,
            String venueCode,
            String tableCode,
            String preferredZone,
            String seatingPreference,
            Instant requestedStartAt,
            Instant requestedEndAt,
            Integer partySize,
            String guestName,
            String guestPhone,
            String guestComment,
            Long managerTelegramId,
            String hostessChatId
    ) {
        TableReservationCommand toCommand() {
            return new TableReservationCommand(
                    chatId,
                    telegramUserId,
                    userId,
                    venueCode,
                    tableCode,
                    preferredZone,
                    seatingPreference,
                    requestedStartAt,
                    requestedEndAt,
                    partySize,
                    guestName,
                    guestPhone,
                    guestComment,
                    managerTelegramId,
                    hostessChatId
            );
        }
    }

    public record EventBookingCreateRequest(
            Long chatId,
            Long telegramUserId,
            Long userId,
            String venueCode,
            String eventType,
            LocalDate requestedDate,
            String requestedTimeText,
            Integer guestCount,
            String budgetText,
            String menuPreferences,
            String technicalRequirements,
            String contact,
            String guestName,
            String guestPhone,
            String guestComment,
            Long managerTelegramId,
            String managerChatId
    ) {
        EventBookingCommand toCommand() {
            return new EventBookingCommand(
                    chatId,
                    telegramUserId,
                    userId,
                    venueCode,
                    eventType,
                    requestedDate,
                    requestedTimeText,
                    guestCount,
                    budgetText,
                    menuPreferences,
                    technicalRequirements,
                    contact,
                    guestName,
                    guestPhone,
                    guestComment,
                    managerTelegramId,
                    managerChatId
            );
        }
    }

    public record VenueTableResponse(
            Long id,
            String venueCode,
            String tableCode,
            String displayName,
            String zone,
            Integer capacityMin,
            Integer capacityMax,
            String combinableGroup,
            Boolean bookable,
            Boolean active,
            Integer planPage,
            String planRef,
            Integer sortOrder
    ) {
        static VenueTableResponse from(VenueTable table) {
            return new VenueTableResponse(
                    table.id(),
                    table.venueCode(),
                    table.tableCode(),
                    table.displayName(),
                    table.zone(),
                    table.capacityMin(),
                    table.capacityMax(),
                    table.combinableGroup(),
                    table.bookable(),
                    table.active(),
                    table.planPage(),
                    table.planRef(),
                    table.sortOrder()
            );
        }
    }

    public record TableAvailabilityResponse(
            VenueTableResponse table,
            boolean available,
            String reason
    ) {
        static TableAvailabilityResponse from(TableAvailability availability) {
            return new TableAvailabilityResponse(
                    VenueTableResponse.from(availability.table()),
                    availability.available(),
                    availability.reason()
            );
        }
    }

    public record TableReservationResponse(
            Long id,
            Long chatId,
            Long telegramUserId,
            Long userId,
            Long tableId,
            String tableCode,
            String tableDisplayName,
            String preferredZone,
            String seatingPreference,
            TableReservationStatus status,
            Instant requestedStartAt,
            Instant requestedEndAt,
            Integer partySize,
            String guestName,
            String guestPhone,
            String guestComment,
            Long managerTelegramId,
            String hostessChatId,
            String sbisExternalId,
            Instant createdAt,
            Instant updatedAt
    ) {
        static TableReservationResponse from(TableReservationOrder order) {
            return new TableReservationResponse(
                    order.id(),
                    order.chatId(),
                    order.telegramUserId(),
                    order.userId(),
                    order.tableId(),
                    order.tableCode(),
                    order.tableDisplayName(),
                    order.preferredZone(),
                    order.seatingPreference(),
                    order.status(),
                    order.requestedStartAt(),
                    order.requestedEndAt(),
                    order.partySize(),
                    order.guestName(),
                    order.guestPhone(),
                    order.guestComment(),
                    order.managerTelegramId(),
                    order.hostessChatId(),
                    order.sbisExternalId(),
                    order.createdAt(),
                    order.updatedAt()
            );
        }
    }

    public record EventBookingResponse(
            Long id,
            Long chatId,
            Long telegramUserId,
            Long userId,
            String venueCode,
            EventBookingStatus status,
            String source,
            String eventType,
            LocalDate requestedDate,
            String requestedTimeText,
            Integer guestCount,
            String budgetText,
            String menuPreferences,
            String technicalRequirements,
            String contact,
            String guestName,
            String guestPhone,
            String guestComment,
            Long managerTelegramId,
            Long managerUserId,
            String managerChatId,
            String externalId,
            Instant createdAt,
            Instant updatedAt
    ) {
        static EventBookingResponse from(EventBookingOrder order) {
            return new EventBookingResponse(
                    order.id(),
                    order.chatId(),
                    order.telegramUserId(),
                    order.userId(),
                    order.venueCode(),
                    order.status(),
                    order.source(),
                    order.eventType(),
                    order.requestedDate(),
                    order.requestedTimeText(),
                    order.guestCount(),
                    order.budgetText(),
                    order.menuPreferences(),
                    order.technicalRequirements(),
                    order.contact(),
                    order.guestName(),
                    order.guestPhone(),
                    order.guestComment(),
                    order.managerTelegramId(),
                    order.managerUserId(),
                    order.managerChatId(),
                    order.externalId(),
                    order.createdAt(),
                    order.updatedAt()
            );
        }
    }
}
