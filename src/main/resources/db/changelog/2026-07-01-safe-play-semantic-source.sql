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
        'AERIS_SAFE_PLAY_SOURCE',
        'FSM_SEMANTIC_SEED',
        'Safe Play / сабражный ритуал AERIS',
        'AERIS',
        'classpath:semantic/aeris/safe-play-sabrage-rag-seed.md',
        NULL,
        '{"domain":"SAFE_PLAY","ragScope":"sabrage"}'::jsonb
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
