package museon_online.astor_butler.api.merch;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import museon_online.astor_butler.domain.merch.MerchItem;
import museon_online.astor_butler.domain.merch.MerchItemStatus;
import museon_online.astor_butler.domain.merch.MerchOrder;
import museon_online.astor_butler.domain.merch.MerchOrderCommand;
import museon_online.astor_butler.domain.merch.MerchOrderStatus;
import museon_online.astor_butler.domain.merch.MerchService;
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
@RequestMapping("/api/merch")
@Tag(name = "Merch API", description = "Merch catalog and guest merch order drafts")
@RequiredArgsConstructor
public class MerchController {

    private final MerchService merchService;

    @GetMapping("/items")
    @Operation(summary = "List active merch items")
    public ResponseEntity<List<MerchItemResponse>> listItems(
            @RequestParam(name = "venueCode", defaultValue = "AERIS") String venueCode
    ) {
        return ResponseEntity.ok(merchService.listActiveItems(venueCode).stream()
                .map(MerchItemResponse::from)
                .toList());
    }

    @PostMapping("/orders")
    @Operation(summary = "Create merch order draft")
    public ResponseEntity<MerchOrderResponse> createOrder(@RequestBody MerchOrderCreateRequest request) {
        MerchOrder order = merchService.createOrder(request.toCommand());
        return ResponseEntity.status(HttpStatus.CREATED).body(MerchOrderResponse.from(order));
    }

    @GetMapping("/orders/{id}")
    @Operation(summary = "Get merch order")
    public ResponseEntity<MerchOrderResponse> getOrder(@PathVariable("id") Long id) {
        return ResponseEntity.ok(MerchOrderResponse.from(merchService.getOrder(id)));
    }

    @GetMapping("/orders/telegram/{chatId}")
    @Operation(summary = "List recent merch orders for Telegram chat")
    public ResponseEntity<List<MerchOrderResponse>> listTelegramOrders(
            @PathVariable("chatId") Long chatId,
            @RequestParam(name = "limit", defaultValue = "10") Integer limit
    ) {
        return ResponseEntity.ok(merchService.listOrdersByChatId(chatId, limit).stream()
                .map(MerchOrderResponse::from)
                .toList());
    }

    public record MerchOrderCreateRequest(
            Long chatId,
            Long telegramUserId,
            Long userId,
            String venueCode,
            Long itemId,
            String itemTitle,
            Integer quantity,
            String guestName,
            String guestComment,
            String paymentMethodHint
    ) {
        MerchOrderCommand toCommand() {
            return new MerchOrderCommand(chatId, telegramUserId, userId, venueCode, itemId, itemTitle, quantity, guestName, guestComment, paymentMethodHint);
        }
    }

    public record MerchItemResponse(
            Long id,
            String venueCode,
            String itemCode,
            String title,
            String description,
            MerchItemStatus status,
            Long priceMinor,
            String currency,
            String stockHint,
            String mediaAssetCode
    ) {
        static MerchItemResponse from(MerchItem item) {
            return new MerchItemResponse(item.id(), item.venueCode(), item.itemCode(), item.title(), item.description(), item.status(), item.priceMinor(), item.currency(), item.stockHint(), item.mediaAssetCode());
        }
    }

    public record MerchOrderResponse(
            Long id,
            Long chatId,
            Long telegramUserId,
            Long userId,
            String venueCode,
            Long itemId,
            String itemTitle,
            MerchOrderStatus status,
            Integer quantity,
            Long priceMinor,
            String currency,
            String guestName,
            String guestComment,
            String paymentMethodHint,
            String paymentExternalId,
            Instant createdAt,
            Instant updatedAt
    ) {
        static MerchOrderResponse from(MerchOrder order) {
            return new MerchOrderResponse(order.id(), order.chatId(), order.telegramUserId(), order.userId(), order.venueCode(), order.itemId(), order.itemTitle(), order.status(), order.quantity(), order.priceMinor(), order.currency(), order.guestName(), order.guestComment(), order.paymentMethodHint(), order.paymentExternalId(), order.createdAt(), order.updatedAt());
        }
    }
}
