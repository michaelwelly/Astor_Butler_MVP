package museon_online.astor_butler.fsm.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import museon_online.astor_butler.alisa.AlisaClient;
import museon_online.astor_butler.fsm.core.BotState;
import museon_online.astor_butler.fsm.core.CommandContext;
import museon_online.astor_butler.fsm.storage.FSMStorage;
import museon_online.astor_butler.telegram.TelegramSender;
import museon_online.astor_butler.telegram.command.MainMenuCommand;
import museon_online.astor_butler.telegram.utils.BotResponse;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MenuHandler implements FSMHandler {

    private final TelegramSender sender;
    private final FSMStorage storage;
    private final AlisaClient alisaClient;
    private final MainMenuCommand mainMenuCommand;

    @Override
    public BotState getState() {
        return BotState.MENU;
    }

    @Override
    public void handle(CommandContext ctx) {
        Long chatId = ctx.getChatId();
        log.info("🟢 [FSM] MENU → start (chatId={})", chatId);

        try {
            // 🧭 1. Получаем главное меню из MainMenuCommand
            BotResponse response = mainMenuCommand.execute(null);
            sender.sendText(chatId, response.getText(), response.getKeyboard());
            storage.setState(chatId, BotState.MENU);
            log.info("✅ [FSM] MENU rendered via MainMenuCommand (chatId={})", chatId);

            // 🎙️ 2. Вызов Алисы для генерации динамического приветствия
            String prompt = "Создай короткое приветствие пользователю в главном меню вежливо и в стиле AI-дворецкого Astor Butler.";
            log.info("🎙️ [AI] Sending prompt to Alisa: {}", prompt);
            String aiResponse = alisaClient.ask(prompt);
            sender.sendText(chatId, aiResponse);
            log.info("💬 [AI] Response from Alisa: {}", aiResponse);

        } catch (Exception e) {
            log.error("💥 [FSM] MENU rendering failed: {}", e.getMessage(), e);
            sender.sendText(chatId, "Произошла ошибка при открытии меню. Попробуйте позже 🙏");
        }

        log.info("✅ [FSM] MENU → stay (chatId={})", chatId);
    }
}