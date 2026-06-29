# Current Status

## Update 2026-06-08

### Текущий backend baseline

Актуальный `main` уже ушел дальше статуса от 2026-06-04:

- Docker weekend stand собран: `app`, `api-gateway`, Kafka/Redpanda, PostgreSQL, Redis, MongoDB, MinIO, Prometheus/Grafana.
- LLM dev-контур поднят как 3 Ollama контейнера за `llm-gateway`.
- Kafka `astor.user.events` остается backbone topic; admin chat получает human-readable projection.
- Admin analytics delivery стабилизирован: Telegram projection сериализуется, учитывает `429 retry_after`, Kafka event считается processed только после успешной доставки.
- Deterministic k6 baseline зеленый: 9 fresh guests и paced 45 guests проходят FSM без падений; резкий burst упирается в nginx rate limit как ожидаемая защита.
- `TableBookingScenario` уже умеет частичный ручной flow с Redis draft: дата, время, гости, отправка AERIS plan, выбор стола, заявка хостес с кнопками.
- В рабочей копии 2026-06-08 есть незакоммиченные изменения в `TableBookingScenario`, `TableBookingDraftStorage`, `TableBookingScenarioTest`: они улучшают накопление date/time/party между отдельными сообщениями и должны быть внимательно дочитаны перед новым коммитом.

### Preview issue

Telegram preview хранится через `telegram_profiles.preview_message_id` + `preview_version`.
Если тестовый гость Наталья уже видел preview, система не шлет его повторно, даже если пользователь удалил/не видит старое сообщение. Для ручных тестов нужен reset тестового профиля или отдельная политика resend-on-start.

Добавлен/планируется локальный reset helper:

```text
/Users/michaelwelly/IdeaProjects/Astor_Butler_MVP/scripts/reset_natalia_test_user.sh
```

Он должен чистить PostgreSQL записи Натальи и Redis keys:

- `astor:fsm:telegram:1773317437:state`
- `astor:booking:table:draft:telegram:1773317437`

### Что проверять перед ручным тестом

1. Запустить reset Натальи.
2. Проверить, что `astor_app`/Spring Boot запущен ровно в одном экземпляре.
3. Отправить `/start` от Натальи.
4. Убедиться, что preview снова приходит.
5. Пройти first-touch/contact.
6. Пройти table booking: дата -> время -> гости -> план зала -> выбор стола -> кнопки хостес.
7. Проверить admin analytics chat и Kafka lag.

### FSM spec update 2026-06-08

`docs/fsm/FSM_SCENARIOS.md` и `docs/FSM_SCENARIOS_VIEWER.html` обновлены как source-of-truth перед кодом:

- `/start` трактуется как safe restart: сбросить активный runtime-сценарий, не удалять durable facts, заново отправить/pin persistent AERIS preview.
- Если контакт/consent уже есть, `/start` возвращает гостя в `READY_FOR_DIALOG`, а не просит контакт повторно.
- Voice/audio вход должен нормализоваться на уровне transport/intake adapter: Telegram voice -> transcript -> canonical incoming message -> обычный FSM path.
- `MenuAssetsScenario` сфокусирован на 4 актуальных PDF: кухня, бар, elements/коктейли, вино.
- Menu RAG проектируется как shared retrieval/index слой, общий для всех локальных LLM-инстансов.
- `QuietGuideScenario` расширен видео-туром интерьера и approved concept copy про гастрономическую экспедицию Георгия Матвеева в AERIS.
- `INTERIOR.mp4` не должен попадать в git/jar; целевой runtime - MinIO object `content/aeris/interior/INTERIOR.mp4` + manifest/metadata в проекте.

### Implementation update 2026-06-08

- Добавлены `MenuAssetsScenario` и `QuietGuideScenario`.
- `MessageGatewayService` теперь маршрутизирует явные domain scenarios до общего `MainMenuScenario` и LLM fallback.
- `/start` в `FirstTouchScenario` стал safe restart: чистит table booking Redis draft, проверяет granted privacy consent, возвращает known guest в `READY_FOR_DIALOG`.
- `TelegramRouter` умеет:
  - force-send preview на `/start`;
  - пробовать pin preview;
  - отправлять несколько PDF из `metadata.documents`;
  - отправлять video из MinIO object key `metadata.videoObjectKey`.
