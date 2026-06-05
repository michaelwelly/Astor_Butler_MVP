-- Harden relational constraints after the first identity normalization pass.
-- This migration is intentionally idempotent because local MVP databases may
-- already contain parts of the structure from manual experiments.

DO $$
BEGIN
    IF to_regclass('public.telegram_profiles') IS NOT NULL
       AND to_regclass('public.users') IS NOT NULL
       AND NOT EXISTS (
           SELECT 1
           FROM pg_constraint c
           JOIN pg_attribute a ON a.attrelid = c.conrelid AND a.attnum = ANY(c.conkey)
           WHERE c.contype = 'f'
             AND c.conrelid = 'telegram_profiles'::regclass
             AND c.confrelid = 'users'::regclass
             AND a.attname = 'user_id'
       ) THEN
        ALTER TABLE telegram_profiles
            ADD CONSTRAINT fk_telegram_profiles_user
                FOREIGN KEY (user_id) REFERENCES users(id)
                ON DELETE SET NULL;
    END IF;
END $$;

CREATE UNIQUE INDEX IF NOT EXISTS uq_telegram_profiles_user_id
    ON telegram_profiles(user_id)
    WHERE user_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_telegram_profiles_telegram_user_id
    ON telegram_profiles(telegram_user_id);

DO $$
BEGIN
    IF to_regclass('public.telegram_messages') IS NOT NULL
       AND to_regclass('public.users') IS NOT NULL
       AND NOT EXISTS (
           SELECT 1
           FROM pg_constraint c
           JOIN pg_attribute a ON a.attrelid = c.conrelid AND a.attnum = ANY(c.conkey)
           WHERE c.contype = 'f'
             AND c.conrelid = 'telegram_messages'::regclass
             AND c.confrelid = 'users'::regclass
             AND a.attname = 'user_id'
       ) THEN
        ALTER TABLE telegram_messages
            ADD CONSTRAINT fk_telegram_messages_user
                FOREIGN KEY (user_id) REFERENCES users(id)
                ON DELETE SET NULL;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_telegram_messages_user_id_time
    ON telegram_messages(user_id, received_at);

CREATE INDEX IF NOT EXISTS idx_telegram_messages_telegram_user_id_time
    ON telegram_messages(telegram_user_id, received_at);

DO $$
BEGIN
    IF to_regclass('public.user_consents') IS NOT NULL
       AND to_regclass('public.users') IS NOT NULL
       AND NOT EXISTS (
           SELECT 1
           FROM pg_constraint c
           JOIN pg_attribute a ON a.attrelid = c.conrelid AND a.attnum = ANY(c.conkey)
           WHERE c.contype = 'f'
             AND c.conrelid = 'user_consents'::regclass
             AND c.confrelid = 'users'::regclass
             AND a.attname = 'user_id'
       ) THEN
        ALTER TABLE user_consents
            ADD CONSTRAINT fk_user_consents_user
                FOREIGN KEY (user_id) REFERENCES users(id)
                ON DELETE SET NULL;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_user_consents_user_type_version
    ON user_consents(user_id, consent_type, policy_version);

DO $$
BEGIN
    IF to_regclass('public.user_contacts') IS NOT NULL
       AND to_regclass('public.users') IS NOT NULL
       AND NOT EXISTS (
           SELECT 1
           FROM pg_constraint c
           JOIN pg_attribute a ON a.attrelid = c.conrelid AND a.attnum = ANY(c.conkey)
           WHERE c.contype = 'f'
             AND c.conrelid = 'user_contacts'::regclass
             AND c.confrelid = 'users'::regclass
             AND a.attname = 'user_id'
       ) THEN
        ALTER TABLE user_contacts
            ADD CONSTRAINT fk_user_contacts_user
                FOREIGN KEY (user_id) REFERENCES users(id)
                ON DELETE CASCADE;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_user_contacts_type_value
    ON user_contacts(contact_type, contact_value);

DO $$
BEGIN
    IF to_regclass('public.event_bookings') IS NOT NULL THEN
        ALTER TABLE event_bookings
            ADD COLUMN IF NOT EXISTS user_id BIGINT,
            ADD COLUMN IF NOT EXISTS manager_user_id BIGINT;

        UPDATE event_bookings eb
        SET user_id = tp.user_id
        FROM telegram_profiles tp
        WHERE eb.user_id IS NULL
          AND eb.chat_id = tp.chat_id
          AND tp.user_id IS NOT NULL;
    END IF;
END $$;

DO $$
BEGIN
    IF to_regclass('public.event_bookings') IS NOT NULL
       AND to_regclass('public.users') IS NOT NULL
       AND NOT EXISTS (
           SELECT 1
           FROM pg_constraint c
           JOIN pg_attribute a ON a.attrelid = c.conrelid AND a.attnum = ANY(c.conkey)
           WHERE c.contype = 'f'
             AND c.conrelid = 'event_bookings'::regclass
             AND c.confrelid = 'users'::regclass
             AND a.attname = 'user_id'
       ) THEN
        ALTER TABLE event_bookings
            ADD CONSTRAINT fk_event_bookings_user
                FOREIGN KEY (user_id) REFERENCES users(id)
                ON DELETE SET NULL;
    END IF;

    IF to_regclass('public.event_bookings') IS NOT NULL
       AND to_regclass('public.users') IS NOT NULL
       AND NOT EXISTS (
           SELECT 1
           FROM pg_constraint c
           JOIN pg_attribute a ON a.attrelid = c.conrelid AND a.attnum = ANY(c.conkey)
           WHERE c.contype = 'f'
             AND c.conrelid = 'event_bookings'::regclass
             AND c.confrelid = 'users'::regclass
             AND a.attname = 'manager_user_id'
       ) THEN
        ALTER TABLE event_bookings
            ADD CONSTRAINT fk_event_bookings_manager_user
                FOREIGN KEY (manager_user_id) REFERENCES users(id)
                ON DELETE SET NULL;
    END IF;
END $$;

DO $$
BEGIN
    IF to_regclass('public.event_bookings') IS NOT NULL THEN
        CREATE INDEX IF NOT EXISTS idx_event_bookings_user_status_created
            ON event_bookings(user_id, status, created_at);

        CREATE INDEX IF NOT EXISTS idx_event_bookings_manager_status_created
            ON event_bookings(manager_user_id, status, created_at);

        CREATE INDEX IF NOT EXISTS idx_event_bookings_chat_id_created
            ON event_bookings(chat_id, created_at);
    END IF;
END $$;
