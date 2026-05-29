package museon_online.astor_butler.api.booking;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
public class BookingController {

    @PostMapping
    @Operation(summary = "Create booking request")
    public ResponseEntity<BookingResponse> create(@RequestBody BookingCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(BookingResponse.created(UUID.randomUUID(), request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get booking request")
    public ResponseEntity<BookingResponse> get(@PathVariable UUID id) {
        return ResponseEntity.ok(BookingResponse.stub(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Replace booking request")
    public ResponseEntity<BookingResponse> replace(@PathVariable UUID id, @RequestBody BookingCreateRequest request) {
        return ResponseEntity.ok(BookingResponse.created(id, request));
    }

    @GetMapping
    @Operation(summary = "Search booking requests")
    public ResponseEntity<BookingSearchResponse> search(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to
    ) {
        return ResponseEntity.ok(new BookingSearchResponse(status, query, from, to, List.of()));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Change booking status")
    public ResponseEntity<BookingResponse> changeStatus(@PathVariable UUID id, @RequestBody ChangeBookingStatusRequest request) {
        return ResponseEntity.ok(BookingResponse.stub(id).withStatus(request.status()));
    }

    @PostMapping("/{id}/manager-notes")
    @Operation(summary = "Add manager note")
    public ResponseEntity<ManagerNoteResponse> addManagerNote(@PathVariable UUID id, @RequestBody ManagerNoteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(new ManagerNoteResponse(UUID.randomUUID(), id, request.note(), Instant.now()));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Cancel or soft-delete booking request")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
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
}
