//package museon_online.astor_butler.telegram.command;
//
//import lombok.RequiredArgsConstructor;
//import museon_online.astor_butler.feedback.FeedbackState;
//import museon_online.astor_butler.feedback.FeedbackUserState;
//import museon_online.astor_butler.telegram.utils.BotCommand;
//import museon_online.astor_butler.telegram.utils.BotResponse;
//import org.telegram.telegrambots.meta.api.objects.Update;
//
//@RequiredArgsConstructor
//public class FeedbackCommand implements BotCommand {
//
//    private final FeedbackUserState userState;
//
//    @Override
//    public String getCommand() {
//        return "/feedback";
//    }
//
//    @Override
//    public String getDescription() {
//        return "Оставить отзыв";
//    }
//
//    @Override
//    public BotResponse execute(Update update) {
//        Long userId = update.getMessage().getFrom().getId();
//        userState.set(userId, FeedbackState.AWAITING_FEEDBACK);
//
//        return new BotResponse(
//                "Напишите ваш отзыв одним сообщением. Мы его обязательно прочтём 🙏");
//    }
//}
