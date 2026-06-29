# Media Pipeline

## Goal

Prepare a repeatable pipeline for frontend and promo media:

```text
Yandex Disk / local folder
  -> local inventory
  -> S3-compatible object storage
  -> metadata in PostgreSQL/MongoDB
  -> Redis cache for hot menus/previews/landing blocks
  -> frontend URLs
```

Current C3FLEX.com rule:

- all 102 portfolio files can be public after curation;
- local MinIO keeps only the lightweight video sample set for development;
- the sample set is enough for the first frontend tabs/cards without syncing the whole Yandex Disk folder;
- nested folders from Yandex Disk remain category metadata for the production catalog.

## Local S3

Docker Compose provides MinIO:

- API: `http://localhost:9000`
- Console: `http://localhost:9001`
- media bucket: `astor-media`
- documents bucket: `astor-documents`

Credentials are configured through local `.env`.

Local access policy:

- `astor-media` is read-only/public for local frontend playback;
- `astor-documents` stays private;
- MinIO Console is available at `http://localhost:9001`.

`minio-init` is an init job. It creates buckets and exits with code `0`; it is expected to look stopped after successful startup.

## Inventory

After Yandex Disk is synced locally:

```bash
python3 scripts/media_inventory.py "/path/to/Yandex Disk/Astor" --out /private/tmp/astor_media_manifest.jsonl
```

The manifest contains:

- stable `_id`;
- `sourcePath`;
- `relativePath`;
- `bucket`;
- `objectKey`;
- `mediaType`;
- `contentType`;
- `sizeBytes`;
- `sha256`;
- `tags`;
- lifecycle `status`.

## S3 Sync

```bash
scripts/sync_media_to_minio.sh "/path/to/Yandex Disk/Astor" astor-media
```

This syncs source files to:

```text
s3://astor-media/raw
```

For local C3FLEX.com frontend work, prefer the lightweight sample workflow instead of syncing the full media set:

```bash
python3 scripts/media_sample.py /private/tmp/astor_media_manifest.jsonl --limit 10 --out /private/tmp/astor_media_sample_manifest.jsonl --copy-dir /private/tmp/astor_media_sample
scripts/sync_media_to_minio.sh /private/tmp/astor_media_sample astor-media
```

Current local sample bucket contains `10` video objects under:

```text
http://localhost:9000/astor-media/raw/...
```

Example:

```text
http://localhost:9000/astor-media/raw/AI/TANGIERS.mp4
```

## Metadata Import

After inventory is created:

```bash
scripts/import_media_manifest_to_mongo.sh /private/tmp/astor_media_manifest.jsonl aether media_assets
```

This upserts media metadata into MongoDB and creates indexes:

- unique `sourcePath`;
- unique `bucket + objectKey`;
- `mediaType + tags`;
- `sha256`.

## Metadata Model

MVP media metadata should include:

- `id`;
- `bucket`;
- `objectKey`;
- `publicUrl`;
- `mediaType`;
- `contentType`;
- `sizeBytes`;
- `sha256`;
- `sourcePath`;
- `category`;
- `tags`;
- `status`;
- `createdAt`;
- `updatedAt`.

## Derived Assets

Future processing stages:

- image thumbnails and webp derivatives;
- video poster images;
- mp4/webm normalized versions;
- adaptive streaming output;
- PDF/document previews;
- AI-readable text extraction for document-like media.

## Redis Cache

Cache targets:

- menu blocks;
- landing sections;
- media previews;
- document lists;
- frequently used frontend references.

Suggested keys:

```text
media:metadata:{id}
media:by-tag:{tag}
landing:block:{slug}
menu:active:{locationId}
```

## Ephemeral Telegram Voice

Голосовые сообщения гостя считаются временными бинарными файлами:

- Telegram adapter скачивает `voice/audio` во временную локальную папку;
- backend загружает файл в MinIO/S3 bucket `astor-media`;
- object key строится под prefix `transient/telegram-voice/YYYY-MM-DD/...`;
- MinIO lifecycle удаляет эти объекты через `3` дня;
- transcript, Telegram metadata, `storageBucket`, `storageObjectKey`, `storageTtlDays` остаются в PostgreSQL `telegram_messages.raw_payload`.
- Kafka admin summary показывает `transcript`, `transcriptionStatus` и `storageObjectKey`, чтобы админ видел смысл голосового без поиска по логам IDEA.

Локальные env:

```bash
S3_EPHEMERAL_PREFIX=transient
S3_VOICE_PREFIX=telegram-voice
S3_VOICE_TTL_DAYS=3
ASTOR_STT_KEEP_LOCAL_FILES=false
TELEGRAM_UI_CLEANUP_ENABLED=false
TELEGRAM_UI_DELETE_USER_MESSAGES_ENABLED=false
```

