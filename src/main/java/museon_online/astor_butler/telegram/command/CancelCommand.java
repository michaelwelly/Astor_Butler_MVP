//package museon_online.astor_butler.telegram.command;
//
//import lombok.RequiredArgsConstructor;
//import museon_online.astor_butler.fsm.core.FSMRouter;
//import museon_online.astor_butler.telegram.utils.BotCommand;
//import museon_online.astor_butler.telegram.utils.BotResponse;
//import org.springframework.stereotype.Component;
//import org.telegram.telegrambots.meta.api.objects.Update;
//
///**
// * Команда для отмены текущего FSM-сценария.
// */
//@Component
//@RequiredArgsConstructor
//public class CancelCommand implements BotCommand {
//
//    private final FSMRouter fsmRouter;
//
//    @Override
//    public String getDescription() {
//        return "❌ Отменить текущий сценарий";
//    }
//
//    @Override
//    public String getCommand() {
//        return "/cancel";
//    }
//
//    @Override
//    public BotResponse execute(Update update) {
//        Long userId = update.getMessage().getChatId();
//        fsmRouter.resetFSM(userId);
//        return new BotResponse("❌ Сценарий отменён. Вы можете начать заново.");
//    }
//}