package museon_online.astor_butler.fsm.understanding;

import museon_online.astor_butler.fsm.core.BotState;

public interface RussianNluAdapter {

    RussianNluResult analyze(String text, BotState currentState);
}
