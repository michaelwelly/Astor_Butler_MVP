CREATE TABLE IF NOT EXISTS media_assets (
    asset_code VARCHAR(120) PRIMARY KEY,
    venue_code VARCHAR(64) NOT NULL DEFAULT 'AERIS',
    domain VARCHAR(64) NOT NULL,
    kind VARCHAR(64) NOT NULL,
    title VARCHAR(200) NOT NULL,
    bucket VARCHAR(120) NOT NULL,
    object_key TEXT NOT NULL,
    filename VARCHAR(240) NOT NULL,
    content_type VARCHAR(120) NOT NULL DEFAULT 'application/octet-stream',
    active BOOLEAN NOT NULL DEFAULT TRUE,
    version_label VARCHAR(80),
    checksum_sha256 VARCHAR(128),
    metadata_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_media_assets_bucket_object
    ON media_assets(bucket, object_key);

CREATE INDEX IF NOT EXISTS idx_media_assets_lookup
    ON media_assets(venue_code, domain, kind, active);

INSERT INTO media_assets (
    asset_code,
    venue_code,
    domain,
    kind,
    title,
    bucket,
    object_key,
    filename,
    content_type,
    active,
    version_label,
    metadata_json
) VALUES
    (
        'AERIS_MENU_KITCHEN',
        'AERIS',
        'QUIET_GUIDE',
        'PDF_MENU',
        'Кухня / основное меню',
        'astor-media',
        'content/aeris/menu/kitchen/MENU_AERIS_A4_2026_DIGITAL.pdf',
        'MENU AERIS A4 2026 DIGITAL.pdf',
        'application/pdf',
        TRUE,
        '2026-06',
        '{"keywords":["меню","еда","кухня","основное меню"]}'::jsonb
    ),
    (
        'AERIS_MENU_BAR',
        'AERIS',
        'QUIET_GUIDE',
        'PDF_MENU',
        'Барная карта',
        'astor-media',
        'content/aeris/menu/bar/BAR_CARD.pdf',
        'BAR CARD.pdf',
        'application/pdf',
        TRUE,
        '2026-06',
        '{"keywords":["бар","напитки","барная карта"]}'::jsonb
    ),
    (
        'AERIS_MENU_ELEMENTS',
        'AERIS',
        'QUIET_GUIDE',
        'PDF_MENU',
        'Коктейли / Elements',
        'astor-media',
        'content/aeris/menu/elements/ELEMENTS_CARD.pdf',
        'ELEMENTS CARD.pdf',
        'application/pdf',
        TRUE,
        '2026-06',
        '{"keywords":["коктейли","elements","авторские коктейли"]}'::jsonb
    ),
    (
        'AERIS_MENU_WINE',
        'AERIS',
        'QUIET_GUIDE',
        'PDF_MENU',
        'Винная карта',
        'astor-media',
        'content/aeris/menu/wine/WINE_MENU_2026_FINAL.pdf',
        'WINE MENU 2026 FINAL.pdf',
        'application/pdf',
        TRUE,
        '2026-06',
        '{"keywords":["вино","винная карта","шампанское"]}'::jsonb
    ),
    (
        'AERIS_FLOOR_PLAN',
        'AERIS',
        'TABLE_BOOKING',
        'FLOOR_PLAN',
        'План зала AERIS',
        'astor-media',
        'content/aeris/floor-plan/AERIS_PLAN.pdf',
        'AERIS PLAN.pdf',
        'application/pdf',
        TRUE,
        '2026-06',
        '{"usage":["table_booking"]}'::jsonb
    ),
    (
        'AERIS_INTERIOR_TOUR',
        'AERIS',
        'QUIET_GUIDE',
        'VIDEO_TOUR',
        'AERIS interior tour',
        'astor-media',
        'content/aeris/interior/INTERIOR.mp4',
        'INTERIOR.mp4',
        'video/mp4',
        TRUE,
        '2026-06',
        '{"sendMode":"DOCUMENT"}'::jsonb
    )
ON CONFLICT (asset_code) DO UPDATE SET
    venue_code = EXCLUDED.venue_code,
    domain = EXCLUDED.domain,
    kind = EXCLUDED.kind,
    title = EXCLUDED.title,
    bucket = EXCLUDED.bucket,
    object_key = EXCLUDED.object_key,
    filename = EXCLUDED.filename,
    content_type = EXCLUDED.content_type,
    active = EXCLUDED.active,
    version_label = EXCLUDED.version_label,
    metadata_json = EXCLUDED.metadata_json,
    updated_at = CURRENT_TIMESTAMP;
