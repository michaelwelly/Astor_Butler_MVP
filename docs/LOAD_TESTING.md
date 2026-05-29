# Local Load Testing

This project keeps the backend as a local Spring Boot process and runs the infrastructure in Docker Compose.

## Local environment

1. Copy `.env.local.example` to `.env.local`.
2. Adjust passwords or ports if needed.
3. Check `ASTOR_JAVA_HOME` points to a Java 21 installation.
4. Start infrastructure:

```bash
docker compose up -d postgres redis ollama
```

5. Start the backend locally:

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

The first load profile is intentionally small. It validates the local path before heavier scenarios are added for users, bookings, media, timeline and notifications.
