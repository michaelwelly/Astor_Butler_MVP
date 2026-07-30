# AERIS User Mode Runtime Config

Дата: 2026-07-23

Этот конфиг включает AERIS в пользовательский режим: REST/FSM gateway работает, YandexGPT помогает пониманию, Telegram включается только после рабочего external proxy.

## VM Runtime

Файл на сервере:

```text
/opt/astor-butler/.env.production
```

Секреты не коммитить.

```bash
SPRING_PROFILES_ACTIVE=prod

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
ASTOR_SCENARIO_REPLY_LLM_ENABLED=false

TELEGRAM_BOT_ENABLED=false
TELEGRAM_PROXY_TYPE=NO_PROXY
TELEGRAM_PROXY_HOST=
TELEGRAM_PROXY_PORT=0
```

## German Proxy Mode

Когда proxy от Егора готов:

```bash
TELEGRAM_PROXY_TYPE=SOCKS5
TELEGRAM_PROXY_HOST=<germany-proxy-host>
TELEGRAM_PROXY_PORT=<germany-proxy-port>
TELEGRAM_BOT_ENABLED=true
```

Для текущей TelegramBots 6.8 поддержаны `NO_PROXY`, `HTTP`, `SOCKS4`, `SOCKS5` через host/port. Username/password в этом adapter пока не подключены, поэтому предпочтительный режим для немецкого сервера:

- firewall/allowlist только с VM IP `51.250.31.97`;
- egress proxy разрешает только `api.telegram.org:443`;
- наружу proxy port не открыт для всего интернета.

## Deploy

```bash
cd /opt/astor-butler
docker compose --env-file .env.production \
  -f docker-compose.yml \
  -f docker-compose.prod.yml \
  up -d --build aeris-astor-butler-bot
```

## Checks

```bash
curl -s http://127.0.0.1:8089/actuator/health

curl -s http://127.0.0.1:8089/api/messages \
  -H 'Content-Type: application/json' \
  -d '{
    "channel":"TELEGRAM",
    "chatId":777001,
    "externalUserId":"777001",
    "text":"Хочу забронировать стол завтра в 20:00 на двоих",
    "firstName":"Smoke"
  }'
```

Expected first-time user result: consent/contact request. After contact is captured, booking should route to `TABLE_BOOKING`.

## Production Smoke 2026-07-30

VM:

```text
ssh -i ~/.ssh/astor_yandex_vm_ed25519 -p 2222 ubuntu@51.250.31.97
```

Runtime status:

- `aeris_astor_butler_bot` rebuilt and healthy.
- `TELEGRAM_BOT_ENABLED=false` until external proxy is ready.
- `ASTOR_MODEL_PROVIDER=yandex`.
- `ASTOR_UNDERSTANDING_LLM_ENABLED=true`.
- `ASTOR_SCENARIO_REPLY_LLM_ENABLED=false`.
- `/actuator/health`: `UP`.
- `/api/system/readiness`: `ready=true`.
- `/v3/api-docs`: 117 paths.

YandexGPT Lite probe:

- model: `gpt://b1gug0tmrgmsq5pfsvhs/yandexgpt-5-lite/latest`;
- duration: about 1.3s;
- usage: 129 input tokens + 71 completion tokens = 200 total tokens;
- result returned strict JSON with `TABLE_BOOKING`, `date=завтра`, `time=20:00`, `partySize=2`, `seatingPreference=тихий стол в винной комнате`.

REST booking smoke:

- contact captured for smoke chat `777033`;
- booking phrase processed through `/api/messages`;
- response returned `RESERVATION_CREATED`, `WAIT_HOSTESS_CONFIRMATION`, `RETURN_MAIN_MENU`;
- `table_reservation_orders` created order `#1`;
- PostgreSQL persisted profile, contact, incoming contact event and booking text.

Regression fixed during smoke:

- composite route `TABLE_BOOKING + MENU_ASSETS` now passes `UnderstoodInput` into `TableBookingScenario`;
- explicit `time` slot from understanding is no longer dropped when the same text also looks like a table/zone selection.
