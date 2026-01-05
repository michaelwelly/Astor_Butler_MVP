//package museon_online.astor_butler.telegram.command;
//
//import lombok.RequiredArgsConstructor;
//import museon_online.astor_butler.fsm.BookingHandler;
//import museon_online.astor_butler.fsm.BookingState;
//import museon_online.astor_butler.fsm.UserSession;
//import museon_online.astor_butler.fsm.UserSessionManager;
//import museon_online.astor_butler.telegram.utils.BotCommand;
//import museon_online.astor_butler.telegram.utils.BotResponse;
//import org.springframework.stereotype.Component;
//import org.telegram.telegrambots.meta.api.objects.Update;
//
//@Component
//@RequiredArgsConstructor
//public class TableBookingCommand implements BotCommand {
//
//    private final UserSessionManager sessionManager;
//    private final BookingHandler bookingHandler;
//
//    @Override
//    public String getDescription() {
//        return "\uD83C\uDF7D Забронировать столик в заведении";
//    }
//
//    @Override
//    public String getCommand() {
//        return "/table";
//    }
//
//    @Override
//    public BotResponse execute(Update update) {
//        Long userId = update.getMessage().getFrom().getId();
//        UserSession session = sessionManager.getOrCreateSession(userId);
//        session.setState(BookingState.SELECTING_LOCATION);
//
//        String reply = bookingHandler.handle(update);
//        return new BotResponse(reply);
//    }
//}