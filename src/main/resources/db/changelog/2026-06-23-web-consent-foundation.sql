-- Anonymous website consent is stored before OAuth/login exists.
-- Later it can be linked from web_session_id to an internal user.

CREATE TABLE IF NOT EXISTS web_consents (
    id UUID PRIMARY KEY,
    web_session_id UUID NOT NULL REFERENCES web_sessions(id) ON DELETE CASCADE,
    consent_type VARCHAR(80) NOT NULL,
    policy_version VARCHAR(80) NOT NULL,
    status VARCHAR(40) NOT NULL DEFAULT 'GRANTED',
    source VARCHAR(80) NOT NULL DEFAULT 'WEB_CHAT',
    evidence_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    granted_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    revoked_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_web_consents_session_policy UNIQUE (web_session_id, consent_type, policy_version)
);

CREATE INDEX IF NOT EXISTS idx_web_consents_session_status
    ON web_consents(web_session_id, status, updated_at DESC);

CREATE INDEX IF NOT EXISTS idx_web_consents_policy_status
    ON web_consents(consent_type, policy_version, status);
