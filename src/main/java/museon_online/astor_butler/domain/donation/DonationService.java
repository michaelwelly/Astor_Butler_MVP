package museon_online.astor_butler.domain.donation;

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
public class DonationService {

    private final DonationRepository repository;

    public List<DonationInitiative> listActiveInitiatives(String venueCode) {
        return repository.findActiveInitiatives(venueCode);
    }

    public DonationOrder getOrder(Long id) {
        return requireOrder(id);
    }

    public List<DonationOrder> listOrdersByChatId(Long chatId, int limit) {
        if (chatId == null) {
            throw badRequest("chatId is required");
        }
        int safeLimit = Math.max(1, Math.min(limit, 100));
        return repository.findOrdersByChatId(chatId, safeLimit);
    }

    @Transactional
    public DonationOrder createDraft(DonationOrderCommand command) {
        validateCommand(command);
        DonationInitiative initiative = resolveInitiative(command);
        return repository.createDraft(command, initiative);
    }

    private DonationInitiative resolveInitiative(DonationOrderCommand command) {
        if (command.initiativeId() != null) {
            return repository.findInitiative(command.initiativeId())
                    .orElseThrow(() -> new ApiException(
                            HttpStatus.NOT_FOUND,
                            ErrorCode.NOT_FOUND,
                            "Donation initiative was not found",
                            Map.of("initiativeId", command.initiativeId())
                    ));
        }
        return repository.findDefaultInitiative(command.venueCode()).orElse(null);
    }

    private DonationOrder requireOrder(Long id) {
        if (id == null) {
            throw badRequest("donation order id is required");
        }
        return repository.findOrder(id)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        ErrorCode.NOT_FOUND,
                        "Donation order was not found",
                        Map.of("id", id)
                ));
    }

    private void validateCommand(DonationOrderCommand command) {
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
