#!/usr/bin/env bash
set -euo pipefail

if [[ $# -lt 1 ]]; then
  echo "Usage: scripts/sync_media_to_minio.sh <source-dir> [bucket]" >&2
  exit 2
fi

SOURCE_DIR="$(cd "$1" && pwd)"
BUCKET="${2:-${S3_BUCKET_MEDIA:-astor-media}}"
COMPOSE_PROJECT="${COMPOSE_PROJECT_NAME:-astor_butler_mvp}"
NETWORK="${COMPOSE_PROJECT}_default"
ACCESS_KEY="${S3_ACCESS_KEY:-astor}"
SECRET_KEY="${S3_SECRET_KEY:-astor_minio_password}"
ENDPOINT="${S3_ENDPOINT_INTERNAL:-http://minio:9000}"

docker compose up -d minio minio-init

docker run --rm \
  --network "${NETWORK}" \
  -v "${SOURCE_DIR}:/source:ro" \
  --entrypoint sh \
  minio/mc:RELEASE.2025-04-16T18-13-26Z \
  -c "mc alias set astor '${ENDPOINT}' '${ACCESS_KEY}' '${SECRET_KEY}' && mc mb --ignore-existing astor/'${BUCKET}' && mc mirror --overwrite --exclude '.DS_Store' /source astor/'${BUCKET}'/raw"

echo "Synced ${SOURCE_DIR} to s3://${BUCKET}/raw"
