package museon_online.astor_butler.fsm.core.idempotency;

import lombok.RequiredArgsConstructor;
import museon_online.astor_butler.fsm.core.event.InboundEvent;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class IdempotencyGuard {

    private final IdempotencyService idempotencyService;

    public boolean accept(InboundEvent event) {
        return idempotencyService.accept(event.getEventId());
    }
}