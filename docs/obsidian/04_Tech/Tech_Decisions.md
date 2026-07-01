# Tech Decisions

## Базовый стек

- Java 21
- Spring Boot
- PostgreSQL для пользователей и основной бизнес-структуры
- Redis для быстрых операций, кеша и ускорения загрузки
- MongoDB для хранения файлов/документов и связанных метаданных
- Telegram Bot API как транспорт/UI
- FSM как single source of truth для диалогов

## Архитектурный принцип

FSM управляет состоянием диалога, разрешенными переходами и структурой сбора данных. Telegram не содержит бизнес-логики и работает как транспорт.

## Документы

Архитектура MVP лежит в:

`/Users/michaelwelly/IdeaProjects/Astor_Butler_MVP/docs/architecture/ARCHITECTURE.md`

## Следующее техническое решение

Нужно отдельно описать:

- стабилизацию первого Telegram/Consent flow;
- контракт сохранения Telegram profile/messages/consents;
- Kafka event taxonomy и idempotency key;
- границу analytics/admin notifications, чтобы отладочный поток можно было выключать feature flag;
- структуру FSM-сценариев `EVENT_BOOKING` и `Slot Keeper`;
- модель бронирования, события, клиента и документов;
- границу между MVP и legacy-стилем общения.

## Обновление backend/frontend ТЗ 2026-05-29

