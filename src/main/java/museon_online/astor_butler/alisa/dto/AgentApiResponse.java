package museon_online.astor_butler.alisa.dto;

public record AgentApiResponse(
        Result result
) {
    public record Result(
            AgentResponse message
    ) {}
}