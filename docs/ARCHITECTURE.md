# Astor Butler Architecture

## Назначение

Astor Butler - soft-governance tool для HoReCa. Система объединяет Telegram UI, manager/staff/admin web app и публичную promo/lead-gen витрину. Telegram остается транспортом для гостя, web app - рабочим местом команды, promo frontend - витриной для production story, System Design/JavaGuru-материалов и сбора лидов.

Главный архитектурный принцип: FSM является single source of truth для сценариев взаимодействия. UI-слои не содержат бизнес-логики.

## High-Level Diagram

```mermaid
flowchart LR
    Guest["Guest / Telegram user"]
    Staff["Staff / Manager / Admin"]
    Visitor["Promo visitor / Lead"]

    Telegram["Telegram Bot API"]
    Web["Manager Web App<br/>Next.js / React"]
    Promo["Promo Lead-Gen Frontend<br/>Next.js / React / GSAP / Framer Motion / Lenis"]

    Gateway["API Gateway<br/>TLS / routing / rate limiting / payload limits"]
    Keycloak["Keycloak<br/>OAuth2 / OIDC / JWT Stateless"]

    Rest["REST API<br/>Swagger / OpenAPI"]
    KafkaIn["Kafka Listener<br/>Consumer / Producer"]
    Services["Service Layer / gRPC Boundary<br/>module-to-module calls"]

    FSM["FSM Core / Orchestration Layer<br/>single source of truth"]
    AI["AI Adapter<br/>intent parsing / entity extraction"]

    User["User Domain"]
    Booking["Booking Domain"]
    Content["Content Domain"]
    Media["Media Domain"]
    Timeline["Timeline Domain"]
    Manager["Manager Domain"]
    Notify["Notifications Domain"]
    Capability["Capability Extensions<br/>Memory / Preference / Tips / Guide / Play / Safety"]

    Pg["PostgreSQL<br/>JDBC / Liquibase"]
    Mongo["MongoDB<br/>internal documents / flexible metadata"]
    Redis["Redis<br/>FSM hot context / idempotency / cache / short queues"]
    S3["S3-compatible Object Storage<br/>video / photo / docs / menu / media"]
    Kafka["Kafka / RabbitMQ / Artemis<br/>events / audit / analytics / notifications"]
    Search["Elasticsearch / OpenSearch<br/>future full-text search"]

    Obs["Observability<br/>Prometheus / Grafana / ELK / Jaeger / OpenTelemetry"]
    CICD["CI/CD<br/>GitHub Actions / TeamCity / Jenkins / Nexus / Registry"]

    Guest --> Telegram --> Gateway
    Staff --> Web --> Gateway
    Visitor --> Promo --> Gateway

    Gateway --> Keycloak
    Gateway --> Rest
    KafkaIn --> FSM
    Rest --> FSM
    FSM --> AI
    FSM --> Services

    Services --> User
    Services --> Booking
    Services --> Content
    Services --> Media
    Services --> Timeline
    Services --> Manager
    Services --> Notify
    Services --> Capability

    Capability --> User
    Capability --> Booking
    Capability --> Content
    Capability --> Media
    Capability --> Timeline
    Capability --> Notify

    User --> Pg
    Booking --> Pg
    Content --> Pg
    Timeline --> Pg
    Manager --> Pg
    Media --> S3
    Media --> Mongo
    Content --> Mongo

    FSM --> Redis
    User --> Redis
    Content --> Redis
    Booking --> Kafka
    Timeline --> Kafka
    Notify --> Kafka
    Capability --> Redis
    Capability --> Kafka
    Content --> Search

    Rest --> Obs
    Kafka --> Obs
    Pg --> Obs
    Redis --> Obs
    S3 --> Obs
    CICD --> Rest
```

## Входные каналы

### Telegram

Telegram используется как первый guest-facing UI:

- сообщения;
- callbacks;
- контакты;
- safe exit из сценария;
- уведомления и переходы в бронирование.

