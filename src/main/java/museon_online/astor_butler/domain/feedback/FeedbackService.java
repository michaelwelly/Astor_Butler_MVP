package museon_online.astor_butler.domain.feedback;

import lombok.RequiredArgsConstructor;
import museon_online.astor_butler.api.common.ApiException;
import museon_online.astor_butler.api.common.ErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class FeedbackService {

    private final FeedbackRepository repository;

    public GuestFeedback get(Long id) {
        if (id == null) {
            throw badRequest("feedback id is required");
        }
        return repository.find(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ErrorCode.NOT_FOUND, "Feedback was not found", Map.of("id", id)));
    }

    public List<GuestFeedback> listByChatId(Long chatId, int limit) {
        if (chatId == null) {
            throw badRequest("chatId is required");
        }
        return repository.findByChatId(chatId, safeLimit(limit));
    }

    public List<GuestFeedback> listOpen(int limit) {
        return repository.findOpen(safeLimit(limit));
    }

    @Transactional
    public GuestFeedback create(GuestFeedbackCommand command) {
        validate(command);
        String text = command.text().toLowerCase(Locale.ROOT);
        FeedbackType type = classifyType(text);
        FeedbackSentiment sentiment = classifySentiment(text);
        FeedbackPriority priority = priority(type, text);
        return repository.create(command, type, sentiment, priority);
    }

    private FeedbackType classifyType(String text) {
        if (containsAny(text, "жалоб", "плохо", "ужас", "не понрав", "проблем")) {
            return FeedbackType.COMPLAINT;
        }
        if (containsAny(text, "идея", "предлож", "можно было", "хотелось бы")) {
            return FeedbackType.IDEA;
        }
        if (containsAny(text, "сервис", "официант", "персонал", "хостес")) {
            return FeedbackType.SERVICE_ISSUE;
        }
        if (containsAny(text, "бот", "сайт", "оплата", "ошибка", "не работает")) {
            return FeedbackType.TECH_ISSUE;
        }
        if (containsAny(text, "спасибо", "понрав", "класс", "отлич", "кайф")) {
            return FeedbackType.PRAISE;
        }
        return FeedbackType.GENERAL;
    }

    private FeedbackSentiment classifySentiment(String text) {
        boolean negative = containsAny(text, "плохо", "ужас", "не понрав", "жалоб", "проблем", "ошибка");
        boolean positive = containsAny(text, "спасибо", "понрав", "класс", "отлич", "кайф", "красив");
        if (negative && positive) {
            return FeedbackSentiment.MIXED;
        }
        if (negative) {
            return FeedbackSentiment.NEGATIVE;
        }
        if (positive) {
            return FeedbackSentiment.POSITIVE;
        }
        return FeedbackSentiment.NEUTRAL;
    }

    private FeedbackPriority priority(FeedbackType type, String text) {
        if (containsAny(text, "отрав", "опас", "скорая", "конфликт", "угроз")) {
            return FeedbackPriority.URGENT;
        }
        if (type == FeedbackType.COMPLAINT || type == FeedbackType.TECH_ISSUE) {
            return FeedbackPriority.HIGH;
        }
        return FeedbackPriority.NORMAL;
    }

    private void validate(GuestFeedbackCommand command) {
        if (command == null) {
            throw badRequest("Request body is required");
        }
        if (command.chatId() == null) {
            throw badRequest("chatId is required");
        }
        if (command.text() == null || command.text().isBlank()) {
            throw badRequest("feedback text is required");
        }
    }

    private int safeLimit(int limit) {
        return Math.max(1, Math.min(limit, 100));
    }

    private boolean containsAny(String text, String... variants) {
        for (String variant : variants) {
            if (text.contains(variant)) {
                return true;
            }
        }
        return false;
    }

    private ApiException badRequest(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, ErrorCode.BAD_REQUEST, message);
    }
}
