package museon_online.astor_butler.domain.merch;

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
public class MerchService {

    private final MerchRepository repository;

    public List<MerchItem> listActiveItems(String venueCode) {
        return repository.findActiveItems(venueCode);
    }

    public MerchOrder getOrder(Long id) {
        if (id == null) {
            throw badRequest("merch order id is required");
        }
        return repository.findOrder(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ErrorCode.NOT_FOUND, "Merch order was not found", Map.of("id", id)));
    }

    public List<MerchOrder> listOrdersByChatId(Long chatId, int limit) {
        if (chatId == null) {
            throw badRequest("chatId is required");
        }
        return repository.findOrdersByChatId(chatId, Math.max(1, Math.min(limit, 100)));
    }

    @Transactional
    public MerchOrder createOrder(MerchOrderCommand command) {
        validate(command);
        MerchItem item = resolveItem(command);
        return repository.createOrder(command, item);
    }

    @Transactional
    public MerchOrder confirmLatestDraft(Long chatId) {
        MerchOrder order = requireLatestAwaitingConfirmation(chatId);
        return confirmDraft(order.id());
    }

    @Transactional
    public MerchOrder cancelLatestDraft(Long chatId) {
        MerchOrder order = requireLatestAwaitingConfirmation(chatId);
        return cancelDraft(order.id());
    }

    @Transactional
    public MerchOrder confirmDraft(Long id) {
        MerchOrder order = requireAwaitingConfirmation(id);
        return repository.updateStatus(order.id(), MerchOrderStatus.PENDING_TEAM);
    }

    @Transactional
    public MerchOrder cancelDraft(Long id) {
        MerchOrder order = requireAwaitingConfirmation(id);
        return repository.updateStatus(order.id(), MerchOrderStatus.CANCELLED);
    }

    private MerchItem resolveItem(MerchOrderCommand command) {
        if (command.itemId() != null) {
            return repository.findItem(command.itemId())
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ErrorCode.NOT_FOUND, "Merch item was not found", Map.of("itemId", command.itemId())));
        }
        return repository.findDefaultItem(command.venueCode()).orElse(null);
    }

    private void validate(MerchOrderCommand command) {
        if (command == null) {
            throw badRequest("Request body is required");
        }
        if (command.chatId() == null) {
            throw badRequest("chatId is required");
        }
        if (command.quantity() != null && command.quantity() < 1) {
            throw badRequest("quantity must be positive");
        }
        if ((command.itemTitle() == null || command.itemTitle().isBlank()) && command.itemId() == null) {
            // Default item can still be selected from catalog; this just keeps totally empty API requests honest.
            return;
        }
    }

    private MerchOrder requireLatestAwaitingConfirmation(Long chatId) {
        if (chatId == null) {
            throw badRequest("chatId is required");
        }
        return repository.findLatestAwaitingGuestConfirmation(chatId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        ErrorCode.NOT_FOUND,
                        "Active merch draft was not found",
                        Map.of("chatId", chatId)
                ));
    }

    private MerchOrder requireAwaitingConfirmation(Long id) {
        MerchOrder order = getOrder(id);
        if (order.status() != MerchOrderStatus.AWAITING_GUEST_CONFIRMATION) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    ErrorCode.CONFLICT,
                    "Only awaiting guest confirmation merch drafts can be changed",
                    Map.of("id", id, "status", order.status())
            );
        }
        return order;
    }

    private ApiException badRequest(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, ErrorCode.BAD_REQUEST, message);
    }
}
