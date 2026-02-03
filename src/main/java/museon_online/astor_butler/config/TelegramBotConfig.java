package museon_online.astor_butler.config;

import lombok.Getter;
import lombok.Setter;
import museon_online.astor_butler.telegram.utils.TelegramBot;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@Configuration
@ConfigurationProperties(prefix = "telegram.bot")
@Getter @Setter
public class TelegramBotConfig {
    private String username;
    private String token;

    @Bean
    public TelegramBotsApi telegramBotsApi(TelegramBot bot) throws TelegramApiException {
        TelegramBotsApi api = new TelegramBotsApi(DefaultBotSession.class);
        api.registerBot(bot);
        return api;
    }
}