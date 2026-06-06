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
  - `astor_ollama_3`
  - `astor_llm_gateway`

## Verified

- `mvn test`: 43 tests, 0 failures.
- Docker health:
  - `astor_app`: healthy
  - `astor_api_gateway`: healthy
  - `astor_llm_gateway`: healthy
  - `astor_ollama_1/2/3`: healthy
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
