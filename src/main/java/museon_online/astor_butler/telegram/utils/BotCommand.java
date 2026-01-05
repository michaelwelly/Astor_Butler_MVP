package museon_online.astor_butler.telegram.utils;

import org.telegram.telegrambots.meta.api.objects.Update;

public interface BotCommand {
    String getCommand();                        // Имя команды (например, "/start")
    String getDescription();                   // Описание команды
    BotResponse execute(Update update);        // Новый формат: возвращаем объект ответа
}