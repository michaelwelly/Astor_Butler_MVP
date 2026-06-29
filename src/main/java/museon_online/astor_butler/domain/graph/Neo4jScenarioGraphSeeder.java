package museon_online.astor_butler.domain.graph;

import lombok.extern.slf4j.Slf4j;
import jakarta.annotation.PreDestroy;
import museon_online.astor_butler.fsm.core.BotState;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Session;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "astor.graph-memory.neo4j.enabled", havingValue = "true")
@Slf4j
public class Neo4jScenarioGraphSeeder implements ApplicationRunner, AutoCloseable {

    private final Driver driver;

    public Neo4jScenarioGraphSeeder(
            @Value("${astor.graph-memory.neo4j.uri}") String uri,
            @Value("${astor.graph-memory.neo4j.username}") String username,
            @Value("${astor.graph-memory.neo4j.password}") String password
    ) {
        this.driver = GraphDatabase.driver(uri, AuthTokens.basic(username, password));
    }

    @Override
    public void run(ApplicationArguments args) {
        try (Session session = driver.session()) {
            ensureConstraints(session);
            seedStates(session);
            seedScenarios(session);
            seedTransitions(session);
            log.info("Neo4j scenario graph seed completed");
        } catch (Exception e) {
            log.warn("Neo4j scenario graph seed skipped: {}", e.getMessage());
        }
    }

    @Override
    @PreDestroy
    public void close() {
        driver.close();
    }

    private void ensureConstraints(Session session) {
        session.run("CREATE CONSTRAINT astor_state_name IF NOT EXISTS FOR (s:FsmState) REQUIRE s.name IS UNIQUE");
        session.run("CREATE CONSTRAINT astor_scenario_code IF NOT EXISTS FOR (s:Scenario) REQUIRE s.code IS UNIQUE");
        session.run("CREATE CONSTRAINT astor_capability_code IF NOT EXISTS FOR (c:Capability) REQUIRE c.code IS UNIQUE");
    }

    private void seedStates(Session session) {
        for (BotState state : BotState.values()) {
            session.run(
                    "MERGE (s:FsmState {name: $name}) SET s.canonical = $canonical",
                    Map.of("name", state.name(), "canonical", state.canonical().name())
            );
        }
    }

    private void seedScenarios(Session session) {
        List<Map<String, Object>> scenarios = List.of(
                Map.of("code", "FIRST_TOUCH", "title", "Первое касание", "capability", "Consent Vault"),
                Map.of("code", "MAIN_MENU", "title", "Главное меню", "capability", "Memory Engine"),
                Map.of("code", "MENU_ASSETS", "title", "Меню и карты", "capability", "Quiet Guide"),
                Map.of("code", "QUIET_GUIDE", "title", "Инфо-поддержка и видео-тур", "capability", "Quiet Guide"),
                Map.of("code", "TABLE_BOOKING", "title", "Бронь стола", "capability", "Slot Keeper"),
                Map.of("code", "SMART_TIP", "title", "Чаевые", "capability", "Smart Tip"),
                Map.of("code", "HIDDEN_HEART", "title", "Благотворительность / аукцион", "capability", "Hidden Heart"),
                Map.of("code", "SAFE_EXIT", "title", "Безопасный выход", "capability", "Panic Exit")
        );

        for (Map<String, Object> scenario : scenarios) {
            session.run(
                    """
                    MERGE (s:Scenario {code: $code})
                    SET s.title = $title
                    MERGE (c:Capability {code: $capability})
                    MERGE (s)-[:BELONGS_TO]->(c)
                    """,
                    scenario
            );
        }

        linkScenarioStates(session, "FIRST_TOUCH", List.of("UNKNOWN", "CONSENT_REQUIRED", "READY_FOR_DIALOG"));
        linkScenarioStates(session, "MAIN_MENU", List.of("READY_FOR_DIALOG", "AI_FALLBACK"));
        linkScenarioStates(session, "MENU_ASSETS", List.of("MENU_ASSETS_CLARIFY", "MENU_ASSETS_DELIVERED"));
        linkScenarioStates(session, "QUIET_GUIDE", List.of("QUIET_GUIDE_CLARIFY", "QUIET_GUIDE_DELIVERED"));
        linkScenarioStates(session, "TABLE_BOOKING", List.of(
                "TABLE_BOOKING_INTENT",
                "TABLE_BOOKING_SHOW_PLAN",
                "TABLE_BOOKING_WAIT_TABLE_SELECTION",
                "TABLE_BOOKING_COLLECT_DATE",
                "TABLE_BOOKING_COLLECT_TIME",
                "TABLE_BOOKING_COLLECT_PARTY_SIZE",
                "TABLE_BOOKING_COLLECT_SEATING_PREFERENCE",
                "TABLE_BOOKING_WAIT_HOSTESS_CONFIRMATION",
                "TABLE_BOOKING_CONFIRMED",
                "TABLE_BOOKING_REJECTED",
                "TABLE_BOOKING_CHANGE_REQUESTED",
                "TABLE_BOOKING_CANCELLED"
        ));
        linkScenarioStates(session, "SMART_TIP", List.of("TIP_COLLECT_AMOUNT", "TIP_CONFIRMATION"));
        linkScenarioStates(session, "HIDDEN_HEART", List.of("DONATION_COLLECT_AMOUNT", "DONATION_CONFIRMATION", "AUCTION_RUNNING", "AUCTION_WAIT_BID"));
        linkScenarioStates(session, "SAFE_EXIT", List.of("SAFE_EXIT"));
    }

