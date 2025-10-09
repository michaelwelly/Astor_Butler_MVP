package museon_online.astor_butler.fsm.core;

import lombok.RequiredArgsConstructor;
import museon_online.astor_butler.fsm.handler.FSMHandler;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class FSMRouter {

    private final List<FSMHandler> handlers;

    public void route(CommandContext ctx) {
        handlers.stream()
                .filter(h -> h.canHandle(ctx))
                .findFirst()
                .ifPresent(h -> h.handle(ctx));
    }
}