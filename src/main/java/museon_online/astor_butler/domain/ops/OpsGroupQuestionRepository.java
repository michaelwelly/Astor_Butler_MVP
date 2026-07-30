package museon_online.astor_butler.domain.ops;

import lombok.RequiredArgsConstructor;
import museon_online.astor_butler.service.message.IncomingMessage;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class OpsGroupQuestionRepository {

    private final JdbcTemplate jdbcTemplate;

    public OpsGroupQuestion savePending(IncomingMessage incoming, String questionText) {
        Long id = jdbcTemplate.queryForObject("""
                INSERT INTO ops_group_questions (
                    chat_id, telegram_message_id, asker_telegram_user_id, asker_username,
                    asker_display_name, question_text, status, metadata_json, created_at, updated_at
                )
                VALUES (?, ?, ?, ?, ?, ?, 'PENDING_HUMAN', '{}'::jsonb, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                ON CONFLICT (chat_id, telegram_message_id)
                DO UPDATE SET
                    question_text = EXCLUDED.question_text,
                    status = CASE
                        WHEN ops_group_questions.status = 'PENDING_HUMAN' THEN 'PENDING_HUMAN'
                        ELSE ops_group_questions.status
                    END,
                    updated_at = CURRENT_TIMESTAMP
                RETURNING id
                """,
                Long.class,
                incoming.chatId(),
                incoming.telegramMessageId(),
                incoming.telegramUserId(),
                blankToNull(incoming.username()),
                blankToNull(displayName(incoming)),
                questionText
        );
        return findById(id).orElseThrow();
    }

    public Optional<OpsGroupQuestion> findPendingByMessage(Long chatId, Integer messageId) {
        if (chatId == null || messageId == null) {
            return Optional.empty();
        }
        return jdbcTemplate.query("""
                SELECT *
                FROM ops_group_questions
                WHERE chat_id = ?
                  AND telegram_message_id = ?
                  AND status = 'PENDING_HUMAN'
                LIMIT 1
                """, mapper(), chatId, messageId).stream().findFirst();
    }

    public OpsGroupQuestion markAnsweredByBot(OpsGroupQuestion question, String answerText, String answerSource) {
        return markAnswered(question, OpsGroupQuestionStatus.ANSWERED_BY_BOT, answerText, answerSource, "bot");
    }

    public OpsGroupQuestion markAnsweredByHuman(OpsGroupQuestion question, String answerText, String answeredBy) {
        return markAnswered(question, OpsGroupQuestionStatus.ANSWERED_BY_HUMAN, answerText, "human_reply", answeredBy);
    }

    private OpsGroupQuestion markAnswered(OpsGroupQuestion question,
                                          OpsGroupQuestionStatus status,
                                          String answerText,
                                          String answerSource,
                                          String answeredBy) {
        if (question == null || question.id() == null) {
            throw new IllegalArgumentException("question id is required");
        }
        jdbcTemplate.update("""
                UPDATE ops_group_questions
                SET status = ?,
                    answer_text = ?,
                    answer_source = ?,
                    answered_by = ?,
                    answered_at = CURRENT_TIMESTAMP,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """,
                status.name(),
                answerText,
                answerSource,
                answeredBy,
                question.id()
        );
        return findById(question.id()).orElseThrow();
    }

    private Optional<OpsGroupQuestion> findById(Long id) {
        return jdbcTemplate.query("""
                SELECT *
                FROM ops_group_questions
                WHERE id = ?
                """, mapper(), id).stream().findFirst();
    }

    private RowMapper<OpsGroupQuestion> mapper() {
        return (rs, rowNum) -> new OpsGroupQuestion(
                rs.getLong("id"),
                rs.getLong("chat_id"),
                rs.getInt("telegram_message_id"),
                longOrNull(rs, "asker_telegram_user_id"),
                rs.getString("asker_username"),
                rs.getString("asker_display_name"),
                rs.getString("question_text"),
                OpsGroupQuestionStatus.valueOf(rs.getString("status")),
                rs.getString("answer_text"),
                rs.getString("answer_source"),
                rs.getString("answered_by"),
                instant(rs, "answered_at"),
                rs.getString("metadata_json"),
                instant(rs, "created_at"),
                instant(rs, "updated_at")
        );
    }

    private Long longOrNull(ResultSet rs, String column) throws java.sql.SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    private Instant instant(ResultSet rs, String column) throws java.sql.SQLException {
        Timestamp timestamp = rs.getTimestamp(column);
        return timestamp == null ? null : timestamp.toInstant();
    }

    private String displayName(IncomingMessage incoming) {
        String firstName = incoming.firstName() == null ? "" : incoming.firstName().trim();
        String lastName = incoming.lastName() == null ? "" : incoming.lastName().trim();
        String fullName = (firstName + " " + lastName).trim();
        if (!fullName.isBlank()) {
            return fullName;
        }
        if (incoming.username() != null && !incoming.username().isBlank()) {
            return "@" + incoming.username().trim();
        }
        return null;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
