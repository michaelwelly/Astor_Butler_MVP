package museon_online.astor_butler.telegram.adapter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import museon_online.astor_butler.fsm.core.BotState;
import museon_online.astor_butler.fsm.core.CommandContext;
import museon_online.astor_butler.fsm.core.FSMRouter;
import museon_online.astor_butler.fsm.core.event.InboundEvent;
import museon_online.astor_butler.fsm.core.idempotency.IdempotencyGuard;
import museon_online.astor_butler.fsm.storage.FSMStorage;
import museon_online.astor_butler.telegram.exeption.TelegramExceptionHandler;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.bots.AbsSender;

@Service
@RequiredArgsConstructor
@Slf4j
public class TelegramRouter {

    private final ObjectProvider<FSMRouter> fsmRouterProvider;
    private final TelegramExceptionHandler exceptionHandler;
    private final FSMStorage fsmStorage;
    private final IdempotencyGuard idempotencyGuard;


    public void handle(Update update, AbsSender sender) {
        try {

            // Architecture entrypoint: Update → InboundEvent → FSM
            processInboundEvent(update);

            // ⬇️ Старый код — без изменений
            CommandContext ctx = CommandContext.from(update);
            FSMRouter fsmRouter = fsmRouterProvider.getObject();
            Long chatId = ctx.getChatId();
            String text = ctx.getMessageText();

            // 👀 1. Логируем входящее сообщение
            log.info("📨 [TG] Incoming message from {}: {}", chatId, text);

            // 🚀 2. Обработка команды /start
            if ("/start".equalsIgnoreCase(text)) {
                log.info("🚀 [CMD] /start received → FSM set to GREETING (chatId={})", chatId);
                fsmStorage.setState(chatId, BotState.GREETING);
            }

            // 🧭 3. Проверяем текущее состояние
            BotState currentState = fsmStorage.getState(chatId);
            if (currentState == null) {
                currentState = BotState.UNKNOWN;
                fsmStorage.setState(chatId, BotState.UNKNOWN);
                log.warn("⚠️ [FSM] No state found in Redis → set to UNKNOWN (chatId={})", chatId);
            }

            log.info("📊 [FSM] Current state for chatId={} → {}", chatId, currentState);

            // 🔄 4. Передаём в FSMRouter (он выберет нужный handler)
            fsmRouter.route(ctx);

        } catch (Exception e) {
            log.error("💥 [TG] Exception while handling update: {}", e.getMessage(), e);
            exceptionHandler.handle(update, e, sender);
        }
    }
    private void processInboundEvent(Update update) {
        try {
            InboundEvent inboundEvent = InboundEvent.from(update);

            if (inboundEvent == null) {
                log.debug("📭 [PIPELINE] Update ignored (cannot be mapped to InboundEvent)");
                return;
            }

            boolean accepted = idempotencyGuard.accept(inboundEvent);
            if (!accepted) {
                log.info(
                        "🔁 [PIPELINE] Duplicate event ignored (eventId={}, chatId={})",
                        inboundEvent.getEventId(),
                        inboundEvent.getChatId()
                );
                return;
            }

            log.info(
                    "➡️ [PIPELINE] InboundEvent accepted → forwarding to FSM (eventId={}, type={}, chatId={})",
                    inboundEvent.getEventId(),
                    inboundEvent.getType(),
                    inboundEvent.getChatId()
            );

            // ⚠️ FSM пока может не уметь принимать InboundEvent —
            // этот вызов будет подключён на следующем шаге
            // FSMRouter fsmRouter = fsmRouterProvider.getObject();
            // fsmRouter.handle(inboundEvent);

        } catch (Exception e) {
            log.error("💥 [PIPELINE] Error while processing inbound event", e);
        }
    }
}