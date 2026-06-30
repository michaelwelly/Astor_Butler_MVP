#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SCENARIO_FILE="${1:-$ROOT_DIR/scripts/replay_scenarios/table-booking-happy-path.jsonl}"
BASE_URL="${BASE_URL:-http://localhost:8089}"
CHAT_ID="${REPLAY_CHAT_ID:-$((990000000 + $(date +%s) % 1000000))}"
EXTERNAL_USER_ID="${REPLAY_EXTERNAL_USER_ID:-$CHAT_ID}"
FIRST_NAME="${REPLAY_FIRST_NAME:-Replay Guest}"
USERNAME="${REPLAY_USERNAME:-astor_replay_guest}"
DEFAULT_TIMEOUT="${REPLAY_TIMEOUT_SECONDS:-12}"
RESULT_DIR="${REPLAY_RESULT_DIR:-$ROOT_DIR/build/replay-results}"
RUN_ID="$(date +%Y%m%d-%H%M%S)"
RESULT_FILE="$RESULT_DIR/$(basename "$SCENARIO_FILE" .jsonl)-$RUN_ID.jsonl"

if [[ ! -f "$SCENARIO_FILE" ]]; then
  echo "Scenario file not found: $SCENARIO_FILE" >&2
  exit 2
fi

if ! command -v jq >/dev/null 2>&1; then
  echo "jq is required for FSM replay. Install jq and retry." >&2
  exit 2
fi

mkdir -p "$RESULT_DIR"

echo "FSM replay"
echo "  scenario: $SCENARIO_FILE"
echo "  baseUrl:  $BASE_URL"
echo "  chatId:   $CHAT_ID"
echo "  result:   $RESULT_FILE"
echo

step_no=0
while IFS= read -r raw_line || [[ -n "$raw_line" ]]; do
  line="$(printf '%s' "$raw_line" | sed 's/^[[:space:]]*//;s/[[:space:]]*$//')"
  [[ -z "$line" || "$line" == \#* ]] && continue

  step_no=$((step_no + 1))
  name="$(jq -r '.name // ("step-" + (input_line_number|tostring))' <<<"$line")"
  text="$(jq -r '.text // ""' <<<"$line")"
  contact_phone="$(jq -r '.contactPhone // empty' <<<"$line")"
  correlation_id="replay-$RUN_ID-$step_no"
  timeout="$(jq -r ".timeoutSeconds // $DEFAULT_TIMEOUT" <<<"$line")"
  sleep_ms="$(jq -r '.sleepMs // 0' <<<"$line")"

  request="$(jq -n \
    --arg channel "TELEGRAM" \
    --arg externalUserId "$EXTERNAL_USER_ID" \
    --argjson chatId "$CHAT_ID" \
    --arg text "$text" \
    --arg firstName "$FIRST_NAME" \
    --arg username "$USERNAME" \
    --arg correlationId "$correlation_id" \
    --arg scenarioName "$(basename "$SCENARIO_FILE")" \
    --arg stepNo "$step_no" \
    --arg contactPhone "$contact_phone" \
    '{
      channel: $channel,
      externalUserId: $externalUserId,
      chatId: $chatId,
      text: $text,
      firstName: $firstName,
      username: $username,
      correlationId: $correlationId,
      payload: { replay: true, scenario: $scenarioName, step: $stepNo }
    }
    + (if $contactPhone == "" then {} else {contactPhone: $contactPhone} end)')"

  echo "[$step_no] $name -> ${text:-<empty>}${contact_phone:+ contact=$contact_phone}"
  http_body="$(mktemp)"
  http_code="$(
    curl -sS \
      --max-time "$timeout" \
      -o "$http_body" \
      -w "%{http_code}" \
      -H "Content-Type: application/json" \
      -X POST "$BASE_URL/api/messages" \
      -d "$request"
  )"
  response="$(cat "$http_body")"
  rm -f "$http_body"

  jq -n \
    --argjson request "$request" \
    --argjson response "$response" \
    --argjson expected "$line" \
    --arg httpCode "$http_code" \
    --arg step "$step_no" \
    '{step: ($step|tonumber), httpCode: ($httpCode|tonumber), expected: $expected, request: $request, response: $response}' \
    >> "$RESULT_FILE"

  if [[ "$http_code" != "200" ]]; then
    echo "  FAIL: HTTP $http_code" >&2
    echo "$response" | jq . >&2 || echo "$response" >&2
    exit 1
  fi

  expected_state="$(jq -r '.expectNextState // empty' <<<"$line")"
  if [[ -n "$expected_state" ]]; then
    actual_state="$(jq -r '.nextState // ""' <<<"$response")"
    if [[ "$actual_state" != "$expected_state" ]]; then
      echo "  FAIL: nextState expected '$expected_state', got '$actual_state'" >&2
      echo "$response" | jq . >&2
      exit 1
    fi
  fi

  contains_count="$(jq '.expectTextContains // [] | length' <<<"$line")"
  for ((i=0; i<contains_count; i++)); do
    needle="$(jq -r ".expectTextContains[$i]" <<<"$line")"
    if ! jq -r '.text // ""' <<<"$response" | grep -Fq "$needle"; then
      echo "  FAIL: response text does not contain '$needle'" >&2
      echo "$response" | jq . >&2
      exit 1
    fi
  done

  action_count="$(jq '.expectActions // [] | length' <<<"$line")"
  for ((i=0; i<action_count; i++)); do
    action="$(jq -r ".expectActions[$i]" <<<"$line")"
    if ! jq -e --arg action "$action" '(.actions // []) | index($action)' <<<"$response" >/dev/null; then
      echo "  FAIL: response actions do not contain '$action'" >&2
      echo "$response" | jq . >&2
      exit 1
    fi
  done

  echo "  OK: $(jq -r '.nextState // "-"' <<<"$response") | $(jq -r '.text // ""' <<<"$response" | tr '\n' ' ' | cut -c1-120)"

  if [[ "$sleep_ms" =~ ^[0-9]+$ && "$sleep_ms" -gt 0 ]]; then
    sleep "$(awk "BEGIN { printf \"%.3f\", $sleep_ms / 1000 }")"
  fi
done < "$SCENARIO_FILE"

echo
echo "Replay passed: $step_no steps"
echo "Saved responses: $RESULT_FILE"
