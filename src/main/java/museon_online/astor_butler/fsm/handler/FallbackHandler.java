//package museon_online.astor_butler.fsm.handler;
//
//import lombok.RequiredArgsConstructor;
//import museon_online.astor_butler.deepseek.DeepSeekClient;
//import museon_online.astor_butler.fsm.core.BotState;
//import museon_online.astor_butler.fsm.core.CommandContext;
//import museon_online.astor_butler.telegram.TelegramSender;
//import org.springframework.stereotype.Component;
//
//@Component
//@RequiredArgsConstructor
//public class FallbackHandler implements FSMHandler {
//
//   private final DeepSeekClient deepSeekClient;
//   private final TelegramSender telegramSender;
//
//    @Override
//    public BotState getState() {
//        return BotState.AI_FALLBACK;
//    }
//
//    @Override
//    public void handle(CommandContext ctx) {
//        deepSeekClient.askAsync(ctx.getMessageText())
//                .subscribe(reply -> telegramSender.sendText(ctx.getChatId(), reply));
//    }
//}
