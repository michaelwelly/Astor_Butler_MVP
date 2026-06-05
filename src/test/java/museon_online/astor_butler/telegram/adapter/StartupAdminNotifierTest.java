package museon_online.astor_butler.telegram.adapter;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

class StartupAdminNotifierTest {

    @Test
    void buildsHumanReadableStartupMessage() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("spring.application.name", "astor-butler")
                .withProperty("server.port", "8088");
        environment.setActiveProfiles("local");

        StartupAdminNotifier notifier = new StartupAdminNotifier(null, environment);

        String message = notifier.buildMessage();

        assertThat(message).contains("Astor Butler / startup");
        assertThat(message).contains("Я в строю. Все работает.");
        assertThat(message).contains("App: astor-butler");
        assertThat(message).contains("Profile: local");
        assertThat(message).contains("Backend: localhost:8088");
        assertThat(message).contains("Kafka admin stream: watching");
    }

    @Test
    void escapesHtmlValuesFromEnvironment() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("spring.application.name", "astor<&butler")
                .withProperty("server.port", "8088");

        StartupAdminNotifier notifier = new StartupAdminNotifier(null, environment);

        String message = notifier.buildMessage();

        assertThat(message).contains("astor&lt;&amp;butler");
    }
}
