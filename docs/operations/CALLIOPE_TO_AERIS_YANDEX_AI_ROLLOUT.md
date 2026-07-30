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