Telegram adapter только нормализует входящие события в `InboundEvent` и отправляет ответы. Он не принимает бизнес-решений.

### Manager Web App

Manager/staff/admin web app нужен для операционной работы:

- dashboard менеджера;
- список и карточка заявок;
- пользователи и роли;
- поиск;
- таймлайны;
- posts/afisha management;
- media library;
- staff tasks;
- notifications center;
- admin settings.

Авторизация идет через Keycloak/OAuth2/OIDC. Frontend передает JWT в backend через `Authorization: Bearer`.

### Promo / Lead-Gen Frontend

Promo frontend - публичная витрина для презентации production story, System Design/JavaGuru-материалов и сбора лидов.

Стек:

- Next.js;
- React;
- GSAP;
- Framer Motion;
- Lenis smooth scroll.

Функции:

- immersive landing page;
- видео `mp4/webm`, muted autoplay, lazy loading, adaptive streaming target;
- CTA в Telegram, CRM, курс или форму заявки;
- сбор UTM/source/campaign;
- отправка lead events в backend.

WordPress/Headless CMS не является целевой backend-архитектурой. CMS-функции реализуются собственным `content/admin` модулем на общем backend stack.

## Backend

### Public Boundary

Наружу система предоставляет:

- REST API;
- Swagger/OpenAPI contracts;
- Kafka Listener / Consumer / Producer для event boundary;
- Prometheus metrics endpoint;
- readiness/liveness probes для Kubernetes/OpenShift.

### Service Layer / gRPC Boundary

Внутреннее взаимодействие backend-модулей проектируется через service layer. Для межмодульных и будущих межсервисных вызовов используется gRPC boundary:

- `UserService`;
- `BookingService`;
- `ContentService`;
- `MediaService`;
- `TimelineService`;
- `ManagerService`;
- `NotificationService`.

REST API не должен напрямую размазывать бизнес-логику по контроллерам. Контроллеры принимают запрос, валидируют контракт и передают команду в application/FSM/orchestration layer.

## FSM Core

FSM Core отвечает за:

- текущее состояние пользователя;
- допустимые переходы;
- fallback и safe state;
- late/offline messages;
- idempotency-aware обработку событий;
- orchestration между AI Adapter и доменными модулями.

FSM не живет в Telegram, web app или promo frontend. Все UI-каналы являются транспортами.

## AI Adapter

AI Adapter - заменяемый модуль для первого понимания человеческого сообщения:

- intent parsing;
- entity extraction;
- normalization свободного текста;
- fallback к rule-based логике при timeout/error.

AI Adapter не является источником бизнес-правил. Он помогает понять, что пользователь хочет, после чего сценарий продолжает FSM.

## Domain Modules

- `Auth` - authorization language, permissions, JWT claims mapping, access decisions. Auth is separate from User.
- `User` - профиль, роли, идентичность, связки Telegram/web.
- `Booking` - заявки, мероприятия, статусы, менеджерская обработка.
- `Content` - посты, афиши, promo blocks, SEO metadata, draft/published/archived state.
- `Media` - metadata и связи с S3 objects.
- `Timeline` - действия пользователя, менеджера, FSM transitions, domain events.
- `Manager` - dashboard, tasks, escalation workflow.
- `Notifications` - Telegram/CRM/email/event notifications.

Доменные модули не содержат Telegram-логики и не должны напрямую зависеть от UI.

## Capability Extensions

Capability modules are product-level extension points above the core domains. MVP creates package boundaries and integration contracts, but does not implement full business logic for every capability in the first iteration.

| Pain axis | Capability module | MVP responsibility | Depends on |
| --- | --- | --- | --- |
| Identity | `Memory Engine` | recognize returning guests by phone, profile and preference history | User, Booking, Timeline |
| Personalization | `Preference Map` | prepare "as last time" suggestions | User, Booking, Content, Timeline |
| Gratitude | `Smart Tip` | digital tips and gratitude scenario tracking | User, Booking, Timeline, Notifications |
| Information support | `Quiet Guide` | menus, posters and useful info without spam | Content, Media, Redis |
| Social impact | `Hidden Heart` | anonymous donation extension point | User, Timeline, Notifications |
| Game experience | `Safe Play` | mini-scenarios with immediate exit | FSM, Timeline, Panic Exit |
| Time management | `Slot Keeper` | slot reminders and time-window coordination | Booking, Timeline, Notifications |
| Safety | `Panic Exit` | immediate scenario termination and safe state recovery | FSM, Redis, Timeline |

