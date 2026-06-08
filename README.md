# Astor Butler — цифровой дворецкий для AERIS и премиального HoReCa

Astor Butler — backend MVP для Telegram/FSM-сценариев, где мессенджер является только транспортом, а состояние диалога, согласия, профиль гостя, события и аудит живут внутри системы.

Текущий практический фокус проекта: проверить на реальном контуре AERIS gastro bar первый путь гостя — от `/start` до сохраненного профиля, согласия, сообщений, голосовых запросов, Kafka-событий и админского наблюдения.

Проект развивается как soft-governance tool для HoReCa: не давить на гостя, а спокойно распознавать его, помнить контекст и передавать команде только то, что действительно важно.

## Что уже есть

- Telegram bot long polling как первый UI.
- AERIS preview-карточка с изображением цифрового дворецкого и ссылкой на `https://aeris.bar/`.
- First-touch flow: `/start` -> Consent Vault -> контакт -> режим свободного диалога.
- PostgreSQL persistence для пользователей, Telegram-профилей, сообщений, контактов и согласий.
- Redis FSM hot state с TTL для сценариев.
- Kafka/Redpanda user event trail и admin-chat projection.
- Voice/audio intake: Telegram voice -> MinIO/S3 -> STT boundary -> transcript в Postgres/Kafka.
- MinIO lifecycle для временных voice-бинарей: `transient/telegram-voice/...`, TTL 3 дня.
- Admin Telegram chat видит user events, AI responses, voice transcript/status и object key.
- Swagger/OpenAPI группы для backend API и будущей frontend-интеграции.
- Local API gateway на `localhost:8080`.
- MongoDB `aether` для document/media metadata.
- Prometheus/Grafana и Redpanda Console для локального наблюдения.

## UX-контракт Telegram

Telegram-экран гостя держится в формате:

```text
persistent AERIS preview
+ current guest request
+ current Astor Butler response
```

При следующем сообщении гостя предыдущая пара `request/response` удаляется из Telegram UI. При этом в системе ничего не теряется: входящие сообщения, metadata, transcript, consent evidence и Kafka events сохраняются в PostgreSQL/Kafka.

Первое сообщение после preview:

```text
Нажимая кнопку "Согласиться и поделиться контактом", вы соглашаетесь с политикой обработки персональных данных.
```

Кнопка контакта одновременно является явным согласием с политикой. До контакта бот не собирает бизнес-данные и не запускает бронирование.

## Архитектурные принципы

- FSM является source of truth для сценариев.
- Telegram, будущий web chat и другие мессенджеры не содержат бизнес-логики.
- AI Adapter помогает интерпретировать ввод, но не принимает доменные решения.
- Consent Vault фиксирует согласие, источник, версию политики и evidence.
- Voice binaries хранятся временно в object storage; смысл и аудит хранятся долго.
- Kafka event trail нужен для аналитики, admin projection и будущих read models.
- PostgreSQL publication ограничена доменными/outbox-таблицами и не включает служебные таблицы Liquibase.

## Стек MVP

- Java 21
- Spring Boot 3
- JDBC/Liquibase как целевое направление persistence
- PostgreSQL
- Redis
- MongoDB
- Kafka / Redpanda
- MinIO как S3-compatible object storage
- Telegram Bot API
- Swagger / OpenAPI
- Docker Compose
- Nginx local API gateway
- Prometheus / Grafana
- Ollama/local LLM как заменяемый AI Adapter для dev-контура

## Локальный запуск

Локальная схема: инфраструктура работает в Docker Compose, Spring Boot запускается как один локальный инстанс из IDEA или Maven.

```bash
scripts/start_local_infra.sh
scripts/run_local_app.sh
```

Файл `.env` не коммитится. В нем лежат локальные креды, Telegram token, admin chat id и настройки инфраструктуры.

Основные локальные адреса:

- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- API Gateway health: `http://localhost:8080/gateway/health`
- Backend health: `http://localhost:8080/actuator/health`
- Redpanda Console: `http://localhost:8081`
- Grafana: `http://localhost:3000`
- C3FLEX frontend: `http://localhost:3001`

API Gateway проксирует внешний `localhost:8080` на Spring Boot dev-порт. Это позволяет держать единый вход для Swagger/frontend и не путаться между локальными портами.

## Voice pipeline

Голосовые сообщения обрабатываются как временные media assets:

1. Telegram adapter принимает `voice/audio`.
2. Файл скачивается во временную локальную папку.
3. Backend загружает бинарь в MinIO/S3 bucket `astor-media`.
4. Object key строится под `transient/telegram-voice/YYYY-MM-DD/...`.
5. MinIO lifecycle удаляет voice-бинарь через `S3_VOICE_TTL_DAYS=3`.
6. `SpeechToTextService` пытается получить transcript через внешний command adapter.
7. Transcript, Telegram metadata, `storageObjectKey` и audit trail сохраняются в PostgreSQL/Kafka.
8. Admin chat получает human-readable summary с transcript/status/object key.

Локальные env:

```bash
ASTOR_STT_ENABLED=false
ASTOR_STT_COMMAND=
ASTOR_STT_WORK_DIR=/private/tmp/astor-butler-stt
ASTOR_STT_TIMEOUT_SECONDS=30
ASTOR_STT_KEEP_LOCAL_FILES=false
S3_EPHEMERAL_PREFIX=transient
S3_VOICE_PREFIX=telegram-voice
S3_VOICE_TTL_DAYS=3
```

