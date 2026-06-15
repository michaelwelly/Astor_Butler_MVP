package museon_online.astor_butler.domain.booking;

import lombok.RequiredArgsConstructor;
import museon_online.astor_butler.api.common.ApiException;
import museon_online.astor_butler.api.common.ErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class EventBookingService {

    private final EventBookingRepository repository;

    public EventBookingOrder getOrder(Long id) {
        return requireOrder(id);
    }

    public List<EventBookingOrder> listOrdersByChatId(Long chatId, int limit) {
        if (chatId == null) {
            throw badRequest("chatId is required");
        }
        int safeLimit = Math.max(1, Math.min(limit, 100));
        return repository.findOrdersByChatId(chatId, safeLimit);
    }

    public List<EventBookingOrder> listActiveOrdersByChatId(Long chatId) {
        if (chatId == null) {
            throw badRequest("chatId is required");
        }
        return repository.findActiveOrdersByChatId(chatId);
    }

    @Transactional
    public EventBookingOrder createOrder(EventBookingCommand command) {
        validateCommand(command);
        return repository.createAwaitingManagerReview(command);
    }

    private EventBookingOrder requireOrder(Long id) {
        if (id == null) {
            throw badRequest("event booking id is required");
        }
        return repository.findOrder(id)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        ErrorCode.NOT_FOUND,
                        "Event booking order was not found",
                        Map.of("id", id)
                ));
    }

    private void validateCommand(EventBookingCommand command) {
        if (command == null) {
            throw badRequest("Request body is required");
        }
        if (command.chatId() == null) {
            throw badRequest("chatId is required");
        }
        if (command.guestCount() != null && command.guestCount() < 1) {
            throw badRequest("guestCount must be positive");
        }
    }

    private ApiException badRequest(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, ErrorCode.BAD_REQUEST, message);
    }
}
