-- Normalize Astor identity around internal users while keeping legacy columns
-- for the current JPA layer. Telegram becomes an external profile, not the user.

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS display_name TEXT,
    ADD COLUMN IF NOT EXISTS status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE';

UPDATE users
SET display_name = COALESCE(
        NULLIF(TRIM(CONCAT_WS(' ', first_name, last_name)), ''),
        NULLIF(username, ''),
        CONCAT('Guest #', id)
    )
WHERE display_name IS NULL;

ALTER TABLE telegram_profiles
    ADD COLUMN IF NOT EXISTS user_id BIGINT;

UPDATE telegram_profiles tp
SET user_id = u.id
FROM users u
WHERE tp.user_id IS NULL
  AND tp.telegram_user_id = u.telegram_id;

ALTER TABLE telegram_profiles
    ADD CONSTRAINT fk_telegram_profiles_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_telegram_profiles_user_id
    ON telegram_profiles(user_id);

ALTER TABLE telegram_messages
    ADD COLUMN IF NOT EXISTS user_id BIGINT;

UPDATE telegram_messages tm
SET user_id = u.id
FROM users u
WHERE tm.user_id IS NULL
  AND tm.telegram_user_id = u.telegram_id;

ALTER TABLE telegram_messages
    ADD CONSTRAINT fk_telegram_messages_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_telegram_messages_user_id_time
    ON telegram_messages(user_id, received_at);

ALTER TABLE user_consents
    ADD COLUMN IF NOT EXISTS user_id BIGINT;

UPDATE user_consents uc
SET user_id = u.id
FROM users u
WHERE uc.user_id IS NULL
  AND uc.telegram_user_id = u.telegram_id;

ALTER TABLE user_consents
    ADD CONSTRAINT fk_user_consents_user
        FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_user_consents_user_id
    ON user_consents(user_id);

CREATE TABLE IF NOT EXISTS user_contacts (
    id UUID PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    contact_type VARCHAR(32) NOT NULL,
    contact_value TEXT NOT NULL,
    source VARCHAR(64) NOT NULL,
    is_primary BOOLEAN NOT NULL DEFAULT false,
    verified_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_user_contacts_user_type_value UNIQUE (user_id, contact_type, contact_value)
);

CREATE INDEX IF NOT EXISTS idx_user_contacts_user_id
    ON user_contacts(user_id);

INSERT INTO user_contacts (
    id, user_id, contact_type, contact_value, source, is_primary, verified_at, created_at, updated_at
)
SELECT md5(random()::text || clock_timestamp()::text || u.id::text)::uuid,
       u.id, 'PHONE', u.phone, 'LEGACY_USERS_PHONE', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM users u
WHERE u.phone IS NOT NULL
  AND TRIM(u.phone) <> ''
ON CONFLICT ON CONSTRAINT uq_user_contacts_user_type_value DO NOTHING;

INSERT INTO user_contacts (
    id, user_id, contact_type, contact_value, source, is_primary, verified_at, created_at, updated_at
)
SELECT md5(random()::text || clock_timestamp()::text || tp.user_id::text || tp.telegram_user_id::text)::uuid,
       tp.user_id, 'PHONE', tp.phone_number, 'TELEGRAM_CONTACT', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM telegram_profiles tp
WHERE tp.user_id IS NOT NULL
  AND tp.phone_number IS NOT NULL
  AND TRIM(tp.phone_number) <> ''
ON CONFLICT ON CONSTRAINT uq_user_contacts_user_type_value DO NOTHING;
