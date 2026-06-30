CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE IF NOT EXISTS model_interaction_audit (
    interaction_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    venue_code VARCHAR(64) NOT NULL DEFAULT 'AERIS',
    channel VARCHAR(40),
    chat_id BIGINT,
    telegram_user_id BIGINT,
    correlation_id VARCHAR(120),
    scenario VARCHAR(120),
    state VARCHAR(120),
    purpose VARCHAR(120),
    provider VARCHAR(80),
    model VARCHAR(160),
    profile VARCHAR(80),
    prompt TEXT NOT NULL,
    guest_text TEXT,
    approved_fallback TEXT,
    response_text TEXT,
    generated BOOLEAN NOT NULL DEFAULT FALSE,
    fallback_used BOOLEAN NOT NULL DEFAULT FALSE,
    success BOOLEAN NOT NULL DEFAULT TRUE,
    error_type VARCHAR(160),
    error_message TEXT,
    latency_ms BIGINT,
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_model_interaction_audit_chat_time
    ON model_interaction_audit(chat_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_model_interaction_audit_scenario_time
    ON model_interaction_audit(venue_code, scenario, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_model_interaction_audit_success_time
    ON model_interaction_audit(success, created_at DESC);
