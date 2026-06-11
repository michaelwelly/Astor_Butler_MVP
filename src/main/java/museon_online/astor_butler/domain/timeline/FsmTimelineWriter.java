package museon_online.astor_butler.domain.timeline;

public interface FsmTimelineWriter {

    void append(FsmTimelineEvent event);
}
