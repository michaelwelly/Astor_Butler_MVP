package museon_online.astor_butler.domain.merch;

import lombok.RequiredArgsConstructor;
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
public class MerchRepository {

    private final JdbcTemplate jdbcTemplate;

    public List<MerchItem> findActiveItems(String venueCode) {
        return jdbcTemplate.query("""
                SELECT *
                FROM merch_items
                WHERE venue_code = ?
                  AND status = 'ACTIVE'
                ORDER BY title
                """,
                itemMapper(),
                normalizeVenue(venueCode)
        );
    }

    public Optional<MerchItem> findItem(Long id) {
        List<MerchItem> result = jdbcTemplate.query("""
                SELECT *
                FROM merch_items
                WHERE id = ?
                """,
                itemMapper(),
                id
        );
        return result.stream().findFirst();
    }

    public Optional<MerchItem> findDefaultItem(String venueCode) {
        List<MerchItem> result = jdbcTemplate.query("""
                SELECT *
                FROM merch_items
                WHERE venue_code = ?
                  AND status = 'ACTIVE'
                ORDER BY CASE WHEN metadata_json ->> 'default' = 'true' THEN 0 ELSE 1 END, title
                LIMIT 1
                """,
                itemMapper(),
                normalizeVenue(venueCode)
        );
        return result.stream().findFirst();
    }

    public MerchOrder createOrder(MerchOrderCommand command, MerchItem item) {
        Long id = jdbcTemplate.queryForObject("""
                INSERT INTO merch_orders (
                    chat_id, telegram_user_id, user_id, venue_code, item_id, item_title,
                    status, source, quantity, price_minor, currency, guest_name, guest_comment,
                    payment_method_hint, created_at, updated_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, 'TELEGRAM', ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                RETURNING id
                """,
                Long.class,
                command.chatId(),
                command.telegramUserId(),
                command.userId(),
                normalizeVenue(command.venueCode()),
                item == null ? command.itemId() : item.id(),
                item == null ? blankToNull(command.itemTitle()) : item.title(),
                MerchOrderStatus.AWAITING_GUEST_CONFIRMATION.name(),
                command.quantity() == null ? 1 : command.quantity(),
                item == null ? null : item.priceMinor(),
                item == null ? "RUB" : item.currency(),
                blankToNull(command.guestName()),
                blankToNull(command.guestComment()),
                blankToNull(command.paymentMethodHint())
        );
        return findOrder(id).orElseThrow();
    }

    public Optional<MerchOrder> findOrder(Long id) {
        List<MerchOrder> result = jdbcTemplate.query("""
                SELECT *
                FROM merch_orders
                WHERE id = ?
                """,
                orderMapper(),
                id
        );
        return result.stream().findFirst();
    }

    public Optional<MerchOrder> findLatestAwaitingGuestConfirmation(Long chatId) {
        List<MerchOrder> result = jdbcTemplate.query("""
                SELECT *
                FROM merch_orders
                WHERE chat_id = ?
                  AND status = ?
                ORDER BY created_at DESC
                LIMIT 1
                """,
                orderMapper(),
                chatId,
                MerchOrderStatus.AWAITING_GUEST_CONFIRMATION.name()
        );
        return result.stream().findFirst();
    }

    public List<MerchOrder> findOrdersByChatId(Long chatId, int limit) {
        return jdbcTemplate.query("""
                SELECT *
                FROM merch_orders
                WHERE chat_id = ?
                ORDER BY created_at DESC
                LIMIT ?
                """,
                orderMapper(),
                chatId,
                limit
        );
    }

    public MerchOrder updateStatus(Long id, MerchOrderStatus status) {
        jdbcTemplate.update("""
                UPDATE merch_orders
                SET status = ?,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """,
                status.name(),
                id
        );
        return findOrder(id).orElseThrow();
    }

    private RowMapper<MerchItem> itemMapper() {
        return (rs, rowNum) -> new MerchItem(
                rs.getLong("id"),
                rs.getString("venue_code"),
                rs.getString("item_code"),
                rs.getString("title"),
                rs.getString("description"),
                MerchItemStatus.valueOf(rs.getString("status")),
                nullableLong(rs, "price_minor"),
                rs.getString("currency"),
                rs.getString("stock_hint"),
                rs.getString("media_asset_code"),
                rs.getString("metadata_json"),
                instant(rs, "created_at"),
                instant(rs, "updated_at")
        );
    }

    private RowMapper<MerchOrder> orderMapper() {
        return (rs, rowNum) -> new MerchOrder(
                rs.getLong("id"),
                rs.getLong("chat_id"),
                nullableLong(rs, "telegram_user_id"),
                nullableLong(rs, "user_id"),
                rs.getString("venue_code"),
                nullableLong(rs, "item_id"),
                rs.getString("item_title"),
                MerchOrderStatus.valueOf(rs.getString("status")),
                rs.getString("source"),
                rs.getInt("quantity"),
                nullableLong(rs, "price_minor"),
                rs.getString("currency"),
                rs.getString("guest_name"),
                rs.getString("guest_comment"),
                rs.getString("payment_method_hint"),
                rs.getString("payment_external_id"),
                rs.getString("metadata_json"),
                instant(rs, "created_at"),
                instant(rs, "updated_at")
        );
    }

    private String normalizeVenue(String venueCode) {
        return venueCode == null || venueCode.isBlank() ? "AERIS" : venueCode.trim().toUpperCase();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private Instant instant(ResultSet rs, String column) throws java.sql.SQLException {
        Timestamp timestamp = rs.getTimestamp(column);
        return timestamp == null ? null : timestamp.toInstant();
    }

    private Long nullableLong(ResultSet rs, String column) throws java.sql.SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }
}
