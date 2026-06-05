package museon_online.astor_butler.service.message;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import java.nio.charset.StandardCharsets;

@Service
@Slf4j
public class LlmScenarioPromptCatalog {

    private final String tableBookingContract;

    public LlmScenarioPromptCatalog(@Value("classpath:fsm/table-booking-llm-contract.md") Resource tableBookingContract) {
        this.tableBookingContract = read(tableBookingContract);
    }

    public String tableBookingContract() {
        return tableBookingContract;
    }

    private String read(Resource resource) {
        try {
            return StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.warn("LLM scenario prompt resource was not loaded: {}", e.getMessage());
            return "Table booking FSM contract unavailable. Ask for date, time, party size, table preference, and contact.";
        }
    }
}