Optional external blocks for post-MVP:

- `Direct Channel Hub` - direct API boundary for guest-to-PMS interaction.
- `Arena Reboot Engine` - hotel-to-stadium scenarios.
- `Consent Vault` - consent storage/export for GDPR, PDPA, PIPL and 152-FZ.
- `Impact Meter` - cultural KPI and reporting boundary.

Capability modules must communicate through application services/FSM and domain events. They do not bypass auth, idempotency guard or persistence constraints.

## Package Map

High-level diagram выше является context map. Для разработки используется package map:

### API boundary packages

- `api.auth` - OAuth2/OIDC, JWT, login/logout/current principal endpoints.
- `api.user` - user profiles, roles, lookup.
- `api.fsm` - FSM events, state read model and safe reset.
- `api.booking` - booking requests, statuses, manager notes.
- `api.timeline` - user, booking, manager and system timelines.
- `api.content` - posts, afisha, promo blocks, SEO content.
- `api.media` - upload, media metadata, file links.
- `api.manager` - dashboard, tasks, escalation workflow.
- `api.notification` - notification read model and test commands.
- `api.integration` - Gmail, CRM, analytics and external integrations.
- `api.observability` - internal health/readiness/liveness/metrics boundary.

### Domain packages

- `domain.auth` - permissions, role mapping and access decisions.
- `domain.user` - user profile and user business data.
- `domain.booking` - first production domain after user/auth skeleton.
- `domain.content` - posts, afisha and promo content.
- `domain.media` - S3 metadata and file-to-entity links.
- `domain.timeline` - immutable events and user/manager history.
- `domain.manager` - dashboard and operational workflows.
- `domain.notification` - notification commands and delivery state.
- `domain.document` - internal documents, MongoDB metadata and parsing state.

### Service packages

- `service` - application service layer. Controllers stay thin and call services.
- `service.grpc` - future gRPC boundary implementations.

### Capability packages

- `capability.memory` - Memory Engine.
- `capability.preference` - Preference Map.
- `capability.smarttip` - Smart Tip.
- `capability.quietguide` - Quiet Guide.
- `capability.hiddenheart` - Hidden Heart.
- `capability.safeplay` - Safe Play.
- `capability.slotkeeper` - Slot Keeper.
- `capability.panicexit` - Panic Exit.
- `capability.directchannel` - Direct Channel Hub extension point.
- `capability.arenareboot` - Arena Reboot Engine extension point.
- `capability.consent` - Consent Vault extension point.
- `capability.impact` - Impact Meter extension point.

These packages are intentionally thin at MVP start. They give the team stable places for future scenario logic, contracts, metrics and domain-event handlers without polluting CRUD/API packages.

## Data Layer

### PostgreSQL

PostgreSQL - основная СУБД для:

- пользователей;
- ролей;
- заявок;
- статусов;
- таймлайнов;
- постов;
- лидов;
- SEO metadata;
- связей между сущностями.

Persistence strategy: JDBC without JPA/Hibernate. Причина: явный контроль SQL, транзакций, индексов и performance-critical запросов. Миграции схемы - Liquibase.

PostgreSQL design rules:

- primary/master принимает write traffic;
- read replicas/slaves используются для read-heavy аналитики и dashboard-запросов;
- таблицы получают понятные имена в предметной области;
- связи `one-to-many`, `many-to-one` и `many-to-many` выражаются явными foreign keys и join tables;
- constraints защищают базу от невалидного состояния;
- индексы создаются по селективным полям и реальным query patterns;
- аналитические запросы не должны ломать OLTP-контур;
- схема мигрируется через Liquibase changesets.

