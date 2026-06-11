package museon_online.astor_butler.domain.timeline;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnMissingBean(FsmTimelineWriter.class)
public class NoopFsmTimelineWriter implements FsmTimelineWriter {

    @Override
    public void append(FsmTimelineEvent event) {
        // Timeline storage is optional for local unit tests and lightweight IDE runs.
    }
}
