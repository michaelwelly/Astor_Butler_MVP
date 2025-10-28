package museon_online.astor_butler.fsm.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import museon_online.astor_butler.fsm.core.BotState;
import museon_online.astor_butler.fsm.core.CommandContext;
import museon_online.astor_butler.fsm.storage.FSMStorage;
import museon_online.astor_butler.telegram.TelegramSender;
import museon_online.astor_butler.user.*;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Contact;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ContactHandler implements FSMHandler {

    private final UserRepository  userRepo;
    private final TelegramSender  sender;
    private final FSMStorage storage;

    @Override
    public BotState getState() {
        return BotState.CONTACT;
    }

    @Override
    public void handle(CommandContext ctx) {
        Contact c = ctx.getContact();

        // 🧩 если контакт не передан — пользователь написал текст или стикер
        if (c == null) {
            log.warn("⚠️ Пользователь {} не поделился контактом, повторный запрос", ctx.getChatId());

            KeyboardButton shareContact = KeyboardButton.builder()
                    .text("📱 Поделиться контактом")
                    .requestContact(true)
                    .build();

            KeyboardRow row = new KeyboardRow(List.of(shareContact));
            ReplyKeyboardMarkup kb = ReplyKeyboardMarkup.builder()
                    .keyboard(List.of(row))
                    .resizeKeyboard(true)
                    .oneTimeKeyboard(true)
                    .build();

            sender.sendText(ctx.getChatId(),
                    "Пожалуйста, нажмите кнопку ниже, чтобы поделиться контактом. Это нужно один раз для начала работы 🙏",
                    kb);

            // 🚫 не сохраняем, не меняем FSM — остаёмся в состоянии CONTACT
            storage.setState(ctx.getChatId(), BotState.CONTACT);
            return;
        }

        Long tgId = c.getUserId();
        log.info("📞 Получен контакт от {} ({} {})", tgId, c.getFirstName(), c.getLastName());

        User user = userRepo.findByTelegramId(tgId)
                .orElse(User.builder()
                        .telegramId(tgId)
                        .role(UserRole.GUEST)
                        .build());

        user.setFirstName(c.getFirstName());
        user.setLastName(c.getLastName());
        user.setUsername(ctx.getMessage().getFrom().getUserName());
        user.setPhone(c.getPhoneNumber());
        userRepo.save(user);

        storage.setState(tgId, BotState.MENU); // 👈 или следующее состояние после регистрации

        sender.sendText(tgId, "Благодарю, данные внесены. Чем могу быть полезен прямо сейчас?");
    }
}