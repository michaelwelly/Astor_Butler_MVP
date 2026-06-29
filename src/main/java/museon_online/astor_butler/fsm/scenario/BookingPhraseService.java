package museon_online.astor_butler.fsm.scenario;

import org.springframework.stereotype.Component;

@Component
public class BookingPhraseService {

    public String ask(TableBookingStepRegistry.Step step, TableBookingDraftStorage.Draft draft) {
        return step.deterministicPrompt();
    }
}
