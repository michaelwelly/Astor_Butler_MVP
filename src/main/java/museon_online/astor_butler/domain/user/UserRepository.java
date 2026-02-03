package museon_online.astor_butler.domain.user;

import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface UserRepository extends CrudRepository<User, Long> {
    Optional<User> findByTelegramId(Long telegramId);
}