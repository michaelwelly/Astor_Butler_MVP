package museon_online.astor_butler.domain.fsm;

import lombok.RequiredArgsConstructor;
import museon_online.astor_butler.api.common.ApiException;
import museon_online.astor_butler.api.common.ErrorCode;
import museon_online.astor_butler.domain.consent.ConsentVaultService;
import museon_online.astor_butler.fsm.core.BotState;
import museon_online.astor_butler.fsm.scenario.TableBookingDraftStorage;
import museon_online.astor_butler.fsm.storage.FSMStorage;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class FsmRuntimeStateService {

    private final FSMStorage fsmStorage;
    private final TableBookingDraftStorage tableBookingDraftStorage;
    private final JdbcTemplate jdbcTemplate;

    public TelegramFsmStateView getTelegramState(Long chatId) {
        ensureChatId(chatId);
        ProfileFacts profile = findProfileFacts(chatId);
        MessageFacts messages = findMessageFacts(chatId);
        BotState state = fsmStorage.getState(chatId);
        List<String> pendingIntents = fsmStorage.getPendingIntents(chatId);
        boolean tableBookingDraftPresent = tableBookingDraftStorage.find(chatId).isPresent();

        return new TelegramFsmStateView(
                chatId,
                profile.telegramUserId(),
                profile.userId(),
                profile.username(),
                profile.firstName(),
                profile.lastName(),
                profile.displayName(),
                state == null ? BotState.UNKNOWN.name() : state.name(),
                pendingIntents,
                profile.consentGranted(),
                tableBookingDraftPresent,
                messages.messageCount(),
                profile.lastSeenAt(),
                messages.lastMessageAt(),
                Map.of(
                        "stateStorage", "redis",
                        "identityStorage", "postgresql",
                        "policyVersion", ConsentVaultService.CURRENT_POLICY_VERSION
                )
        );
    }

    public TelegramFsmStateView resetTelegramState(Long chatId) {
        ensureChatId(chatId);
        TelegramFsmStateView beforeReset = getTelegramState(chatId);
        fsmStorage.clear(chatId);
        tableBookingDraftStorage.clear(chatId);

        BotState target = resetTarget(beforeReset);
        if (target != BotState.UNKNOWN) {
            fsmStorage.setState(chatId, target);
        }
        return getTelegramState(chatId);
    }

    public TelegramFsmStateView replaceTelegramState(Long chatId, String state) {
        ensureChatId(chatId);
        BotState botState = parseState(state);
        fsmStorage.setState(chatId, botState);
        return getTelegramState(chatId);
    }

    public void deleteTelegramState(Long chatId) {
        ensureChatId(chatId);
        fsmStorage.clear(chatId);
        tableBookingDraftStorage.clear(chatId);
    }

    private BotState resetTarget(TelegramFsmStateView beforeReset) {
        if (beforeReset.consentGranted()) {
            return BotState.READY_FOR_DIALOG;
        }
        if (beforeReset.telegramUserId() != null) {
            return BotState.CONSENT_REQUIRED;
        }
        return BotState.UNKNOWN;
    }

    private ProfileFacts findProfileFacts(Long chatId) {
        List<ProfileFacts> rows = jdbcTemplate.query("""
                SELECT tp.telegram_user_id,
                       tp.user_id,
                       tp.username,
                       tp.first_name,
                       tp.last_name,
                       u.display_name,
                       tp.last_seen_at,
                       EXISTS (
                           SELECT 1
                           FROM user_consents uc
                           WHERE (uc.user_id = tp.user_id OR uc.telegram_user_id = tp.telegram_user_id)
                             AND uc.consent_type = ?
                             AND uc.policy_version = ?
                             AND uc.status = 'GRANTED'
                             AND uc.revoked_at IS NULL
                       ) AS consent_granted
                FROM telegram_profiles tp
                LEFT JOIN users u ON u.id = tp.user_id
                WHERE tp.chat_id = ?
                ORDER BY tp.last_seen_at DESC
                LIMIT 1
                """,
                (resultSet, rowNum) -> mapProfile(resultSet),
                ConsentVaultService.PRIVACY_POLICY,
                ConsentVaultService.CURRENT_POLICY_VERSION,
                chatId
        );
        return rows.isEmpty() ? ProfileFacts.empty() : rows.getFirst();
    }

    private MessageFacts findMessageFacts(Long chatId) {
        return jdbcTemplate.query("""
                SELECT COUNT(*) AS message_count,
                       MAX(received_at) AS last_message_at
                FROM telegram_messages
                WHERE chat_id = ?
                """,
                resultSet -> {
                    if (!resultSet.next()) {
                        return MessageFacts.empty();
                    }
                    return new MessageFacts(
                            resultSet.getLong("message_count"),
                            instant(resultSet, "last_message_at")
                    );
                },
                chatId
        );
    }

    private ProfileFacts mapProfile(ResultSet resultSet) throws SQLException {
        return new ProfileFacts(
                resultSet.getObject("telegram_user_id", Long.class),
                resultSet.getObject("user_id", Long.class),
                resultSet.getString("username"),
                resultSet.getString("first_name"),
                resultSet.getString("last_name"),
                resultSet.getString("display_name"),
                resultSet.getBoolean("consent_granted"),
                instant(resultSet, "last_seen_at")
        );
    }

    private Instant instant(ResultSet resultSet, String column) throws SQLException {
        OffsetDateTime value = resultSet.getObject(column, OffsetDateTime.class);
        return value == null ? null : value.toInstant();
    }

    private BotState parseState(String state) {
        if (state == null || state.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCode.BAD_REQUEST, "FSM state is required");
        }
        try {
            return BotState.valueOf(state.trim()).canonical();
        } catch (IllegalArgumentException exception) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    ErrorCode.BAD_REQUEST,
                    "Unknown FSM state",
                    Map.of("state", state)
            );
        }
    }

    private void ensureChatId(Long chatId) {
        if (chatId == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCode.BAD_REQUEST, "Telegram chatId is required");
        }
    }

    private record ProfileFacts(
            Long telegramUserId,
            Long userId,
            String username,
            String firstName,
            String lastName,
            String displayName,
            boolean consentGranted,
            Instant lastSeenAt
    ) {
        private static ProfileFacts empty() {
            return new ProfileFacts(null, null, null, null, null, null, false, null);
        }
    }

    private record MessageFacts(long messageCount, Instant lastMessageAt) {
        private static MessageFacts empty() {
            return new MessageFacts(0L, null);
        }
    }

    public record TelegramFsmStateView(
            Long chatId,
            Long telegramUserId,
            Long userId,
            String username,
            String firstName,
            String lastName,
            String displayName,
            String state,
            List<String> pendingIntents,
            boolean consentGranted,
            boolean tableBookingDraftPresent,
            long messageCount,
            Instant lastSeenAt,
            Instant lastMessageAt,
            Map<String, Object> storage
    ) {
    }
}
