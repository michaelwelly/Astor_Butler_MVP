package museon_online.astor_butler.telegram.adapter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Arrays;

@Component
@RequiredArgsConstructor
@Slf4j
public class StartupAdminNotifier {

    private final TelegramAdminNotifier telegramAdminNotifier;
    private final Environment environment;

    @Value("${astor.startup.admin-notification-enabled:true}")
    private boolean enabled;

    @EventListener(ApplicationReadyEvent.class)
    public void notifyAdminChat() {
        if (!enabled) {
            log.debug("Startup admin notification skipped: disabled by configuration");
            return;
        }

        telegramAdminNotifier.sendAnalytics(buildMessage());
    }

    String buildMessage() {
        String profiles = activeProfiles();
        String port = environment.getProperty("server.port", "8088");
        String appName = environment.getProperty("spring.application.name", "astor-butler");

        return """
                <b>Astor Butler / startup</b>
                Я в строю. Все работает.

                <b>Система</b>
                App: %s
                Profile: %s
                Backend: localhost:%s

                <b>Контур</b>
                Telegram: online
                FSM: ready
                Kafka admin stream: watching

                <b>Инструкции</b>
                Guest: https://michaelwelly.github.io/Astor_Butler_MVP/docs/guest-guide.html
                Staff: https://michaelwelly.github.io/Astor_Butler_MVP/docs/staff-guide.html

                <b>Техника</b>
                Started at: %s
                """.formatted(
                html(appName),
                html(profiles),
                html(port),
                html(Instant.now().toString())
        );
    }

    private String activeProfiles() {
        String[] profiles = environment.getActiveProfiles();
        if (profiles.length == 0) {
            return "default";
        }
        return String.join(", ", Arrays.asList(profiles));
    }

    private String html(String value) {
        return value == null ? ""
                : value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}
