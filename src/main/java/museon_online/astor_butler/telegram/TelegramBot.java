package museon_online.astor_butler.telegram;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;

@Component
@RequiredArgsConstructor
public class TelegramBot extends TelegramLongPollingBot {

    private final TelegramRouter telegramRouter;

    @Override
    public void onUpdateReceived(Update update) {
        telegramRouter.handle(update, this);
    }

    @Override
    public String getBotUsername() {
        return "astor_butler_bot";
    }

    @Override
    @SuppressWarnings("deprecation")
    public String getBotToken() { return System.getenv("TELEGRAM_BOT_TOKEN");}
}