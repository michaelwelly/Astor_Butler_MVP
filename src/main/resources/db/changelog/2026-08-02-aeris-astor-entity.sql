CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE IF NOT EXISTS venue_profiles (
    venue_code VARCHAR(64) PRIMARY KEY,
    display_name VARCHAR(160) NOT NULL,
    venue_kind VARCHAR(80) NOT NULL,
    address_line TEXT,
    city VARCHAR(120),
    country_code VARCHAR(8) NOT NULL DEFAULT 'RU',
    timezone VARCHAR(80) NOT NULL DEFAULT 'Asia/Yekaterinburg',
    website_url TEXT,
    public_phone VARCHAR(64),
    metadata_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS astor_assistant_profiles (
    profile_code VARCHAR(80) PRIMARY KEY,
    venue_code VARCHAR(64) NOT NULL REFERENCES venue_profiles(venue_code),
    display_name VARCHAR(160) NOT NULL,
    role_title VARCHAR(160) NOT NULL,
    persona_summary TEXT NOT NULL,
    fsm_source_of_truth BOOLEAN NOT NULL DEFAULT TRUE,
    can_monitor_sources BOOLEAN NOT NULL DEFAULT TRUE,
    can_update_rag_context BOOLEAN NOT NULL DEFAULT TRUE,
    metadata_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS venue_monitored_sources (
    source_code VARCHAR(120) PRIMARY KEY,
    venue_code VARCHAR(64) NOT NULL REFERENCES venue_profiles(venue_code),
    source_type VARCHAR(64) NOT NULL,
    source_name VARCHAR(160) NOT NULL,
    source_url TEXT NOT NULL,
    polling_interval_seconds INTEGER NOT NULL DEFAULT 3600,
    enabled BOOLEAN NOT NULL DEFAULT FALSE,
    ingest_target VARCHAR(120) NOT NULL DEFAULT 'venue_content_posts',
    notes TEXT NOT NULL DEFAULT '',
    metadata_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_venue_monitored_sources_runtime
    ON venue_monitored_sources(venue_code, enabled, source_type);

CREATE TABLE IF NOT EXISTS review_distribution_drafts (
    draft_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    venue_code VARCHAR(64) NOT NULL REFERENCES venue_profiles(venue_code),
    guest_user_id BIGINT,
    source_channel VARCHAR(64) NOT NULL DEFAULT 'TELEGRAM',
    source_message_id VARCHAR(160),
    review_text TEXT NOT NULL,
    status VARCHAR(64) NOT NULL DEFAULT 'DRAFT',
    consent_version VARCHAR(80),
    consented_at TIMESTAMPTZ,
    confirmed_at TIMESTAMPTZ,
    metadata_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS review_distribution_targets (
    target_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    draft_id UUID NOT NULL REFERENCES review_distribution_drafts(draft_id) ON DELETE CASCADE,
    platform VARCHAR(64) NOT NULL,
    target_account_ref VARCHAR(240) NOT NULL,
    status VARCHAR(64) NOT NULL DEFAULT 'PENDING_CONFIRMATION',
    external_post_url TEXT,
    error_code VARCHAR(120),
    error_message TEXT,
    requested_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    metadata_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_review_distribution_target UNIQUE (draft_id, platform, target_account_ref)
);

CREATE INDEX IF NOT EXISTS idx_review_distribution_drafts_status
    ON review_distribution_drafts(venue_code, status, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_review_distribution_targets_status
    ON review_distribution_targets(platform, status, created_at DESC);

CREATE TABLE IF NOT EXISTS guest_social_profile_connections (
    connection_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    guest_user_id BIGINT,
    venue_code VARCHAR(64) NOT NULL REFERENCES venue_profiles(venue_code),
    platform VARCHAR(64) NOT NULL,
    account_ref VARCHAR(240) NOT NULL,
    account_kind VARCHAR(80) NOT NULL DEFAULT 'UNKNOWN',
    consent_version VARCHAR(80) NOT NULL,
    consented_at TIMESTAMPTZ NOT NULL,
    scopes_json JSONB NOT NULL DEFAULT '[]'::jsonb,
    token_ref VARCHAR(240),
    status VARCHAR(64) NOT NULL DEFAULT 'CONNECTED',
    metadata_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_guest_social_connection UNIQUE (venue_code, platform, account_ref)
);

CREATE TABLE IF NOT EXISTS guest_media_analysis_batches (
    batch_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    connection_id UUID NOT NULL REFERENCES guest_social_profile_connections(connection_id) ON DELETE CASCADE,
    venue_code VARCHAR(64) NOT NULL REFERENCES venue_profiles(venue_code),
    purpose VARCHAR(120) NOT NULL DEFAULT 'SERVICE_PREFERENCE_ENRICHMENT',
    status VARCHAR(64) NOT NULL DEFAULT 'PENDING',
    media_count INTEGER NOT NULL DEFAULT 0,
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    error_message TEXT,
    metadata_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS guest_media_analysis_items (
    item_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    batch_id UUID NOT NULL REFERENCES guest_media_analysis_batches(batch_id) ON DELETE CASCADE,
    platform_media_id VARCHAR(240) NOT NULL,
    media_type VARCHAR(64) NOT NULL,
    source_url TEXT,
    captured_at TIMESTAMPTZ,
    analysis_status VARCHAR(64) NOT NULL DEFAULT 'PENDING',
    labels_json JSONB NOT NULL DEFAULT '[]'::jsonb,
    transcript TEXT,
    caption TEXT,
    metadata_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_guest_media_analysis_item UNIQUE (batch_id, platform_media_id)
);

CREATE TABLE IF NOT EXISTS guest_preference_insights (
    insight_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    batch_id UUID NOT NULL REFERENCES guest_media_analysis_batches(batch_id) ON DELETE CASCADE,
    guest_user_id BIGINT,
    venue_code VARCHAR(64) NOT NULL REFERENCES venue_profiles(venue_code),
    insight_type VARCHAR(80) NOT NULL,
    insight_value TEXT NOT NULL,
    confidence NUMERIC(5, 4) NOT NULL DEFAULT 0,
    evidence_json JSONB NOT NULL DEFAULT '[]'::jsonb,
    status VARCHAR(64) NOT NULL DEFAULT 'NEEDS_REVIEW',
    metadata_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_guest_media_analysis_batches_status
    ON guest_media_analysis_batches(venue_code, status, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_guest_preference_insights_lookup
    ON guest_preference_insights(venue_code, guest_user_id, status, insight_type);

INSERT INTO venue_profiles (
    venue_code,
    display_name,
    venue_kind,
    address_line,
    city,
    country_code,
    timezone,
    website_url,
    public_phone,
    metadata_json
) VALUES (
    'AERIS',
    'AERIS',
    'MEDITERRANEAN_GASTROBAR',
    'ул. Мамина-Сибиряка, 58',
    'Екатеринбург',
    'RU',
    'Asia/Yekaterinburg',
    'https://aeris.bar/',
    '+7 996 592-21-16',
    '{"verifiedFrom":["https://aeris.bar/","https://yandex.ru/maps/org/aeris/103837967593/"],"verificationDate":"2026-08-02"}'::jsonb
) ON CONFLICT (venue_code) DO UPDATE SET
    display_name = EXCLUDED.display_name,
    venue_kind = EXCLUDED.venue_kind,
    address_line = EXCLUDED.address_line,
    city = EXCLUDED.city,
    country_code = EXCLUDED.country_code,
    timezone = EXCLUDED.timezone,
    website_url = EXCLUDED.website_url,
    public_phone = EXCLUDED.public_phone,
    metadata_json = EXCLUDED.metadata_json,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO astor_assistant_profiles (
    profile_code,
    venue_code,
    display_name,
    role_title,
    persona_summary,
    fsm_source_of_truth,
    can_monitor_sources,
    can_update_rag_context,
    metadata_json
) VALUES (
    'ASTOR_AERIS_BUTLER',
    'AERIS',
    'Astor',
    'Цифровой дворецкий ресторана AERIS',
    'Astor помогает гостю AERIS выбрать стол, меню, напитки, видео-тур, актуальные события и безопасно передать заявку команде. Он не подтверждает брони и не меняет бизнес-статусы вне FSM/domain services.',
    TRUE,
    TRUE,
    TRUE,
    '{"voice":"calm_business_hospitality","boundary":"FSM/domain services remain source of truth"}'::jsonb
) ON CONFLICT (profile_code) DO UPDATE SET
    venue_code = EXCLUDED.venue_code,
    display_name = EXCLUDED.display_name,
    role_title = EXCLUDED.role_title,
    persona_summary = EXCLUDED.persona_summary,
    fsm_source_of_truth = EXCLUDED.fsm_source_of_truth,
    can_monitor_sources = EXCLUDED.can_monitor_sources,
    can_update_rag_context = EXCLUDED.can_update_rag_context,
    metadata_json = EXCLUDED.metadata_json,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO venue_monitored_sources (
    source_code,
    venue_code,
    source_type,
    source_name,
    source_url,
    polling_interval_seconds,
    enabled,
    ingest_target,
    notes,
    metadata_json
) VALUES
    (
        'AERIS_OFFICIAL_WEBSITE',
        'AERIS',
        'WEBSITE',
        'Официальный сайт AERIS',
        'https://aeris.bar/',
        3600,
        FALSE,
        'semantic_sources',
        'Reference source only until a website crawler/parser is explicitly implemented.',
        '{"verified":true,"fields":["address","phone","concept","menu_links"]}'::jsonb
    ),
    (
        'AERIS_YANDEX_MAPS',
        'AERIS',
        'MAPS_PROFILE',
        'Yandex Maps AERIS profile',
        'https://yandex.ru/maps/org/aeris/103837967593/',
        3600,
        FALSE,
        'venue_profiles',
        'Reference source for public address, phone, hours and public rating. Do not scrape reviews or personal data without a reviewed adapter.',
        '{"verified":true,"fields":["address","phone","hours","public_profile"],"source":"Yandex Maps"}'::jsonb
    ),
    (
        'AERIS_2GIS_PROFILE',
        'AERIS',
        'MAPS_PROFILE',
        '2GIS AERIS profile',
        'https://2gis.ru/ekaterinburg/firm/70000001085768632',
        3600,
        FALSE,
        'venue_profiles',
        'Reference source for public address, contacts, hours and features. Disabled until adapter/legal boundary is reviewed.',
        '{"verified":true,"fields":["address","phone","hours","features"],"source":"2GIS"}'::jsonb
    ),
    (
        'AERIS_INSTAGRAM_PUBLIC_PROFILE',
        'AERIS',
        'SOCIAL_PROFILE',
        'Instagram AERIS profile',
        'https://www.instagram.com/aeris.bar/',
        3600,
        FALSE,
        'venue_content_posts',
        'Known public profile. Disabled until access method, platform policy and media rights are explicitly confirmed.',
        '{"verified":true,"handle":"aeris.bar","fields":["bio","public_posts"],"requiresAdapterReview":true}'::jsonb
    ),
    (
        'USER_REVIEW_DISTRIBUTION_OUTBOUND',
        'AERIS',
        'OUTBOUND_SOCIAL_POSTING',
        'User review distribution connector boundary',
        'app://review-distribution',
        3600,
        FALSE,
        'review_distribution_drafts',
        'Future outbound connector: after explicit user consent and final confirmation, one approved review can be posted to connected user social accounts. No silent posting and no platform tokens in repository or public logs.',
        '{"requiresConsent":true,"requiresFinalConfirmation":true,"storesTokens":false,"supportedPlatforms":"connector-dependent"}'::jsonb
    ),
    (
        'USER_CONSENTED_MEDIA_INSIGHTS',
        'AERIS',
        'INBOUND_USER_MEDIA_ANALYSIS',
        'Consented user media preference insights',
        'app://guest-media-insights',
        3600,
        FALSE,
        'guest_media_analysis_batches',
        'Future inbound connector: only after explicit consent and supported platform OAuth/export. The output is practical service preferences, not psychological diagnosis or covert profiling.',
        '{"requiresConsent":true,"requiresPreview":true,"prohibitedInferences":["mental_health","personality_diagnosis","sensitive_traits"],"allowedInsights":["food_preferences","drink_preferences","occasion_style","visual_style","hospitality_preferences","communication_preferences"]}'::jsonb
    ),
    (
        'AERIS_TELEGRAM_PUBLIC_CHANNEL',
        'AERIS',
        'TELEGRAM_PUBLIC_HTML',
        'Telegram channel AERIS',
        'https://t.me/s/aeris_gastrobar',
        3600,
        TRUE,
        'venue_content_posts',
        'Current implemented source for афиша, промо, новости and atmosphere content.',
        '{"username":"aeris_gastrobar","implementedBy":"PublicTelegramHtmlSource"}'::jsonb
    )
ON CONFLICT (source_code) DO UPDATE SET
    venue_code = EXCLUDED.venue_code,
    source_type = EXCLUDED.source_type,
    source_name = EXCLUDED.source_name,
    source_url = EXCLUDED.source_url,
    polling_interval_seconds = EXCLUDED.polling_interval_seconds,
    enabled = EXCLUDED.enabled,
    ingest_target = EXCLUDED.ingest_target,
    notes = EXCLUDED.notes,
    metadata_json = EXCLUDED.metadata_json,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO semantic_sources (
    source_code,
    source_type,
    title,
    venue_code,
    uri,
    media_asset_code,
    metadata_json
) VALUES
    (
        'AERIS_PUBLIC_PROFILE_SOURCE',
        'VENUE_PROFILE',
        'Публичный профиль AERIS',
        'AERIS',
        'db:venue_profiles/AERIS',
        NULL,
        '{"domain":"VENUE_PROFILE","ragScope":"identity","provenance":["aeris.bar","Yandex Maps"]}'::jsonb
    ),
    (
        'AERIS_MEDIA_ASSETS_SOURCE',
        'MEDIA_ASSET_REGISTRY',
        'Загруженные документы и медиа AERIS',
        'AERIS',
        'db:media_assets?venue=AERIS',
        NULL,
        '{"domain":"MEDIA_ASSETS","ragScope":"documents"}'::jsonb
    ),
    (
        'ASTOR_REVIEW_DISTRIBUTION_SOURCE',
        'INTEGRATION_BOUNDARY',
        'Review distribution / публикация отзывов',
        'AERIS',
        'db:review_distribution_drafts',
        NULL,
        '{"domain":"REVIEWS","ragScope":"integration_boundary","enabled":false}'::jsonb
    ),
    (
        'ASTOR_GUEST_MEDIA_INSIGHTS_SOURCE',
        'INTEGRATION_BOUNDARY',
        'Consented guest media insights / предпочтения гостя',
        'AERIS',
        'db:guest_media_analysis_batches',
        NULL,
        '{"domain":"PREFERENCE_MAP","ragScope":"integration_boundary","enabled":false}'::jsonb
    )
ON CONFLICT (source_code) DO UPDATE SET
    source_type = EXCLUDED.source_type,
    title = EXCLUDED.title,
    venue_code = EXCLUDED.venue_code,
    uri = EXCLUDED.uri,
    media_asset_code = EXCLUDED.media_asset_code,
    active = TRUE,
    metadata_json = EXCLUDED.metadata_json,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO semantic_sources (
    source_code,
    source_type,
    title,
    venue_code,
    uri,
    media_asset_code,
    metadata_json
) VALUES
    (
        'AERIS_ASTOR_PROFILE_SOURCE',
        'ASSISTANT_PROFILE',
        'Astor / цифровой дворецкий AERIS',
        'AERIS',
        'classpath:semantic/aeris/astor-aeris-profile-rag-seed.md',
        NULL,
        '{"domain":"ASSISTANT_PERSONA","ragScope":"identity"}'::jsonb
    ),
    (
        'AERIS_MONITORED_SOURCES_SOURCE',
        'SOURCE_REGISTRY',
        'AERIS monitored sources',
        'AERIS',
        'classpath:semantic/aeris/astor-aeris-profile-rag-seed.md',
        NULL,
        '{"domain":"CONTENT_INGEST","ragScope":"freshness"}'::jsonb
    )
ON CONFLICT (source_code) DO UPDATE SET
    source_type = EXCLUDED.source_type,
    title = EXCLUDED.title,
    venue_code = EXCLUDED.venue_code,
    uri = EXCLUDED.uri,
    media_asset_code = EXCLUDED.media_asset_code,
    active = TRUE,
    metadata_json = EXCLUDED.metadata_json,
    updated_at = CURRENT_TIMESTAMP;
