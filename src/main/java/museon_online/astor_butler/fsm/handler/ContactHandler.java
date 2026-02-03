package museon_online.astor_butler.fsm.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import museon_online.astor_butler.fsm.core.BotState;
import museon_online.astor_butler.fsm.core.CommandContext;
import museon_online.astor_butler.fsm.storage.FSMStorage;
import museon_online.astor_butler.telegram.utils.TelegramSender;
import museon_online.astor_butler.domain.user.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Contact;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ContactHandler implements FSMHandler {

    private final UserRepository userRepo;
    private final TelegramSender sender;
    private final FSMStorage storage;
    private final ObjectProvider<MenuHandler> menuHandlerProvider; // 👈 безопасная лениво-инжектированная зависимость

    @Override
    public BotState getState() {
        return BotState.CONTACT;
    }

    @Override
    public void handle(CommandContext ctx) {
        Long chatId = ctx.getChatId();
        Contact contact = ctx.getContact();

        log.info("🟢 [FSM] CONTACT → start (chatId={})", chatId);

        // 🧩 1. Проверяем, действительно ли это контакт
        if (contact == null || contact.getPhoneNumber() == null) {
            log.warn("⚠️ [FSM] CONTACT → no valid contact received (chatId={})", chatId);

            KeyboardButton shareContact = KeyboardButton.builder()
                    .text("📱 Поделиться контактом")
                    .requestContact(true)
                    .build();

            ReplyKeyboardMarkup kb = ReplyKeyboardMarkup.builder()
                    .keyboard(List.of(new KeyboardRow(List.of(shareContact))))
                    .resizeKeyboard(true)
                    .oneTimeKeyboard(true)
                    .build();

            sender.sendText(chatId,
                    "Пожалуйста, нажмите кнопку ниже, чтобы поделиться контактом 🙏",
                    kb);
            storage.setState(chatId, BotState.CONTACT);
            return;
        }

        // ✅ 2. Сохраняем данные пользователя
        log.info("📞 [FSM] CONTACT → received contact: {} {} ({})",
                contact.getFirstName(), contact.getLastName(), contact.getPhoneNumber());

        User user = userRepo.findByTelegramId(contact.getUserId())
                .orElse(User.builder()
                        .telegramId(contact.getUserId())
                        .role(UserRole.GUEST)
                        .build());

        user.setFirstName(contact.getFirstName());
        user.setLastName(contact.getLastName());
        user.setUsername(ctx.getMessage().getFrom().getUserName());
        user.setPhone(contact.getPhoneNumber());
        userRepo.save(user);

        // 🚀 3. Переводим в состояние MENU
        storage.setState(chatId, BotState.MENU);
        log.info("✅ [FSM] CONTACT → next state: MENU (chatId={})", chatId);

        // 🎯 4. Автоматически вызываем MenuHandler для отображения кнопок
        try {
            log.info("🧭 [FSM] Triggering MenuHandler for chatId={}", chatId);
            menuHandlerProvider.getObject().handle(ctx);
        } catch (Exception e) {
            log.error("💥 [FSM] Failed to call MenuHandler after contact: {}", e.getMessage(), e);
            sender.sendText(chatId, "Контакт сохранён, но не удалось открыть меню. Попробуйте команду /menu.");
        }
    }
}