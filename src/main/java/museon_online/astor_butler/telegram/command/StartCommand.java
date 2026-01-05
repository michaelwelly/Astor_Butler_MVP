//package museon_online.astor_butler.telegram.command;
//
//import lombok.extern.slf4j.Slf4j;
//import museon_online.astor_butler.telegram.utils.BotCommand;
//import museon_online.astor_butler.telegram.utils.BotResponse;
//import org.springframework.stereotype.Component;
//import org.telegram.telegrambots.meta.api.objects.Update;
//import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
//import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
//
//import java.util.List;
//
//@Slf4j
//@Component
//public class StartCommand implements BotCommand {
//
//    @Override
//    public String getCommand() {
//        return "/start";
//    }
//
//    @Override
//    public String getDescription() {
//        return "Запуск бота и приветствие пользователя.";
//    }
//
//    @Override
//    public BotResponse execute(Update update) {
//        try {
//            InlineKeyboardMarkup markup = createPolicyKeyboard();
//
//            String text = """
//                🔐 Уважаемый гость,
//
//                Перед тем как открыть Вам доступ к эфиру и продолжить наше знакомство, прошу подтвердить своё согласие на обработку персональных данных.
//
//                📄 Ознакомиться с политикой: https://docs.google.com/document/d/1RxoK6MYSmOR4nL_0MIWhvtbuNLOdWEXuKMhP1lhKnTw
//
//                Прошу выбрать, как мы с Вами поступим:
//                """;
//
//            return new BotResponse(text, markup);
//
//        } catch (Exception e) {
//            log.error("Ошибка при выполнении команды /start", e);
//            return new BotResponse("Произошла ошибка при запуске. Пожалуйста, попробуйте позже.");
//        }
//    }
//
//    private InlineKeyboardMarkup createPolicyKeyboard() {
//        InlineKeyboardButton agreeButton = new InlineKeyboardButton("✓ Согласен и продолжаем");
//        agreeButton.setCallbackData("policy:agree");
//
//        InlineKeyboardButton declineButton = new InlineKeyboardButton("🚪 Не согласен");
//        declineButton.setCallbackData("policy:decline");
//
//        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
//        markup.setKeyboard(List.of(
//                List.of(agreeButton),
//                List.of(declineButton)
//        ));
//
//        return markup;
//    }
//}