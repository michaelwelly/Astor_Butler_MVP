package museon_online.astor_butler.fsm.storage;

import museon_online.astor_butler.fsm.core.BotState;

import java.util.List;

public interface FSMStorage {

    void setState(Long chatId, BotState state);

    void clear(Long chatId);

    BotState getState(Long chatId);

    default void setPendingIntents(Long chatId, List<String> intents) {
    }

    default List<String> getPendingIntents(Long chatId) {
        return List.of();
    }

    default void clearPendingIntents(Long chatId) {
    }

}
