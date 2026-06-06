# Local Load Testing

This project can run either with the backend as a local Spring Boot process or as the `app` container inside Docker Compose.

## Local environment

1. Make sure local `.env` exists and contains database/service credentials.
2. Check `ASTOR_JAVA_HOME` points to a Java 21 installation.
3. Start infrastructure:

```bash
docker compose up -d postgres redis mongo kafka minio minio-init prometheus grafana
```

4. Start the backend locally:

```bash
scripts/run_local_app.sh
```

Swagger UI should be available at:

```text
http://localhost:8080/swagger-ui/index.html
```

## Smoke test

```bash
scripts/run_k6_smoke.sh
```

The smoke scenario checks:

- `/api/system/ping`
- `/api/system/readiness`
- `/actuator/health`
- `/v3/api-docs`

## Read API load test

```bash
scripts/run_k6_read_load.sh
```

The first load scenario is intentionally small. It validates the local path before heavier scenarios are added for users, bookings, media, timeline and notifications.

## Weekend guest FSM matrix

For long manual/end-to-end weekend checks, run the full container stack and then execute the guest matrix:

```bash
scripts/start_container_stack.sh
docker run --rm \
  -e K6_BASE_URL=http://host.docker.internal:8080 \
  -v "$PWD/k6:/scripts:ro" \
  grafana/k6 run /scripts/weekend-guest-scenarios.js
```

The matrix simulates 9 different guests through the shared `/api/messages` gateway:

- first touch
- contact share
- booking request
- menu request
- event poster request
- charity/art purchase request
- auction request
- art mediation request
- manager handoff
- voice-like payload placeholder

This is an API-level FSM and observability test. It does not replace a real Telegram update test, but it exercises the same `MessageGatewayService`, FSM persistence, outbox/Kafka path and admin-chat observability path.

## Container LLM pool

The weekend container stack starts 3 Ollama workers behind an nginx load balancer:

- `ollama-1`
- `ollama-2`
- `ollama-3`
- `llm-gateway`

The application talks to:

```text
http://llm-gateway:11434
```

The host can check the load balancer at:

```bash
curl http://localhost:11434/api/tags
```

Kafka remains the domain/event backbone. The LLM pool is synchronous request-response infrastructure and does not consume Kafka directly yet.
