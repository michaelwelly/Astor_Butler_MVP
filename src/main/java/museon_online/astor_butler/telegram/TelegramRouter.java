package museon_online.astor_butler.telegram;

import lombok.RequiredArgsConstructor;
import museon_online.astor_butler.fsm.core.CommandContext;
import museon_online.astor_butler.fsm.core.CommandContextBuilder;
import museon_online.astor_butler.fsm.core.FSMRouter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.bots.AbsSender;

@Component
@RequiredArgsConstructor
public class TelegramRouter {

    private final FSMRouter fsmRouter;
    private final CommandContextBuilder  ctxBuilder;
    private final TelegramExceptionHandler exceptionHandler;

    public void handle(Update update, AbsSender sender) {
        try {
            CommandContext ctx = ctxBuilder.from(update);
            fsmRouter.route(ctx);
        } catch (Exception e) {
            exceptionHandler.handle(update, e, sender);
        }
    }
}