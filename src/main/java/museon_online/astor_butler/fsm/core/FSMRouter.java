package museon_online.astor_butler.fsm.core;

import lombok.RequiredArgsConstructor;
import museon_online.astor_butler.fsm.handler.FSMHandler;
import museon_online.astor_butler.fsm.storage.FSMStorage;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class FSMRouter {

    private final List<FSMHandler> handlers;
    private final FSMStorage fsmStorage;

    public void route(CommandContext ctx) {
        Long chatId = ctx.getChatId();

        // 1️⃣ Получаем текущее состояние из Redis
        BotState currentState = fsmStorage.getState(chatId);

        // 2️⃣ Если состояния нет — начинаем с GREETING
        if (currentState == null) {
            currentState = BotState.GREETING;
            fsmStorage.setState(chatId, currentState);
        }

        final BotState stateToHandle = currentState;

        // 3️⃣ Находим хэндлер по состоянию
        FSMHandler handler = handlers.stream()
                .filter(h -> h.getState().equals(stateToHandle))
                .findFirst()
                .orElseGet(() -> handlers.stream()
                        .filter(h -> h.getState().equals(BotState.AI_FALLBACK))
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException("No fallback handler found")));

        // 4️⃣ Вызываем обработку
        handler.handle(ctx);
    }
}