Правило: Postgres хранит смысл и аудит, MinIO/S3 хранит тяжелый временный бинарник. Telegram runtime message deletion отключен; UX-чистка будет отдельной session policy, а не `DeleteMessage`.

## AERIS Runtime Assets

Runtime assets used by Telegram scenarios are stored outside the application jar.

```text
Git: code, docs, manifests, ingestion scripts
PostgreSQL: media_assets active runtime index
MinIO/S3: PDF/video binaries
MongoDB: optional inventory/document metadata for search and RAG workflows
```

Canonical runtime object keys:

```text
content/aeris/menu/kitchen/MENU_AERIS_A4_2026_DIGITAL.pdf
content/aeris/menu/bar/BAR_CARD.pdf
content/aeris/menu/elements/ELEMENTS_CARD.pdf
content/aeris/menu/wine/WINE_MENU_2026_FINAL.pdf
content/aeris/floor-plan/AERIS_PLAN.pdf
content/aeris/interior/INTERIOR.mp4
```

Local runtime ingest:

```bash
scripts/ingest_aeris_runtime_assets.sh "/Users/michaelwelly/Desktop/AERISMENU"
```

The script uploads canonical objects to MinIO and upserts `media_assets` rows in PostgreSQL. It is the runtime path for `MenuAssetsScenario`, `TableBookingScenario` and `QuietGuideScenario`.

## AERIS Channel Content

Афиши, промо и свежий атмосферный контент берутся из официального Telegram-канала AERIS.

Текущий MVP-источник:

```text
https://t.me/s/aeris_gastrobar
```

Целевой production-источник после доступа в заведении:

```text
Telegram channel_post / edited_channel_post, когда Astor Butler bot добавлен админом канала
```

Подробный план ingest, классификации, хранения в MinIO/PostgreSQL/pgvector и retention:

```text
docs/content/AERIS_CHANNEL_INGEST.md
```

MVP runtime status:

- manual ingest endpoint: `POST /api/content/ingest/aeris-channel`;
- source parser: public `https://t.me/s/aeris_gastrobar`;
- durable store: PostgreSQL `venue_content_posts` and `venue_content_assets`;
- guest read path: `QuietGuideScenario` uses active channel posts for афиша/акции/что сегодня;
- channel media mirroring into MinIO is enabled by `ASTOR_AERIS_CHANNEL_ASSET_MIRRORING_ENABLED` and stores objects under `content/aeris/channel/YYYY/MM/...`; if a Telegram CDN URL cannot be downloaded, the source URL remains in `venue_content_assets` for review.

## AERIS Menu Inventory

Current local source:

```text
/Users/michaelwelly/Desktop/AERISMENU
```

This folder contains current AERIS menu PDFs, images, an interior video and event/budget supporting files. It must not be committed to Git.

Storage decision:

- original files live on local disk or Yandex Disk as source of truth;
- broad inventory copy lives in MinIO bucket `astor-media`;
- inventory object prefix for AERIS menu: `content/aeris-menu`;
- MongoDB database: `aether`;
- MongoDB collection: `menu_assets`;
- Redis should cache only the active menu index / selected URLs, not binary files.

Run local bootstrap after Docker infrastructure is up:

```bash
docker compose up -d minio minio-init mongo
scripts/ingest_aeris_runtime_assets.sh "/Users/michaelwelly/Desktop/AERISMENU"
scripts/ingest_aeris_menu_assets.sh "/Users/michaelwelly/Desktop/AERISMENU"
```

Result:

```text
s3://astor-media/content/aeris-menu/...
http://localhost:9000/astor-media/content/aeris-menu/...
Mongo: aether.menu_assets
```

Recommended hot cache keys:

```text
menu:aeris:active
menu:aeris:pdfs
menu:aeris:images
menu:aeris:last-updated
```

Guest flow target:

```text
Guest asks "меню"
  -> Quiet Guide / Menu API
  -> Redis active menu index
  -> Mongo fallback if cache miss
  -> public MinIO/S3 URL
  -> Telegram sends document/link or web frontend renders card
```

## Next Steps

1. Keep the full Yandex Disk source as the media origin.
2. Keep the full 102-file inventory in MongoDB.
3. Select one video sample per top-level branch for local MinIO.
4. Import sample metadata into MongoDB/PostgreSQL if needed for frontend API development.
5. Extend Media API with search, signed/public URL and lifecycle endpoints.

## Commands For Next Local Run

```bash
docker compose up -d minio minio-init mongo
python3 scripts/media_inventory.py "/Users/michaelwelly/<local-yandex-disk-folder>" --out /private/tmp/astor_media_manifest.jsonl
scripts/sync_media_to_minio.sh "/Users/michaelwelly/<local-yandex-disk-folder>" astor-media
scripts/import_media_manifest_to_mongo.sh /private/tmp/astor_media_manifest.jsonl
```
