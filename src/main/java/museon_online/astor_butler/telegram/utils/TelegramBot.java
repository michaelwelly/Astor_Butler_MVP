package museon_online.astor_butler.telegram.utils;

import museon_online.astor_butler.config.TelegramBotConfig;
import museon_online.astor_butler.telegram.adapter.TelegramRouter;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;

@Component
public class TelegramBot extends TelegramLongPollingBot {

    private final TelegramRouter telegramRouter;
    private final TelegramBotConfig telegramBotConfig;

    public TelegramBot(TelegramRouter telegramRouter, TelegramBotConfig telegramBotConfig) {
        super(telegramBotConfig.botOptions());
        this.telegramRouter = telegramRouter;
        this.telegramBotConfig = telegramBotConfig;
    }

    @Override
    public String getBotUsername() {
        return telegramBotConfig.getUsername();
    }

    @Override
    public String getBotToken() {
        return telegramBotConfig.getToken();
    }
    @Override
    public void onUpdateReceived(Update update) {
        telegramRouter.handle(update, this);
    }

}
