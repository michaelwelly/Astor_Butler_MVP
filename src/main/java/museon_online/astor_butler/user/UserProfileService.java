package museon_online.astor_butler.user;

import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Contact;

import java.time.LocalDateTime;

@Service
public class UserProfileService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public UserProfileService(UserRepository userRepository, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
    }

    /**
     * Создает или обновляет профиль пользователя по данным из Telegram.
     */
    public User createOrUpdateFromTelegram(org.telegram.telegrambots.meta.api.objects.User telegramUser) {
        Long telegramId = telegramUser.getId();

        return userRepository.findByTelegramId(telegramId)
                .map(existing -> {
                    existing.setFirstName(telegramUser.getFirstName());
                    existing.setLastName(telegramUser.getLastName());
                    existing.setUsername(telegramUser.getUserName());
                    existing.setUpdatedAt(LocalDateTime.now());
                    return userRepository.save(existing);
                })
                .orElseGet(() -> userRepository.save(userMapper.fromTelegramUser(telegramUser)));
    }

    /**
     * Обновляет контактные данные пользователя (номер телефона).
     */
    public void updateContact(Long telegramId, Contact contact) {
        userRepository.findByTelegramId(telegramId).ifPresent(user -> {
            user.setPhone(contact.getPhoneNumber());
            user.setUpdatedAt(LocalDateTime.now());
            userRepository.save(user);
        });
    }

    /**
     * Возвращает профиль пользователя по Telegram ID.
     */
    public User findOrCreate(Long telegramId) {
        return userRepository.findByTelegramId(telegramId)
                .orElseGet(() -> {
                    User u = new User();
                    u.setTelegramId(telegramId);
                    u.setCreatedAt(LocalDateTime.now());
                    return userRepository.save(u);
                });
    }
}