- Наружу система предоставляет REST API и Kafka event boundary.
- Внутреннее взаимодействие backend-модулей проектируется через gRPC.
- Для PostgreSQL выбран JDBC без JPA/Hibernate, чтобы явно контролировать SQL, транзакции и performance-critical участки.
- Безопасность: Keycloak, OAuth2/OIDC, stateless JWT, роли и доступы через claims/scopes.
- Observability: Prometheus, Grafana, ELK, Jaeger/OpenTelemetry; минимум 6 dashboard views для API, JVM, PostgreSQL, Redis, Kafka, business/FSM.
- API Gateway перед backend: rate limiting, routing, TLS termination, request validation и защита от перегрузки.
- Quality gates: JUnit, Mockito, Testcontainers, JaCoCo, Checkstyle/PMD/SpotBugs.
- Frontend ТЗ добавлено в `docs/research/DIPLOMA_TZ_DRAFT.md`: manager dashboard, users, bookings, search, timelines, posts/afisha, media library, notifications, admin settings.
- Promo/lead-gen frontend выделен отдельно: Next.js, React, GSAP, Framer Motion, Lenis; публичная витрина для production story, System Design/JavaGuru-рекламы и сбора лидов.
- WordPress/Headless CMS не выбран как целевой backend. CMS-функции реализуются через собственный content/admin module на общем backend stack: Spring Boot, REST, JDBC/PostgreSQL, Redis, S3, Kafka, Keycloak.
- MongoDB возвращается в целевую архитектуру как отдельное хранилище внутренних документов, гибкой metadata, результатов парсинга файлов и обезличенных материалов.
- PostgreSQL: primary/master для write traffic, read replicas/slaves для read-heavy запросов и аналитики; обязательны constraints, foreign keys, join tables, индексы по query patterns и Liquibase migrations.
- Capability-модули фиксируются отдельным слоем расширений поверх доменов: Memory Engine, Preference Map, Smart Tip, Quiet Guide, Hidden Heart, Safe Play, Slot Keeper, Panic Exit. Внешние extension points: Direct Channel Hub, Arena Reboot Engine, Consent Vault, Impact Meter.
- На старте MVP capability-пакеты содержат границы и контракты, но полная бизнес-логика пишется позже, после core API, Swagger-проверки и базового нагрузочного сценария.
- Legacy repository `Astor_Butler_Legacy` разобран как источник сценариев, но не как база для wholesale merge. Review перенесен в архив: `docs/archive/2026-06-29-docs-cleanup/LEGACY_REVIEW.md`.
- Первый перенос из Legacy: идея `TelegramAuthService` переписана как `domain.auth.TelegramLoginVerifier` с проверкой полного Telegram login payload, `auth_date`, HMAC-SHA256 и unit-тестами.
- Swagger UI и `/v3/api-docs` локально проверены на `test` profile; UI отвечает, но OpenAPI `paths` пока пустые, потому что основные REST controllers еще не написаны.
- API Contract v0 добавлен в `docs/contracts/API_CONTRACT.md`: DTO/schema-first, общий `ApiErrorResponse`, `ErrorCode`, `GlobalApiExceptionHandler`, стандартные статусы `2xx/3xx/4xx/5xx`, CRUD endpoints для основных ресурсов и OpenAPI customizer для стандартных error responses.
- Team delivery workflow добавлен в `docs/operations/TEAM_DELIVERY_WORKFLOW.md`: Confluence как командная база знаний, Jira как backlog/epic/task слой, Markdown/voice-note формат как промежуточный контракт для автогенерации задач.
- Текущий код в `main` уже содержит local gateway, Swagger groups, Telegram/FSM первый контакт, PostgreSQL persistence для Telegram intake/consents, Redis FSM state, MongoDB/MinIO контур и Kafka user event trail.
- Kafka direction updated 2026-06-04 and simplified 2026-06-05: `astor.user.events` is treated as future backbone topic, local topic uses 3 partitions, and local MVP event values are JSON. Admin Telegram notifications are a human-readable projection of Kafka events, not raw event transport. Kafka record key is a stable user/chat partition key; `eventId`/`idempotencyKey` stays deterministic from source update id.
- Outbox/CDC direction updated 2026-06-04: PostgreSQL `outbox_events` is the durable event handoff, Debezium Connect runs locally on `localhost:8083`, and `scripts/register_debezium_outbox_connector.sh` registers the Debezium Outbox Event Router.
- Debezium/Kafka Connect direction simplified 2026-06-05: local Connect uses built-in `JsonConverter` with schemas disabled. Outbox events route directly into `astor.user.events` as JSON. Direct Spring Kafka publishing was removed from the MVP path to avoid duplicate event routes.
- Kafka Streams is postponed until event projections/aggregations are needed.
- Startup admin notification updated 2026-06-05: after `ApplicationReadyEvent`, backend sends a human-readable Telegram admin/analytics message ("Я в строю. Все работает.") through `TelegramAdminNotifier`. It is enabled by default and can be disabled with `ASTOR_STARTUP_ADMIN_NOTIFICATION_ENABLED=false`.
- LLM timeout handling updated 2026-06-05: local Ollama `ResourceAccessException`/read timeout is treated as graceful degradation. `MessageGatewayService` logs a concise WARN and returns FSM fallback/admin alert instead of printing an ERROR stacktrace.
- Analytics admin Kafka consumer fix 2026-06-05: canonical admin consumer group is `astor-admin-events`. Older local groups `astor-analytics-admin*` came from Avro experiments and are obsolete. The consumer reads JSON and sends human-readable Telegram cards.
- Table booking foundation added 2026-06-05: regular table reservations are modeled separately from `event_bookings`. `venue_tables` stores the AERIS physical seating plan, `table_reservation_orders` stores guest requests, and `table_reservation_holds` stores local occupancy windows. Manager confirmation defaults to Telegram id `876857557`; SBIS is planned behind a table availability/reservation port.
- Table booking REST layer added 2026-06-05: `/api/bookings/tables`, `/api/bookings/tables/availability`, and `/api/bookings/table-reservations` expose the local table model. Creating a reservation inserts an `AWAITING_MANAGER_CONFIRMATION` order and a `HELD` hold; overlapping `HELD`/`CONFIRMED` windows are rejected.
- Table booking hostess confirmation updated 2026-06-05: creating a table reservation now sends an inline-button approval card to `TELEGRAM_HOSTESS_CHAT_ID` instead of manager private chat. Hostess chat buttons `Да`/`Нет` are handled as callback queries before guest FSM. `Да` confirms the reservation, promotes the hold to `CONFIRMED`, acknowledges the hostess chat, and sends a human-readable order to the guest. `Нет` rejects the reservation, releases the hold, and sends the guest a polite refusal with an offer to choose another table/time. Plain text in the hostess chat is consumed but no longer confirms anything, to avoid accidental approvals.
- Table booking plan rule 2026-06-05: before asking the guest to choose a table, Astor must send the AERIS hall plan PDF. Runtime asset is stored in project resources at `src/main/resources/booking/aeris-plan.pdf`; `TELEGRAM_BOOKING_PLAN_PDF_PATH` defaults to `classpath:booking/aeris-plan.pdf`. Do not rely on Desktop/local absolute paths for runtime assets.
- Analytics delivery backpressure 2026-06-06: Kafka remains the durable observability stream, while Telegram admin chat is a human-readable projection with real Telegram API limits. `TelegramAdminNotifier` serializes analytics delivery, waits between messages (`TELEGRAM_ANALYTICS_MIN_SEND_INTERVAL_MS`, default 3200 ms), retries `429 retry_after`, and `AnalyticsKafkaConsumer` marks events processed only after successful admin delivery. Under load, Kafka lag is allowed to grow temporarily instead of silently dropping admin messages.
- FSM spec decision 2026-06-08: `/start` is a safe guest restart, not only first registration. It resets active runtime FSM/drafts, keeps durable PostgreSQL/Kafka facts, sends or refreshes/pins the persistent AERIS preview, and routes by existing consent: `CONSENT_REQUIRED` for unknown guests, `READY_FOR_DIALOG` for known guests.
- Voice input boundary 2026-06-08: Telegram voice/audio should be normalized in the transport/intake layer before business FSM. Downstream scenarios receive canonical text + metadata, so text and transcribed voice share the same routing, observability, and tests.
- STT runtime decision 2026-06-08: containerized `app` enables local STT by default through `ffmpeg` + `faster-whisper` (`ASTOR_STT_ENABLED=true`, command `python3 /app/stt_faster_whisper.py {file}`, model `base`, language `ru`). HuggingFace model cache is persisted in Docker volume `huggingface-cache`. Failed voice transcription is tracked in Redis per chat for 30 minutes: first failure asks the guest to record again, second consecutive failure asks to continue by text. Any successful text/non-voice input resets the retry counter.
- STT diagnostics boundary 2026-06-08: external STT stdout is the only source of guest transcript. stderr/engine warnings are diagnostics metadata and must never be appended to guest text, FSM input or admin fallback quote.
- Semantic router direction 2026-06-08: LLM should move from plain answer generation to intent/entity/stage understanding. Target contract: `SemanticRouter` consumes normalized input + guest context snapshot + scenario graph, returns `SemanticDecision`, and FSM validates/executes transitions. LLM does not own business state.
- Menu/RAG direction 2026-06-08: menu PDFs are source-of-truth assets, while LLM uses a shared menu retrieval/index layer. The RAG context is not stored inside each local LLM instance; all LLM instances receive the same retrieved context from shared service/index.
- AERIS media storage decision 2026-06-08: large video assets such as `INTERIOR.mp4` should live in MinIO/cache, not inside git or the application jar. Target object prefix: `content/aeris/interior/INTERIOR.mp4`; repository stores manifest/metadata and fallback behavior.
- Telegram media fidelity decision 2026-06-08: if a media asset must preserve original format/quality, send it as `SendDocument` rather than `SendVideo`. Inline Telegram video may crop/re-encode previews.
- Public guide decision 2026-06-08: persistent Telegram preview should stay short and link to static public docs. Guest guide and staff guide live under `docs/guest-guide.html` and `docs/staff-guide.html` for GitHub Pages access.
- Graphify evaluation 2026-06-08: `safishamsi/graphify` matches the current graph-memory direction (code/docs/media -> queryable graph, Obsidian/wiki/callflow exports). Do not commit generated `graphify-out` until a separate pilot confirms value and ignore policy.
- Runtime media storage decision 2026-06-08: PDF menus, AERIS floor plan and video tours must not live in `src/main/resources` or the application jar. Runtime binary storage is MinIO/S3, active asset lookup is PostgreSQL `media_assets`, optional inventory/search metadata can still live in MongoDB. Canonical local ingest is `scripts/ingest_aeris_runtime_assets.sh`, which uploads MinIO objects and upserts `media_assets`.
- Telegram start UX decision 2026-06-08: `/start` for a known/consented guest is a safe restart that refreshes/pins the persistent preview and returns to `READY_FOR_DIALOG` without sending a second plain text message below the preview.
- При этом `pom.xml` еще может содержать исторические зависимости JPA/Hibernate/Actuator; целевой persistence direction остается JDBC/Liquibase, а observability direction - Prometheus/Grafana/OpenTelemetry.
- Keycloak/JWT, полноценная idempotency для consumer side, production AI Adapter и load-balancer/multi-instance режим остаются следующими архитектурными шагами.