### MongoDB

MongoDB выделяется отдельно для внутренних документов и гибких document-like данных:

- загруженные внутренние документы;
- metadata документов;
- промежуточные распознавания/парсинг файлов;
- обезличенные примеры материалов;
- гибкие структуры, которые не должны раздувать PostgreSQL schema.

MongoDB не заменяет PostgreSQL как основную СУБД. Связь с бизнес-сущностями хранится через stable IDs и metadata references.

### Redis

Redis используется для:

- FSM hot context;
- idempotency guard;
- processed event IDs;
- краткоживущих booking drafts;
- краткосрочных очередей;
- кеша меню, справочников и публичного контента;
- feature flags/cache для landing blocks.

### S3-Compatible Object Storage

Object Storage используется для:

- фото;
- видео;
- меню;
- документов;
- приложений;
- медиа постов;
- презентаций и PDF;
- derivative assets для promo frontend.

### Kafka / RabbitMQ / Artemis

Event bus используется для:

- audit events;
- notification commands/events;
- analytics events;
- timeline enrichment;
- lead events;
- achievement events;
- интеграций с CRM и внешними системами.

Draft topics:

- `astor.booking.events`;
- `astor.user.events`;
- `astor.timeline.events`;
- `astor.media.events`;
- `astor.lead.events`;
- `astor.notification.commands`;
- `astor.notification.events`;
- `astor.audit.events`;
- `astor.analytics.events`;
- `astor.achievement.events`.

Финальный набор топиков и партиционирование уточняются после нагрузочного тестирования.

## Security

- API Gateway перед backend: TLS termination, routing, rate limiting, payload limits.
- Keycloak как OAuth2/OIDC provider.
- JWT Stateless authentication.
- Spring Security как OAuth2 Resource Server.
- Роли и permissions через JWT claims/scopes.
- Backend не хранит пользовательские web-сессии.
- UI скрывает недоступные действия, backend остается финальной точкой проверки прав.
- Admin actions пишутся в audit/timeline.
- Секреты не хранятся в git. `.env` остается только локально.

## Observability

Observability stack:

- Prometheus для метрик;
- Grafana для dashboard views;
- ELK для централизованных логов;
- Jaeger/OpenTelemetry для tracing.

Минимум 6 Grafana dashboard views:

- API;
- JVM;
- PostgreSQL;
- Redis;
- Kafka;
- business/FSM.

Отслеживаемые SLI:

- availability;
- error rate;
- P50/P95/P99 latency по REST;
- P50/P95/P99 latency по gRPC;
- Kafka consumer lag;
- Redis hit ratio;
- PostgreSQL query latency;
- S3 upload/download errors;
- lead conversion events;
- FSM transition failures.

Целевой availability SLO после production-выхода - 99.9% uptime в год. Конкретные latency thresholds фиксируются после нагрузочного тестирования.

## CI/CD And Quality Gates

CI/CD:

- GitHub Actions;
- TeamCity/Jenkins as optional enterprise CI;
- Nexus/Container Registry для артефактов и Docker images;
- Docker для контейнеризации;
- Docker Compose для локального запуска;
- Kubernetes/OpenShift для production-ready deployment.

Quality gates:

- JUnit;
- Mockito;
- Testcontainers;
- JaCoCo;
- Checkstyle;
- PMD or SpotBugs;
- OpenAPI contract checks;
- integration tests for PostgreSQL, Redis, API contracts and Kafka flows.

Цель - максимальное покрытие business-critical кода. Формальный процент coverage утверждается после выделения слоев, где покрытие действительно отражает качество, а не декоративную метрику.

## Runtime Profile And Local Environment

Spring profile:

- `local` - единый профиль для разработки, Swagger/API-проверки и локальных нагрузочных сценариев.

Config files:

