# AERIS Telegram Connectors Status

Дата: 2026-08-02.

## Production runtime

- Telegram bot uses the scoped HTTP proxy at `10.233.200.2:8888` for Telegram API egress.
- `TELEGRAM_BOT_ENABLED=true`.
- `AERIS_ASTOR_BUTLER_BOT_ENABLED=true`.
- `ASTOR_MODEL_PROVIDER=yandex`.
- `ASTOR_UNDERSTANDING_LLM_ENABLED=true`.
- `ASTOR_SCENARIO_REPLY_LLM_ENABLED=false`.
- `ASTOR_STT_ENABLED=false`.
- `ASTOR_SABY_ENABLED=false`.

Secrets, API keys, bot token and WireGuard config are runtime-only and must not be committed.

## Controlled Yandex LLM smoke

Read-only provider smoke was run from the production VM with the configured Yandex API key and model.

Result:

- HTTP status: `200`.
- Usage: `inputTextTokens=109`, `completionTokens=68`, `totalTokens=177`.
- The model returned a structured JSON-like intent reply for an AERIS menu/booking prompt.

Decision:

- Keep `ASTOR_UNDERSTANDING_LLM_ENABLED=true` for intent understanding.
- Keep `ASTOR_SCENARIO_REPLY_LLM_ENABLED=false` until tone/corpus prompts are tightened; deterministic menu, video, booking and manager-help flows must remain the action source.

## AI Studio Agent adapter

Update 2026-08-05:

- Added provider `ASTOR_MODEL_PROVIDER=yandex-agent`.
- Free-form text calls use Yandex Responses API with `YANDEX_AGENT_ID=fvt18kmmnas336paia3g`.
- JSON understanding calls continue to use direct Foundation Models completion with `jsonObject=true`, so booking/menu/payment FSM routing remains stable.
- RAG query/document embeddings continue to use Yandex Foundation Models `textEmbedding` from the same adapter, so semantic search is not disabled by the agent switch.
- Probe command: `node scripts/probe_yandex_agent.mjs --agent-id=fvt18kmmnas336paia3g`.
- `fvt18kmmnas336paia3g` must be sent as Responses API `prompt.id`; using it as `model` returns `invalid_value`.

## Telegram guest keyboard

Primary guest keyboard labels:

- `Меню кухни`
- `Бар`
- `Коктейли`
- `Винная карта`
- `Видео-тур`
- `Бронь стола`
- `Связаться с командой`
- `Главное меню`

These labels route through existing FSM scenarios and do not bypass the conversation state.

## Voice status

Current production state:

- voice messages can be received as Telegram media;
- STT is disabled;
- text fallback is explicit and does not claim live voice understanding;
- safety limits are configured before download/STT:
  - `TELEGRAM_VOICE_MAX_DURATION_SECONDS=60`
  - `TELEGRAM_VOICE_MAX_FILE_SIZE_BYTES=10485760`

To enable real Yandex SpeechKit STT, configure server-side credentials and a tested STT adapter/command via secret storage, then run one controlled smoke before enabling production voice routing.

## Saby status

Local repo, Obsidian memory and non-secret production runtime config were inspected for a real Saby contract and credentials.

Found:

- vendor spelling is `Saby`, historically `СБИС/sbis`;
- docs describe Saby Presto and the desired external reservation provider boundary;
- no implemented real Saby HTTP client was present before this batch;
- no non-secret production runtime config for Saby was present.

Not found:

- official reservation API contract/endpoints;
- production or sandbox base URL;
- auth method;
- credentials;
- organization id;
- restaurant id;
- availability endpoint/path;
- reservation endpoint/path;
- rate limits/tariffs;
- representative guidance for booking semantics.

Implemented boundary:

- compile-ready Saby provider skeleton;
- disabled by default;
- no network calls;
- availability/write methods fail closed with structured `PROVIDER_NOT_CONFIGURED` or `PROVIDER_CONTRACT_NOT_IMPLEMENTED`.

Exact request to Saby/restaurant representative:

```text
Нужен официальный API-контракт Saby для ресторанных бронирований столов:
1. sandbox/prod base URL и доступный тестовый контур;
2. способ авторизации (OAuth/API token/etc.) и инструкция по выпуску credentials;
3. organization/company id и restaurant/venue id;
4. read-only метод проверки доступности столов/зон по дате, времени, длительности и размеру компании;
5. метод создания заявки/брони, обязательные поля payload и формат idempotency key;
6. методы изменения, отмены и получения статуса брони;
7. webhook/callback contract, если Saby отправляет обновления статуса;
8. rate limits, retry policy, timeout expectations and error codes;
9. перечень персональных данных, разрешенных к передаче, и правовая роль Saby в обработке;
10. confirmation workflow: может ли бронь подтверждаться автоматически или требуется подтверждение хостес/оператора.
```
