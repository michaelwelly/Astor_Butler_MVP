package museon_online.astor_butler.telegram.command;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import museon_online.astor_butler.telegram.button.*;
import museon_online.astor_butler.telegram.utils.BotButton;
import museon_online.astor_butler.telegram.utils.BotCommand;
import museon_online.astor_butler.telegram.utils.BotResponse;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Component
public class MainMenuCommand implements BotCommand {

    private final MenuButton menuButton;
    private final TableReservationButton tableReservationButton;
    private final SlotButton slotButton;
    private final BalanceButton balanceButton;
    private final MerchButton merchButton;
    private final FeedbackButton feedbackButton;
    private final RazjebButton razjebButton;
    private final ContactButton contactButton;
    private final CancelButton cancelButton;
    private final TipButton tipButton;
    private final CharityButton charityButton;
    private final PosterButton posterButton;

    @Override
    public String getCommand() {
        return "/main_menu";
    }

    @Override
    public String getDescription() {
        return "Показать главное меню";
    }

    @Override
    public BotResponse execute(Update update) {
        try {
            InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
            markup.setKeyboard(List.of(
                    List.of(buildButton(menuButton), buildButton(balanceButton)),
                    List.of(buildButton(slotButton), buildButton(tableReservationButton)),
                    List.of(buildButton(merchButton), buildButton(razjebButton)),
                    List.of(buildButton(posterButton), buildButton(feedbackButton)),
                    List.of(buildButton(tipButton), buildButton(charityButton)),
                    List.of(buildButton(contactButton), buildButton(cancelButton))
            ));

            return new BotResponse("Главное меню 👇", markup);

        } catch (Exception e) {
            log.error("Ошибка при построении главного меню", e);
            return new BotResponse("Произошла ошибка при открытии главного меню. Пожалуйста, попробуйте позже.");
        }
    }

    private InlineKeyboardButton buildButton(Object button) {
        return ((BotButton) button).buildButton().getKeyboard().get(0).get(0);
    }
}