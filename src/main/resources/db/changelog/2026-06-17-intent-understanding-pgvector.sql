CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS pgcrypto;
CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE TABLE IF NOT EXISTS intent_examples (
    example_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    venue_code VARCHAR(64) NOT NULL DEFAULT 'AERIS',
    scenario_id VARCHAR(120) NOT NULL,
    state VARCHAR(120),
    intent VARCHAR(120) NOT NULL,
    phrase TEXT NOT NULL,
    normalized_phrase TEXT NOT NULL,
    expected_slots_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    source VARCHAR(80) NOT NULL DEFAULT 'GOLDEN_CORPUS',
    locale VARCHAR(16) NOT NULL DEFAULT 'ru',
    status VARCHAR(40) NOT NULL DEFAULT 'APPROVED',
    weight NUMERIC(6, 3) NOT NULL DEFAULT 1.000,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_intent_examples_phrase UNIQUE (venue_code, state, intent, normalized_phrase)
);

CREATE TABLE IF NOT EXISTS intent_example_embeddings (
    example_id UUID PRIMARY KEY REFERENCES intent_examples(example_id) ON DELETE CASCADE,
    embedding_model VARCHAR(120) NOT NULL,
    embedding_dimension INTEGER NOT NULL,
    embedding vector NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS intent_understanding_misses (
    miss_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    venue_code VARCHAR(64) NOT NULL DEFAULT 'AERIS',
    chat_id BIGINT,
    telegram_user_id BIGINT,
    state VARCHAR(120),
    raw_text TEXT NOT NULL,
    normalized_text TEXT NOT NULL,
    detected_intent VARCHAR(120),
    confidence NUMERIC(6, 3),
    resolution_status VARCHAR(40) NOT NULL DEFAULT 'NEW',
    reviewer_note TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_intent_examples_lookup
    ON intent_examples(venue_code, state, status, intent);

CREATE INDEX IF NOT EXISTS idx_intent_examples_phrase_trgm
    ON intent_examples USING gin(normalized_phrase gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_intent_understanding_misses_queue
    ON intent_understanding_misses(venue_code, resolution_status, created_at DESC);
