CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE IF NOT EXISTS semantic_sources (
    source_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    source_code VARCHAR(160) NOT NULL UNIQUE,
    source_type VARCHAR(80) NOT NULL,
    title VARCHAR(240) NOT NULL,
    venue_code VARCHAR(64) NOT NULL DEFAULT 'AERIS',
    uri TEXT,
    media_asset_code VARCHAR(120) REFERENCES media_assets(asset_code),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    metadata_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS semantic_chunks (
    chunk_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    source_id UUID NOT NULL REFERENCES semantic_sources(source_id) ON DELETE CASCADE,
    chunk_key VARCHAR(240) NOT NULL,
    chunk_index INTEGER NOT NULL,
    language_code VARCHAR(16) NOT NULL DEFAULT 'ru',
    title VARCHAR(240),
    content TEXT NOT NULL,
    token_count INTEGER,
    metadata_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_semantic_chunks_source_key UNIQUE (source_id, chunk_key)
);

CREATE TABLE IF NOT EXISTS semantic_embeddings (
    chunk_id UUID PRIMARY KEY REFERENCES semantic_chunks(chunk_id) ON DELETE CASCADE,
    embedding_model VARCHAR(120) NOT NULL,
    embedding_dimension INTEGER NOT NULL DEFAULT 1536,
    embedding vector(1536) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_semantic_sources_lookup
    ON semantic_sources(venue_code, source_type, active);

CREATE INDEX IF NOT EXISTS idx_semantic_chunks_source
    ON semantic_chunks(source_id, chunk_index);

CREATE INDEX IF NOT EXISTS idx_semantic_chunks_metadata_gin
    ON semantic_chunks USING gin(metadata_json);

CREATE INDEX IF NOT EXISTS idx_semantic_embeddings_vector
    ON semantic_embeddings USING ivfflat (embedding vector_cosine_ops)
    WITH (lists = 100);

INSERT INTO semantic_sources (
    source_code,
    source_type,
    title,
    venue_code,
    uri,
    media_asset_code,
    metadata_json
) VALUES
    ('AERIS_MENU_KITCHEN_SOURCE', 'MENU_PDF', 'Кухня / основное меню AERIS', 'AERIS', 'minio://astor-media/content/aeris/menu/kitchen/MENU_AERIS_A4_2026_DIGITAL.pdf', 'AERIS_MENU_KITCHEN', '{"domain":"QUIET_GUIDE","ragScope":"menu"}'::jsonb),
    ('AERIS_MENU_BAR_SOURCE', 'MENU_PDF', 'Барная карта AERIS', 'AERIS', 'minio://astor-media/content/aeris/menu/bar/BAR_CARD.pdf', 'AERIS_MENU_BAR', '{"domain":"QUIET_GUIDE","ragScope":"menu"}'::jsonb),
    ('AERIS_MENU_ELEMENTS_SOURCE', 'MENU_PDF', 'Коктейли / Elements AERIS', 'AERIS', 'minio://astor-media/content/aeris/menu/elements/ELEMENTS_CARD.pdf', 'AERIS_MENU_ELEMENTS', '{"domain":"QUIET_GUIDE","ragScope":"menu"}'::jsonb),
    ('AERIS_MENU_WINE_SOURCE', 'MENU_PDF', 'Винная карта AERIS', 'AERIS', 'minio://astor-media/content/aeris/menu/wine/WINE_MENU_2026_FINAL.pdf', 'AERIS_MENU_WINE', '{"domain":"QUIET_GUIDE","ragScope":"menu"}'::jsonb),
    ('AERIS_GUEST_GUIDE_SOURCE', 'GUIDE_HTML', 'Инструкция гостя Astor Butler', 'AERIS', 'docs/guest-guide.html', NULL, '{"domain":"GUEST_SUPPORT","ragScope":"instructions"}'::jsonb),
    ('AERIS_STAFF_GUIDE_SOURCE', 'GUIDE_HTML', 'Инструкция команды Astor Butler', 'AERIS', 'docs/staff-guide.html', NULL, '{"domain":"STAFF_SUPPORT","ragScope":"instructions"}'::jsonb),
    ('ASTOR_FSM_SCENARIOS_SOURCE', 'FSM_SPEC', 'FSM сценарии Astor Butler', 'AERIS', 'docs/fsm/FSM_SCENARIOS.md', NULL, '{"domain":"FSM","ragScope":"scenario_graph"}'::jsonb)
ON CONFLICT (source_code) DO UPDATE SET
    source_type = EXCLUDED.source_type,
    title = EXCLUDED.title,
    venue_code = EXCLUDED.venue_code,
    uri = EXCLUDED.uri,
    media_asset_code = EXCLUDED.media_asset_code,
    active = TRUE,
    metadata_json = EXCLUDED.metadata_json,
    updated_at = CURRENT_TIMESTAMP;
