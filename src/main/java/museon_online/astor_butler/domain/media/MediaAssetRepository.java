package museon_online.astor_butler.domain.media;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
@Slf4j
public class MediaAssetRepository {

    private final JdbcTemplate jdbcTemplate;

    public Optional<MediaAsset> findActiveByCode(String assetCode) {
        if (assetCode == null || assetCode.isBlank()) {
            return Optional.empty();
        }
        try {
            return jdbcTemplate.query("""
                            SELECT asset_code,
                                   venue_code,
                                   domain,
                                   kind,
                                   title,
                                   bucket,
                                   object_key,
                                   filename,
                                   content_type,
                                   active
                            FROM media_assets
                            WHERE asset_code = ?
                              AND active = TRUE
                            """,
                    (rs, rowNum) -> new MediaAsset(
                            rs.getString("asset_code"),
                            rs.getString("venue_code"),
                            rs.getString("domain"),
                            rs.getString("kind"),
                            rs.getString("title"),
                            rs.getString("bucket"),
                            rs.getString("object_key"),
                            rs.getString("filename"),
                            rs.getString("content_type"),
                            rs.getBoolean("active")
                    ),
                    assetCode.strip()
            ).stream().findFirst();
        } catch (DataAccessException e) {
            log.warn("Media asset lookup failed for {}: {}", assetCode, e.getMessage());
            return Optional.empty();
        }
    }
}