## Semantic Memory / Timeline / Graph Decision 2026-06-11

- PostgreSQL расширяется через `pgvector` и становится первым semantic/RAG хранилищем проекта.
- Новые таблицы: `semantic_sources`, `semantic_chunks`, `semantic_embeddings`.
- Первый scope pgvector: меню AERIS, guest/staff guides, FSM docs, концепция и будущий semantic routing.
- Redis остается hot runtime слоем: текущий FSM state, drafts, idempotency, retries, rate limits, locks, cache.
- Redis-only state признан недостаточным для аналитики и восстановления, но не ошибочным как hot-path решение.
- Для durable append-only истории вводится ScyllaDB как Cassandra-compatible wide-column layer.
- Scylla/Cassandra хранит guest timeline по query pattern `guest_id -> occurred_at desc`: previous state, next state, intent/actions, text, metadata.
- PostgreSQL не заменяется Cassandra: users, bookings, media catalog, consents и transactional facts остаются в PostgreSQL.
- Neo4j вводится как graph-memory/projection layer для FSM/scenario/capability graph.
- Первый Neo4j graph содержит `FsmState`, `Scenario`, `Capability`, связи `OWNS_STATE` и `CAN_TRANSITION_TO`.
- Neo4j нужен для визуального мышления, impact analysis и будущей preference/recommendation graph, но не является OLTP source of truth.
- LLM не пишет бизнес-состояние напрямую; semantic/router layers читают context, а FSM валидирует и выполняет переходы.

## FSM Implementation Contract 2026-06-12

- Перед глубоким написанием FSM зафиксирован контракт: `/Users/michaelwelly/IdeaProjects/Astor_Butler_MVP/docs/fsm/FSM_IMPLEMENTATION_PLAN.md`.
- Priority order: service chat guard -> transport normalization -> `/start`/first touch -> active state continuation -> composite intent -> side-effect scenarios -> safe content -> money/confirmation -> main menu -> recovery -> AI fallback.
- Каждый сценарий должен двигаться к единому `FsmScenario` contract: `id`, `priority`, `supports`, `handle`, `owns`, `sideEffecting`, `canRunInParallel`.
- `IntentPlan` становится целевым форматом распознавания: `SINGLE`, `PARALLEL_CONTENT`, `SEQUENTIAL`, `ASK_CONFIRMATION`, `RECOVERY`.
- Active state важнее нового intent; новые намерения во время активного сценария уходят в pending intents.
- Recovery policy: сначала одно полезное уточнение, затем повторное уточнение с вариантами, затем recovery/admin handoff. `AI_FALLBACK` не является нормальным маршрутом.
- Redis остается hot layer для current state/drafts/pending intents/retries/idempotency; PostgreSQL хранит transactional facts; Scylla пишет timeline; pgvector помогает semantic routing; Neo4j является graph projection, не source of truth.

## AERIS Channel Content Ingest 2026-06-14

- Для афиши/промо/новостей Quiet Guide вводится отдельный content ingest plan: `/Users/michaelwelly/IdeaProjects/Astor_Butler_MVP/docs/content/AERIS_CHANNEL_INGEST.md`.
- Production target: Astor Butler bot становится админом официального канала AERIS и получает `channel_post` / `edited_channel_post`.
- MVP target до доступа в канал: public HTML parser для `https://t.me/s/aeris_gastrobar`.
- Домен ниже источника строится через `VenueContentSource`, чтобы позже заменить parser на bot-admin source без изменения Quiet Guide и storage слоя.
- Media из постов скачивается сразу в MinIO: `content/aeris/channel/YYYY/MM/{sourceMessageId}/...`.
- PostgreSQL хранит posts/assets/classification, pgvector хранит semantic chunks, Kafka/Scylla пишут ingest timeline.
- Fast media pool чистится через 90 дней, если контент не активен; metadata/raw payload можно держать дольше для аналитики.

## Service Chats And Telegram Cleanup 2026-06-15

- Создан отдельный system chat для служебного контура Astor Butler.
- ID из runtime логов: `TELEGRAM_SYSTEM_CHAT_ID=-5403153261`.
- Service-chat guard теперь должен считать служебными admin, analytics, hostess и system chats.
- Служебные чаты сохраняются/audit/event trail, но не запускают guest FSM, FirstTouch, MainMenu, LLM fallback или сценарии гостя.
- Runtime Telegram message cleanup через `DeleteMessage` временно отключен полностью.
- Причина: на этапе ручного тестирования важно видеть полную историю диалога; будущая UX-чистка должна быть отдельной session policy поверх Redis/timeline, а не немедленным удалением сообщений.
- `FSM_SCENARIOS_VIEWER.html` принят как актуальный визуальный source of truth перед кодингом FSM.

## Table Booking Time Policy 2026-06-27

