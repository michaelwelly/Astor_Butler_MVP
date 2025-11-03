package museon_online.astor_butler.alisa.dto;

import java.util.List;

public record AlisaResponse(Result result) {
    public record Result(List<Alternative> alternatives) {}
    public record Alternative(Message message) {}
    public record Message(String text) {}
}