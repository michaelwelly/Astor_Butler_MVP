package museon_online.astor_butler.fsm.storage;

public interface FSMStorage {

    void setState(String userId, String state);

    String getState(String userId);

    void clear(String userId);
}