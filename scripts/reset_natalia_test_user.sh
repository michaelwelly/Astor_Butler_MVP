#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ENV_FILE="${ENV_FILE:-$ROOT_DIR/.env}"

TELEGRAM_ID="${TELEGRAM_ID:-1773317437}"
CHAT_ID="${CHAT_ID:-}"
POSTGRES_CONTAINER="${POSTGRES_CONTAINER:-astor_postgres_test}"
REDIS_CONTAINER="${REDIS_CONTAINER:-astor_redis_test}"
ASTOR_REDIS_KEY_PREFIX="${ASTOR_REDIS_KEY_PREFIX:-astor}"
DRY_RUN=false

usage() {
  cat <<USAGE
Usage:
  scripts/reset_natalia_test_user.sh [--dry-run] [--telegram-id ID] [--chat-id ID]

Defaults:
  --telegram-id 1773317437
  --chat-id      same as telegram id

The script resets the local manual-test guest in PostgreSQL and Redis:
  - telegram profile preview/current-message pointers
  - telegram messages and consents
  - table reservation orders/holds for the guest
  - local Redis FSM state and table-booking draft

It refuses to reset admin users.
USAGE
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --dry-run)
      DRY_RUN=true
      shift
      ;;
    --telegram-id)
      TELEGRAM_ID="${2:?--telegram-id requires a value}"
      shift 2
      ;;
    --chat-id)
      CHAT_ID="${2:?--chat-id requires a value}"
      shift 2
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

CHAT_ID="${CHAT_ID:-$TELEGRAM_ID}"

if [[ -f "$ENV_FILE" ]]; then
  set -a
  # shellcheck disable=SC1090
  source "$ENV_FILE"
  set +a
fi

POSTGRES_DB="${POSTGRES_DB:-aether}"
POSTGRES_USER="${POSTGRES_USER:-oracle}"
POSTGRES_PASSWORD="${POSTGRES_PASSWORD:-}"
REDIS_PASSWORD="${REDIS_PASSWORD:-}"

if [[ "$TELEGRAM_ID" == "421441838" || "$CHAT_ID" == "421441838" ]]; then
  echo "Refusing to reset michael_welly/admin telegram id 421441838." >&2
  exit 3
fi

echo "Reset target: telegram_id=$TELEGRAM_ID chat_id=$CHAT_ID dry_run=$DRY_RUN"

SQL_END="COMMIT;"
if [[ "$DRY_RUN" == "true" ]]; then
  SQL_END="ROLLBACK;"
fi

docker exec -i \
  -e PGPASSWORD="$POSTGRES_PASSWORD" \
  "$POSTGRES_CONTAINER" \
  psql -v ON_ERROR_STOP=1 \
       -v telegram_id="$TELEGRAM_ID" \
       -v chat_id="$CHAT_ID" \
       -U "$POSTGRES_USER" \
       -d "$POSTGRES_DB" <<SQL
BEGIN;

CREATE TEMP TABLE reset_target (
    user_id BIGINT,
    telegram_id BIGINT NOT NULL,
    chat_id BIGINT NOT NULL
) ON COMMIT DROP;

INSERT INTO reset_target (user_id, telegram_id, chat_id)
SELECT DISTINCT u.id, :telegram_id::BIGINT, :chat_id::BIGINT
FROM users u
WHERE u.telegram_id = :telegram_id::BIGINT
UNION
SELECT DISTINCT tp.user_id, :telegram_id::BIGINT, :chat_id::BIGINT
FROM telegram_profiles tp
WHERE tp.telegram_user_id = :telegram_id::BIGINT
   OR tp.chat_id = :chat_id::BIGINT;

DO \$\$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM reset_target rt
        JOIN users u ON u.id = rt.user_id
        WHERE u.role = 'ADMIN' OR u.username = 'michael_welly'
    ) THEN
        RAISE EXCEPTION 'Refusing to reset admin user';
    END IF;
END
\$\$;

SELECT 'before.users' AS item, COUNT(*) AS count
FROM users u
WHERE u.telegram_id = :telegram_id::BIGINT
UNION ALL
SELECT 'before.telegram_profiles', COUNT(*)
FROM telegram_profiles tp
WHERE tp.telegram_user_id = :telegram_id::BIGINT OR tp.chat_id = :chat_id::BIGINT
UNION ALL
SELECT 'before.telegram_messages', COUNT(*)
FROM telegram_messages tm
WHERE tm.telegram_user_id = :telegram_id::BIGINT OR tm.chat_id = :chat_id::BIGINT
UNION ALL
SELECT 'before.user_consents', COUNT(*)
FROM user_consents uc
WHERE uc.telegram_user_id = :telegram_id::BIGINT
   OR uc.chat_id = :chat_id::BIGINT
   OR uc.user_id IN (SELECT user_id FROM reset_target WHERE user_id IS NOT NULL)
