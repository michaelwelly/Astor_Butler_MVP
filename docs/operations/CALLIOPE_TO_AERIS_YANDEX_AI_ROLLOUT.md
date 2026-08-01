# Calliope To AERIS Yandex AI Rollout

Дата: 2026-07-23

## Решение

Для первого production-теста AERIS не создаем отдельного агента в Yandex AI Studio. Astor Butler уже имеет `YandexModelGateway`, который ходит напрямую в Yandex Foundation Models по `gpt://...` model URI через существующий `ModelGateway` contract.

Отдельный AI Studio Agent нужен позже, если мы переносим в Yandex managed agent runtime инструменты, thread/state, RAG/search index или отдельные агентские сценарии. Сейчас это хуже для управляемости: FSM должна оставаться single source of truth, а YandexGPT работает как слой понимания, черновиков и безопасного обогащения.

## Model Routing

| Назначение | Runtime profile | Модель | Режим |
| --- | --- | --- | --- |
| Frontline understanding | `ModelProfile.FRONTLINE` | `gpt://b1gug0tmrgmsq5pfsvhs/yandexgpt-5-lite/latest` | дешевые intent/slot JSON вызовы |
| Quality / complex | `ModelProfile.QUALITY` | `gpt://b1gug0tmrgmsq5pfsvhs/yandexgpt-5.1/latest` | сложные не-FSM вопросы, recovery, manager summaries |
| Speech realtime | отдельный future adapter | `gpt://b1gug0tmrgmsq5pfsvhs/speech-realtime-260528/latest` | не подключен текущим Java gateway, нужен отдельный speech path |

Стартовая политика:

- `Lite` включается первым для `GuestInputUnderstandingService`.
- `Pro 5.1` включается только для сложных non-FSM запросов и ручных тестов, чтобы не разгонять стоимость.
- `Scenario reply LLM` не включать массово, пока Telegram proxy не готов и не собран golden corpus.
- Любое действие с бронью, оплатой, отменой, переносом и подтверждением проходит только через FSM/domain services.

## Runtime Env

Секреты живут только на VM в `/opt/astor-butler/.env.production` или в secret vault. Не коммитить.

```bash
ASTOR_MODEL_PROVIDER=yandex
YANDEX_FOLDER_ID=b1gug0tmrgmsq5pfsvhs
YANDEX_API_KEY=<runtime-secret>
YANDEX_MODEL=gpt://b1gug0tmrgmsq5pfsvhs/yandexgpt-5-lite/latest
YANDEX_QUALITY_MODEL=gpt://b1gug0tmrgmsq5pfsvhs/yandexgpt-5.1/latest
YANDEX_MAX_TOKENS=256
YANDEX_TEMPERATURE=0.1

ASTOR_UNDERSTANDING_LLM_ENABLED=true
ASTOR_UNDERSTANDING_LLM_MIN_CONFIDENCE=0.70
ASTOR_UNDERSTANDING_LLM_TIMEOUT_MS=6500

# Keep false until external Telegram proxy is ready.
TELEGRAM_BOT_ENABLED=false
```

Для Smart Solution runtime использовать те же модели, но включать Q&A отдельно:

```bash
SMART_SOLUTION_MODEL_PROVIDER=yandex
SMART_SOLUTION_YANDEX_MODEL=gpt://b1gug0tmrgmsq5pfsvhs/yandexgpt-5-lite/latest
SMART_SOLUTION_YANDEX_QUALITY_MODEL=gpt://b1gug0tmrgmsq5pfsvhs/yandexgpt-5.1/latest
SMART_SOLUTION_GROUP_QA_LLM_ENABLED=true
```

## Economics

Официальные цены Yandex AI Studio для синхронных Model Gallery вызовов в России на 2026-07. Для рублевых аккаунтов Yandex Cloud цены указываются с НДС, но клиентский счет от Исполнителя должен отдельно учитывать налоговый режим Исполнителя.

| Модель | Input / 1000 tokens | Output / 1000 tokens |
| --- | ---: | ---: |
| YandexGPT Lite | 0.20 RUB | 0.20 RUB |
| YandexGPT Pro 5.1 | 0.80 RUB | 0.80 RUB |
| Speech Realtime 260528 | 0.10 RUB | 0.20 RUB |

Формула для одного вызова:

