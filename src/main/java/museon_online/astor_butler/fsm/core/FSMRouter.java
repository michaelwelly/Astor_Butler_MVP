package museon_online.astor_butler.fsm.core;

import lombok.extern.slf4j.Slf4j;
import museon_online.astor_butler.fsm.handler.FSMHandler;
import museon_online.astor_butler.fsm.storage.FSMStorage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class FSMRouter {

    private final Map<BotState, FSMHandler> handlers = new ConcurrentHashMap<>();
    private final FSMStorage storage;

    @Autowired
    public FSMRouter(FSMStorage storage, List<FSMHandler> handlerList) {
        this.storage = storage;
        handlerList.forEach(h -> handlers.put(h.getState(), h));
        log.info("🧩 [FSM] Registered {} handlers: {}", handlers.size(), handlers.keySet());
    }

    public void route(CommandContext ctx) {
        Long chatId = ctx.getChatId();
        BotState current = storage.getState(chatId);
        if (current == null) current = BotState.UNKNOWN;

        FSMHandler handler = handlers.get(current);
        if (handler == null) {
            log.warn("⚠️ [FSM] No handler for {}, switching to MENU", current);
            storage.setState(chatId, BotState.MENU);
            handler = handlers.get(BotState.MENU);
        }

        log.info("▶️ [FSM] Executing {} for chatId={}", handler.getClass().getSimpleName(), chatId);
        handler.handle(ctx);
    }
}