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

## AERIS Menu Assets

Current local source:

```text
/Users/michaelwelly/Desktop/AERISMENU
```

This folder contains current AERIS menu PDFs, images, an interior video and event/budget supporting files. It must not be committed to Git.

Storage decision:

- original files live on local disk or Yandex Disk as source of truth;
- local runtime copy lives in MinIO bucket `astor-media`;
- object prefix for AERIS menu: `content/aeris-menu`;
- MongoDB database: `aether`;
- MongoDB collection: `menu_assets`;
- Redis should cache only the active menu index / selected URLs, not binary files.

Run local bootstrap after Docker infrastructure is up:

```bash
docker compose up -d minio minio-init mongo
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
