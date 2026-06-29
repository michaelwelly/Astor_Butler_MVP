package museon_online.astor_butler.fsm.scenario;

import museon_online.astor_butler.fsm.core.BotState;
import museon_online.astor_butler.fsm.understanding.UnderstoodInput;
import museon_online.astor_butler.service.message.IncomingMessage;
import museon_online.astor_butler.service.message.OutgoingMessage;

public interface FsmScenario {

    String id();

    int priority();

    boolean supports(IncomingMessage incoming, BotState currentState, String text);

    OutgoingMessage handle(IncomingMessage incoming, BotState currentState, String text);

    default boolean supports(IncomingMessage incoming, BotState currentState, String text, UnderstoodInput understood) {
        return supports(incoming, currentState, text);
    }

    default OutgoingMessage handle(IncomingMessage incoming, BotState currentState, String text, UnderstoodInput understood) {
        return handle(incoming, currentState, text);
    }

    default boolean owns(BotState state) {
        return false;
    }

    default boolean sideEffecting() {
        return false;
    }

    default boolean canRunInParallel() {
        return false;
    }
}
