package museon_online.astor_butler.fsm.handler;

import museon_online.astor_butler.fsm.core.BotState;
import museon_online.astor_butler.fsm.core.CommandContext;

/** Любой хэндлер одной «сцены» FSM. */

public interface FSMHandler {
    BotState getState();
    void handle(CommandContext ctx);
}