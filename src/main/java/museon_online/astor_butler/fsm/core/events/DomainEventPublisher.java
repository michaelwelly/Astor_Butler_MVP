package museon_online.astor_butler.fsm.core.events;

public interface DomainEventPublisher {

    void publish(DomainEvent event);
}