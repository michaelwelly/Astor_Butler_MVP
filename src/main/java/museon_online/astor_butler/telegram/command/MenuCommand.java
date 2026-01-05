//package museon_online.astor_butler.telegram.command;
//
//import lombok.RequiredArgsConstructor;
//import museon_online.astor_butler.telegram.button.MenuButton;
//import museon_online.astor_butler.telegram.utils.BotCommand;
//import museon_online.astor_butler.telegram.utils.BotResponse;
//import org.springframework.stereotype.Component;
//import org.telegram.telegrambots.meta.api.objects.Update;
//
//@Component
//@RequiredArgsConstructor
//public class MenuCommand implements BotCommand {
//
//    private final MenuButton menuButton;
//
//    @Override
//    public String getCommand() {
//        return "/menu";
//    }
//
//    @Override
//    public String getDescription() {
//        return "📋 Меню заведения";
//    }
//
//    @Override
//    public BotResponse execute(Update update) {
//        return new BotResponse(
//                "Выберите категорию меню:",
//                menuButton.createMenuButton()
//        );
//    }
//}