-- Keep logical replication publication focused on domain/outbox tables.
-- Liquibase service tables are intentionally excluded: Liquibase updates its
-- checksums on startup, and FOR ALL TABLES publication can block that update
-- unless those service tables have a replica identity.

DROP PUBLICATION IF EXISTS astor_outbox_publication;

DO $$
DECLARE
    publication_tables TEXT := '';
    candidate_table TEXT;
BEGIN
    FOREACH candidate_table IN ARRAY ARRAY[
        'outbox_events',
        'processed_kafka_events',
        'users',
        'telegram_profiles',
        'telegram_messages',
        'user_consents',
        'user_contacts',
        'event_bookings'
    ]
    LOOP
        IF to_regclass('public.' || candidate_table) IS NOT NULL THEN
            publication_tables := publication_tables
                || CASE WHEN publication_tables = '' THEN '' ELSE ', ' END
                || 'public.' || quote_ident(candidate_table);
        END IF;
    END LOOP;

    IF publication_tables <> '' THEN
        EXECUTE 'CREATE PUBLICATION astor_outbox_publication FOR TABLE ' || publication_tables;
    END IF;
END $$;
