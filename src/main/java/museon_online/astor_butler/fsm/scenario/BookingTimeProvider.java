package museon_online.astor_butler.fsm.scenario;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

@Component
public class BookingTimeProvider {

    public static final ZoneId VENUE_ZONE = ZoneId.of("Asia/Yekaterinburg");

    private final Clock clock;

    public BookingTimeProvider() {
        this(Clock.system(VENUE_ZONE));
    }

    public BookingTimeProvider(Clock clock) {
        this.clock = clock.withZone(VENUE_ZONE);
    }

    public Instant now() {
        return clock.instant();
    }

    public LocalDate today() {
        return LocalDate.now(clock);
    }

    public LocalTime nowTime() {
        return LocalTime.now(clock);
    }

    public LocalTime nextWholeHour() {
        LocalTime now = nowTime();
        LocalTime hour = now.withMinute(0).withSecond(0).withNano(0);
        return now.equals(hour) ? hour : hour.plusHours(1);
    }
}
