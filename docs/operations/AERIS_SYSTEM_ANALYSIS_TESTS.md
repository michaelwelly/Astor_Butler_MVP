# AERIS System Analysis Tests

Дата: 2026-08-05

## Цель

Проверить production/backend состояние одной командой и руками перед демо:

- backend `readiness/liveness`;
- runtime env без секретов;
- `/api/messages` для Telegram contact, table booking и RAG/AI Studio Agent reply;
- сохранение в PostgreSQL;
- `model_interaction_audit`, включая `usage` и примерную стоимость;
- semantic RAG: sources, chunks, embeddings;
- выгрузка состояния чатов для админа без удаления данных.

## Автоматический анализ production

Запуск с локальной машины, где есть SSH-доступ к VM:

```bash
node scripts/analyze_aeris_system.mjs \
  --base-url=http://51.250.31.97:8089 \
  --ssh-host=51.250.31.97 \
  --ssh-port=2222 \
  --ssh-key=/Users/michaelwelly/.ssh/astor_yandex_vm_ed25519 \
  --ssh-user=ubuntu \
  --dump-chats-file=auto
```

Ожидаем:

- `result.ok=true`;
- `health.readiness.payload.status=UP`;
- `checks.booking.actions` содержит `RESERVATION_CREATED` или продолжение booking flow;
- `checks.rag-agent.replyProvider=yandex-ai-studio-agent`;
- `checks.rag-agent.replyGenerated=true`;
- `checks.rag-agent.ragContextSize > 0`;
- `database.semantic.embeddings > 0`;
- `database.audit` содержит строки `LLM_UNDERSTANDING` и `MENU_ASSETS`;
- `auditCost` содержит token usage и оценку стоимости.

Если указан `--dump-chats-file=auto`, файл появится в `output/chat-state-<runId>.json`. По умолчанию тексты сообщений в этой выгрузке скрыты. Для внутреннего разбора можно явно добавить:

```bash
--include-message-text=true
```

## Автоматический анализ локального docker compose

Когда backend поднят локально на `8089`, а PostgreSQL доступен через docker:

```bash
BASE_URL=http://127.0.0.1:8089 \
PSQL_COMMAND="docker exec astor_postgres_test psql -U oracle -d aether" \
node scripts/analyze_aeris_system.mjs --dump-chats-file=auto
```

Если agent runtime локально не включен:

```bash
node scripts/analyze_aeris_system.mjs --expect-agent=false
```

## Ручной тест через Swagger

Открыть:

```text
http://51.250.31.97:8089/swagger-ui/index.html
```

Проверить `GET /actuator/health/readiness`.

Далее `POST /api/messages`.

Шаг 1. Contact/bootstrap:

```json
{
  "channel": "TELEGRAM",
  "chatId": 7781999001,
  "externalUserId": "7781999001",
  "firstName": "Manual",
  "username": "manual_smoke",
  "contactPhone": "+79000000000",
  "text": "",
  "correlationId": "manual-analysis-contact"
}
```

Шаг 2. Бронь:

```json
{
  "channel": "TELEGRAM",
  "chatId": 7781999001,
  "externalUserId": "7781999001",
  "firstName": "Manual",
  "username": "manual_smoke",
  "text": "Хочу забронировать стол завтра в 20:00 на двоих, тихий стол",
  "correlationId": "manual-analysis-booking"
}
```

Ожидание: `nextState=READY_FOR_DIALOG`, action `RESERVATION_CREATED` или controlled booking continuation.

Шаг 3. RAG + AI Studio Agent:

```json
{
  "channel": "TELEGRAM",
  "chatId": 7781999001,
  "externalUserId": "7781999001",
  "firstName": "Manual",
  "username": "manual_smoke",
  "text": "Покажи винную карту и подскажи шампанское для сабража",
  "correlationId": "manual-analysis-rag"
}
```

Ожидание:

- `metadata.replyProvider=yandex-ai-studio-agent`;
- `metadata.replyGenerated=true`;
- `metadata.ragContext` не пустой;
- ответ не обещает наличие/бронь без команды ресторана.

## SQL-проверка после ручного теста

На VM:

```bash
ssh -p 2222 -i /Users/michaelwelly/.ssh/astor_yandex_vm_ed25519 ubuntu@51.250.31.97
docker exec astor_postgres_test psql -U oracle -d aether
```

Audit:

```sql
SELECT
  correlation_id,
  provider,
  model,
  scenario,
  purpose,
  generated,
  fallback_used,
  success,
  latency_ms,
  metadata->'modelResponse'->>'agentId' AS agent_id,
  metadata->'modelResponse'->'usage' AS usage,
  left(response_text, 180) AS response_preview,
  created_at
FROM model_interaction_audit
WHERE correlation_id IN (
  'manual-analysis-booking',
  'manual-analysis-rag'
)
ORDER BY created_at DESC;
```

RAG:

```sql
SELECT
  s.source_code,
  s.title,
  count(c.chunk_id) AS chunks,
  count(e.chunk_id) AS embeddings
FROM semantic_sources s
LEFT JOIN semantic_chunks c ON c.source_id = s.source_id
LEFT JOIN semantic_embeddings e ON e.chunk_id = c.chunk_id
WHERE s.venue_code = 'AERIS' AND s.active
GROUP BY s.source_code, s.title
ORDER BY s.source_code;
```

Chats summary:

```sql
SELECT
  tp.chat_id,
  tp.telegram_user_id,
  tp.username,
  tp.first_name,
  tp.last_seen_at,
  count(tm.id) AS messages
FROM telegram_profiles tp
LEFT JOIN telegram_messages tm ON tm.chat_id = tp.chat_id
GROUP BY tp.chat_id, tp.telegram_user_id, tp.username, tp.first_name, tp.last_seen_at
ORDER BY tp.last_seen_at DESC
LIMIT 50;
```

## Reset boundaries

Автоматический анализ не удаляет данные. Если нужен чистый прогон для конкретного тестового пользователя, сначала dry-run:

```bash
scripts/reset_natalia_test_user.sh --dry-run --telegram-id <id> --chat-id <id>
```

Потом отдельным явным решением запускается reset без `--dry-run`. Admin user защищен: reset script откажется очищать `michael_welly/admin`.