- В сценарии брони стола запрещены разрозненные вызовы `LocalDate.now()` / `LocalTime.now()`; текущая дата и время проходят через `BookingTimeProvider`.
- `BookingTimeProvider` использует venue timezone `Asia/Yekaterinburg`, чтобы "сегодня", "завтра" и weekdays считались как день заведения, а не случайная timezone JVM/container.
- Default AERIS NLU runtime: state-aware rules + Natasha. Duckling оставлен в коде как archived experimental adapter, но удален из Docker Compose runtime, чтобы не конфликтовать с FSM-правилами даты и текущей недели.
- `TABLE_BOOKING_COLLECT_DATE` показывает reply-кнопки на 21 день вперед с шагом 1 день. Эти кнопки являются обычным текстовым вводом Telegram и проходят через общий NLU/FSM pipeline.
- `TABLE_BOOKING_COLLECT_TIME` показывает reply-кнопки времени с шагом 1 час: для сегодняшней даты старт от ближайшего целого часа, для будущих дат старт от 12:00.
- Свободный ввод остается обязательным: `20:00`, "в 8 вечера", "на пятницу", "на субботу" должны работать через тот же `GuestInputUnderstandingService` + `TableBookingDraftMerger`.
- По умолчанию бронь занимает 2 часа (`requestedStartAt` -> `requestedEndAt`). Post-stay lifecycle ("гости еще сидят?" через 2 часа и продление еще на 2 часа) выделен в следующий отдельный scheduler/notification slice.
- Для ручной разработки быстрее использовать Docker только для инфраструктуры, а `aeris_astor_butler_bot` запускать локально из IDEA; контейнерный app пересобирать перед финальным ручным smoke test.

## Model Gateway / Local Qwen Decision 2026-06-27

- Локальная LLM-топология упрощена до двух Ollama instances: `ollama-1` как frontline inference и `ollama-2` как shadow/learner для промптов, сравнений и будущего fine-tuning review.
- Третий локальный LLM worker не является baseline runtime: бизнес-потоки не должны зависеть от трех моделей или LLM-only решения.
- Local language model split: `FRONTLINE=qwen2.5:1.5b` для быстрых гостевых ответов и `QUALITY=qwen2.5:3b` для shadow/learner, prompt review, summary и более тяжелого анализа.
- В код добавлен первый Java boundary `ModelGateway`: `MessageGatewayService`, `LlmWarmup` и legacy `GreetingHandler` больше не должны зависеть напрямую от `OllamaClient`.
- Текущая основная реализация `SpringAiOllamaModelGateway` использует Spring AI `OllamaChatModel` внутри `ModelGateway`, возвращает provider/model/latency metadata и оставляет FSM источником решений.
- `OllamaModelGateway` / raw `OllamaClient` оставлены как fallback provider (`ASTOR_MODEL_PROVIDER=ollama-raw`) и аварийный fallback внутри Spring AI adapter.
- Spring AI теперь является внутренним provider layer, а не частью FSM: дальше через него можно подключать `ChatClient`, `EmbeddingModel`, `VectorStore`, Advisors и observability. FSM-сценарии не должны знать, локальная это Qwen/Ollama, OpenAI API или другой provider.
- Embeddings подключены как capability contract за тем же gateway: `ModelGateway.generateEmbedding(...)` -> local `nomic-embed-text` -> pgvector/RAG.
- VLM подключен как первый vision capability contract: `ModelGateway.analyzeImage(...)` -> Ollama `/api/chat` with `images`, default `LLM_OLLAMA_VISION_MODEL=qwen2.5vl:3b`.
- Runtime note: `qwen2.5vl:3b` скачан локально, но текущий Docker Desktop memory limit не дает выполнить smoke (`requires 10.1 GiB`, available `9.5 GiB`). Для Qwen2.5-VL нужен больший лимит памяти или отдельный lighter dev fallback; это не меняет gateway-контракт.
- Future STT/TTS/cloud vision/cloud embeddings подключаются тем же способом: модель возвращает candidates/slots/text/summary/confidence, а state transition, order, hold, payment и confirmation решает FSM/domain layer.

## Support Ticket System Design Draft 2026-06-23

- Добавлен текстовый system design draft для AI-powered support/ticket-системы: `/Users/michaelwelly/IdeaProjects/Astor_Butler_MVP/docs/SUPPORT_TICKET_SYSTEM_DESIGN.md`.
- Дизайн переносит принципы Astor Butler на техподдержку: каналы email/site/Telegram как транспорт, Ticket FSM как source of truth, AI Agent как помощник по классификации/ответам/статусам, Kafka/outbox как event trail, system/team chats как human-readable control plane.
- Зафиксирована модель развертывания из трех агентов: один active support agent работает с клиентами, два shadow agents учатся на том же потоке через gateway/observability без customer-facing side effects.
- В дизайн добавлен Semantic Layer по паттерну Butler: repo-owned golden corpus, runtime examples в PostgreSQL, pgvector embeddings, understanding misses, operator corrections и multilingual routing без разветвления Ticket FSM по языкам.
- В качестве GitHub/open-source reference options зафиксированы Chatwoot, Zammad, osTicket, FreeScout, UVdesk, Rasa, Haystack, fastText и Awesome-RAG; это источники паттернов/адаптеров, а не обязательные зависимости core-архитектуры.
- Пока это только текстовый архитектурный черновик, без реализации в production-коде.

## FSM Scenario Router 2026-06-15

