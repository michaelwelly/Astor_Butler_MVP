package museon_online.astor_butler.fsm.handler;

import museon_online.astor_butler.fsm.core.CommandContext;

/** Любой хэндлер одной «сцены» FSM. */

public interface FSMHandler {

    boolean canHandle(CommandContext ctx);
    void handle(CommandContext ctx);
}