```text
cost = inputTokens / 1000 * inputRate + outputTokens / 1000 * outputRate
```

Примеры:

| Сценарий | Tokens | Стоимость |
| --- | --- | ---: |
| Lite understanding | 900 input + 120 output | 0.204 RUB |
| Pro complex answer | 2500 input + 700 output | 2.56 RUB |
| 100 guest messages/day, все Lite | 90k input + 12k output | 20.4 RUB/day |
| 1000 guest messages/day, все Lite | 900k input + 120k output | 204 RUB/day |
| 1000/day, 10% escalate to Pro | Lite 900 msgs + Pro 100 msgs | about 440 RUB/day |

### Per Guest Economics

Рабочая единица для ресторана - не одно сообщение, а один гостевой диалог. Для MVP считаем один диалог как 8 guest messages. Один Lite-вызов понимания: 900 input + 120 output = 0.204 RUB.

| Режим | Расчет | Себестоимость AI на 1 гостя | Если счет с НДС 22% |
| --- | --- | ---: | ---: |
| Lean | 8 Lite calls | 1.63 RUB | 1.99 RUB |
| Standard | 8 Lite calls + 10% Pro escalation | 3.68 RUB | 4.49 RUB |
| Heavy | 20 Lite calls + 10% Pro escalation | 9.20 RUB | 11.22 RUB |

### Monthly AI Economics

| Гостей/мес | Сообщений/мес | All Lite | Standard: 10% Pro escalation |
| ---: | ---: | ---: | ---: |
| 100 | 800 | 163 RUB | 368 RUB |
| 300 | 2400 | 490 RUB | 1104 RUB |
| 500 | 4000 | 816 RUB | 1840 RUB |
| 1000 | 8000 | 1632 RUB | 3680 RUB |
| 1500 | 12000 | 2448 RUB | 5520 RUB |

Практический бюджет на один ресторан уровня AERIS/ООО «Счастье»:

```text
AI token reserve: 5 000 RUB/month
AI stress reserve: 15 000 RUB/month
```

Такой reserve покрывает первые production-тесты с запасом, если Pro не включать на каждый ответ.

### Tokenization

- Yandex AI Studio считает usage в input/output tokens и возвращает `usage` в ответе completion API.
- Tokenizer calls в AI Studio бесплатны; токенизация нужна для budget guard и prompt-trimming, а не как отдельная статья расхода.
- Production правило: сохранять `inputTextTokens`, `completionTokens`, `totalTokens`, `modelUri`, `scenario`, `state`, `correlationId` в model audit.
- Budget guard target:
  - warning at 50% monthly token reserve;
  - alert at 80%;
  - Pro escalation off or manual approval after 100%.

Оптимизация:

- для FSM intent/slot prompts держать `maxTokens=128-256`;
- просить строгий JSON без длинных объяснений;
- Pro использовать только при `confidence < threshold`, complex concierge и manager-facing summaries;
- логировать provider/model/usage/latency для реальной unit-economy после первых 100-500 запросов.

## Current Infra Estimate

Текущая VM `astor-butler-aeris-mvp`:

- platform: `standard-v3`;
- vCPU: 4 x 100%;
- RAM: 16 GB;
- boot disk: 200 GB `network-ssd`;
- public IPv4: `51.250.31.97`;
- NAT gateway: отсутствует;
- Docker containers: оплачиваются как ресурсы этой VM, отдельно не биллятся.

Оценка за 30 дней при постоянном `RUNNING`:

| Ресурс | Расчет | Оценка |
| --- | --- | ---: |
| vCPU | `720h * 4 * 1.24 RUB` | 3571.20 RUB |
| RAM | `720h * 16 * 0.33 RUB` | 3801.60 RUB |
| Public IP | `720h * 0.26352 RUB` | 189.73 RUB |
| Disk network-ssd | `720h * 200 * 0.0229 RUB` | 3297.60 RUB |
| Итого без трафика |  | about 10860 RUB/month |

Исходящий трафик: первые 100 GB/month бесплатны. Security Groups бесплатны. NAT Gateway сейчас удален, поэтому постоянной NAT-стоимости нет.

Если эту инфраструктуру перевыставлять клиенту отдельной строкой со ставкой НДС 22%, коммерческий gross будет:

```text
10860 RUB + 22% VAT = 13249.20 RUB/month
```

