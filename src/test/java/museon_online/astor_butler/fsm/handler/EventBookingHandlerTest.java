package museon_online.astor_butler.fsm.handler;

import museon_online.astor_butler.domain.booking.EventBookingDraft;
import museon_online.astor_butler.domain.booking.EventBookingDraftStorage;
import museon_online.astor_butler.fsm.core.BotState;
import museon_online.astor_butler.fsm.core.CommandContext;
import museon_online.astor_butler.fsm.storage.FSMStorage;
import museon_online.astor_butler.telegram.utils.TelegramSender;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class EventBookingHandlerTest {

    private static final long CHAT_ID = 42L;

    private final TelegramSender sender = mock(TelegramSender.class);
    private final InMemoryFSMStorage fsmStorage = new InMemoryFSMStorage();
    private final InMemoryDraftStorage draftStorage = new InMemoryDraftStorage();
    private final EventBookingHandler handler = new EventBookingHandler(sender, fsmStorage, draftStorage);

    @Test
    void startsEventBookingFromCommand() {
        fsmStorage.setState(CHAT_ID, BotState.EVENT_BOOKING_TYPE);

        handler.handle(ctx("/event_booking"));

        assertThat(fsmStorage.getState(CHAT_ID)).isEqualTo(BotState.EVENT_BOOKING_TYPE);
        assertThat(draftStorage.getOrCreate(CHAT_ID).getEventType()).isNull();
        verify(sender).sendText(eq(CHAT_ID), contains("Давайте соберем заявку"));
    }

    @Test
    void collectsEventTypeAndMovesToDate() {
        fsmStorage.setState(CHAT_ID, BotState.EVENT_BOOKING_TYPE);

        handler.handle(ctx("банкет"));

        assertThat(fsmStorage.getState(CHAT_ID)).isEqualTo(BotState.EVENT_BOOKING_DATE);
        assertThat(draftStorage.getOrCreate(CHAT_ID).getEventType()).isEqualTo("банкет");
        verify(sender).sendText(eq(CHAT_ID), contains("дату"));
    }

    @Test
    void walksToSummaryAndWaitsForConfirmation() {
        fsmStorage.setState(CHAT_ID, BotState.EVENT_BOOKING_TYPE);

        handler.handle(ctx("корпоратив"));
        handler.handle(ctx("25 мая, 19:00"));
        handler.handle(ctx("40"));
        handler.handle(ctx("фуршет"));
        handler.handle(ctx("300000"));
        handler.handle(ctx("легкие закуски и игристое"));
        handler.handle(ctx("экран и звук"));
        handler.handle(ctx("Михаил, @michael_welly"));

        EventBookingDraft draft = draftStorage.getOrCreate(CHAT_ID);
        assertThat(fsmStorage.getState(CHAT_ID)).isEqualTo(BotState.EVENT_BOOKING_SUMMARY);
        assertThat(draft.getEventType()).isEqualTo("корпоратив");
        assertThat(draft.getEventDate()).isEqualTo("25 мая, 19:00");
        assertThat(draft.getGuestCount()).isEqualTo("40");
        assertThat(draft.getContactDetails()).isEqualTo("Михаил, @michael_welly");
        verify(sender).sendText(eq(CHAT_ID), contains("Проверьте"));
    }

    @Test
    void confirmsReadyForManager() {
        fsmStorage.setState(CHAT_ID, BotState.EVENT_BOOKING_SUMMARY);

        handler.handle(ctx("да"));

        assertThat(fsmStorage.getState(CHAT_ID)).isEqualTo(BotState.EVENT_BOOKING_READY_FOR_MANAGER);
        verify(sender).sendText(eq(CHAT_ID), contains("Заявка собрана"));
    }

    private CommandContext ctx(String text) {
        return new CommandContext(CHAT_ID, text, null, null, null, null);
    }

    private static class InMemoryFSMStorage implements FSMStorage {

        private final Map<Long, BotState> states = new HashMap<>();

        @Override
        public void setState(Long chatId, BotState state) {
            states.put(chatId, state);
        }

        @Override
        public void clear(Long chatId) {
            states.remove(chatId);
        }

        @Override
        public BotState getState(Long chatId) {
            return states.get(chatId);
        }
    }

    private static class InMemoryDraftStorage implements EventBookingDraftStorage {

        private final Map<Long, EventBookingDraft> drafts = new HashMap<>();

        @Override
        public EventBookingDraft getOrCreate(Long chatId) {
            return drafts.computeIfAbsent(chatId, ignored -> new EventBookingDraft());
        }

        @Override
        public void save(Long chatId, EventBookingDraft draft) {
            drafts.put(chatId, draft);
        }

        @Override
        public void clear(Long chatId) {
            drafts.remove(chatId);
        }
    }
}
