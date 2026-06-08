#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
if [[ -f "${ROOT_DIR}/.env" ]]; then
  set -a
  # shellcheck disable=SC1091
  source "${ROOT_DIR}/.env"
  set +a
fi

SOURCE_DIR="${1:-/Users/michaelwelly/Desktop/AERISMENU}"
CARDS_DIR="${AERIS_CARDS_DIR:-${SOURCE_DIR}/Карты бара - обновления }"
BUCKET="${S3_BUCKET_MEDIA:-astor-media}"
ACCESS_KEY="${S3_ACCESS_KEY:-astor}"
SECRET_KEY="${S3_SECRET_KEY:-astor_minio_password}"
ENDPOINT="${S3_ENDPOINT_INTERNAL:-http://minio:9000}"
COMPOSE_PROJECT="${COMPOSE_PROJECT_NAME:-astor_butler_mvp}"
NETWORK="${COMPOSE_PROJECT}_default"
POSTGRES_DB="${POSTGRES_DB:-aether}"
POSTGRES_USER="${POSTGRES_USER:-oracle}"
FLOOR_PLAN_FILE="${AERIS_FLOOR_PLAN_FILE:-}"

if [[ ! -d "$SOURCE_DIR" ]]; then
  echo "AERIS source folder not found: $SOURCE_DIR" >&2
  exit 2
fi

if [[ ! -d "$CARDS_DIR" ]]; then
  echo "AERIS cards folder not found: $CARDS_DIR" >&2
  exit 2
fi

if [[ -z "$FLOOR_PLAN_FILE" ]]; then
  if [[ -f "${CARDS_DIR}/AERIS PLAN.pdf" ]]; then
    FLOOR_PLAN_FILE="${CARDS_DIR}/AERIS PLAN.pdf"
  elif [[ -f "${SOURCE_DIR}/AERIS PLAN.pdf" ]]; then
    FLOOR_PLAN_FILE="${SOURCE_DIR}/AERIS PLAN.pdf"
  else
    FLOOR_PLAN_FILE="${CARDS_DIR}/AERIS PLAN.pdf"
  fi
fi

required_files=(
  "${CARDS_DIR}/MENU AERIS A4 2026 DIGITAL.pdf"
  "${CARDS_DIR}/BAR CARD.pdf"
  "${CARDS_DIR}/ELEMENTS CARD.pdf"
  "${CARDS_DIR}/WINE MENU 2026 FINAL.pdf"
  "${FLOOR_PLAN_FILE}"
  "${SOURCE_DIR}/INTERIOR.mp4"
)

for file in "${required_files[@]}"; do
  if [[ ! -f "$file" ]]; then
    echo "Required AERIS runtime asset not found: $file" >&2
    exit 2
  fi
done

tmp_dir="$(mktemp -d)"
trap 'rm -rf "$tmp_dir"' EXIT

mkdir -p \
  "$tmp_dir/content/aeris/menu/kitchen" \
  "$tmp_dir/content/aeris/menu/bar" \
  "$tmp_dir/content/aeris/menu/elements" \
  "$tmp_dir/content/aeris/menu/wine" \
  "$tmp_dir/content/aeris/floor-plan" \
  "$tmp_dir/content/aeris/interior"

cp "${CARDS_DIR}/MENU AERIS A4 2026 DIGITAL.pdf" "$tmp_dir/content/aeris/menu/kitchen/MENU_AERIS_A4_2026_DIGITAL.pdf"
cp "${CARDS_DIR}/BAR CARD.pdf" "$tmp_dir/content/aeris/menu/bar/BAR_CARD.pdf"
cp "${CARDS_DIR}/ELEMENTS CARD.pdf" "$tmp_dir/content/aeris/menu/elements/ELEMENTS_CARD.pdf"
cp "${CARDS_DIR}/WINE MENU 2026 FINAL.pdf" "$tmp_dir/content/aeris/menu/wine/WINE_MENU_2026_FINAL.pdf"
cp "${FLOOR_PLAN_FILE}" "$tmp_dir/content/aeris/floor-plan/AERIS_PLAN.pdf"
cp "${SOURCE_DIR}/INTERIOR.mp4" "$tmp_dir/content/aeris/interior/INTERIOR.mp4"

docker compose up -d minio minio-init postgres

docker run --rm \
  --network "${NETWORK}" \
  -v "${tmp_dir}/content:/content:ro" \
  --entrypoint sh \
  minio/mc:RELEASE.2025-04-16T18-13-26Z \
  -c "mc alias set astor '${ENDPOINT}' '${ACCESS_KEY}' '${SECRET_KEY}' >/dev/null && mc mb --ignore-existing astor/'${BUCKET}' >/dev/null && mc mirror --overwrite /content astor/'${BUCKET}'/content"