- `application.yaml` - базовая конфигурация и defaults;
- `application-local.yaml` - локальные overrides для запуска Spring Boot на машине разработчика.
- `.env.example` - шаблон переменных без реальных секретов;
- `.env` - локальный файл разработчика с реальными значениями, не попадает в git.

Local Docker Compose поднимает:

- PostgreSQL;
- Redis;
- MongoDB;
- Kafka-compatible broker for local tests;
- MinIO S3-compatible object storage;
- Prometheus;
- Grafana;
- опционально Ollama/local LLM через `--profile ai`.

The Spring Boot application is intentionally started locally from IDE or Maven, not inside Docker Compose. This keeps Swagger/API development fast and lets the backend connect to Dockerized dependencies through `localhost`.

Local dependency stack:

```bash
docker compose up -d
```

Local backend startup:

```bash
scripts/run_local_app.sh
```

After local backend startup:

- Swagger UI: `http://localhost:8080/swagger-ui/index.html`;
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`;
- metrics: `http://localhost:8080/actuator/prometheus`.

Ollama is intentionally excluded from the default local infrastructure profile. Heavy local models can take tens of gigabytes and should not block PostgreSQL, Redis, MongoDB, Kafka, MinIO, Prometheus or Grafana startup.

`.env.example` хранит безопасный шаблон переменных. Реальный `.env` не коммитится.

## Sequence Diagram

```mermaid
sequenceDiagram
    actor Guest as Guest / Telegram
    actor Manager as Manager Web UI
    participant Gateway as API Gateway
    participant Keycloak as Keycloak
    participant Tg as Telegram Adapter
    participant Rest as REST API
    participant Idem as Idempotency Guard
    participant FSM as FSM Core
    participant AI as AI Adapter
    participant Services as Service Layer / gRPC Boundary
    participant Booking as Booking Domain
    participant DB as PostgreSQL
    participant Redis as Redis
    participant S3 as S3 Storage
    participant Bus as Kafka / MQ

    Guest->>Tg: message / callback
    Tg->>Gateway: normalized transport event
    Manager->>Gateway: REST request with JWT
    Gateway->>Keycloak: validate token and roles
    Keycloak-->>Gateway: claims / scopes
    Gateway->>Rest: authorized request
    Rest->>Idem: normalize command/event
    Idem->>Redis: check duplicate eventId
    Redis-->>Idem: not processed
    Idem->>FSM: route event
    FSM->>AI: intent parsing / entity extraction
    AI-->>FSM: intent and entities
    FSM->>Redis: load current state and draft
    FSM->>Services: call BookingService
    Services->>Booking: update booking draft/request
    Booking->>DB: persist booking and status
    Booking->>S3: attach media if exists
    Booking->>Bus: publish BookingReadyForManager
    FSM->>Redis: save next state
    FSM-->>Rest: response DTO
    Rest-->>Gateway: HTTP response
    Gateway-->>Manager: dashboard update
    Tg-->>Guest: Telegram response
```

## Current Implementation Status

Текущий код в `main` частично отстает от целевой архитектуры:

- Telegram `Update -> TelegramRouter -> CommandContext -> FSMRouter` уже работает как основной путь.
- `InboundEvent` и `IdempotencyGuard` уже есть.
- Duplicate text events теперь останавливаются в `TelegramRouter` до legacy FSM route.
- `IdempotencyService` пока in-memory; целевое состояние - Redis-backed guard.
- `FSMRouter.handle(InboundEvent)` существует, но полноценный `InboundEvent -> FSM -> response DTO` путь еще не является основным.
- AI Adapter как контракт пока не реализован полноценно: есть `AIInterpreter`, `OllamaClient` и старые Alisa-классы, но нет единого production adapter для intent/entity extraction.
- `pom.xml` пока содержит JPA/Hibernate/Actuator dependencies; целевое состояние - JDBC without JPA/Hibernate и Prometheus/observability stack.
- Keycloak/JWT Stateless пока не подключен в коде: `SecurityConfig` временно разрешает большую часть запросов.
- Booking domain в `main` еще не вынесен как первый production-домен; его нужно строить после User/service/data layer.