- 4 AERIS menu PDF скопированы в `src/main/resources/menu/aeris/`.
- `INTERIOR.mp4` загружен в MinIO: `s3://astor-media/content/aeris/interior/INTERIOR.mp4`.
- STT включен в контейнерном `app`: `ffmpeg` + `faster-whisper` внутри Docker image, `ASTOR_STT_ENABLED=true`, model `base`, language `ru`, HuggingFace cache в Docker volume `huggingface-cache`.
- Голосовой fallback: первая неудачная расшифровка просит перезаписать voice, вторая подряд просит перейти на текст; счетчик хранится в Redis с TTL 1800 секунд.
- Контейнер `astor_app` пересобран и запущен, health через gateway `UP`.
- Проверка: `mvn test` в Docker JDK 21 -> 52/52 green.
- REST smoke:
  - unknown guest -> `CONSENT_REQUIRED`;
  - known guest "скинь меню" -> 4 PDF metadata;
  - known guest "какая у вас концепция?" -> approved concept copy;
  - known guest "покажи ресторан внутри" -> `videoObjectKey`.
  - contact -> failed voice #1 -> `TRANSCRIPTION_RETRY_REQUESTED`;
  - failed voice #2 -> `TRANSCRIPTION_FAILED_TWICE`, `ASK_TEXT_INPUT`.

Дата актуализации: 2026-06-04

## Один экран

Astor Butler MVP сейчас строится как Java 21 + Spring Boot backend для Telegram/FSM сценариев с локальной инфраструктурой вокруг PostgreSQL, Redis, MongoDB, MinIO, Kafka, Redpanda Console, Prometheus и Grafana.

Главный рабочий контур:

1. Пользователь пишет в Telegram.
2. Telegram adapter принимает update.
3. Message gateway сохраняет входящее событие.
4. FSM определяет состояние и следующий ответ.
5. Consent Vault фиксирует согласие и контакт.
6. Kafka получает user event trail.
7. Admin chat может получать отладочные события через feature flag.

## Что работает

- Локальный Swagger через gateway: `http://localhost:8080/swagger-ui/index.html`.
- Telegram bot long polling при `TELEGRAM_BOT_ENABLED=true`.
- Первый контакт Telegram: `/start`, запрос контакта, согласие с политикой.
- Сохранение Telegram profile/messages/consents в PostgreSQL.
- Redis FSM hot state.
- Kafka user events.
- Redpanda Console: `http://localhost:8081`.
- MongoDB database `aether` для document/media metadata: проверено, `media_assets=102`, `media_sample_assets=10`, `project_documents=19`, `document_chunks=114`.
- MinIO как локальный S3-compatible слой: проверено, `astor-media` содержит 10 sample videos, `astor-documents` пока пустой.

## Что проверять вручную

- После `/start` guest видит понятное приветствие и кнопку контакта.
- До контакта бот не должен начинать бизнес-сценарий бронирования.
- После контакта keyboard должен меняться на основной режим общения.
- Все входящие Telegram messages должны сохраняться в PostgreSQL.
- Kafka topic должен получать user events без дублей.
- Admin chat не должен спамиться, если выключен соответствующий feature flag.

## Что не готово

- Production auth через Keycloak/JWT.
- Полноценный booking flow поверх новых Consent/Telegram intake таблиц.
- Consumer-side idempotency как отдельный слой.
- Multi-instance/load-balancer режим.
- Production AI Adapter и сильная LLM.
- CI/CD quality gates.
- System Design ДЗ в финальном формате.
- Большая часть Swagger CRUD API пока является stub/reserved контрактом, а не реальной бизнес-логикой.

## Локальные риски

- `.env` содержит реальные креды и не коммитится.
- `.idea/dataSources.xml` и `.idea/misc.xml` локально изменены и не должны попадать в commit без решения.
- Docker volumes Astor Butler сохраняют локальные данные; не удалять без отдельного решения.
- Telegram bot нельзя запускать одновременно из двух локальных сред с одним token.
