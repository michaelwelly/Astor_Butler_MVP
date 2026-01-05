//package museon_online.astor_butler.telegram.command;
//
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import museon_online.astor_butler.telegram.handler.AfishaHandler;
//import museon_online.astor_butler.telegram.utils.BotCommand;
//import museon_online.astor_butler.telegram.utils.BotResponse;
//import org.telegram.telegrambots.meta.api.objects.Update;
//
//@Slf4j
//@RequiredArgsConstructor
//public class AfishaCommand implements BotCommand {
//
//    private final AfishaHandler afishaHandler;
//
//    @Override
//    public String getCommand() {
//        return "/afisha";
//    }
//
//    @Override
//    public String getDescription() {
//        return "🎭 Посмотреть афишу ближайших событий";
//    }
//
//    @Override
//    public BotResponse execute(Update update) {
//        return new BotResponse(afishaHandler.handleAfisha());
//    }
//}
