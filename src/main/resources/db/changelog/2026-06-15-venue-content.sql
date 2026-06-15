CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE IF NOT EXISTS venue_content_posts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    venue_code VARCHAR(64) NOT NULL,
    source_type VARCHAR(64) NOT NULL,
    source_channel VARCHAR(120) NOT NULL,
    source_message_id VARCHAR(120) NOT NULL,
    source_url TEXT,
    source_hash VARCHAR(128) NOT NULL,
    content_type VARCHAR(64) NOT NULL,
    status VARCHAR(64) NOT NULL,
    title VARCHAR(240) NOT NULL,
    body TEXT NOT NULL DEFAULT '',
    event_starts_at TIMESTAMPTZ,
    event_ends_at TIMESTAMPTZ,
    active_from TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    active_until TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP + INTERVAL '30 days',
    classification_confidence NUMERIC(5, 4) NOT NULL DEFAULT 0,
    raw_payload JSONB NOT NULL DEFAULT '{}'::jsonb,
    published_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_venue_content_source UNIQUE (venue_code, source_type, source_channel, source_message_id)
);

CREATE INDEX IF NOT EXISTS idx_venue_content_quiet_guide
    ON venue_content_posts(venue_code, status, content_type, active_until);

CREATE INDEX IF NOT EXISTS idx_venue_content_published
    ON venue_content_posts(venue_code, published_at DESC);

CREATE TABLE IF NOT EXISTS venue_content_assets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    post_id UUID NOT NULL REFERENCES venue_content_posts(id) ON DELETE CASCADE,
    asset_kind VARCHAR(64) NOT NULL,
    source_url TEXT,
    bucket VARCHAR(120),
    object_key TEXT,
    content_type VARCHAR(120) NOT NULL DEFAULT 'application/octet-stream',
    width INT,
    height INT,
    duration_seconds INT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_venue_content_assets_post
    ON venue_content_assets(post_id);
