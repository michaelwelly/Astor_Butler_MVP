#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
AERIS_MENU_SOURCE_DIR="${AERIS_MENU_SOURCE_DIR:-/Users/michaelwelly/Desktop/AERISMENU}"

cd "$ROOT_DIR"

docker compose up -d \
  api-gateway \
  postgres \
  redis \
  mongo \
  kafka \
  redpanda-console \
  minio \
  minio-init \
  prometheus \
  grafana

if [[ -d "$AERIS_MENU_SOURCE_DIR" ]]; then
  scripts/ingest_aeris_runtime_assets.sh "$AERIS_MENU_SOURCE_DIR"
  scripts/ingest_aeris_menu_assets.sh "$AERIS_MENU_SOURCE_DIR"
else
  echo "AERIS menu folder not found, content ingest skipped: $AERIS_MENU_SOURCE_DIR" >&2
  echo "Set AERIS_MENU_SOURCE_DIR=/path/to/AERISMENU and rerun scripts/ingest_aeris_runtime_assets.sh if needed." >&2
fi

echo "Local infrastructure is ready."
echo "- Swagger via gateway: http://localhost:8080/swagger-ui/index.html"
echo "- Redpanda Console: http://localhost:8081"
echo "- MinIO Console: http://localhost:9001"
