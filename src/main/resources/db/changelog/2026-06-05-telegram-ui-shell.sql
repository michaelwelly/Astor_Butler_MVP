ALTER TABLE telegram_profiles
    ADD COLUMN IF NOT EXISTS preview_message_id INTEGER,
    ADD COLUMN IF NOT EXISTS last_bot_message_id INTEGER,
    ADD COLUMN IF NOT EXISTS preview_version VARCHAR(64),
    ADD COLUMN IF NOT EXISTS preview_updated_at TIMESTAMP WITH TIME ZONE;

CREATE INDEX IF NOT EXISTS idx_telegram_profiles_chat_id
    ON telegram_profiles(chat_id);
