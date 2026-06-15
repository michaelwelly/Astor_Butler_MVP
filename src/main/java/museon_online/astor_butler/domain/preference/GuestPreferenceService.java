package museon_online.astor_butler.domain.preference;

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
public class GuestPreferenceService {

    private final GuestPreferenceRepository repository;

    public GuestPreference getPreference(Long id) {
        if (id == null) {
            throw badRequest("preference id is required");
        }
        return repository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, ErrorCode.NOT_FOUND, "Guest preference was not found", Map.of("id", id)));
    }

    public List<GuestPreference> listActiveByChatId(Long chatId, int limit) {
        if (chatId == null) {
            throw badRequest("chatId is required");
        }
        return repository.findActiveByChatId(chatId, Math.max(1, Math.min(limit, 100)));
    }

    @Transactional
    public GuestPreference createPreference(GuestPreferenceCommand command) {
        validate(command);
        return repository.create(command);
    }

    public GuestPreferenceCategory classify(String text) {
        String normalized = text == null ? "" : text.toLowerCase(Locale.ROOT);
        if (containsAny(normalized, "аллерг", "неперенос", "без глютена", "лактоз", "орех")) {
            return GuestPreferenceCategory.ALLERGY;
        }
        if (containsAny(normalized, "вино", "коктейл", "бар", "шампан", "игрист", "красное", "белое")) {
            return GuestPreferenceCategory.DRINK;
        }
        if (containsAny(normalized, "остро", "мяс", "рыб", "веган", "вегетари", "десерт", "кухн", "блюд")) {
            return GuestPreferenceCategory.FOOD;
        }
        if (containsAny(normalized, "стол", "тихий", "окн", "диван", "зал", "посад", "мест")) {
            return GuestPreferenceCategory.SEATING;
        }
        if (containsAny(normalized, "пиши", "звони", "не звони", "сообщени", "голос", "текст")) {
            return GuestPreferenceCategory.COMMUNICATION;
        }
        if (containsAny(normalized, "быстро", "спокойно", "не беспоко", "сервис", "обслуж")) {
            return GuestPreferenceCategory.SERVICE;
        }
        return GuestPreferenceCategory.GENERAL;
    }

    private void validate(GuestPreferenceCommand command) {
        if (command == null) {
            throw badRequest("Request body is required");
        }
        if (command.chatId() == null) {
            throw badRequest("chatId is required");
        }
        if (command.category() == null) {
            throw badRequest("category is required");
        }
        if (command.preferenceText() == null || command.preferenceText().isBlank()) {
            throw badRequest("preferenceText is required");
        }
        if (command.preferenceText().length() > 2000) {
            throw badRequest("preferenceText is too long");
        }
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