- Начато кодовое внедрение предкодового FSM-плана.
- Введен общий Java-контракт `FsmScenario`: `id`, `priority`, `supports`, `handle`, `owns`, `sideEffecting`, `canRunInParallel`.
- `FirstTouchScenario`, `TableBookingScenario`, `MenuAssetsScenario`, `QuietGuideScenario` и `MainMenuScenario` подключены к этому контракту напрямую.
- Введен `ScenarioRouter`: он выбирает сценарий, держит текущую composite-intent логику и освобождает `MessageGatewayService` от знания о конкретных доменных сценариях.
- `MessageGatewayService` остается входным gateway: capture/logging, service-chat guard, STT retry boundary, вызов router, затем LLM/recovery fallback.
- Первый новый explicit-сценарий поверх router-а: `ManagerHelpScenario`.
- `ManagerHelpScenario` вводит states `MANAGER_HELP_COLLECT_REASON` и `MANAGER_HELP_SENT`.
- Короткая просьба "менеджер" сначала собирает причину; содержательная просьба отправляет admin handoff card и возвращает гостя в `READY_FOR_DIALOG`.
- Добавлен `RecoveryScenario` как последний router-сценарий перед LLM/fallback.
- Recovery использует Redis counter `astor:fsm:recovery:retry:telegram:{chatId}` с TTL 1800 секунд.
- Первый непонятный текст в `READY_FOR_DIALOG`/`AI_FALLBACK` дает гостю понятные варианты сценариев без admin alert.
- Повторный непонятный текст отправляет admin recovery card, но возвращает гостя в `READY_FOR_DIALOG`, не делая `AI_FALLBACK` нормальным состоянием.
- Добавлен `ChangeCancelScenario` как безопасный explicit-сценарий изменения/отмены брони.
- Первый слой `ChangeCancelScenario`: собрать дату/время/номер заявки и отправить admin handoff; holds/orders не меняются автоматически.
- Следующий слой для ChangeCancel: поиск active booking/order, explicit confirmation от гостя/команды, затем release/update holds.
- Добавлен первый слой `EventBookingScenario`.
- `EventBookingScenario` отделяет банкет/корпоратив/свадьбу/выкуп зала от обычной посадки за стол.
- MVP-поведение: короткий intent просит структурированные детали; запрос с датой и числом гостей отправляет admin event booking card. Автоподтверждения мероприятия нет.
- Добавлен `SmartTipScenario` как отдельный FSM-сценарий чаевых.
- MVP-поведение Smart Tip: сумма -> подтверждение draft -> подтверждено/отменено -> `READY_FOR_DIALOG`.
- Payment boundary зафиксирован как future SBP integration: бот не берет комиссию и пока не обещает оплату.
- Добавлен `HiddenHeartScenario` как отдельный FSM-сценарий благотворительного вклада.
- MVP-поведение Hidden Heart: intent доната -> сумма -> подтверждение анонимного draft -> подтверждено/отменено -> `READY_FOR_DIALOG`.
- Impact/privacy boundary: в impact-отчет уходит только агрегированный вклад, приватные платежные данные не раскрываются; реальный платеж остается future SBP integration.
- Добавлен `ArtAuctionScenario` как отдельный FSM-сценарий ставок по активному event/lot.
- MVP-поведение Art Auction: intent аукциона -> сумма ставки или запрос суммы -> explicit confirmation -> manager/event owner validation required -> `READY_FOR_DIALOG`.
- Safety boundary: LLM не принимает ставку и не объявляет победителя; финальное принятие требует проверки активного лота, минимального шага и owner/manager confirmation.
- Добавлен `ImpactMeterScenario` как read-only FSM-сценарий культурного вклада.
- MVP-поведение Impact Meter: показать агрегированные итоги донатов/аукционов/чаевых/событий без приватных платежных данных и без side effects.
- Добавлен `FeedbackScenario` как отдельный FSM-сценарий отзыва/обратной связи.
- MVP-поведение Feedback: короткое "оставить отзыв" просит текст, содержательный отзыв отправляет admin handoff card и возвращает гостя в `READY_FOR_DIALOG`.
- Добавлен `MerchScenario` как отдельный FSM-сценарий merch request.
- MVP-поведение Merch: короткий intent просит уточнить предмет, содержательный запрос по мерчу/сабражной цепи отправляет admin handoff card; оплаты и обещания наличия нет до ручного подтверждения.
- Добавлен `SafePlayScenario` как отдельный FSM-сценарий безопасного игрового hospitality-ритуала.
- MVP-поведение Safe Play: короткий intent сабража просит детали, содержательный запрос отправляет team/admin card, dangerous how-to получает отказ и предложение безопасного ритуала с командой AERIS.
- Safety boundary: бот не объясняет технику сабража и не инструктирует гостя выполнять опасное действие самостоятельно; ритуал выполняет только обученная команда после проверки условий.
- `MainMenuScenario` очищен от старых product branches: теперь это только explicit main menu и safe exit из активного сценария.
- Product intents больше не должны добавляться в `MainMenuScenario`; для них создается отдельный `FsmScenario` component + focused tests.
- Sequential composite-intent теперь сохраняет deferred safe-intents в Redis через `FSMStorage.setPendingIntents`.
- Первый Redis key для pending intents: `astor:fsm:telegram:{chatId}:pending-intents`; TTL совпадает с FSM state TTL.
- Pending intent хранит код + точную prompt-подсказку, например `MENU_ASSETS::покажи винную карту`, чтобы при resume не отправлять лишние материалы.
- `ScenarioRouter` умеет исполнять сохраненный safe-content pending intent после успешного completion-action и очищать Redis pending key.
- `TableReservationNotificationService` после guest-confirmed notification вызывает bridge для pending intents: если хостес подтвердила бронь кнопкой, гость все равно получает обещанную винную карту/меню/Quiet Guide материал.
- Сверка с viewer на 2026-06-15: 16 сценарных блоков имеют MVP Java `FsmScenario`; `AERIS Channel Ingest` теперь реализован как MVP runtime pipeline для Quiet Guide, а `MerchOrderScenario` в viewer сейчас реализован как MVP `MerchScenario`.
- AERIS Channel Ingest MVP: `PublicTelegramHtmlSource` парсит public `t.me/s/aeris_gastrobar`, `VenueContentClassifier` классифицирует rules-based, `VenueContentAssetStorageService` зеркалит media в MinIO, `VenueContentRepository` хранит `venue_content_posts/assets`, `QuietGuideScenario` отвечает активными постами на афишу/акции/что сегодня.
- Следующие сценарии нужно добавлять через отдельные `FsmScenario` components и focused tests, не раздувая `MessageGatewayService`.

## DB-First FSM Vertical Slices 2026-06-15

- Пользователь зафиксировал способ наращивания сценариев: идти от БД вверх, а не только от Telegram/контроллера вниз.
- Definition of Done для каждого сценария:
  - PostgreSQL durable facts;
  - Redis hot state/drafts/pending intents;
  - Scylla timeline where applicable;
  - pgvector semantic examples/chunks where intent/RAG matters;
  - Neo4j scenario graph projection where useful;
  - MinIO media/files where applicable;
  - service/domain layer;
  - REST API/Swagger;
  - focused tests;
  - container rebuild before Natalia/manual test.
