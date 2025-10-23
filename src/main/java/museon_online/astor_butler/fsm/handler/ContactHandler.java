package museon_online.astor_butler.fsm.handler;

import lombok.RequiredArgsConstructor;
import museon_online.astor_butler.fsm.core.BotState;
import museon_online.astor_butler.fsm.core.CommandContext;
import museon_online.astor_butler.fsm.storage.FSMStorage;
import museon_online.astor_butler.telegram.TelegramSender;
import museon_online.astor_butler.user.*;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Contact;

@Component
@RequiredArgsConstructor
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
        Long tgId = c.getUserId();

        User user = userRepo.findByTelegramId(tgId)
                .orElse(User.builder().telegramId(tgId).build());

        user.setFirstName(c.getFirstName());
        user.setLastName(c.getLastName());
        user.setUsername(ctx.getMessage().getFrom().getUserName());
        user.setPhone(c.getPhoneNumber());
        userRepo.save(user);

        // фиксируем новое состояние (Redis / In-Mem)
        storage.setState(tgId, BotState.CONTACT);

        sender.sendText(tgId,
                "Благодарю, данные внесены. Чем могу быть полезен прямо сейчас?");
    }
}