package museon_online.astor_butler.api.feedback;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import museon_online.astor_butler.domain.feedback.FeedbackPriority;
import museon_online.astor_butler.domain.feedback.FeedbackSentiment;
import museon_online.astor_butler.domain.feedback.FeedbackService;
import museon_online.astor_butler.domain.feedback.FeedbackStatus;
import museon_online.astor_butler.domain.feedback.FeedbackType;
import museon_online.astor_butler.domain.feedback.GuestFeedback;
import museon_online.astor_butler.domain.feedback.GuestFeedbackCommand;
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
@RequestMapping("/api/feedback")
@Tag(name = "Feedback API", description = "Guest feedback cards and quality loop analytics")
@RequiredArgsConstructor
public class FeedbackController {

    private final FeedbackService feedbackService;

    @PostMapping
    @Operation(summary = "Create guest feedback")
    public ResponseEntity<FeedbackResponse> create(@RequestBody FeedbackCreateRequest request) {
        GuestFeedback feedback = feedbackService.create(request.toCommand());
        return ResponseEntity.status(HttpStatus.CREATED).body(FeedbackResponse.from(feedback));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get guest feedback")
    public ResponseEntity<FeedbackResponse> get(@PathVariable("id") Long id) {
        return ResponseEntity.ok(FeedbackResponse.from(feedbackService.get(id)));
    }

    @GetMapping("/telegram/{chatId}")
    @Operation(summary = "List feedback items for Telegram chat")
    public ResponseEntity<List<FeedbackResponse>> listByChatId(
            @PathVariable("chatId") Long chatId,
            @RequestParam(name = "limit", defaultValue = "10") Integer limit
    ) {
        return ResponseEntity.ok(feedbackService.listByChatId(chatId, limit).stream()
                .map(FeedbackResponse::from)
                .toList());
    }

    @GetMapping("/open")
    @Operation(summary = "List open feedback items")
    public ResponseEntity<List<FeedbackResponse>> listOpen(
            @RequestParam(name = "limit", defaultValue = "50") Integer limit
    ) {
        return ResponseEntity.ok(feedbackService.listOpen(limit).stream()
                .map(FeedbackResponse::from)
                .toList());
    }

    public record FeedbackCreateRequest(
            Long chatId,
            Long telegramUserId,
            Long userId,
            String venueCode,
            String guestName,
            String text,
            String previousState,
            String correlationId,
            String adminChatId
    ) {
        GuestFeedbackCommand toCommand() {
            return new GuestFeedbackCommand(
                    chatId,
                    telegramUserId,
                    userId,
                    venueCode,
                    guestName,
                    text,
                    previousState,
                    correlationId,
                    adminChatId
            );
        }
    }

    public record FeedbackResponse(
            Long id,
            Long chatId,
            Long telegramUserId,
            Long userId,
            String venueCode,
            FeedbackStatus status,
            FeedbackType feedbackType,
            FeedbackSentiment sentiment,
            FeedbackPriority priority,
            String guestName,
            String text,
            String previousState,
            String correlationId,
            String adminChatId,
            Long adminMessageId,
            Instant createdAt,
            Instant updatedAt
    ) {
        static FeedbackResponse from(GuestFeedback feedback) {
            return new FeedbackResponse(
                    feedback.id(),
                    feedback.chatId(),
                    feedback.telegramUserId(),
                    feedback.userId(),
                    feedback.venueCode(),
                    feedback.status(),
                    feedback.feedbackType(),
                    feedback.sentiment(),
                    feedback.priority(),
                    feedback.guestName(),
                    feedback.text(),
                    feedback.previousState(),
                    feedback.correlationId(),
                    feedback.adminChatId(),
                    feedback.adminMessageId(),
                    feedback.createdAt(),
                    feedback.updatedAt()
            );
        }
    }
}
