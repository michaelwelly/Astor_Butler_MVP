package museon_online.astor_butler.api.tip;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import museon_online.astor_butler.domain.tip.StaffProfile;
import museon_online.astor_butler.domain.tip.TipOrder;
import museon_online.astor_butler.domain.tip.TipOrderCommand;
import museon_online.astor_butler.domain.tip.TipOrderStatus;
import museon_online.astor_butler.domain.tip.TipService;
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
@RequestMapping("/api/tips")
@Tag(name = "Tip API", description = "Direct staff gratitude and SBP-ready tip orders")
@RequiredArgsConstructor
public class TipController {

    private final TipService tipService;

    @GetMapping("/staff")
    @Operation(summary = "List active staff profiles for tips")
    public ResponseEntity<List<StaffProfileResponse>> listStaff(
            @RequestParam(name = "venueCode", defaultValue = "AERIS") String venueCode
    ) {
        return ResponseEntity.ok(tipService.listActiveStaff(venueCode).stream()
                .map(StaffProfileResponse::from)
                .toList());
    }

    @PostMapping("/orders")
    @Operation(summary = "Create direct tip order draft")
    public ResponseEntity<TipOrderResponse> createOrder(@RequestBody TipOrderCreateRequest request) {
        TipOrder order = tipService.createDraft(request.toCommand());
        return ResponseEntity.status(HttpStatus.CREATED).body(TipOrderResponse.from(order));
    }

    @GetMapping("/orders/{id}")
    @Operation(summary = "Get tip order")
    public ResponseEntity<TipOrderResponse> getOrder(@PathVariable("id") Long id) {
        return ResponseEntity.ok(TipOrderResponse.from(tipService.getOrder(id)));
    }

    @PostMapping("/orders/{id}/confirm")
    @Operation(summary = "Confirm tip order draft and move it to awaiting payment")
    public ResponseEntity<TipOrderResponse> confirmOrder(@PathVariable("id") Long id) {
        return ResponseEntity.ok(TipOrderResponse.from(tipService.confirmDraft(id)));
    }

    @PostMapping("/orders/{id}/cancel")
    @Operation(summary = "Cancel tip order draft")
    public ResponseEntity<TipOrderResponse> cancelOrder(@PathVariable("id") Long id) {
        return ResponseEntity.ok(TipOrderResponse.from(tipService.cancelDraft(id)));
    }

    @GetMapping("/orders/telegram/{chatId}")
    @Operation(summary = "List recent tip orders for Telegram chat")
    public ResponseEntity<List<TipOrderResponse>> listTelegramOrders(
            @PathVariable("chatId") Long chatId,
            @RequestParam(name = "limit", defaultValue = "10") Integer limit
    ) {
        return ResponseEntity.ok(tipService.listOrdersByChatId(chatId, limit).stream()
                .map(TipOrderResponse::from)
                .toList());
    }

    public record TipOrderCreateRequest(
            Long chatId,
            Long telegramUserId,
            Long userId,
            String venueCode,
            Long staffId,
            String staffDisplayName,
            Long amountMinor,
            String currency,
            String guestName,
            String guestComment
    ) {
        TipOrderCommand toCommand() {
            return new TipOrderCommand(
                    chatId,
                    telegramUserId,
                    userId,
                    venueCode,
                    staffId,
                    staffDisplayName,
                    amountMinor,
                    currency,
                    guestName,
                    guestComment
            );
        }
    }

    public record StaffProfileResponse(
            Long id,
            String venueCode,
            String displayName,
            String role,
            Long telegramUserId,
            Boolean active
    ) {
        static StaffProfileResponse from(StaffProfile profile) {
            return new StaffProfileResponse(
                    profile.id(),
                    profile.venueCode(),
                    profile.displayName(),
                    profile.role(),
                    profile.telegramUserId(),
                    profile.active()
            );
        }
    }

    public record TipOrderResponse(
            Long id,
            Long chatId,
            Long telegramUserId,
            Long userId,
            String venueCode,
            Long staffId,
            String staffDisplayName,
            TipOrderStatus status,
            Long amountMinor,
            String currency,
            String guestName,
            String guestComment,
            String sbpUrl,
            String paymentExternalId,
            Instant createdAt,
            Instant updatedAt
    ) {
        static TipOrderResponse from(TipOrder order) {
            return new TipOrderResponse(
                    order.id(),
                    order.chatId(),
                    order.telegramUserId(),
                    order.userId(),
                    order.venueCode(),
                    order.staffId(),
                    order.staffDisplayName(),
                    order.status(),
                    order.amountMinor(),
                    order.currency(),
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
