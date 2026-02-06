package museon_online.astor_butler.alisa.dto;

public record AgentResponse(
        String text,
        String intent,
        Double confidence
) {
}