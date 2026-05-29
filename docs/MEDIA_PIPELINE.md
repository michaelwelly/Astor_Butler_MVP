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

## Local S3

Docker Compose provides MinIO:

- API: `http://localhost:9000`
- Console: `http://localhost:9001`
- media bucket: `astor-media`
- documents bucket: `astor-documents`

Credentials are configured through `.env` / `.env.example`.

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

## Metadata Import

After inventory is created:

```bash
scripts/import_media_manifest_to_mongo.sh /private/tmp/astor_media_manifest.jsonl astor_butler_documents_test media_assets
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

## Next Steps

1. Receive local synced Yandex Disk path.
2. Run `media_inventory.py`.
3. Run `sync_media_to_minio.sh`.
4. Import manifest metadata into MongoDB/PostgreSQL.
5. Extend Media API with search, signed/public URL and lifecycle endpoints.

## Commands For Next Local Run

```bash
docker compose up -d minio minio-init mongo
python3 scripts/media_inventory.py "/Users/michaelwelly/<local-yandex-disk-folder>" --out /private/tmp/astor_media_manifest.jsonl
scripts/sync_media_to_minio.sh "/Users/michaelwelly/<local-yandex-disk-folder>" astor-media
scripts/import_media_manifest_to_mongo.sh /private/tmp/astor_media_manifest.jsonl
```