docker compose exec -T postgres psql -v ON_ERROR_STOP=1 -U "${POSTGRES_USER}" -d "${POSTGRES_DB}" \
  < "${ROOT_DIR}/src/main/resources/db/changelog/2026-06-08-media-assets.sql"

docker compose exec -T postgres psql -v ON_ERROR_STOP=1 -U "${POSTGRES_USER}" -d "${POSTGRES_DB}" <<SQL
INSERT INTO media_assets (
    asset_code,
    venue_code,
    domain,
    kind,
    title,
    bucket,
    object_key,
    filename,
    content_type,
    active,
    version_label,
    metadata_json
) VALUES
    ('AERIS_MENU_KITCHEN', 'AERIS', 'QUIET_GUIDE', 'PDF_MENU', 'Кухня / основное меню', '${BUCKET}', 'content/aeris/menu/kitchen/MENU_AERIS_A4_2026_DIGITAL.pdf', 'MENU AERIS A4 2026 DIGITAL.pdf', 'application/pdf', TRUE, '2026-06', '{"keywords":["меню","еда","кухня","основное меню"]}'::jsonb),
    ('AERIS_MENU_BAR', 'AERIS', 'QUIET_GUIDE', 'PDF_MENU', 'Барная карта', '${BUCKET}', 'content/aeris/menu/bar/BAR_CARD.pdf', 'BAR CARD.pdf', 'application/pdf', TRUE, '2026-06', '{"keywords":["бар","напитки","барная карта"]}'::jsonb),
    ('AERIS_MENU_ELEMENTS', 'AERIS', 'QUIET_GUIDE', 'PDF_MENU', 'Коктейли / Elements', '${BUCKET}', 'content/aeris/menu/elements/ELEMENTS_CARD.pdf', 'ELEMENTS CARD.pdf', 'application/pdf', TRUE, '2026-06', '{"keywords":["коктейли","elements","авторские коктейли"]}'::jsonb),
    ('AERIS_MENU_WINE', 'AERIS', 'QUIET_GUIDE', 'PDF_MENU', 'Винная карта', '${BUCKET}', 'content/aeris/menu/wine/WINE_MENU_2026_FINAL.pdf', 'WINE MENU 2026 FINAL.pdf', 'application/pdf', TRUE, '2026-06', '{"keywords":["вино","винная карта","шампанское"]}'::jsonb),
    ('AERIS_FLOOR_PLAN', 'AERIS', 'TABLE_BOOKING', 'FLOOR_PLAN', 'План зала AERIS', '${BUCKET}', 'content/aeris/floor-plan/AERIS_PLAN.pdf', 'AERIS PLAN.pdf', 'application/pdf', TRUE, '2026-06', '{"usage":["table_booking"]}'::jsonb),
    ('AERIS_INTERIOR_TOUR', 'AERIS', 'QUIET_GUIDE', 'VIDEO_TOUR', 'AERIS interior tour', '${BUCKET}', 'content/aeris/interior/INTERIOR.mp4', 'INTERIOR.mp4', 'video/mp4', TRUE, '2026-06', '{"sendMode":"DOCUMENT"}'::jsonb)
ON CONFLICT (asset_code) DO UPDATE SET
    bucket = EXCLUDED.bucket,
    object_key = EXCLUDED.object_key,
    filename = EXCLUDED.filename,
    content_type = EXCLUDED.content_type,
    active = EXCLUDED.active,
    version_label = EXCLUDED.version_label,
    metadata_json = EXCLUDED.metadata_json,
    updated_at = CURRENT_TIMESTAMP;
SQL

echo "AERIS runtime assets are ready:"
echo "- source: $SOURCE_DIR"
echo "- bucket: s3://${BUCKET}"
echo "- catalog: PostgreSQL media_assets"
echo "- keys:"
echo "  content/aeris/menu/kitchen/MENU_AERIS_A4_2026_DIGITAL.pdf"
echo "  content/aeris/menu/bar/BAR_CARD.pdf"
echo "  content/aeris/menu/elements/ELEMENTS_CARD.pdf"
echo "  content/aeris/menu/wine/WINE_MENU_2026_FINAL.pdf"
echo "  content/aeris/floor-plan/AERIS_PLAN.pdf"
echo "  content/aeris/interior/INTERIOR.mp4"
