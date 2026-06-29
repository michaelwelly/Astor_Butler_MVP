#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ENV_FILE="${ENV_FILE:-$ROOT_DIR/.env}"
POSTGRES_CONTAINER="${POSTGRES_CONTAINER:-astor_postgres_test}"
DRY_RUN=false

usage() {
  cat <<USAGE
Usage:
  scripts/reset_manual_test_users.sh [--dry-run]

Resets known manual-test guests:
  - Natalia Poedinenko: telegram/chat id 1773317437
  - Sergey: lookup by Telegram username yziizy in local PostgreSQL

The script delegates the actual cleanup to scripts/reset_natalia_test_user.sh.
USAGE
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --dry-run)
      DRY_RUN=true
      shift
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "Unknown argument: $1" >&2
      usage >&2
      exit 2
      ;;
  esac
done

if [[ -f "$ENV_FILE" ]]; then
  set -a
  # shellcheck disable=SC1090
  source "$ENV_FILE"
  set +a
fi

POSTGRES_DB="${POSTGRES_DB:-aether}"
POSTGRES_USER="${POSTGRES_USER:-oracle}"
POSTGRES_PASSWORD="${POSTGRES_PASSWORD:-}"

run_reset_user() {
  if [[ "$DRY_RUN" == "true" ]]; then
    "$ROOT_DIR/scripts/reset_natalia_test_user.sh" --dry-run "$@"
  else
    "$ROOT_DIR/scripts/reset_natalia_test_user.sh" "$@"
  fi
}

echo "Resetting Natalia manual-test account..."
run_reset_user \
  --telegram-id 1773317437 \
  --chat-id 1773317437

echo
echo "Looking up Sergey manual-test account by username yziizy..."
sergey_row="$(
  docker exec -i \
    -e PGPASSWORD="$POSTGRES_PASSWORD" \
    "$POSTGRES_CONTAINER" \
    psql -v ON_ERROR_STOP=1 \
         -v username="yziizy" \
         -U "$POSTGRES_USER" \
         -d "$POSTGRES_DB" \
         -At -F ' ' <<'SQL'
WITH candidates AS (
    SELECT tp.telegram_user_id AS telegram_id, tp.chat_id AS chat_id, tp.username AS username
    FROM telegram_profiles tp
    WHERE lower(tp.username) = lower(:'username')
    UNION ALL
    SELECT u.telegram_id AS telegram_id, u.telegram_id AS chat_id, u.username AS username
    FROM users u
    WHERE lower(u.username) = lower(:'username')
)
SELECT telegram_id, chat_id
FROM candidates
WHERE telegram_id IS NOT NULL
ORDER BY chat_id NULLS LAST
LIMIT 1;
SQL
)"

if [[ -z "$sergey_row" ]]; then
  echo "Sergey/yziizy was not found in PostgreSQL yet. Skipping him for now."
  echo "Send any message from Sergey once, then rerun this script."
  exit 0
fi

read -r sergey_telegram_id sergey_chat_id <<<"$sergey_row"
sergey_chat_id="${sergey_chat_id:-$sergey_telegram_id}"

if [[ -z "$sergey_telegram_id" ]]; then
  echo "Sergey/yziizy lookup returned an empty telegram_id. Skipping him." >&2
  exit 0
fi

echo "Resetting Sergey/yziizy manual-test account: telegram_id=$sergey_telegram_id chat_id=$sergey_chat_id"
run_reset_user \
  --telegram-id "$sergey_telegram_id" \
  --chat-id "$sergey_chat_id"

echo
echo "Manual-test users reset complete."
