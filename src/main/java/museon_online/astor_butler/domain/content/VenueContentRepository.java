package museon_online.astor_butler.domain.content;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class VenueContentRepository {

    private final JdbcTemplate jdbcTemplate;

    public VenueContentPost upsert(ClassifiedVenueContentPost classified) {
        UUID id = jdbcTemplate.queryForObject("""
                INSERT INTO venue_content_posts (
                    venue_code, source_type, source_channel, source_message_id,
                    source_url, source_hash, content_type, status, title, body,
                    event_starts_at, event_ends_at, active_from, active_until,
                    classification_confidence, raw_payload, published_at,
                    created_at, updated_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                ON CONFLICT (venue_code, source_type, source_channel, source_message_id)
                DO UPDATE SET
                    source_url = EXCLUDED.source_url,
                    source_hash = EXCLUDED.source_hash,
                    content_type = EXCLUDED.content_type,
                    status = EXCLUDED.status,
                    title = EXCLUDED.title,
                    body = EXCLUDED.body,
                    event_starts_at = EXCLUDED.event_starts_at,
                    event_ends_at = EXCLUDED.event_ends_at,
                    active_from = EXCLUDED.active_from,
                    active_until = EXCLUDED.active_until,
                    classification_confidence = EXCLUDED.classification_confidence,
                    raw_payload = EXCLUDED.raw_payload,
                    published_at = EXCLUDED.published_at,
                    updated_at = CURRENT_TIMESTAMP
                RETURNING id
                """,
                UUID.class,
                classified.source().venueCode(),
                classified.source().sourceType(),
                classified.source().sourceChannel(),
                classified.source().sourceMessageId(),
                classified.source().sourceUrl(),
                classified.source().sourceHash(),
                classified.contentType().name(),
                classified.status().name(),
                classified.title(),
                classified.body(),
                timestamp(classified.eventStartsAt()),
                timestamp(classified.eventEndsAt()),
                timestamp(classified.activeFrom()),
                timestamp(classified.activeUntil()),
                classified.classificationConfidence(),
                classified.source().rawPayloadJson() == null || classified.source().rawPayloadJson().isBlank()
                        ? "{}"
                        : classified.source().rawPayloadJson(),
                timestamp(classified.source().publishedAt())
        );
        replaceAssets(id, classified.source().assets());
        return findById(id).orElseThrow();
    }

    public List<VenueContentPost> findActiveForQuietGuide(String venueCode, boolean includePromos, Instant now, int limit) {
        return jdbcTemplate.query("""
                SELECT *
                FROM venue_content_posts
                WHERE venue_code = ?
                  AND status = 'ACTIVE'
                  AND active_from <= ?
                  AND active_until >= ?
                  AND content_type IN (
                      'AFISHA_EVENT',
                      'MENU_UPDATE',
                      'VENUE_NEWS',
                      'ATMOSPHERE_CONTENT',
                      CASE WHEN ? THEN 'PROMO_OFFER' ELSE 'AFISHA_EVENT' END
                  )
                ORDER BY
                  CASE content_type
                    WHEN 'AFISHA_EVENT' THEN 1
                    WHEN 'PROMO_OFFER' THEN 2
                    WHEN 'MENU_UPDATE' THEN 3
                    ELSE 4
                  END,
                  COALESCE(event_starts_at, published_at, created_at) DESC
                LIMIT ?
                """,
                postMapper(),
                normalizeVenue(venueCode),
                timestamp(now),
                timestamp(now),
                includePromos,
                limit
        );
    }

    public List<VenueContentAsset> findAssetsByPostId(UUID postId) {
        if (postId == null) {
            return List.of();
        }
        return jdbcTemplate.query("""
                SELECT asset_kind,
                       source_url,
                       bucket,
                       object_key,
                       content_type
                FROM venue_content_assets
                WHERE post_id = ?
                ORDER BY created_at, asset_kind
                """,
                (rs, rowNum) -> new VenueContentAsset(
                        rs.getString("asset_kind"),
                        rs.getString("source_url"),
                        rs.getString("bucket"),
                        rs.getString("object_key"),
                        rs.getString("content_type")
                ),
                postId
        );
    }

    private java.util.Optional<VenueContentPost> findById(UUID id) {
        return jdbcTemplate.query("""
                SELECT *
                FROM venue_content_posts
                WHERE id = ?
                """,
                postMapper(),
                id
        ).stream().findFirst();
    }

    private void replaceAssets(UUID postId, List<VenueContentAsset> assets) {
        jdbcTemplate.update("DELETE FROM venue_content_assets WHERE post_id = ?", postId);
        if (assets == null || assets.isEmpty()) {
            return;
        }
        for (VenueContentAsset asset : assets) {
            jdbcTemplate.update("""
                    INSERT INTO venue_content_assets (
                        post_id, asset_kind, source_url, bucket, object_key, content_type, created_at
                    )
                    VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
                    """,
                    postId,
                    asset.assetKind(),
                    asset.sourceUrl(),
                    asset.bucket(),
                    asset.objectKey(),
                    asset.contentType()
            );
        }
    }

    private RowMapper<VenueContentPost> postMapper() {
        return (rs, rowNum) -> new VenueContentPost(
                rs.getObject("id", UUID.class),
                rs.getString("venue_code"),
                rs.getString("source_type"),
                rs.getString("source_channel"),
                rs.getString("source_message_id"),
                rs.getString("source_url"),
                VenueContentType.valueOf(rs.getString("content_type")),
                VenueContentStatus.valueOf(rs.getString("status")),
                rs.getString("title"),
                rs.getString("body"),
                instant(rs, "event_starts_at"),
                instant(rs, "active_until"),
                rs.getDouble("classification_confidence"),
                instant(rs, "published_at"),
                instant(rs, "created_at"),
                instant(rs, "updated_at")
        );
    }

    private Timestamp timestamp(Instant instant) {
        return instant == null ? null : Timestamp.from(instant);
    }

    private Instant instant(ResultSet rs, String column) throws java.sql.SQLException {
        Timestamp timestamp = rs.getTimestamp(column);
        return timestamp == null ? null : timestamp.toInstant();
    }

    private String normalizeVenue(String value) {
        return value == null || value.isBlank() ? "AERIS" : value.trim().toUpperCase();
    }
}
