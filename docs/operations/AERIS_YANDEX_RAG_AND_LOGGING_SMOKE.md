# AERIS Yandex RAG And Logging Smoke

## Purpose

Production AERIS uses API providers for LLM and embeddings. Vectors are stored and searched only in PostgreSQL `pgvector`.

## YandexGPT reply probe

Run from an environment that already has `YANDEX_FOLDER_ID` and either `YANDEX_API_KEY` or `YANDEX_IAM_TOKEN`.

```bash
node scripts/probe_yandex_reply.mjs \
  --prompt="Ты Astor, цифровой дворецкий AERIS. Ответь где находится ресторан и чем ты помогаешь."
```

Expected:

- `ok=true`;
- non-empty `text`;
- `modelUri=gpt://...`;
- `usage` present when returned by Yandex.

## AI Studio Agent probe

Use this when `ASTOR_MODEL_PROVIDER=yandex-agent` or before switching a runtime to the agent adapter.

```bash
node scripts/probe_yandex_agent.mjs \
  --agent-id=fvt18kmmnas336paia3g \
  --prompt="Ты Astor Butler. Ответь гостю AERIS коротко и по делу."
```

Expected:

- `ok=true`;
- `responseStatus=completed` or a controlled `incomplete` with `incompleteDetails.reason=max_output_tokens`;
- non-empty `text`;
- `usage` present when returned by Yandex Responses API.

Important: `fvt18kmmnas336paia3g` is a saved AI Studio prompt/agent ID for the Responses API `prompt.id` field, not a `model` URI.

## Backend logging smoke

Run against a live backend with DB access available through either `DATABASE_URL` or `PSQL_COMMAND`.

```bash
BASE_URL=http://127.0.0.1:8089 \
PSQL_COMMAND="docker exec astor_postgres_test psql -U <user> -d <db>" \
node scripts/smoke_system_logging.mjs
```

Expected:

- `/api/messages` returns HTTP 2xx;
- synthetic correlation IDs are persisted;
- `model_interaction_audit` has at least one row when scenario LLM replies are enabled;
- no secrets are printed.

## RAG runtime checks

PostgreSQL should show:

- extension `vector`;
- AERIS semantic sources and chunks;
- embeddings for active AERIS chunks;
- `embedding_model` matching `emb://<folder>/text-search-doc/latest`;
- direct pgvector retrieval query returns AERIS chunks.

## Boundaries

- Do not start local Ollama for production RAG.
- Do not expose PostgreSQL or vector search publicly.
- Do not enable Instagram/media analysis or review posting without explicit user consent, supported official connector, token storage, preview and final confirmation.
