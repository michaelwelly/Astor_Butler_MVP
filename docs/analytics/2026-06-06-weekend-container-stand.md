# Weekend Container Stand - 2026-06-06

## Stand

- Backend runs as Docker Compose service `app`; IDEA is not required for weekend testing.
- API Gateway: `http://localhost:8080`
- Swagger: `http://localhost:8080/swagger-ui/index.html`
- Redpanda Console: `http://localhost:8081`
- Ollama load balancer: `http://localhost:11434`
- LLM pool:
  - `astor_ollama_1`
  - `astor_ollama_2`
  - `astor_llm_gateway`
  - default model split after 2026-06-27: `FRONTLINE=qwen2.5:1.5b`, `QUALITY=qwen2.5:3b`

## Verified

- `mvn test`: 43 tests, 0 failures.
- Docker health:
  - `astor_app`: healthy
  - `astor_api_gateway`: healthy
  - `astor_llm_gateway`: healthy
  - `astor_ollama_1/2`: healthy
  - PostgreSQL, Redis, Mongo, Kafka, MinIO: healthy
- Kafka topic `astor.user.events`: 3 partitions.
- Kafka consumer group `astor-admin-events`: total lag 0.
- Analytics consumer delivered events to admin chat during the load run.

## Data Prep

- Preserved admin user:
  - `telegram_id=421441838`
  - `username=michael_welly`
  - `role=ADMIN`
- Cleared old outbox/processed event rows before the weekend run.
- The k6 run then created 9 synthetic guest profiles for analytics.

## k6 Weekend Guest Matrix

Command:

```bash
docker run --rm \
  -e K6_BASE_URL=http://host.docker.internal:8080 \
  -v "$PWD/k6:/scripts:ro" \
  grafana/k6 run /scripts/weekend-guest-scenarios.js
```

Result:

- 9 guest iterations completed.
- First-touch step: 9/9 passed.
- Contact-share step: 9/9 passed.
- Scenario intent step: 6/9 passed.
- Scenario follow-up step: 2/8 passed.
- HTTP failures: 9/35.
- p95 duration: about 30 seconds.

Interpretation:

- The first-touch FSM bug was fixed: an unknown Telegram guest now enters `CONSENT_REQUIRED` even without `/start`.
- The REST Telegram simulation now maps numeric `externalUserId` to `telegramUserId`, so API tests follow the same FSM identity path as Telegram.
- Remaining failures are concentrated in free-form LLM paths, not in first-touch/contact or Kafka/admin observability.

## Open Risks

- Local CPU Ollama still times out under several simultaneous free-form prompts, even with 3 containers.
- This is expected until more guest intents move from `AI_FALLBACK` into explicit FSM scenarios.
- `outbox_events` had 35 rows after the run; `processed_kafka_events` had 9 rows because the admin consumer stores processed Kafka event IDs, not every outbox row.
- Reservation orders stayed at 0 because host-confirmation flow was not completed by this API matrix.

## Next Fixes

- Add explicit FSM routes for:
  - menu recommendation follow-up
  - poster/event reservation
  - art mediation details
  - donation follow-up
  - auction lot list
  - manager handoff follow-up
- Split load tests:
  - deterministic FSM matrix with no LLM dependency
  - LLM stress matrix for capacity measurement
- Consider a faster local LLM model or external inference for weekend parallel tests.

## k6 Deterministic FSM Baseline

Added a separate baseline script:

```text
k6/weekend-fsm-baseline.js
```

The baseline avoids free-form LLM prompts and exercises deterministic FSM routes:

- first touch -> `CONSENT_REQUIRED`
- contact share -> `READY_FOR_DIALOG`
- table booking -> `TABLE_BOOKING_WAIT_TABLE_SELECTION`
- menu / poster / manager / impact / event booking -> `READY_FOR_DIALOG`
- donation -> `DONATION_COLLECT_AMOUNT`
- auction -> `AUCTION_WAIT_BID`
- smart tip with amount -> `TIP_CONFIRMATION`

Fast weekend smoke command:

```bash
docker run --rm \
  -e K6_BASE_URL=http://host.docker.internal:8080 \
  -e K6_CHAT_ID_BASE=910500000 \
  -v "$PWD/k6:/scripts:ro" \
  grafana/k6 run /scripts/weekend-fsm-baseline.js
```

Result after rebuilding the `app` container:

- 9 fresh guests completed.
- 27 HTTP requests.
- 81/81 checks passed.
- HTTP failures: 0/27.
- p95 duration: about 288 ms.

Paced 45-guest command:

```bash
docker run --rm \
  -e K6_BASE_URL=http://host.docker.internal:8080 \
  -e K6_CHAT_ID_BASE=910400000 \
  -e K6_VUS=3 \
  -e K6_ITERATIONS=45 \
  -e K6_STEP_SLEEP=0.6 \
  -v "$PWD/k6:/scripts:ro" \
  grafana/k6 run /scripts/weekend-fsm-baseline.js
```

Result:

- 45 fresh guests completed.
- 135 HTTP requests.
- 405/405 checks passed.
- HTTP failures: 0/135.
- p95 duration: about 100 ms.

## Load Findings

- A bursty 45-guest run with 9 VUs hit `api-gateway` `limit_req` and produced 503 responses. This is expected gateway protection, not an FSM failure.
- Kafka consumer group `astor-admin-events` returned to total lag 0 after the smoke run.
- Telegram admin chat has real API rate limits. A high-volume analytics burst produced `429 Too Many Requests` before throttling was added.
- `TelegramAdminNotifier` now serializes analytics delivery, waits between admin-chat messages and retries when Telegram returns `retry_after`.
- `AnalyticsKafkaConsumer` now marks events processed only after admin notification delivery succeeds.

Manual weekend testing should use the running Docker Compose app container and avoid IDEA-run Spring Boot unless debugging code locally.
