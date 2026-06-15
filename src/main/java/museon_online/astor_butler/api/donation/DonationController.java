package museon_online.astor_butler.api.donation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import museon_online.astor_butler.domain.donation.DonationInitiative;
import museon_online.astor_butler.domain.donation.DonationOrder;
import museon_online.astor_butler.domain.donation.DonationOrderCommand;
import museon_online.astor_butler.domain.donation.DonationOrderStatus;
import museon_online.astor_butler.domain.donation.DonationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/donations")
@Tag(name = "Donation API", description = "Hidden Heart initiatives and anonymous-by-default donation orders")
@RequiredArgsConstructor
public class DonationController {

    private final DonationService donationService;

    @GetMapping("/initiatives")
    @Operation(summary = "List active donation initiatives")
    public ResponseEntity<List<DonationInitiativeResponse>> listInitiatives(
            @RequestParam(name = "venueCode", defaultValue = "AERIS") String venueCode
    ) {
        return ResponseEntity.ok(donationService.listActiveInitiatives(venueCode).stream()
                .map(DonationInitiativeResponse::from)
                .toList());
    }

    @PostMapping("/orders")
    @Operation(summary = "Create donation order draft")
    public ResponseEntity<DonationOrderResponse> createOrder(@RequestBody DonationOrderCreateRequest request) {
        DonationOrder order = donationService.createDraft(request.toCommand());
        return ResponseEntity.status(HttpStatus.CREATED).body(DonationOrderResponse.from(order));
    }

    @GetMapping("/orders/{id}")
    @Operation(summary = "Get donation order")
    public ResponseEntity<DonationOrderResponse> getOrder(@PathVariable("id") Long id) {
        return ResponseEntity.ok(DonationOrderResponse.from(donationService.getOrder(id)));
    }

    @GetMapping("/orders/telegram/{chatId}")
    @Operation(summary = "List recent donation orders for Telegram chat")
    public ResponseEntity<List<DonationOrderResponse>> listTelegramOrders(
            @PathVariable("chatId") Long chatId,
            @RequestParam(name = "limit", defaultValue = "10") Integer limit
    ) {
        return ResponseEntity.ok(donationService.listOrdersByChatId(chatId, limit).stream()
                .map(DonationOrderResponse::from)
                .toList());
    }

    public record DonationOrderCreateRequest(
            Long chatId,
            Long telegramUserId,
            Long userId,
            String venueCode,
            Long initiativeId,
            String initiativeTitle,
            Long amountMinor,
            String currency,
            Boolean anonymous,
            String guestName,
            String guestComment
    ) {
        DonationOrderCommand toCommand() {
            return new DonationOrderCommand(
                    chatId,
                    telegramUserId,
                    userId,
                    venueCode,
                    initiativeId,
                    initiativeTitle,
                    amountMinor,
                    currency,
                    anonymous,
                    guestName,
                    guestComment
            );
        }
    }

    public record DonationInitiativeResponse(
            Long id,
            String venueCode,
            String initiativeCode,
            String title,
            String description,
            Boolean active
    ) {
        static DonationInitiativeResponse from(DonationInitiative initiative) {
            return new DonationInitiativeResponse(
                    initiative.id(),
                    initiative.venueCode(),
                    initiative.initiativeCode(),
                    initiative.title(),
                    initiative.description(),
                    initiative.active()
            );
        }
    }

    public record DonationOrderResponse(
            Long id,
            Long chatId,
            Long telegramUserId,
            Long userId,
            String venueCode,
            Long initiativeId,
            String initiativeTitle,
            DonationOrderStatus status,
            Long amountMinor,
            String currency,
            Boolean anonymous,
            String guestName,
            String guestComment,
            String sbpUrl,
            String paymentExternalId,
            Instant createdAt,
            Instant updatedAt
    ) {
        static DonationOrderResponse from(DonationOrder order) {
            return new DonationOrderResponse(
                    order.id(),
                    order.chatId(),
                    order.telegramUserId(),
                    order.userId(),
                    order.venueCode(),
                    order.initiativeId(),
                    order.initiativeTitle(),
                    order.status(),
                    order.amountMinor(),
                    order.currency(),
                    order.anonymous(),
                    order.guestName(),
                    order.guestComment(),
                    order.sbpUrl(),
                    order.paymentExternalId(),
                    order.createdAt(),
                    order.updatedAt()
            );
        }
    }
}
