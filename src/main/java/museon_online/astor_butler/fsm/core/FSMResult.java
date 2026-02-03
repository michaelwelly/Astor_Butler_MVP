package museon_online.astor_butler.fsm.core;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import museon_online.astor_butler.telegram.utils.BotResponse;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FSMResult {

    private BotState nextState;
    private BotResponse response;

    public static FSMResult empty() {
        return new FSMResult(null, null);
    }

    public boolean hasResponse() {
        return response != null;
    }

    public boolean hasNextState() {
        return nextState != null;
    }
}