package museon_online.astor_butler.api.concierge;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import museon_online.astor_butler.domain.concierge.ConciergeRequest;
import museon_online.astor_butler.domain.concierge.ConciergeRequestCommand;
import museon_online.astor_butler.domain.concierge.ConciergeRequestService;
import museon_online.astor_butler.domain.concierge.ConciergeRequestStatus;
import museon_online.astor_butler.domain.concierge.ConciergeRequestType;
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
@RequestMapping("/api/concierge")
@Tag(name = "Concierge API", description = "Guest service requests routed to the team")
@RequiredArgsConstructor
public class ConciergeRequestController {

    private final ConciergeRequestService requestService;

    @PostMapping("/requests")
    @Operation(summary = "Create concierge request")
    public ResponseEntity<ConciergeRequestResponse> createRequest(@RequestBody ConciergeRequestCreateRequest request) {
        ConciergeRequest created = requestService.createRequest(request.toCommand(requestService));
        return ResponseEntity.status(HttpStatus.CREATED).body(ConciergeRequestResponse.from(created));
    }

    @GetMapping("/requests/{id}")
    @Operation(summary = "Get concierge request")
    public ResponseEntity<ConciergeRequestResponse> getRequest(@PathVariable("id") Long id) {
        return ResponseEntity.ok(ConciergeRequestResponse.from(requestService.getRequest(id)));
    }

    @GetMapping("/requests/telegram/{chatId}")
    @Operation(summary = "List concierge requests for Telegram chat")
    public ResponseEntity<List<ConciergeRequestResponse>> listTelegramRequests(
            @PathVariable("chatId") Long chatId,
            @RequestParam(name = "limit", defaultValue = "20") Integer limit
    ) {
        return ResponseEntity.ok(requestService.listByChatId(chatId, limit).stream()
                .map(ConciergeRequestResponse::from)
                .toList());
    }

    public record ConciergeRequestCreateRequest(
            Long chatId,
            Long telegramUserId,
            Long userId,
            String venueCode,
            ConciergeRequestType requestType,
            String guestName,
            String requestText,
            String managerChatId,
            String capturedFromState,
            String correlationId,
            String metadataJson
    ) {
        ConciergeRequestCommand toCommand(ConciergeRequestService service) {
            ConciergeRequestType resolvedType = requestType == null
                    ? service.classify(requestText)
                    : requestType;
            return new ConciergeRequestCommand(
                    chatId,
                    telegramUserId,
                    userId,
                    venueCode,
                    resolvedType,
                    guestName,
                    requestText,
                    managerChatId,
                    capturedFromState,
                    correlationId,
                    metadataJson
            );
        }
    }

    public record ConciergeRequestResponse(
            Long id,
            Long chatId,
            Long telegramUserId,
            Long userId,
            String venueCode,
            ConciergeRequestType requestType,
            ConciergeRequestStatus status,
            String source,
            String guestName,
            String requestText,
            String managerChatId,
            String capturedFromState,
            String correlationId,
            Instant createdAt,
            Instant updatedAt
    ) {
        static ConciergeRequestResponse from(ConciergeRequest request) {
            return new ConciergeRequestResponse(
                    request.id(),
                    request.chatId(),
                    request.telegramUserId(),
                    request.userId(),
                    request.venueCode(),
                    request.requestType(),
                    request.status(),
                    request.source(),
                    request.guestName(),
                    request.requestText(),
                    request.managerChatId(),
                    request.capturedFromState(),
                    request.correlationId(),
                    request.createdAt(),
                    request.updatedAt()
            );
        }
    }
}
