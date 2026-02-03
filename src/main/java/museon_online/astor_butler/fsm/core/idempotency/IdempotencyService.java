package museon_online.astor_butler.fsm.core.idempotency;

import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class IdempotencyService {

    private final Set<String> processedEventIds = ConcurrentHashMap.newKeySet();

    /**
     * @return true — если событие новое и принято
     *         false — если событие уже обрабатывалось
     */
    public boolean accept(String eventId) {
        return processedEventIds.add(eventId);
    }
}