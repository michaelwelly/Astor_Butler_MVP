# Local Run

## IntelliJ run from macOS host

The Docker Compose service name `postgres` is available only inside the Docker network. When the application is started directly from IntelliJ on macOS, use localhost and the exposed Compose ports.

Set these environment variables in the IntelliJ run configuration:

```text
POSTGRES_URL=jdbc:postgresql://localhost:5434/<POSTGRES_DB>
REDIS_HOST=localhost
REDIS_PORT=6379
```

Keep `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`, `REDIS_PASSWORD`, `TELEGRAM_BOT_TOKEN`, and other secrets in the local `.env` file.

## Why overrides work

`AstorButlerApplication` loads `.env` only as a fallback. Values already provided through environment variables or JVM system properties are not overwritten.
