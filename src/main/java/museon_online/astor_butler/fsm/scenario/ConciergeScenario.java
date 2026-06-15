package museon_online.astor_butler.fsm.scenario;

import lombok.RequiredArgsConstructor;
import museon_online.astor_butler.domain.concierge.ConciergeRequest;
import museon_online.astor_butler.domain.concierge.ConciergeRequestCommand;
import museon_online.astor_butler.domain.concierge.ConciergeRequestService;
import museon_online.astor_butler.domain.concierge.ConciergeRequestType;
import museon_online.astor_butler.fsm.core.BotState;
import museon_online.astor_butler.fsm.storage.FSMStorage;
import museon_online.astor_butler.service.message.AdminAlert;
import museon_online.astor_butler.service.message.IncomingMessage;
import museon_online.astor_butler.service.message.OutgoingMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ConciergeScenario implements FsmScenario {

    private final FSMStorage fsmStorage;
    private final ConciergeRequestService conciergeRequestService;

    @Value("${telegram.admin.chat-id:}")
    private String adminChatId;

    @Override
    public String id() {
        return "CONCIERGE";
    }

    @Override
    public int priority() {
        return 37;
    }

    @Override
    public boolean supports(IncomingMessage incoming, BotState currentState, String text) {
        BotState state = currentState == null ? BotState.UNKNOWN : currentState.canonical();
        String normalized = normalize(text);
        if (normalized.isBlank()) {
            return false;
        }
        return owns(state) || isConciergeIntent(normalized);
    }

    @Override
    public OutgoingMessage handle(IncomingMessage incoming, BotState currentState, String text) {
        BotState state = currentState == null ? BotState.UNKNOWN : currentState.canonical();
        String normalized = normalize(text);
        if (state == BotState.CONCIERGE_COLLECT_REQUEST) {
            return sendConciergeRequest(incoming, currentState, text, "CONCIERGE_DETAILS_RECEIVED");
        }
        if (isShortConciergeCall(normalized)) {
            fsmStorage.setState(incoming.chatId(), BotState.CONCIERGE_COLLECT_REQUEST);
            return OutgoingMessage.of(
                    incoming,
                    "Что передать команде AERIS? Можно попросить подготовить деталь визита, принести что-то к столу или отметить важный повод.",
                    BotState.CONCIERGE_COLLECT_REQUEST.name(),
                    false,
                    false,
                    true,
                    false,
                    AdminAlert.none(),
                    List.of("CONCIERGE", "ASK_CONCIERGE_REQUEST")
            ).withMetadata(Map.of("scenario", id()));
        }
        return sendConciergeRequest(incoming, currentState, text, "CONCIERGE_DIRECT_REQUEST");
    }

    @Override
    public boolean owns(BotState state) {
        BotState canonical = state == null ? BotState.UNKNOWN : state.canonical();
        return canonical == BotState.CONCIERGE_COLLECT_REQUEST || canonical == BotState.CONCIERGE_SENT;
    }

    @Override
    public boolean sideEffecting() {
        return true;
    }

    private OutgoingMessage sendConciergeRequest(
            IncomingMessage incoming,
            BotState previousState,
            String text,
            String reasonAction
    ) {
        String requestText = stripConciergePrefix(text);
        if (requestText.isBlank()) {
            fsmStorage.setState(incoming.chatId(), BotState.CONCIERGE_COLLECT_REQUEST);
            return OutgoingMessage.of(
                    incoming,
                    "Напишите, что именно передать команде. Например: подготовьте свечу к десерту или позовите официанта к столу.",
                    BotState.CONCIERGE_COLLECT_REQUEST.name(),
                    false,
                    false,
                    true,
                    false,
                    AdminAlert.none(),
                    List.of("CONCIERGE", "ASK_CONCIERGE_REQUEST")
            ).withMetadata(Map.of("scenario", id()));
        }
        ConciergeRequestType type = conciergeRequestService.classify(requestText);
        ConciergeRequest request = conciergeRequestService.createRequest(new ConciergeRequestCommand(
                incoming.chatId(),
                incoming.telegramUserId(),
                null,
                "AERIS",
                type,
                displayName(incoming),
                requestText,
                adminChatId,
                previousState == null ? null : previousState.name(),
                incoming.correlationId(),
                "{}"
        ));
        fsmStorage.setState(incoming.chatId(), BotState.READY_FOR_DIALOG);
        return OutgoingMessage.of(
                incoming,
                "Принял сервисную заявку #%s и передал команде AERIS. Я остаюсь на связи в главном меню."
                        .formatted(request.id()),
                BotState.READY_FOR_DIALOG.name(),
                false,
                false,
                true,
                false,
                adminAlert(incoming, previousState, request),
                List.of("CONCIERGE", reasonAction, "ADMIN_ALERT", "RETURN_MAIN_MENU")
        ).withMetadata(Map.of(
                "scenario", id(),
                "conciergeRequestId", request.id(),
                "requestType", request.requestType().name(),
                "handoffBoundary", "TEAM_CONFIRMATION_REQUIRED"
        ));
    }

    private AdminAlert adminAlert(IncomingMessage incoming, BotState previousState, ConciergeRequest request) {
        if (adminChatId == null || adminChatId.isBlank()) {
            return AdminAlert.none();
        }
        String body = """
                <b>Astor Butler / concierge</b>
                Гость просит сервисную помощь

                <b>Гость</b>
                %s
                chat %s / user %s%s

                <b>Заявка #%s</b>
                Type: %s
                <blockquote>%s</blockquote>

                <b>Контекст</b>
                Previous state: %s
                Scenario: CONCIERGE

                <b>Действие</b>
                Проверьте возможность исполнения и ответьте гостю вручную или через будущий staff-flow.

                <b>Техника</b>
                Channel: %s
                Correlation: %s
                """.formatted(
                html(displayName(incoming)),
                html(text(incoming.chatId())),
                html(text(incoming.telegramUserId())),
                incoming.username() == null || incoming.username().isBlank() ? "" : " / @" + html(incoming.username()),
                html(text(request.id())),
                html(text(request.requestType())),
                html(blankAsEmptyLabel(request.requestText())),
                html(text(previousState)),
                html(text(incoming.channel())),
                html(blankAsEmptyLabel(incoming.correlationId()))
        );
        return new AdminAlert(true, adminChatId, body);
    }

    private boolean isConciergeIntent(String text) {
        return containsAny(text,
                "подготовьте", "подготовь", "принесите", "принеси",
                "организуйте", "организуй", "попроси команду", "передай команде",
                "позовите официанта", "позови официанта", "нужен официант",
                "свечу", "свеча", "плед", "сюрприз", "букет"
        );
    }

    private boolean isShortConciergeCall(String text) {
        return text.equals("консьерж")
                || text.equals("concierge")
                || text.equals("/concierge")
                || text.equals("сервис")
                || text.equals("помощь команды");
    }

    private String stripConciergePrefix(String text) {
        String normalized = text == null ? "" : text.trim();
        String lower = normalized.toLowerCase(Locale.ROOT);
        for (String prefix : List.of("передай команде,", "передай команде", "попроси команду,", "попроси команду")) {
            if (lower.startsWith(prefix)) {
                return normalized.substring(prefix.length()).trim();
            }
        }
        return normalized;
    }

    private boolean containsAny(String text, String... variants) {
        for (String variant : variants) {
            if (text.contains(variant)) {
                return true;
            }
        }
        return false;
    }

    private String displayName(IncomingMessage incoming) {
        String firstName = normalizeDisplay(incoming.firstName());
        String lastName = normalizeDisplay(incoming.lastName());
        String username = normalizeDisplay(incoming.username());
        String fullName = (firstName + " " + lastName).trim();
        if (!fullName.isBlank()) {
            return fullName;
        }
        if (!username.isBlank()) {
            return "@" + username;
        }
        return "unknown";
    }

    private String normalize(String text) {
        return text == null ? "" : text.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeDisplay(String text) {
        return text == null ? "" : text.trim();
    }

    private String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String blankAsEmptyLabel(String value) {
        String normalized = normalizeDisplay(value);
        return normalized.isBlank() ? "(empty)" : normalized;
    }

    private String html(String value) {
        return text(value)
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}