    private void linkScenarioStates(Session session, String scenarioCode, List<String> stateNames) {
        for (String stateName : stateNames) {
            session.run(
                    """
                    MATCH (scenario:Scenario {code: $scenarioCode})
                    MATCH (state:FsmState {name: $stateName})
                    MERGE (scenario)-[:OWNS_STATE]->(state)
                    """,
                    Map.of("scenarioCode", scenarioCode, "stateName", stateName)
            );
        }
    }

    private void seedTransitions(Session session) {
        List<List<String>> transitions = List.of(
                List.of("UNKNOWN", "CONSENT_REQUIRED", "first touch without contact"),
                List.of("CONSENT_REQUIRED", "READY_FOR_DIALOG", "contact and consent captured"),
                List.of("READY_FOR_DIALOG", "TABLE_BOOKING_INTENT", "guest asks for booking"),
                List.of("TABLE_BOOKING_INTENT", "TABLE_BOOKING_WAIT_TABLE_SELECTION", "hall plan sent first"),
                List.of("TABLE_BOOKING_WAIT_TABLE_SELECTION", "TABLE_BOOKING_COLLECT_DATE", "table or zone captured"),
                List.of("TABLE_BOOKING_COLLECT_DATE", "TABLE_BOOKING_COLLECT_TIME", "date captured"),
                List.of("TABLE_BOOKING_COLLECT_TIME", "TABLE_BOOKING_COLLECT_PARTY_SIZE", "time captured"),
                List.of("TABLE_BOOKING_COLLECT_PARTY_SIZE", "READY_FOR_DIALOG", "party size captured, order created, guest menu restored"),
                List.of("TABLE_BOOKING_COLLECT_SEATING_PREFERENCE", "READY_FOR_DIALOG", "legacy seating preference resolved, order created, guest menu restored"),
                List.of("TABLE_BOOKING_WAIT_HOSTESS_CONFIRMATION", "TABLE_BOOKING_CONFIRMED", "hostess approves"),
                List.of("TABLE_BOOKING_WAIT_HOSTESS_CONFIRMATION", "TABLE_BOOKING_REJECTED", "hostess rejects"),
                List.of("TABLE_BOOKING_CONFIRMED", "READY_FOR_DIALOG", "order completed"),
                List.of("TABLE_BOOKING_REJECTED", "READY_FOR_DIALOG", "guest gets alternative path"),
                List.of("READY_FOR_DIALOG", "MENU_ASSETS_CLARIFY", "guest asks for menu"),
                List.of("MENU_ASSETS_CLARIFY", "MENU_ASSETS_DELIVERED", "menu intent resolved"),
                List.of("MENU_ASSETS_DELIVERED", "READY_FOR_DIALOG", "menu delivered"),
                List.of("READY_FOR_DIALOG", "QUIET_GUIDE_CLARIFY", "guest asks about venue"),
                List.of("QUIET_GUIDE_CLARIFY", "QUIET_GUIDE_DELIVERED", "guide asset delivered"),
                List.of("QUIET_GUIDE_DELIVERED", "READY_FOR_DIALOG", "guide completed"),
                List.of("READY_FOR_DIALOG", "AI_FALLBACK", "semantic confidence too low"),
                List.of("AI_FALLBACK", "READY_FOR_DIALOG", "manual/admin recovery"),
                List.of("READY_FOR_DIALOG", "SAFE_EXIT", "safe exit requested")
        );

        for (List<String> transition : transitions) {
            session.run(
                    """
                    MATCH (from:FsmState {name: $from})
                    MATCH (to:FsmState {name: $to})
                    MERGE (from)-[r:CAN_TRANSITION_TO {reason: $reason}]->(to)
                    """,
                    Map.of("from", transition.get(0), "to", transition.get(1), "reason", transition.get(2))
            );
        }
    }
}
