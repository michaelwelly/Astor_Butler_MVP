package museon_online.astor_butler.fsm.core.events;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class LoggingEventPublisher implements DomainEventPublisher {

    @Override
    public void publish(DomainEvent event) {
        log.info(
                "📣 [DOMAIN-EVENT] type={}, aggregateId={}, payload={}",
                event.getEventType(),
                event.getAggregateId(),
                event.getPayload()
        );
    }
}