package museon_online.astor_butler.domain.concierge;

import lombok.RequiredArgsConstructor;
import museon_online.astor_butler.api.common.ApiException;
import museon_online.astor_butler.api.common.ErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ConciergeRequestService {

    private final ConciergeRequestRepository repository;

    public ConciergeRequest getRequest(Long id) {
        if (id == null) {
            throw badRequest("concierge request id is required");
        }
        return repository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ErrorCode.NOT_FOUND, "Concierge request was not found", Map.of("id", id)));
    }

    public List<ConciergeRequest> listByChatId(Long chatId, int limit) {
        if (chatId == null) {
            throw badRequest("chatId is required");
        }
        return repository.findByChatId(chatId, Math.max(1, Math.min(limit, 100)));
    }

    @Transactional
    public ConciergeRequest createRequest(ConciergeRequestCommand command) {
        validate(command);
        return repository.create(command);
    }

    @Transactional
    public ConciergeRequest markInProgress(Long id) {
        ConciergeRequest request = requireMutableRequest(id);
        if (request.status() == ConciergeRequestStatus.IN_PROGRESS) {
            return request;
        }
        return repository.updateStatus(request.id(), ConciergeRequestStatus.IN_PROGRESS);
    }

    @Transactional
    public ConciergeRequest complete(Long id) {
        ConciergeRequest request = requireMutableRequest(id);
        return repository.updateStatus(request.id(), ConciergeRequestStatus.COMPLETED);
    }

    @Transactional
    public ConciergeRequest cancel(Long id) {
        ConciergeRequest request = requireMutableRequest(id);
        return repository.updateStatus(request.id(), ConciergeRequestStatus.CANCELLED);
    }

    public ConciergeRequestType classify(String text) {
        String normalized = text == null ? "" : text.toLowerCase(Locale.ROOT);
        if (containsAny(normalized, "день рождения", "свеч", "поздрав", "сюрприз", "торт", "букет")) {
            return ConciergeRequestType.CELEBRATION;
        }
        if (containsAny(normalized, "плед", "тише", "громко", "кондиционер", "посад", "пересад", "удоб")) {
            return ConciergeRequestType.COMFORT;
        }
        if (containsAny(normalized, "менеджер", "управляющ", "администратор", "старш")) {
            return ConciergeRequestType.MANAGER;
        }
        if (containsAny(normalized, "принеси", "подай", "подготов", "организ", "попроси", "позови")) {
            return ConciergeRequestType.SERVICE;
        }
        return ConciergeRequestType.GENERAL;
    }

    private void validate(ConciergeRequestCommand command) {
        if (command == null) {
            throw badRequest("Request body is required");
        }
        if (command.chatId() == null) {
            throw badRequest("chatId is required");
        }
        if (command.requestType() == null) {
            throw badRequest("requestType is required");
        }
        if (command.requestText() == null || command.requestText().isBlank()) {
            throw badRequest("requestText is required");
        }
        if (command.requestText().length() > 3000) {
            throw badRequest("requestText is too long");
        }
    }

    private ConciergeRequest requireMutableRequest(Long id) {
        ConciergeRequest request = getRequest(id);
        if (request.status() == ConciergeRequestStatus.COMPLETED
                || request.status() == ConciergeRequestStatus.CANCELLED) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    ErrorCode.CONFLICT,
                    "Concierge request is already final",
                    Map.of("id", id, "status", request.status())
            );
        }
        return request;
    }

    private boolean containsAny(String text, String... variants) {
        for (String variant : variants) {
            if (text.contains(variant)) {
                return true;
            }
        }
        return false;
    }

    private ApiException badRequest(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, ErrorCode.BAD_REQUEST, message);
    }
}
