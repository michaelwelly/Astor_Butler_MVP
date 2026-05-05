package museon_online.astor_butler.domain.booking;

import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface EventBookingRepository extends CrudRepository<EventBooking, Long> {

    List<EventBooking> findByChatIdOrderByCreatedAtDesc(Long chatId);
}
