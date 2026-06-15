package museon_online.astor_butler.api.payment;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import museon_online.astor_butler.domain.payment.TelegramStarPayment;
import museon_online.astor_butler.domain.payment.TelegramStarPaymentCommand;
import museon_online.astor_butler.domain.payment.TelegramStarPaymentPurpose;
import museon_online.astor_butler.domain.payment.TelegramStarPaymentService;
import museon_online.astor_butler.domain.payment.TelegramStarPaymentStatus;
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
@RequestMapping("/api/payments/telegram-stars")
@Tag(name = "Telegram Stars Payment API", description = "Telegram Stars XTR invoice drafts and payment lifecycle")
@RequiredArgsConstructor
public class TelegramStarPaymentController {

    private final TelegramStarPaymentService paymentService;

    @PostMapping
    @Operation(summary = "Create Telegram Stars payment draft")
    public ResponseEntity<TelegramStarPaymentResponse> create(@RequestBody TelegramStarPaymentCreateRequest request) {
        TelegramStarPayment payment = paymentService.createDraft(request.toCommand());
        return ResponseEntity.status(HttpStatus.CREATED).body(TelegramStarPaymentResponse.from(payment));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get Telegram Stars payment")
    public ResponseEntity<TelegramStarPaymentResponse> get(@PathVariable("id") Long id) {
        return ResponseEntity.ok(TelegramStarPaymentResponse.from(paymentService.get(id)));
    }

    @GetMapping("/telegram/{chatId}")
    @Operation(summary = "List Telegram Stars payments for chat")
    public ResponseEntity<List<TelegramStarPaymentResponse>> listByChat(
            @PathVariable("chatId") Long chatId,
            @RequestParam(name = "limit", defaultValue = "10") Integer limit
    ) {
        return ResponseEntity.ok(paymentService.listByChatId(chatId, limit).stream()
                .map(TelegramStarPaymentResponse::from)
                .toList());
    }

    public record TelegramStarPaymentCreateRequest(
            Long chatId,
            Long telegramUserId,
            Long userId,
            String venueCode,
            TelegramStarPaymentPurpose purpose,
            String relatedEntityType,
            Long relatedEntityId,
            String title,
            String description,
            Long starAmount
    ) {
        TelegramStarPaymentCommand toCommand() {
            return new TelegramStarPaymentCommand(
                    chatId,
                    telegramUserId,
                    userId,
                    venueCode,
                    purpose,
                    relatedEntityType,
                    relatedEntityId,
                    title,
                    description,
                    starAmount
            );
        }
    }

    public record TelegramStarPaymentResponse(
            Long id,
            Long chatId,
            Long telegramUserId,
            Long userId,
            String venueCode,
            TelegramStarPaymentStatus status,
            TelegramStarPaymentPurpose purpose,
            String relatedEntityType,
            Long relatedEntityId,
            String title,
            String description,
            String payload,
            String currency,
            Long starAmount,
            String providerToken,
            Long invoiceMessageId,
            String telegramPaymentChargeId,
            Instant createdAt,
            Instant updatedAt
    ) {
        static TelegramStarPaymentResponse from(TelegramStarPayment payment) {
            return new TelegramStarPaymentResponse(
                    payment.id(),
                    payment.chatId(),
                    payment.telegramUserId(),
                    payment.userId(),
                    payment.venueCode(),
                    payment.status(),
                    payment.purpose(),
                    payment.relatedEntityType(),
                    payment.relatedEntityId(),
                    payment.title(),
                    payment.description(),
                    payment.payload(),
                    payment.currency(),
                    payment.starAmount(),
                    payment.providerToken(),
                    payment.invoiceMessageId(),
                    payment.telegramPaymentChargeId(),
                    payment.createdAt(),
                    payment.updatedAt()
            );
        }
    }
}
