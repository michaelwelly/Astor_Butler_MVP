package museon_online.astor_butler.domain.payment;

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
public class TelegramStarPaymentService {

    private final TelegramStarPaymentRepository repository;

    public TelegramStarPayment get(Long id) {
        if (id == null) {
            throw badRequest("payment id is required");
        }
        return repository.find(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ErrorCode.NOT_FOUND, "Telegram Stars payment was not found", Map.of("id", id)));
    }

    public List<TelegramStarPayment> listByChatId(Long chatId, int limit) {
        if (chatId == null) {
            throw badRequest("chatId is required");
        }
        return repository.findByChatId(chatId, Math.max(1, Math.min(limit, 100)));
    }

    @Transactional
    public TelegramStarPayment createDraft(TelegramStarPaymentCommand command) {
        validate(command);
        return repository.createDraft(command);
    }

    private void validate(TelegramStarPaymentCommand command) {
        if (command == null) {
            throw badRequest("Request body is required");
        }
        if (command.chatId() == null) {
            throw badRequest("chatId is required");
        }
        if (command.purpose() == null) {
            throw badRequest("purpose is required");
        }
        if (command.title() == null || command.title().isBlank()) {
            throw badRequest("title is required");
        }
        if (command.starAmount() == null || command.starAmount() < 1) {
            throw badRequest("starAmount must be positive");
        }
    }

    private ApiException badRequest(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, ErrorCode.BAD_REQUEST, message);
    }
}
