package museon_online.astor_butler.fsm.core.events;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class DomainEvent {

    private String eventType;
    private String aggregateId;
    private String payload;
}