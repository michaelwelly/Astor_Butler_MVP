package museon_online.astor_butler.domain.telegram;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class TelegramGuestContextRepository {

    private final JdbcTemplate jdbcTemplate;

    public List<String> recentMessages(Long chatId, int limit) {
        if (chatId == null) {
            return List.of();
        }

        int safeLimit = Math.max(1, Math.min(limit, 10));
        List<String> newestFirst = jdbcTemplate.queryForList("""
                SELECT text
                FROM telegram_messages
                WHERE chat_id = ?
                  AND text IS NOT NULL
                  AND TRIM(text) <> ''
                ORDER BY received_at DESC
                LIMIT ?
                """,
                String.class,
                chatId,
                safeLimit
        );
        Collections.reverse(newestFirst);
        return newestFirst;
    }
}
