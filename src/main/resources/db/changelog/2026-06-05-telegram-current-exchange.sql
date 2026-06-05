ALTER TABLE telegram_profiles
    ADD COLUMN IF NOT EXISTS last_user_message_id INTEGER;