- Первый vertical slice: `FirstTouch + MainMenu` runtime visibility.
- Добавлен Telegram FSM runtime API:
  - `GET /api/fsm/telegram/{chatId}/state`;
  - `POST /api/fsm/telegram/{chatId}/reset`;
  - `PUT /api/fsm/telegram/{chatId}/state`;
  - `DELETE /api/fsm/telegram/{chatId}/state`.
- Эти endpoints читают PostgreSQL identity/consent/message facts и Redis FSM state/pending intents/table-booking draft.
- Reset ведет себя как безопасный `/start` runtime reset: consent есть -> `READY_FOR_DIALOG`, профиль без consent -> `CONSENT_REQUIRED`, неизвестный chat -> `UNKNOWN`.
- Второй vertical slice: `MenuAssets + QuietGuide` runtime content visibility.

## Guest Input Understanding 2026-06-17

- Перед `ScenarioRouter` вводится общий `GuestInputUnderstandingService`.
- Цель слоя: переводить живой ручной ввод гостя, STT transcript и кнопки главного меню в машинный контракт `UnderstoodInput`.
- `UnderstoodInput` содержит `rawText`, `normalizedText`, `primaryIntent`, `confidence`, `slots`, `candidates`, `needsClarification`.
- Этот слой не принимает бизнес-решений и не подтверждает бронь/платеж/ставку/отмену; FSM остается source of truth по допустимым переходам.
- Первый corpus живых фраз хранится в репозитории: `src/main/resources/understanding/guest-input-golden-corpus.jsonl`.
- Corpus нужен, чтобы проверять фразы без Telegram manual test: "на завтра", "в 8 вечера", "на 2х", "на двоих", "тихий стол в винной комнате".
- Первый corpus расширен по брони стола: full booking intent, date replies, time replies, party size replies, table/zone selection, menu/guide/safe-play button phrases.
- Parser/normalizer теперь понимает часть живых форм: "к восьми", "около 8", "20 ч", "будем вдвоем", "нас двое", "стол 7", "выбери сам".

## Table Booking State-Driven Refactor 2026-06-26

- `TableBookingScenario` больше не должен быть местом, где живут regex parsing, NLU и бизнес-ветвление одновременно.
- Новый runtime shape:
  - `GuestInputUnderstandingService` нормализует ввод и возвращает `UnderstoodInput`;
  - `ScenarioRouter` передает `UnderstoodInput` в выбранный `FsmScenario`;
  - `TableBookingDraftMerger` мержит NLU slots + stored draft + fallback parsing в единый booking draft;
  - `TableBookingStepRegistry` выбирает следующий missing FSM step;
  - `TableBookingScenario` только выполняет step/order side effect и ставит FSM state;
  - `BookingPhraseService` является точкой будущего LLM phrasing, но не владельцем state или side effects.
- Решение отвечает на проблему ручных вариантов вроде "на одного", "на двоих", "на троих": они должны покрываться NLU/slot layer и проверяться через corpus/tests, а сценарий должен работать с `partySize`, а не угадывать фразу гостя.
- FSM остается source of truth: если слот понят, он заполняет draft; если draft неполный, следующий вопрос выбирается строго по `TableBookingStepRegistry`.

### Table Booking Date/Time Guard 2026-06-26

- `TableBookingDraftMerger` сначала парсит ISO date strings, затем русские weekdays, и только потом локальные numeric date patterns.
- Это защищает от бага `2026-07-03` -> `07.03.2027`.
- `TableBookingScenario` не создает order без `requestedTime`; time step обязателен между date и party size/order.
- `TableReservationNotificationService` показывает дату и время отдельными строками в Staff Chat и гостевых сообщениях.
- Seating preference step принимает свободный текст как пожелание, чтобы фразы вроде "возможно будет четыре" не терялись и уходили команде.

### Table Booking UX Boundary 2026-06-27

- Новый happy path брони больше не задает финальный вопрос "Есть пожелания по посадке?" после количества гостей.
- Пожелания по посадке относятся к раннему шагу плана/выбора места: "у окна", "винная комната", "тихий стол", "у бара" сохраняются как `preferredZone` / `seatingPreference` во время `TABLE_BOOKING_WAIT_TABLE_SELECTION`.
- После выбора времени сценарий должен убрать временную reply-клавиатуру, чтобы она не висела под вопросом про количество гостей.
- После создания заявки гость возвращается в `READY_FOR_DIALOG` с главным меню. Ожидание хостес остается order lifecycle в БД/staff chat, а не runtime-state гостевого диалога.
- Финальные сообщения гостю после подтверждения, отказа или отмены брони должны также возвращать главное меню.
- Vision/LLM по картинке плана не является MVP-обязательством. Надежный путь: структурная карта столов в БД (`table_code`, zone, capacity, tags, coordinates/polygon). Vision для фото с обведенным столом можно добавить позже как отдельный multimodal pipeline.

### Model Gateway / Multimodal Layer 2026-06-27

- Astor Butler будет развиваться не как прямой вызов к одной LLM, а как `Model Gateway` со сменными capabilities:
  - язык: local Qwen/Ollama, production fallback OpenAI/API;
  - глаза: local Qwen2.5-VL/MiniCPM-V/OCR/OpenCV, production fallback managed vision;
  - уши: faster-whisper или managed STT;
  - голос: будущий TTS;
  - embeddings: pgvector corpus/RAG или cloud embeddings.
- FSM остается центром принятия решений. Model Gateway возвращает только `candidates`, `slots`, `confidence`, `summary`, `transcript` или `text`.
- Модель не имеет права самостоятельно создавать бронь, платеж, ставку, отмену, hold или подтверждение.
- Local-first policy:
  - простые FSM-bound сценарии, приватные данные и ручной тест идут локально;
  - cloud/API подключается для сложного vision, hard dialogue, summary и customer-facing качества;
  - `ollama-2` может быть shadow/learner, но не блокирует гостевой поток.
