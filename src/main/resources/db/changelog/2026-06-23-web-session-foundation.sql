-- Website chat sessions use the same FSM gateway, but keep their own web-native audit trail.
-- Telegram remains a transport/UI; web_sessions maps browser sessions to stable synthetic chat ids.

CREATE TABLE IF NOT EXISTS web_sessions (
    id UUID PRIMARY KEY,
    session_id VARCHAR(160) NOT NULL UNIQUE,
    site_code VARCHAR(80) NOT NULL DEFAULT 'c3flex',
    external_user_id VARCHAR(200) NOT NULL,
    chat_id BIGINT NOT NULL UNIQUE,
    first_seen_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_seen_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    referrer TEXT,
    landing_page TEXT,
    user_agent_hash VARCHAR(160),
    metadata_json JSONB NOT NULL DEFAULT '{}'::jsonb
);

CREATE INDEX IF NOT EXISTS idx_web_sessions_site_last_seen
    ON web_sessions(site_code, last_seen_at DESC);

CREATE INDEX IF NOT EXISTS idx_web_sessions_external_user
    ON web_sessions(external_user_id);

CREATE TABLE IF NOT EXISTS web_messages (
    id UUID PRIMARY KEY,
    web_session_id UUID NOT NULL REFERENCES web_sessions(id) ON DELETE CASCADE,
    correlation_id VARCHAR(180),
    direction VARCHAR(20) NOT NULL,
    text TEXT,
    payload_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_web_messages_session_created
    ON web_messages(web_session_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_web_messages_correlation
    ON web_messages(correlation_id);
