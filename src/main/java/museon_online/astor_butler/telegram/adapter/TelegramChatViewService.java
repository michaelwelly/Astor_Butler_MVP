package museon_online.astor_butler.telegram.adapter;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TelegramChatViewService {

    private final JdbcTemplate jdbcTemplate;

    public Integer findPreviewMessageId(Long telegramUserId, String previewVersion) {
        if (telegramUserId == null) {
            return null;
        }
        return jdbcTemplate.query("""
                SELECT preview_message_id
                FROM telegram_profiles
                WHERE telegram_user_id = ?
                  AND preview_version = ?
                """,
                resultSet -> resultSet.next() ? resultSet.getObject("preview_message_id", Integer.class) : null,
                telegramUserId,
                previewVersion
        );
    }

    public void savePreviewMessageId(Long telegramUserId, Integer messageId, String previewVersion) {
        if (telegramUserId == null || messageId == null) {
            return;
        }
        jdbcTemplate.update("""
                UPDATE telegram_profiles
                SET preview_message_id = ?,
                    preview_version = ?,
                    preview_updated_at = CURRENT_TIMESTAMP,
                    updated_at = CURRENT_TIMESTAMP
                WHERE telegram_user_id = ?
                """,
                messageId,
                previewVersion,
                telegramUserId
        );
    }

    public Integer findLastBotMessageId(Long telegramUserId) {
        if (telegramUserId == null) {
            return null;
        }
        return jdbcTemplate.query("""
                SELECT last_bot_message_id
                FROM telegram_profiles
                WHERE telegram_user_id = ?
                """,
                resultSet -> resultSet.next() ? resultSet.getObject("last_bot_message_id", Integer.class) : null,
                telegramUserId
        );
    }

    public void saveLastBotMessageId(Long telegramUserId, Integer messageId) {
        if (telegramUserId == null || messageId == null) {
            return;
        }
        jdbcTemplate.update("""
                UPDATE telegram_profiles
                SET last_bot_message_id = ?,
                    updated_at = CURRENT_TIMESTAMP
                WHERE telegram_user_id = ?
                """,
                messageId,
                telegramUserId
        );
    }

    public Integer findLastUserMessageId(Long telegramUserId) {
        if (telegramUserId == null) {
            return null;
        }
        return jdbcTemplate.query("""
                SELECT last_user_message_id
                FROM telegram_profiles
                WHERE telegram_user_id = ?
                """,
                resultSet -> resultSet.next() ? resultSet.getObject("last_user_message_id", Integer.class) : null,
                telegramUserId
        );
    }

    public void saveLastUserMessageId(Long telegramUserId, Integer messageId) {
        if (telegramUserId == null || messageId == null) {
            return;
        }
        jdbcTemplate.update("""
                UPDATE telegram_profiles
                SET last_user_message_id = ?,
                    updated_at = CURRENT_TIMESTAMP
                WHERE telegram_user_id = ?
                """,
                messageId,
                telegramUserId
        );
    }
}