- Local Ollama profile split:
  - `FRONTLINE=qwen2.5:1.5b` для быстрых гостевых ответов в Telegram/web;
  - `QUALITY=qwen2.5:3b` для shadow/learner, промпт-ревью, summary и тяжелого анализа;
  - `ModelTextRequest.profile` выбирает профиль, а текущие пользовательские ответы по умолчанию идут через `FRONTLINE`.
- Vision для плана AERIS:
  1. offline annotation: `AERIS PLAN.pdf` -> `tableCode`, `zone`, `capacity`, `polygon/box`, `tags`;
  2. manual review;
  3. storage in PostgreSQL (`venue_tables` + future plan regions);
  4. runtime photo understanding maps guest circle/checkmark to top table candidates;
  5. FSM validates confidence and availability before draft/order changes.
- Документировано в `docs/architecture/ARCHITECTURE.md`, `docs/fsm/FSM_SCENARIOS.md`, `docs/FSM_SCENARIOS_VIEWER.html#model-gateway`.

## Russian NLU Toolchain 2026-06-25 / Updated 2026-06-29

- Natasha переведена из идеи spike в runtime-ready adapter перед `GuestInputUnderstandingService`.
- Duckling остается только archived spike/experimental adapter и не является частью Docker Compose runtime.
- External NLU не является source of truth: он только обогащает вход слотами (`time`, `date`, `number`, `partySize`, `tableNumber`, `seatingPreference`).
- FSM остается единственным валидатором допустимых переходов и side effects.
- Duckling adapter `astor.nlu.duckling.*` оставлен выключенным для будущего сравнения, но штатно не запускается.
- Natasha подключается как HTTP adapter `astor.nlu.natasha.*` для будущего Python-сервиса морфологии/NER; Java-приложение не тащит Python runtime внутрь себя.
- Все adapters имеют короткий timeout и graceful fallback: если внешний NLU недоступен, бот продолжает работать на локальных rules + pgvector intent examples.
- Golden corpus остается первым контуром качества: новые живые фразы сначала добавляются в `src/main/resources/understanding/guest-input-golden-corpus.jsonl`, затем в approved examples/pgvector.
- Контейнерное подключение через compose profile `nlu`: только `natasha-nlu`.
- `scripts/start_container_stack.sh` поднимает Natasha NLU stack по умолчанию; отключение: `ASTOR_NLU_STACK_ENABLED=false scripts/start_container_stack.sh`.
- Runtime direction: repo-owned corpus остается воспроизводимым seed, а Postgres/pgvector позже хранит runtime-копию examples/negative examples для semantic routing.
- `FSM_SCENARIOS.md` и `FSM_SCENARIOS_VIEWER.html` получили отдельный раздел `Guest Input Understanding` между `Semantic Context / pgvector` и `Composite Intent Plan`.

## Spring AI + pgvector Intent Examples 2026-06-17

- Добавлен runtime слой для approved intent examples поверх PostgreSQL/pgvector:
  - `intent_examples`;
  - `intent_example_embeddings`;
  - `intent_understanding_misses`.
- `GuestInputUnderstandingService` теперь работает как единый NLU gate перед FSM:
  1. быстрые code rules;
  2. lexical lookup по approved examples в PostgreSQL;
  3. optional pgvector lookup, если включен embedding provider.
- Embeddings включаются явно:
  - `ASTOR_SEMANTIC_EMBEDDINGS_PROVIDER=none` остается безопасным локальным default для unit/dev без модели;
  - `model-gateway` является целевым AERIS runtime режимом: `ModelGatewayEmbeddingProvider` вызывает `ModelGateway.generateEmbedding(...)`;
  - активная Spring AI реализация `SpringAiOllamaModelGateway` использует локальный Ollama `nomic-embed-text`, а raw Ollama `/api/embed` остается fallback;
  - прямые `spring-ai` и `ollama` providers оставлены как diagnostic/legacy options, но новые сценарии должны идти через `model-gateway`.
- Startup ingest корпуса выключен по умолчанию и включается флагом `ASTOR_INTENT_EXAMPLES_INGEST_ON_STARTUP=true`.
- Для intent examples vector dimension не фиксируется на 1536, потому что OpenAI и локальные Ollama embedding models могут иметь разные размерности. Retrieval фильтрует rows по `embedding_dimension`.
- Duckling вынесен в отдельный spike, не в runtime: `docs/research/NLU_TOOLS_SPIKE.md` и `scripts/spike_russian_nlu_tools.py`. Natasha остается runtime-сервисом в compose profile `nlu`.
- Добавлен AERIS content read API:
  - `GET /api/content/aeris/menu-assets`;
  - `GET /api/content/aeris/quiet-guide?prompt=...`.
- `menu-assets` возвращает 4 активных PDF меню из PostgreSQL `media_assets` / `AerisMediaCatalog` с MinIO public URL и `ragSource`.
- `quiet-guide` возвращает интерьер-видео, approved concept copy и активные посты AERIS channel ingest с mirrored assets, если они есть в `venue_content_posts/assets`.
- Container smoke 2026-06-15: `/api/content/aeris/menu-assets` вернул 4 asset codes; `/api/content/aeris/quiet-guide?prompt=афиша` вернул interior/concept и `activePosts=0` в текущей локальной БД.
- Третий vertical slice: `TableBooking` runtime read visibility.
- Добавлен `TableBookingRuntimeService` и API:
  - `GET /api/bookings/table-reservations/{id}`;
  - `GET /api/bookings/table-reservations/telegram/{chatId}?limit=...`;
  - `GET /api/bookings/table-reservations/telegram/{chatId}/runtime?venueCode=AERIS`.
- Runtime view показывает Redis draft, floor plan asset, seeded venue tables, availability если draft полон, active reservations и latest reservations.
- Это пока read/observability layer; бизнес-проход Telegram-сценария создания/подтверждения брони остается в существующем `TableBookingScenario` + `TableReservationService`.
- Container smoke 2026-06-15: `astor_app` healthy; Natalia runtime endpoint returned floor plan, tables and no current reservations in local DB.

