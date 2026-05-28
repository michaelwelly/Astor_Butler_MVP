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
    Grpc["Internal gRPC<br/>module-to-module calls"]

    FSM["FSM Core / Orchestration Layer<br/>single source of truth"]
    AI["AI Adapter<br/>intent parsing / entity extraction"]

    User["User Domain"]
    Booking["Booking Domain"]
    Content["Content Domain"]
    Media["Media Domain"]
    Timeline["Timeline Domain"]
    Manager["Manager Domain"]
    Notify["Notifications Domain"]

    Pg["PostgreSQL<br/>JDBC / Liquibase"]
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
    FSM --> Grpc

    Grpc --> User
    Grpc --> Booking
    Grpc --> Content
    Grpc --> Media
    Grpc --> Timeline
    Grpc --> Manager
    Grpc --> Notify

    User --> Pg
    Booking --> Pg
    Content --> Pg
    Timeline --> Pg
    Manager --> Pg
    Media --> S3

    FSM --> Redis
    User --> Redis
    Content --> Redis
    Booking --> Kafka
    Timeline --> Kafka
    Notify --> Kafka
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

### Internal Boundary

Внутреннее взаимодействие backend-модулей проектируется через gRPC:

- `UserInternalService`;
- `BookingInternalService`;
- `ContentInternalService`;
- `MediaInternalService`;
- `TimelineInternalService`;
- `ManagerInternalService`;
- `NotificationInternalService`.

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

- `User` - профиль, роли, идентичность, связки Telegram/web.
- `Booking` - заявки, мероприятия, статусы, менеджерская обработка.
- `Content` - посты, афиши, promo blocks, SEO metadata, draft/published/archived state.
- `Media` - metadata и связи с S3 objects.
- `Timeline` - действия пользователя, менеджера, FSM transitions, domain events.
- `Manager` - dashboard, tasks, escalation workflow.
- `Notifications` - Telegram/CRM/email/event notifications.

Доменные модули не содержат Telegram-логики и не должны напрямую зависеть от UI.

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
    participant Grpc as Internal gRPC Services
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
    FSM->>Grpc: call BookingInternalService
    Grpc->>Booking: update booking draft/request
    Booking->>DB: persist booking and status
    Booking->>S3: attach media if exists
    Booking->>Bus: publish BookingReadyForManager
    FSM->>Redis: save next state
    FSM-->>Rest: response DTO
    Rest-->>Gateway: HTTP response
    Gateway-->>Manager: dashboard update
    Tg-->>Guest: Telegram response
```
