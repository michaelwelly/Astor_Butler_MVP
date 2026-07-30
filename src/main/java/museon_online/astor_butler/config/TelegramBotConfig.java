package museon_online.astor_butler.config;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import museon_online.astor_butler.telegram.utils.TelegramBot;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@Configuration
@ConfigurationProperties(prefix = "telegram.bot")
@Getter @Setter
@Slf4j
public class TelegramBotConfig {
    private String username;
    private String token;

    @Value("${telegram.bot.registration-retry-delay-seconds:20}")
    private long registrationRetryDelaySeconds;

    @Value("${telegram.bot.proxy.type:NO_PROXY}")
    private String proxyType;

    @Value("${telegram.bot.proxy.host:}")
    private String proxyHost;

    @Value("${telegram.bot.proxy.port:0}")
    private int proxyPort;

    @Bean
    @ConditionalOnProperty(prefix = "telegram.bot", name = "enabled", havingValue = "true", matchIfMissing = true)
    public TelegramBotsApi telegramBotsApi() throws TelegramApiException {
        return new TelegramBotsApi(DefaultBotSession.class);
    }

    @Bean
    @ConditionalOnProperty(prefix = "telegram.bot", name = "enabled", havingValue = "true", matchIfMissing = true)
    public ApplicationRunner telegramBotRegistrationRunner(TelegramBotsApi api, TelegramBot bot) {
        return args -> {
            Thread thread = new Thread(() -> registerWithRetry(api, bot), "telegram-bot-registration");
            thread.setDaemon(true);
            thread.start();
        };
    }

    private void registerWithRetry(TelegramBotsApi api, TelegramBot bot) {
        if (token == null || token.isBlank() || username == null || username.isBlank()) {
            log.warn("Telegram bot registration skipped: username/token is blank");
            return;
        }
        long retryDelayMs = Math.max(1, registrationRetryDelaySeconds) * 1000;
        while (!Thread.currentThread().isInterrupted()) {
            try {
                api.registerBot(bot);
                log.info("Telegram bot registered: username={}", username);
                return;
            } catch (Exception e) {
                log.warn(
                        "Telegram bot registration failed, retrying in {}s: {}",
                        Math.max(1, registrationRetryDelaySeconds),
                        e.toString()
                );
                try {
                    Thread.sleep(retryDelayMs);
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    public DefaultBotOptions botOptions() {
        DefaultBotOptions options = new DefaultBotOptions();
        DefaultBotOptions.ProxyType type = proxyType();
        options.setProxyType(type);
        if (type != DefaultBotOptions.ProxyType.NO_PROXY) {
            if (proxyHost == null || proxyHost.isBlank() || proxyPort <= 0) {
                log.warn("Telegram proxy config ignored: type={} host/port is blank", type);
                options.setProxyType(DefaultBotOptions.ProxyType.NO_PROXY);
                return options;
            }
            options.setProxyHost(proxyHost.trim());
            options.setProxyPort(proxyPort);
            log.info("Telegram proxy enabled: type={} host={} port={}", type, proxyHost.trim(), proxyPort);
        }
        return options;
    }

    private DefaultBotOptions.ProxyType proxyType() {
        if (proxyType == null || proxyType.isBlank()) {
            return DefaultBotOptions.ProxyType.NO_PROXY;
        }
        return switch (proxyType.trim().toUpperCase(java.util.Locale.ROOT).replace("-", "_")) {
            case "HTTP" -> DefaultBotOptions.ProxyType.HTTP;
            case "SOCKS4" -> DefaultBotOptions.ProxyType.SOCKS4;
            case "SOCKS5" -> DefaultBotOptions.ProxyType.SOCKS5;
            case "NO_PROXY", "NONE", "OFF", "FALSE" -> DefaultBotOptions.ProxyType.NO_PROXY;
            default -> {
                log.warn("Unknown Telegram proxy type '{}', falling back to NO_PROXY", proxyType);
                yield DefaultBotOptions.ProxyType.NO_PROXY;
            }
        };
    }
}