Если инфраструктура оплачивается напрямую из Yandex Cloud аккаунта клиента в RUB, Yandex Cloud pricing уже указывает цены с НДС для RUB/KZT.

## SABY / Auth Integration

Цель: AERIS создает локальную бронь через FSM, а интеграция с SABY/SBIS становится external reservation port.

Repository discovery on 2026-08-01:

- spelling in project memory/docs is `SABY/SBIS`, not confirmed `Sabby`;
- current backend already has the local booking stack:
  - `TableBookingScenario`;
  - `TableReservationService`;
  - `TableReservationRepository`;
  - `table_reservation_orders`;
  - `table_reservation_holds`;
  - REST endpoints under `/api/bookings/**`;
  - hostess Telegram confirmation via `TableReservationNotificationService`;
  - `sbis_external_id` column for future external sync;
- no implemented `SabyReservationProvider`, `SbisTableAvailabilityAdapter`, HTTP client or runtime env binding was found in `src/main/java`;
- do not design from scratch before reading the actual SABY/SBIS API contract and representative guidance from the user's correspondence.

Grounded capability map for the future adapter:

| Voice / Telegram use case | Existing local capability | External SABY/SBIS gap | Guardrail |
| --- | --- | --- | --- |
| Availability query: "есть стол завтра в 20:00 на двоих?" | `GET /api/bookings/tables/availability`, local holds/orders | map venue/zone/table/time to provider availability API | read-only, no booking side effects |
| Find table: "тихий стол у окна" | `TableBookingDraftMerger`, seating model, local table zones | provider table/area vocabulary and availability status | show candidate, ask guest to confirm |
| Reserve | `TableReservationService.createReservation` creates local order + hold and asks hostess | create external reservation and store `sbis_external_id`/sync status | confirmation-gated, idempotency key from local order id |
| Modify | `TableReservationService.changeByGuest` and `ChangeCancelScenario` | external update endpoint and conflict policy | explicit guest confirmation and hostess/operator confirmation where needed |
| Cancel | `TableReservationService.cancelByGuest` | external cancel endpoint | explicit confirmation, idempotent cancel by external id |
| Confirmation | hostess buttons and local status lifecycle | provider confirmation/status reconciliation | local FSM remains source of truth; external status is synchronized, not allowed to bypass FSM |
| Handoff / limitations | admin/hostess cards | provider errors/rate limits/outages | graceful fallback to local hold + human operator |

Implementation path:

1. Создать порт `ExternalReservationProvider`:
   - `checkAvailability(request)`;
   - `createReservation(order)`;
   - `cancelReservation(externalId)`;
   - `confirmReservation(externalId)`;
   - `health()`.
2. Добавить adapter `SabyReservationProvider`.
3. Секреты SABY держать только в runtime env:
   - `SABY_API_BASE_URL`;
   - `SABY_CLIENT_ID`;
   - `SABY_CLIENT_SECRET`;
   - `SABY_REFRESH_TOKEN` или другой approved credential;
   - `SABY_ORGANIZATION_ID`;
   - `SABY_RESTAURANT_ID`.
4. Авторизацию делать в adapter layer, не в FSM.
5. Локальный `table_reservation_orders` остается source of truth для MVP; external id и sync status сохранять рядом с order.

Open inputs before implementation:

- exact API vendor spelling and product name;
- official base URL and API version;
- authentication flow;
- required organization/restaurant identifiers;
- availability, create, update/cancel, status and webhook/polling endpoints;
- idempotency support;
- rate limits and retry guidance;
- representative instructions from Yandex Mail correspondence.

Do not send external messages or create/cancel real bookings without explicit final user approval.

## Yandex SpeechKit STT/TTS Integration Roadmap

Goal: Telegram/site voice -> STT transcript -> safe text normalization/entity extraction -> existing `GuestInputUnderstandingService`/FSM/AI pipelines -> confirmation-gated booking through local/SABY provider. For the C3AG website, the final textual Clio reply may optionally be converted to speech by TTS after the text response exists.

Current code boundary:

- `TelegramRouter` captures VOICE/AUDIO metadata and calls `TelegramVoiceTranscriptionService.enrich(...)`;
- `TelegramVoiceTranscriptionService` downloads Telegram audio, uploads a short-lived object via `ObjectStorageService.uploadTelegramVoice(...)`, calls `SpeechToTextService`, then forwards canonical text into the normal `MessageGatewayService` path;
- current `SpeechToTextService` implementation is `ExternalCommandSpeechToTextService` with env:
  - `ASTOR_STT_ENABLED`;
  - `ASTOR_STT_COMMAND`;
  - `ASTOR_STT_TIMEOUT_SECONDS`;
  - `ASTOR_STT_RETRY_TTL_SECONDS`;
  - `ASTOR_STT_KEEP_LOCAL_FILES`;
  - `S3_VOICE_TTL_DAYS`;
- `ModelGateway` handles text/embedding/vision, but SpeechKit STT should remain a separate STT adapter, not an LLM intent/router.
- Clio website voice UI is feature-gated. Its local test-double endpoints are `/api/chat/transcribe` and `/api/chat/speak`; both are mocks until server-side SpeechKit credentials and HTTPS are configured.

Official Yandex SpeechKit facts checked on 2026-08-01:

- service URL for STT API v3 is `stt.api.cloud.yandex.net`;
- v3 has streaming `Recognizer` and asynchronous `AsyncRecognizer`;
- supported recognition formats include LPCM, OggOpus and MP3;
- synchronous recognition is limited to 1 MB, 30 seconds, one channel;
- streaming recognition is limited to 5 minutes, 10 MB, one channel;
- asynchronous recognition supports up to 60 MB request body or 1 GB via Object Storage, up to 4 hours, with results retained by Yandex for 3 days;
- authentication uses an IAM token or API key for a service account with SpeechKit permissions.
- Yandex ID/OAuth docs checked on 2026-08-01: Yandex ID authorization is OAuth-based; app registration is required before tokens; user permissions are scoped and revocable; Yandex recommends requesting only permissions the app cannot function without.

Implementation plan:

1. Add `YandexSpeechKitSpeechToTextService` behind existing `SpeechToTextService`.
2. Add env-only config:
   - `ASTOR_STT_PROVIDER=external-command|yandex-speechkit`;
   - `YANDEX_SPEECHKIT_API_KEY` or IAM-token based service account flow;
   - `YANDEX_FOLDER_ID`;
   - `YANDEX_SPEECHKIT_STT_ENDPOINT=stt.api.cloud.yandex.net`;
   - `YANDEX_SPEECHKIT_TTS_ENDPOINT`;
   - `YANDEX_SPEECHKIT_TTS_VOICE`, selected from supported built-in/authorized Yandex voices as a feminine voice for Clio;
   - `YANDEX_SPEECHKIT_MODE=streaming|async`;
   - conservative connect/read deadlines and max audio duration/size.
3. Prefer streaming for short Telegram/site voice notes and async/Object Storage for longer uploads.
4. Keep local raw audio retention short (`S3_VOICE_TTL_DAYS`, default 3) and delete temp files unless diagnostics explicitly require retention.
5. Add retry with bounded backoff, circuit breaker and bulkhead around SpeechKit STT/TTS calls.
6. On STT failure, keep FSM state unchanged and ask the guest to repeat or type the message; do not infer booking actions from failed/low-confidence transcripts.
7. On TTS failure, keep the text reply as source of truth and show a non-blocking voice fallback.
8. Add metrics: request count, latency, success/failure reason, transcript length, provider mode, timeout/circuit state; never log secrets or full private audio.
9. Tests:
   - adapter unit tests with mocked SpeechKit responses;
   - timeout/error fallback tests;
   - Telegram voice normalization test proving transcript enters the same `MessageGatewayService` path as text;
   - website voice tests for permission denied, STT unavailable, recognized text, TTS unavailable;
   - booking safety tests proving reserve/modify/cancel remain confirmation-gated and idempotent.

No real SpeechKit keys or paid external calls should be used without explicit approval.

Production enablement blockers for website voice:

- public HTTPS for the C3AG site, because browser microphone capture is not available on plain HTTP except localhost;
- server-side SpeechKit STT/TTS credentials and billing approval;
- verified Yandex/Telegram networking from the VM;
- privacy/legal copy for audio handling and retention;
- load/smoke test with test flags before any paid external calls.

## Real Booking Smoke Test

REST gateway для первого теста без Telegram:

```bash
curl -s http://51.250.31.97:8089/api/messages \
  -H 'Content-Type: application/json' \
  -d '{
    "channel":"TELEGRAM",
    "chatId":777001,
    "externalUserId":"777001",
    "text":"Хочу забронировать стол завтра в 20:00 на двоих, тихий стол в винной комнате",
    "firstName":"Smoke"
  }'
```

Expected:

- FSM routes to `TABLE_BOOKING`;
- LLM may enrich intent/slots only if enabled;
- missing slots are requested by FSM;
- order is created only after mandatory date/time/party fields;
- hostess/staff confirmation remains human-controlled;
- response and timeline show model provider, model URI and confidence where available.

Telegram production smoke after German proxy:

1. Add proxy env to VM.
2. Set `TELEGRAM_BOT_ENABLED=true`.
3. Restart `aeris_astor_butler_bot`.
4. Send `/start`.
5. Complete consent/contact.
6. Run table booking phrase.
7. Verify staff/hostess confirmation card.
8. Confirm/reject through buttons.
9. Verify guest receives final status.

### VM Smoke Result 2026-07-30

Backend production-like smoke on VM `51.250.31.97` is green with Telegram mocked:

- Docker Compose rebuilt `aeris_astor_butler_bot` successfully.
- Health/readiness are green.
- Swagger/OpenAPI is available with 117 paths.
- YandexGPT Lite completion works through runtime `YANDEX_API_KEY`.
- `/api/messages` contact + booking smoke created reservation order `#1`.
- PostgreSQL confirmed saved profile, contact event, booking text and reservation.
- Smart Solution Ops project `RESTO` was moved to `READY_TO_LAUNCH`, progress `85%`.

Current block for live Telegram:

- Yandex VM IPs still cannot reliably reach Telegram API directly.
- Keep `TELEGRAM_BOT_ENABLED=false`.
- Enable Telegram only after external Germany proxy is tested against `https://api.telegram.org`.

## German Proxy Dependency

Ask Egor for one stable endpoint that can open `https://api.telegram.org` from his Germany server:

```text
Нужен маленький SOCKS5 или HTTP CONNECT proxy на сервере в Германии для Telegram Bot API.
Проверка с сервера должна проходить:
curl -I --proxy <proxy-url> https://api.telegram.org

Нужно прислать:
- host;
- port;
- protocol: socks5 или http;
- username/password или allowlist нашего Yandex VM IP 51.250.31.97;
- ограничение доступа только на api.telegram.org желательно.
```

После получения прокси подключаем env:

```bash
TELEGRAM_PROXY_TYPE=socks5
TELEGRAM_PROXY_HOST=<proxy-host>
TELEGRAM_PROXY_PORT=<proxy-port>
TELEGRAM_PROXY_USERNAME=<proxy-user>
TELEGRAM_PROXY_PASSWORD=<proxy-password>
TELEGRAM_BOT_ENABLED=true
```

Если текущий Telegram adapter не поддерживает proxy env, добавить proxy wiring в `TelegramBotConfig`.

## Implementation Checklist

- [x] Подтвердить, что YandexGPT можно вызвать напрямую без AI Studio Agent.
- [x] Зафиксировать model routing Lite/Pro/Speech.
- [x] Посчитать первичную token economics.
- [x] Посчитать текущую VM infra estimate.
- [x] Включить Yandex runtime env на VM.
- [x] Сделать probe Yandex completion.
- [x] Включить LLM understanding на AERIS runtime.
- [x] Добавить/проверить Telegram proxy support.
- [ ] Получить German proxy credentials.
- [ ] Включить `TELEGRAM_BOT_ENABLED=true` после proxy smoke.
- [ ] Подключить SABY/SBIS external reservation port.
- [ ] Включить Telegram polling.
- [ ] Прогнать реальную бронь стола.
- [ ] Спроектировать и реализовать SABY adapter.

## Sources

- Yandex AI Studio SDK: https://github.com/yandex-cloud/yandex-ai-studio-sdk
- Yandex AI Studio pricing: https://yandex.cloud/ru/docs/foundation-models/pricing
- Compute Cloud pricing: https://yandex.cloud/ru/docs/compute/pricing
- VPC pricing: https://yandex.cloud/ru/docs/vpc/pricing
- Data Processing pricing example for `network-ssd` disk rate: https://yandex.cloud/ru/docs/data-proc/pricing