При выключенном STT бот не падает и не эскалирует голосовое как ошибку. Он сохраняет metadata и просит гостя коротко написать текстом.

## Data layer

PostgreSQL сейчас хранит:

- `users` — внутренняя identity-модель;
- `telegram_profiles` — внешний Telegram-профиль и UI-state сообщения;
- `telegram_messages` — входящие сообщения и raw payload;
- `user_contacts` — контакты пользователя;
- `user_consents` — согласия и evidence;
- `event_bookings` — задел под booking domain;
- `outbox_events` и `processed_kafka_events` — event handoff/idempotency boundary.

Redis хранит быстрый FSM state.

MongoDB `aether` используется для document/media metadata и проектной библиотеки.

MinIO/S3 используется для media/document binaries. В локальном MVP полный продакшн-контент не держится в MinIO: для разработки используется ограниченный sample-набор.

## Kafka и наблюдаемость

Основной топик MVP:

```text
astor.user.events
```

События имеют стабильный partition key по Telegram user/chat и deterministic `eventId`/`idempotencyKey` от source update id.

Admin Telegram chat — это человекочитаемая проекция Kafka-событий, а не основной транспорт. Ее можно выключить feature flag, чтобы не спамить при дальнейшей разработке.

Redpanda Console доступен на:

```text
http://localhost:8081
```

## API и frontend boundary

Swagger уже группирует будущие границы:

- Auth API
- Consent Vault API
- User API
- FSM API
- Booking API
- Timeline API
- Posts/Content API
- Media API
- Notifications API
- Manager API
- Integration API
- System API

Часть REST API пока является stub/reserved contract. Реальная бизнес-логика сейчас сосредоточена в Telegram/FSM/Consent/Kafka pipeline. Это осознанно: сначала проверяется работающая ось гостевого взаимодействия, потом API расширяется до frontend/web-chat и manager dashboard.

## Capability-модули

| Ось боли | Capability | MVP-статус |
| --- | --- | --- |
| Идентичность | Memory Engine | профиль, контакт, сообщения, consent evidence |
| Персонализация | Preference Map | reserved boundary |
| Благодарность | Smart Tip | reserved boundary |
| Инфо-поддержка | Quiet Guide | меню/media pipeline готовится |
| Социальный вклад | Hidden Heart | reserved boundary |
| Игровой опыт | Safe Play | FSM extension point |
| Управление временем | Slot Keeper | booking/timeline next step |
| Безопасность | Panic Exit | safe reset/exit сценарии планируются |

Внешние extension points:

- Direct Channel Hub
- Arena Reboot Engine
- Consent Vault
- Impact Meter

Consent Vault вынесен раньше остальных, потому что первый пользовательский сценарий уже требует политики, согласия и хранения персональных данных.

## Проверки

Unit/integration smoke:

```bash
mvn test
```

Swagger smoke:

```bash
scripts/run_k6_smoke.sh
```

Read-load сценарий:

```bash
scripts/run_k6_read_load.sh
```

Ручной Telegram first-touch чек:

1. Очистить тестового Telegram-пользователя в Postgres/Redis.
2. Перезапустить `AstorButlerApplication`.
3. Отправить `/start`.
4. Проверить AERIS preview, кнопку контакта, запись profile/message/consent.
5. Отправить текст/voice и проверить admin chat, Kafka event и PostgreSQL payload.

## Что не готово

- Production auth через Keycloak/OAuth2/JWT.
- Полный booking flow и manager dashboard.
- Production STT/LLM adapter.
- Web chat поверх того же `MessageGatewayService`.
- Consumer-side idempotency как отдельный hardened слой.
- Multi-instance/load-balancer режим.
- CI/CD quality gates.
- Финальное System Design ДЗ и sequence diagrams.
- SLA/high-availability режим.

## Документация

- [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)
- [docs/FSM_SCENARIOS.md](docs/FSM_SCENARIOS.md)
- [docs/MEDIA_PIPELINE.md](docs/MEDIA_PIPELINE.md)
- [docs/API_CONTRACT.md](docs/API_CONTRACT.md)
- [docs/LOAD_TESTING.md](docs/LOAD_TESTING.md)
- [docs/FRONTEND_HANDOFF.md](docs/FRONTEND_HANDOFF.md)

Локальная проектная память ведется отдельно в Obsidian и не является production-репозиторием.

## Научный фундамент

Проект опирается на исследовательскую рамку Grounded Theory.

Центральная категория:

```text
Потребность быть распознанным — без давления
```

Astor Butler переводит эту идею в инженерную форму: FSM-сценарии, consent-aware identity, soft routing, event trail и спокойный интерфейс вместо навязчивого приложения.

## Git hygiene

Не коммитить:

- `.env`
- `target/`
- локальные `.idea/*`, если это не согласованная настройка проекта;
- локальные медиа-исходники из `Desktop`, `Downloads`, Yandex Disk.

Pull requests приветствуются, но изменения должны сохранять границу: Telegram/UI отдельно, FSM/application logic отдельно, storage/event trail отдельно.

## Контакты

email: `michael.poedinenko.mxr@gmail.com`
telegram: `@michael_welly`
