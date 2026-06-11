package museon_online.astor_butler.service.message;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import museon_online.astor_butler.domain.timeline.FsmTimelineEvent;
import museon_online.astor_butler.domain.timeline.FsmTimelineWriter;
import museon_online.astor_butler.domain.telegram.TelegramIntakeService;
import museon_online.astor_butler.fsm.core.BotState;
import museon_online.astor_butler.fsm.scenario.FirstTouchScenario;
import museon_online.astor_butler.fsm.scenario.MainMenuScenario;
import museon_online.astor_butler.fsm.scenario.MenuAssetsScenario;
import museon_online.astor_butler.fsm.scenario.QuietGuideScenario;
import museon_online.astor_butler.fsm.scenario.TableBookingScenario;
import museon_online.astor_butler.fsm.storage.FSMStorage;
import museon_online.astor_butler.kafka.UserEventProducer;
import museon_online.astor_butler.llm.OllamaClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class MessageGatewayService {

    private final FSMStorage fsmStorage;
    private final OllamaClient ollamaClient;
    private final TelegramIntakeService telegramIntakeService;
    private final FirstTouchScenario firstTouchScenario;
    private final MainMenuScenario mainMenuScenario;
    private final MenuAssetsScenario menuAssetsScenario;
    private final QuietGuideScenario quietGuideScenario;
    private final TableBookingScenario tableBookingScenario;
    private final UserEventProducer userEventProducer;
    private final LlmScenarioPromptCatalog llmScenarioPromptCatalog;
    private final VoiceTranscriptionRetryService voiceTranscriptionRetryService;
    private final FsmTimelineWriter fsmTimelineWriter;

    @Value("${telegram.admin.chat-id:}")
    private String adminChatId;

    @Value("${telegram.analytics.chat-id:}")
    private String analyticsChatId;

    @Value("${astor.message.log-conversations-enabled:true}")
    private boolean logConversationsEnabled;

    public OutgoingMessage handle(IncomingMessage incoming) {
        if (incoming == null || incoming.chatId() == null) {
            throw new IllegalArgumentException("Incoming message must contain chatId for current MVP flow");
        }

        String text = normalize(incoming.text());
        BotState currentState = resolveState(incoming.chatId());
        telegramIntakeService.capture(incoming);

        if (logConversationsEnabled) {
            log.info(
                    "Message gateway received channel={}, chatId={}, state={}, text={}",
                    incoming.channel(),
                    incoming.chatId(),
                    currentState,
                    text
            );
        } else {
            log.debug(
                    "Message gateway received channel={}, chatId={}, state={}",
                    incoming.channel(),
                    incoming.chatId(),
                    currentState
            );
        }

        if (isAdminChat(incoming.chatId())) {
            return finish(incoming, currentState, OutgoingMessage.of(
                    incoming,
                    "Admin chat online. Я вижу этот чат как служебный канал Astor Butler: сообщения сохраняю, Kafka-события публикую, в гостевой FSM-сценарий этот чат не отправляю.",
                    currentState.name(),
                    false,
                    false,
                    false,
                    false,
                    AdminAlert.none(),
                    List.of("ADMIN_CHAT_CHECK", "SKIP_GUEST_FSM")
            ));
        }

        if (!isVoiceMessage(incoming) || !text.isBlank()) {
            voiceTranscriptionRetryService.reset(incoming.chatId());
        }

        if (firstTouchScenario.supports(incoming, currentState, text)) {
            return finish(incoming, currentState, firstTouchScenario.handle(incoming, currentState, text));
        }

        if (tableBookingScenario.supports(incoming, currentState, text)) {
            return finish(incoming, currentState, tableBookingScenario.handle(incoming, currentState, text));
        }

        if (menuAssetsScenario.supports(incoming, currentState, text)) {
            return finish(incoming, currentState, menuAssetsScenario.handle(incoming, currentState, text));
        }

        if (quietGuideScenario.supports(incoming, currentState, text)) {
            return finish(incoming, currentState, quietGuideScenario.handle(incoming, currentState, text));
        }

        if (mainMenuScenario.supports(incoming, currentState, text)) {
            return finish(incoming, currentState, mainMenuScenario.handle(incoming, currentState, text));
        }

        if (isVoiceMessage(incoming) && text.isBlank()) {
            long attempts = voiceTranscriptionRetryService.recordFailure(incoming.chatId());
            if (attempts >= 2) {
                return finish(incoming, currentState, OutgoingMessage.of(
                        incoming,
                        voiceTextFallbackText(incoming),
                        currentState.name(),
                        false,
                        false,
                        true,
                        false,
                        AdminAlert.none(),
                        List.of("VOICE_RECEIVED", "TRANSCRIPTION_FAILED_TWICE", "ASK_TEXT_INPUT")
                ));
            }
            return finish(incoming, currentState, OutgoingMessage.of(
                    incoming,
                    voiceRetryText(incoming),
                    currentState.name(),
                    false,
                    false,
                    true,
                    false,
                    AdminAlert.none(),
                    List.of("VOICE_RECEIVED", "TRANSCRIPTION_RETRY_REQUESTED")
            ));
        }

        if (text.isBlank()) {
            return fallback(incoming, currentState, "Empty message");
        }

        return aiAssistedReply(incoming, currentState, text);
    }

    private OutgoingMessage aiAssistedReply(IncomingMessage incoming, BotState currentState, String text) {
        String prompt = """
                Ты AI-адаптер Astor Butler. Telegram является только UI, бизнес-логика живет в FSM.
                Ответь пользователю коротко и вежливо, строго по FSM-контракту ниже.
                Не подтверждай бронь сам: подтверждение делает только доменный слой после кнопки хостес.
                Если запрос неясен, честно попроси одно уточнение или скажи, что уточнишь у команды.

                %s

                Текущее FSM-состояние: %s
                Сообщение пользователя: "%s"
                """.formatted(llmScenarioPromptCatalog.tableBookingContract(), currentState, text);

        try {
            String aiText = ollamaClient.ask(prompt);
            if (aiText == null || aiText.isBlank()) {
                return fallback(incoming, currentState, "LLM returned blank response");
            }
            userEventProducer.publishLlmResponse(
                    incoming,
                    currentState,
                    "AI_RESPONSE",
                    prompt,
                    aiText,
                    false
            );

            fsmStorage.setState(incoming.chatId(), BotState.AI_FALLBACK);
            return finish(incoming, currentState, OutgoingMessage.of(
                    incoming,
                    aiText,
                    BotState.AI_FALLBACK.name(),
                    false,
                    false,
                    true,
                    false,
                    AdminAlert.none(),
                    List.of("AI_RESPONSE")
            ));
        } catch (Exception e) {
            log.warn(
                    "LLM fallback used for chatId={}, state={}, reason={}: {}",
                    incoming.chatId(),
                    currentState,
                    e.getClass().getSimpleName(),
                    e.getMessage()
            );
            return fallback(incoming, currentState, e.getClass().getSimpleName());
        }
    }

    private OutgoingMessage fallback(IncomingMessage incoming, BotState currentState, String reason) {
        fsmStorage.setState(incoming.chatId(), BotState.AI_FALLBACK);
        String userText = "Я не смог уверенно разобрать запрос. Я передам это администратору, а вы можете написать проще: бронирование, меню, афиша или менеджер.";

        return finish(incoming, currentState, OutgoingMessage.of(
                incoming,
                userText,
                BotState.AI_FALLBACK.name(),
                false,
                false,
                true,
                true,
                adminAlert(incoming, currentState, reason),
                List.of("FALLBACK", "ADMIN_ALERT")
        ));
    }

    private OutgoingMessage finish(IncomingMessage incoming, BotState previousState, OutgoingMessage outgoing) {
        userEventProducer.publishIncomingMessage(incoming, previousState, outgoing);
        fsmTimelineWriter.append(FsmTimelineEvent.from(incoming, previousState, outgoing));
        return outgoing;
    }

    private AdminAlert adminAlert(IncomingMessage incoming, BotState currentState, String reason) {
        if (adminChatId == null || adminChatId.isBlank()) {
            return AdminAlert.none();
        }

        String text = """
                <b>Astor Butler / fallback</b>
                Требуется внимание администратора

                <b>%s</b>
                chat %s / user %s%s

                <b>Сообщение гостя</b>
                <blockquote>%s</blockquote>

                <b>Что случилось</b>
                State: %s -> %s
                Reason: %s

                <b>Действие</b>
                Проверь диалог и ответь гостю вручную, если AI/FSM не восстановится.

                <b>Техника</b>
                Channel: %s
                Correlation: %s
                """.formatted(
                html(displayName(incoming)),
                html(text(incoming.chatId())),
                html(text(incoming.telegramUserId())),
                incoming.username() == null || incoming.username().isBlank() ? "" : " / @" + html(incoming.username()),
                html(blankAsEmptyLabel(incoming.text())),
                html(text(currentState)),
                html(BotState.AI_FALLBACK.name()),
                html(blankAsEmptyLabel(reason)),
                html(text(incoming.channel())),
                html(blankAsEmptyLabel(incoming.correlationId()))
        );
        return new AdminAlert(true, adminChatId, text);
    }

    private String displayName(IncomingMessage incoming) {
        String firstName = normalize(incoming.firstName());
        String lastName = normalize(incoming.lastName());
        String username = normalize(incoming.username());
        String fullName = (firstName + " " + lastName).trim();
        if (!fullName.isBlank()) {
            return fullName;
        }
        if (!username.isBlank()) {
            return "@" + username;
        }
        return "unknown";
    }

    private BotState resolveState(Long chatId) {
        BotState current = fsmStorage.getState(chatId);
        if (current != null) {
            return current;
        }
        fsmStorage.setState(chatId, BotState.UNKNOWN);
        return BotState.UNKNOWN;
    }

    private boolean isVoiceMessage(IncomingMessage incoming) {
        if (incoming == null || incoming.payload() == null) {
            return false;
        }
        Object mediaKind = incoming.payload().get("mediaKind");
        return "VOICE".equals(mediaKind) || "AUDIO".equals(mediaKind);
    }

    private String voiceRetryText(IncomingMessage incoming) {
        Object reason = incoming.payload() == null ? null : incoming.payload().get("transcriptionReason");
        if (reason == null || reason.toString().isBlank() || "STT disabled".equals(reason)) {
            return "Голосовое принял, но сейчас не смог уверенно его разобрать. Запишите, пожалуйста, еще раз чуть ближе к микрофону и одной короткой фразой.";
        }
        return "Голосовое получил, но расшифровка не сложилась. Попробуйте, пожалуйста, еще раз: чуть медленнее и без фонового шума.";
    }

    private String voiceTextFallbackText(IncomingMessage incoming) {
        return "Я дважды не смог надежно разобрать голосовое. Чтобы не потерять ваш запрос, напишите, пожалуйста, коротко текстом — я сразу продолжу сценарий.";
    }

    private boolean isAdminChat(Long chatId) {
        if (chatId == null) {
            return false;
        }
        String value = chatId.toString();
        return value.equals(adminChatId) || value.equals(analyticsChatId);
    }

    private String normalize(String text) {
        return text == null ? "" : text.trim();
    }

    private String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String blankAsEmptyLabel(String value) {
        String normalized = normalize(value);
        return normalized.isBlank() ? "(empty)" : normalized;
    }

    private String html(String value) {
        return text(value)
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}
