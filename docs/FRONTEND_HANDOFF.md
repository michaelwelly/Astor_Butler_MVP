# Frontend Handoff

## Message To The Developer

Привет. Работаем спокойно и через ветки. `main` не трогаем напрямую: ты берешь актуальный `main`, создаешь свою frontend-ветку, делаешь результат, пушишь ветку и открываешь Pull Request. Я смотрю и мержу в `main`, когда результат подходит.

Стартовый процесс:

```bash
git checkout main
git pull origin main
git checkout -b frontend/c3flex-media-start
```

Frontend живет в `frontend/`. Dev server должен быть на `http://localhost:3001`, потому что Grafana занимает `3000`.

Для локального backend/media окружения используй файл `.env.frontend`, который владелец проекта передаст отдельно. В Git он не коммитится.

```bash
docker compose --env-file .env.frontend up -d postgres redis mongo kafka minio minio-init prometheus grafana
scripts/run_local_app.sh .env.frontend
```

Этот env специально ставит `TELEGRAM_BOT_ENABLED=false`, чтобы второй разработчик не запускал Telegram long polling с тем же ботом и не ломал живой тест.

Полезные адреса:

- Frontend: `http://localhost:3001`
- Swagger: `http://localhost:8088/swagger-ui/index.html`
- MinIO Console: `http://localhost:9001`
- MinIO media base URL: `http://localhost:9000/astor-media`

MinIO credentials для локалки:

```text
S3_ACCESS_KEY=astor
S3_SECRET_KEY=astor_minio_password
```

## Message To Frontend Codex

You are working in `Astor_Butler_MVP`.

Rules:

- Never commit directly to `main`.
- Start from `main`, pull latest changes, then create a feature branch.
- Recommended branch: `frontend/c3flex-media-start`.
- Do not commit `.env`, media files, `node_modules`, `.next`, build artifacts or local IDE files.
- Use `.env.frontend` for local backend/media startup.
- Telegram bot must remain disabled in frontend/local work: `TELEGRAM_BOT_ENABLED=false`.

Frontend task:

1. Build C3FLEX media start workflow.
2. Use local frontend env:

```bash
NEXT_PUBLIC_API_BASE_URL=http://localhost:8088
NEXT_PUBLIC_MEDIA_BASE_URL=http://localhost:9000/astor-media
```

3. Use 10 sample videos from local MinIO bucket `astor-media`.
4. Do not connect frontend directly to Mongo at runtime.
5. For temporary visual MVP, direct media URLs may be built as:

```text
{NEXT_PUBLIC_MEDIA_BASE_URL}/{objectKey}
```

Example:

```text
http://localhost:9000/astor-media/raw/AI/TANGIERS.mp4
```

6. Metadata lives in Mongo:

```text
database: aether
collection: media_sample_assets
```

Use Mongo/MinIO only as local development sources. The production direction is:

```text
Frontend -> Backend Media API -> S3/MinIO public URL
```

Current visual categories:

- Event Stories
- Reels / Product Content
- Commercials

Read before implementation:

- `docs/C3AG_FRONTEND_TZ.md`
- `docs/MEDIA_PIPELINE.md`
- `docs/API_CONTRACT.md`
- `docs/ARCHITECTURE.md`

Expected PR:

- Branch contains frontend code/config only.
- PR explains selected media mapping, local run steps and known temporary direct-MinIO assumptions.
- PR does not include videos or secrets.
