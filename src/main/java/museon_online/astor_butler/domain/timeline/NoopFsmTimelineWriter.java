package museon_online.astor_butler.domain.timeline;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "astor.timeline.scylla.enabled", havingValue = "false", matchIfMissing = true)
public class NoopFsmTimelineWriter implements FsmTimelineWriter {

    @Override
    public void append(FsmTimelineEvent event) {
        // Timeline storage is optional for local unit tests and lightweight IDE runs.
    }
}