UNION ALL
SELECT 'before.table_orders', COUNT(*)
FROM table_reservation_orders o
WHERE o.telegram_user_id = :telegram_id::BIGINT
   OR o.chat_id = :chat_id::BIGINT
   OR o.user_id IN (SELECT user_id FROM reset_target WHERE user_id IS NOT NULL);

DELETE FROM table_reservation_holds h
USING table_reservation_orders o
WHERE h.order_id = o.id
  AND (
      o.telegram_user_id = :telegram_id::BIGINT
      OR o.chat_id = :chat_id::BIGINT
      OR o.user_id IN (SELECT user_id FROM reset_target WHERE user_id IS NOT NULL)
  );

DELETE FROM table_reservation_orders o
WHERE o.telegram_user_id = :telegram_id::BIGINT
   OR o.chat_id = :chat_id::BIGINT
   OR o.user_id IN (SELECT user_id FROM reset_target WHERE user_id IS NOT NULL);

DO \$\$
BEGIN
    IF to_regclass('public.event_bookings') IS NOT NULL THEN
        DELETE FROM event_bookings eb
        USING reset_target rt
        WHERE eb.chat_id = rt.chat_id
           OR eb.user_id = rt.user_id;
    END IF;
END
\$\$;

DELETE FROM user_contacts uc
WHERE uc.user_id IN (SELECT user_id FROM reset_target WHERE user_id IS NOT NULL);

DELETE FROM user_consents uc
WHERE uc.telegram_user_id = :telegram_id::BIGINT
   OR uc.chat_id = :chat_id::BIGINT
   OR uc.user_id IN (SELECT user_id FROM reset_target WHERE user_id IS NOT NULL);

DELETE FROM telegram_messages tm
WHERE tm.telegram_user_id = :telegram_id::BIGINT
   OR tm.chat_id = :chat_id::BIGINT
   OR tm.user_id IN (SELECT user_id FROM reset_target WHERE user_id IS NOT NULL);

DELETE FROM telegram_profiles tp
WHERE tp.telegram_user_id = :telegram_id::BIGINT
   OR tp.chat_id = :chat_id::BIGINT
   OR tp.user_id IN (SELECT user_id FROM reset_target WHERE user_id IS NOT NULL);

DELETE FROM users u
WHERE u.telegram_id = :telegram_id::BIGINT
  AND u.role <> 'ADMIN'
  AND COALESCE(u.username, '') <> 'michael_welly';

SELECT 'after.users' AS item, COUNT(*) AS count
FROM users u
WHERE u.telegram_id = :telegram_id::BIGINT
UNION ALL
SELECT 'after.telegram_profiles', COUNT(*)
FROM telegram_profiles tp
WHERE tp.telegram_user_id = :telegram_id::BIGINT OR tp.chat_id = :chat_id::BIGINT
UNION ALL
SELECT 'after.telegram_messages', COUNT(*)
FROM telegram_messages tm
WHERE tm.telegram_user_id = :telegram_id::BIGINT OR tm.chat_id = :chat_id::BIGINT
UNION ALL
SELECT 'after.user_consents', COUNT(*)
FROM user_consents uc
WHERE uc.telegram_user_id = :telegram_id::BIGINT
   OR uc.chat_id = :chat_id::BIGINT
   OR uc.user_id IN (SELECT user_id FROM reset_target WHERE user_id IS NOT NULL)
UNION ALL
SELECT 'after.table_orders', COUNT(*)
FROM table_reservation_orders o
WHERE o.telegram_user_id = :telegram_id::BIGINT
   OR o.chat_id = :chat_id::BIGINT
   OR o.user_id IN (SELECT user_id FROM reset_target WHERE user_id IS NOT NULL);

$SQL_END
SQL

REDIS_KEYS=(
  "$ASTOR_REDIS_KEY_PREFIX:fsm:telegram:$CHAT_ID:state"
  "$ASTOR_REDIS_KEY_PREFIX:booking:table:draft:telegram:$CHAT_ID"
)

if [[ "$DRY_RUN" == "true" ]]; then
  echo "Redis dry-run keys:"
  printf '  %s\n' "${REDIS_KEYS[@]}"
else
  REDIS_ARGS=()
  if [[ -n "$REDIS_PASSWORD" ]]; then
    REDIS_ARGS=(-a "$REDIS_PASSWORD" --no-auth-warning)
  fi
  docker exec "$REDIS_CONTAINER" redis-cli "${REDIS_ARGS[@]}" DEL "${REDIS_KEYS[@]}" >/dev/null
  echo "Redis keys removed:"
  printf '  %s\n' "${REDIS_KEYS[@]}"
fi

if [[ "$DRY_RUN" == "true" ]]; then
  echo "Dry run complete. PostgreSQL transaction rolled back; Redis untouched."
else
  echo "Natalia test user reset complete."
fi
