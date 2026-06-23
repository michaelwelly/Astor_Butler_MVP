package museon_online.astor_butler.domain.web;

import java.util.UUID;

public record WebSessionResolution(
        UUID id,
        String sessionId,
        String externalUserId,
        Long chatId
) {
}
