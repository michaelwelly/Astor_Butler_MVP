package museon_online.astor_butler.domain.tip;

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
public class TipService {

    private final TipRepository repository;

    public List<StaffProfile> listActiveStaff(String venueCode) {
        return repository.findActiveStaff(venueCode);
    }

    public TipOrder getOrder(Long id) {
        return requireOrder(id);
    }

    public List<TipOrder> listOrdersByChatId(Long chatId, int limit) {
        if (chatId == null) {
            throw badRequest("chatId is required");
        }
        int safeLimit = Math.max(1, Math.min(limit, 100));
        return repository.findOrdersByChatId(chatId, safeLimit);
    }

    @Transactional
    public TipOrder createDraft(TipOrderCommand command) {
        validateCommand(command);
        StaffProfile staff = resolveStaff(command);
        return repository.createDraft(command, staff);
    }

    private StaffProfile resolveStaff(TipOrderCommand command) {
        if (command.staffId() != null) {
            return repository.findStaff(command.staffId())
                    .orElseThrow(() -> new ApiException(
                            HttpStatus.NOT_FOUND,
                            ErrorCode.NOT_FOUND,
                            "Staff profile was not found",
                            Map.of("staffId", command.staffId())
                    ));
        }
        return repository.findDefaultStaff(command.venueCode()).orElse(null);
    }

    private TipOrder requireOrder(Long id) {
        if (id == null) {
            throw badRequest("tip order id is required");
        }
        return repository.findOrder(id)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        ErrorCode.NOT_FOUND,
                        "Tip order was not found",
                        Map.of("id", id)
                ));
    }

    private void validateCommand(TipOrderCommand command) {
        if (command == null) {
            throw badRequest("Request body is required");
        }
        if (command.chatId() == null) {
            throw badRequest("chatId is required");
        }
        if (command.amountMinor() != null && command.amountMinor() < 1) {
            throw badRequest("amountMinor must be positive");
        }
    }

    private ApiException badRequest(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, ErrorCode.BAD_REQUEST, message);
    }
}
