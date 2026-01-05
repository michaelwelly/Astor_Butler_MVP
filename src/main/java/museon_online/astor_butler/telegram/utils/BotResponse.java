package museon_online.astor_butler.telegram.utils;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;

public class BotResponse {

    private final String text;
    private final InlineKeyboardMarkup markup;
    private final ReplyKeyboardMarkup replyMarkup;

    // Можно добавить: imageUrl, parseMode, attachments, chatId и т.д.

    public BotResponse(String text) {
        this.text = text;
        this.markup = null;
        this.replyMarkup = null;
    }

    public BotResponse(String text, InlineKeyboardMarkup markup) {
        this.text = text;
        this.markup = markup;
        this.replyMarkup = null;
    }

    public BotResponse(String text, InlineKeyboardMarkup markup, ReplyKeyboardMarkup replyMarkup) {
        this.text = text;
        this.markup = markup;
        this.replyMarkup = replyMarkup;
    }

    public static BotResponse withReplyKeyboard(String text, ReplyKeyboardMarkup replyMarkup) {
        return new BotResponse(text, null, replyMarkup);
    }

    public String getText() {
        return text;
    }

    public InlineKeyboardMarkup getMarkup() {
        return markup;
    }

    public boolean hasMarkup() {
        return markup != null;
    }

    public ReplyKeyboardMarkup getKeyboard() {
        return replyMarkup;
    }

    public boolean hasReplyKeyboard() {
        return replyMarkup != null;
    }
}
