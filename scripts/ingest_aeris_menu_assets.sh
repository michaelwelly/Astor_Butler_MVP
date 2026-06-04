#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SOURCE_DIR="${1:-/Users/michaelwelly/Desktop/AERISMENU}"
OUT_DIR="${2:-/private/tmp/aeris_menu_ingest}"
BUCKET="${S3_BUCKET_MEDIA:-astor-media}"
PREFIX="${AERIS_MENU_S3_PREFIX:-content/aeris-menu}"
DATABASE="${MONGO_DB:-aether}"
COLLECTION="${AERIS_MENU_MONGO_COLLECTION:-menu_assets}"
MANIFEST="${OUT_DIR}/aeris_menu_manifest.jsonl"

if [[ ! -d "$SOURCE_DIR" ]]; then
  echo "AERIS menu source folder not found: $SOURCE_DIR" >&2
  exit 2
fi

mkdir -p "$OUT_DIR"

cd "$ROOT_DIR"

python3 scripts/media_inventory.py "$SOURCE_DIR" \
  --bucket "$BUCKET" \
  --prefix "$PREFIX" \
  --out "$MANIFEST"

scripts/sync_media_to_minio.sh "$SOURCE_DIR" "$BUCKET" "$PREFIX"
scripts/import_media_manifest_to_mongo.sh "$MANIFEST" "$DATABASE" "$COLLECTION"

echo "AERIS menu assets are ready:"
echo "- source: $SOURCE_DIR"
echo "- manifest: $MANIFEST"
echo "- s3: s3://${BUCKET}/${PREFIX}"
echo "- mongo: ${DATABASE}.${COLLECTION}"