## Table Booking / Change-Cancel UX Patch 2026-06-30

- `api-gateway` no longer logs repeated `waiting for c3flex-astor-butler-bot`; the default upstream is the local AERIS app port and the wait loop is silent.
- Duckling runtime remains archived/disabled; orphan `astor_duckling_nlu` was removed with compose `--remove-orphans`.
- Booking prompts now acknowledge the previous understood slot before asking the next step:
  - party size: `Принял, на ... гостей`;
  - date: `Принял, держим дату ...`;
  - time: `Хорошо, на ...`;
  - table/zone: `Отлично, место отметил`.
- If the guest gives invalid input at the time step, FSM stays in `TABLE_BOOKING_COLLECT_TIME`, answers hospitably and shows the time buttons again instead of guessing.
- `TableReservationService.reject(...)` now asks the repository for alternatives before releasing the rejected order:
  - first same-zone alternatives by capacity/rating;
  - then best global alternatives by rating;
  - guest rejection copy includes the best alternative without discarding collected context.
- Staff and guest table labels are localized in runtime text: legacy `Table X` becomes `Стол X`, legacy `VIP X` becomes `Стол VIP X`. A migration updates AERIS display names in `venue_tables`.
- `ChangeCancelScenario` no longer asks for an order number when only one active booking exists. It shows a full booking card plus action keyboard:
  - `Отменить стол`;
  - `Изменить гостей`;
  - `Изменить стол`;
  - `Перенести время`;
  - `Перенести дату`;
  - `Отменить действие`.
- Current MVP depth for change actions:
  - cancel table is implemented end-to-end: releases hold, notifies through existing table reservation service, returns to main menu;
  - change guests/table/date/time is implemented end-to-end for table reservations: the selected pending action is stored in Redis, the next text/button is parsed through the same booking understanding rules, the order/hold is updated, status returns to `AWAITING_MANAGER_CONFIRMATION`, and staff receives a fresh approval card;
  - if several active bookings exist, the guest first selects the target order by button; if only one exists, no order number is requested;
  - invalid values keep the guest inside the same pending action instead of falling into generic fallback.

## Safe Play Wine RAG / Sabrage 2026-07-01

- `SafePlayScenario` теперь разделяет:
  - справочный запрос "какое игристое/шампанское взять под сабраж";
  - операционную заявку "хочу сабраж к столу";
  - dangerous how-to, где бот отказывается давать инструкцию и предлагает безопасный ритуал с командой.
- Справочный запрос идет через `SemanticRetrievalService` по source codes:
  - `AERIS_MENU_WINE_SOURCE`;
  - `AERIS_SAFE_PLAY_SOURCE`.
- Добавлен seed `src/main/resources/semantic/aeris/safe-play-sabrage-rag-seed.md` с позициями и ценами из винной карты AERIS:
  - Mont Marcal Cava Brut;
  - Tenuta Dodici 12 Prosecco;
  - Cuvée Françoise Crémant;
  - Bernard Remy Champagne;
  - Moët & Chandon Brut Imperial;
  - другие sparkling/champagne options.
- Safety boundary сохраняется: бот может подобрать стиль/бюджет и приложить винную карту, но не объясняет технику сабража для самостоятельного выполнения.
- `ScenarioReplyComposer` остается включенным, но ограничен timeout; если локальная LLM медленная, сценарий возвращает approved fallback и сохраняет модельный audit.
- Future event layer: "33 сабража за вечер" фиксируется как Safe Play / event seed и позже связывается с `ArtAuctionScenario` и media/storytelling layer.
- `ScenarioRouter` теперь использует уверенный `GuestInputUnderstandingService.primaryIntent` как приоритетный route hint до общего ordered scenario loop. Это устраняет класс багов, где NLU понимал `SAFE_PLAY`, но `TableBookingScenario` перехватывал фразу из-за слов вроде "стол" или времени.

## System Trace / Semantic Cache 2026-07-01

- `TelegramSystemNotifier` расширен с простого FSM transition до dialog trace card:
  - stable `#dialog_*` tag;
  - guest input;
  - app reply;
  - previous/next state;
  - actions;
  - Kafka outbox status;
  - correlation id.
- `UserEventFactory.USER_MESSAGE_RECEIVED` теперь сохраняет `outgoingText` и `dialogKey` в outbox payload, чтобы диалог можно было восстанавливать из Kafka/PostgreSQL, а не только глазами в Telegram.
- `UserEventProducer.publishIncomingMessage(...)` возвращает boolean `kafkaOutboxQueued`, чтобы system chat видел, ушло ли событие в outbox.
- Redis semantic response cache принят как следующий performance/ML слой:
  - Redis хранит горячие проверенные ответы на частые вопросы;
  - pgvector ищет похожие вопросы и source chunks;
  - PostgreSQL/Kafka остаются durable audit/dataset;
  - learner/shadow model и OpenAI teacher/evaluator готовят улучшенные кандидаты, но не блокируют гостя;
  - cache не имеет права обходить FSM/domain validation.
- Guest pinned preview обновлен до версии `2026-07-01-system-trace-preview`.

## Связанные продуктовые заметки

- `/Users/michaelwelly/Obsidian/Astor_Butler_Knowledge/02_Product/Event_Booking_Process.md`
- `/Users/michaelwelly/Obsidian/Astor_Butler_Knowledge/02_Product/Required_Documents.md`
- `/Users/michaelwelly/Obsidian/Astor_Butler_Knowledge/02_Product/Booking_Data_Model.md`
- `/Users/michaelwelly/Obsidian/Astor_Butler_Knowledge/02_Product/Consent_Vault.md`
- `/Users/michaelwelly/Obsidian/Astor_Butler_Knowledge/03_FSM/FIRST_TOUCH_FSM.md`
- `/Users/michaelwelly/Obsidian/Astor_Butler_Knowledge/04_Tech/Event_Taxonomy.md`
- `/Users/michaelwelly/Obsidian/Astor_Butler_Knowledge/04_Tech/Local_Runbook.md`
