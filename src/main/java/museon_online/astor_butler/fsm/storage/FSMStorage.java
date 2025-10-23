package museon_online.astor_butler.fsm.storage;

import museon_online.astor_butler.fsm.core.BotState;

public interface FSMStorage {

    void setState(Long chatId, BotState state);

    void clear(Long chatId);

    BotState getState(Long chatId);

}