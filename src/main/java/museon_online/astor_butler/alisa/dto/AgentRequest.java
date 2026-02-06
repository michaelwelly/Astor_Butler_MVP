package museon_online.astor_butler.alisa.dto;

import java.util.List;
import java.util.Map;

public record AgentRequest(
        Map<String, Object> input
) {
    public static AgentRequest fromUserText(String text) {
        return new AgentRequest(
                Map.of(
                        "messages", List.of(
                                Map.of(
                                        "role", "user",
                                        "text", text
                                )
                        )
                )
        );
    }
}