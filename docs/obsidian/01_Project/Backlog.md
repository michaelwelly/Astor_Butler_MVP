# Backlog

## Update 2026-06-08

### Now

- Дочитать FSM внимательно еще раз:
  - `docs/fsm/FSM_SCENARIOS.md`;
  - `docs/FSM_SCENARIOS_VIEWER.html`;
  - runtime код `MessageGatewayService`, `FirstTouchScenario`, `MainMenuScenario`, `TableBookingScenario`.
- Старый table-booking FSM draft лежит в архиве `docs/archive/2026-06-29-docs-cleanup/`; не использовать его как актуальную постановку.
- Сверить, что diagram/spec/runtime говорят одно и то же:
  - `UNKNOWN` -> `CONSENT_REQUIRED`;
  - contact/consent -> `READY_FOR_DIALOG`;
  - `READY_FOR_DIALOG` -> scenario router;
  - каждый сценарий возвращается в `READY_FOR_DIALOG`;
  - admin/analytics chats не попадают в гостевую FSM.
- Разобраться с preview UX:
  - для ручных тестов reset Натальи должен сбрасывать `preview_message_id`;
  - продуктово решить, нужно ли resend preview на `/start`, если preview уже был отправлен.
- Использовать `scripts/reset_natalia_test_user.sh` перед каждым ручным тестом Натальи.
- Довести/закоммитить незакоммиченный fix table booking draft accumulation, если тесты подтверждают поведение.
- Прогнать ручной сценарий Натальи:
  - `/start`;
  - contact/consent;
  - "Хочу забронировать столик завтра на 20:00 на двоих";
  - AERIS plan виден гостю;
  - выбор стола;
  - карточка хостес с кнопками;
  - подтверждение гостю;
  - Kafka/admin events.

### Next

- Добавить автоматический E2E-smoke для Telegram-like REST simulation именно под table booking manual flow.
- Решить стратегию Telegram preview:
  - persistent once per preview version;
  - resend on `/start`;
  - или reset-only для QA.
- Развести `MainMenuScenario` и `TableBookingScenario` так, чтобы все product axes были видны в FSM, но не уходили в LLM без явного сценария.
- Обновить `FSM_SCENARIOS_VIEWER.html` после runtime-сверки, если найдены расхождения.
- Зафиксировать отдельный ручной test run report после проверки Натальи.

### Backlog / Later

- Production AI adapter вместо локального Ollama.
- Keycloak/JWT.
- CI quality gates.
- Web chat на том же `MessageGatewayService`.
- SBIS port для реального статуса столов.
- Manager dashboard API.
- Event booking v2 по материалам Яны.

Дата актуализации: 2026-06-04

## Now

- Cleanup repository: удалить sandbox-классы и локальные `.codex*` артефакты.
- Проверить `mvn test`.
- Сделать cleanup commit без `.env`, `target/**`, `.idea/**`.
- Зафиксировать Obsidian как источник оперативной проектной памяти.
- Проверить после reboot локальный запуск Docker infrastructure + Spring Boot.
- Провести архитектурную приборку по `01_Project/Cleanup_And_Architecture_Plan.md`.
- Разделить документы/API на реальные, черновые и устаревшие.
- Составить карту кода: где Telegram, FSM, Consent, Kafka, Mongo/Media, capability boundaries.
- Зафиксировать cleanup item: старые `FSMRouter`/`FSMHandler` не должны развиваться параллельно с `MessageGatewayService`.
- Выполнено: legacy `FSMRouter`/concrete handlers выведены из Spring runtime, чтобы не шумели как активная FSM.
- Использовать новые audit docs:
  - `04_Tech/Code_Map.md`;
  - `01_Project/Documentation_Audit.md`;
  - `03_FSM/FSM_Master_Plan.md`;
  - `04_Tech/Capability_Map.md`;
  - `04_Tech/API_Reality_Check.md`;
  - `04_Tech/Document_Media_Audit.md`;
  - `01_Project/Local_Workspace_Audit.md`.
- Учебные SQL из `src/main/resources/sqriptText/` удалены из production repo.

## Next

- Решить судьбу legacy FSM layer:
  - проверить зависимости `FSMRouter`/`FSMHandler`;
  - выбрать финально: удалить или переписать как `FSM Orchestrator`;
  - не плодить новую бизнес-логику в двух местах.
- Довести `FIRST_TOUCH` FSM:
  - `/start` - done in `FirstTouchScenario`;
  - consent prompt - done in `FirstTouchScenario`;
  - contact sharing - done in `FirstTouchScenario`;
  - profile/consent persistence - first slice done;
  - first free text before consent - done via LLM nudge;
  - fallback to LLM;
  - admin escalation.
- Добавить transition table для `FirstTouchScenario` - done.
- Введены `CONSENT_REQUIRED`/`READY_FOR_DIALOG`, старые `CONTACT`/`MENU` оставлены как Redis-compatible legacy aliases.
- Решить, нужен ли отдельный `CONTACT_REQUIRED`, или `CONSENT_REQUIRED` достаточно для первого MVP.
- Разделить admin/debug delivery feature flags:
  - Kafka events в admin chat;
  - LLM responses в admin chat;
  - raw Telegram messages в admin chat.
- Описать и стабилизировать Kafka event taxonomy.
- Подготовить Swagger/API группы под Consent Vault, FSM, User, Booking, Timeline, Media, Notifications.
- Согласовать FSM-статусы с capability modules.
- Подготовить простую цепочку сценариев для будущей диаграммы защиты.
- Подготовить System Design ДЗ:
  - sequence diagram;
  - API list;
  - functional requirements diagram;
  - non-functional requirements technologies.

## Later

- `EVENT_BOOKING` v2 поверх сохраненного Telegram profile.
- Slot Keeper: слоты, напоминания, timeline events.
- Memory Engine: распознавание гостя по phone/profile/history.
- Preference Map: "как в прошлый раз".
- Manager dashboard API.
- Keycloak/OAuth2/JWT.
- Load balancer + multi-instance local simulation.
- k6 load scenario for read/idempotent endpoints.
- GitHub Actions build/test/checkstyle.
- Confluence/Jira или Notion-as-Confluence workflow.

## Blocked / Needs Input

- Реальные материалы ресторана/площадки от Яны.
- Финальные правила политики обработки данных.
- Решение по production S3/Yandex Object Storage.
- Решение по сильной LLM и бюджетам.
- Решение по публичному senior review date.

## Parking Lot

- C3FLEX frontend правит отдельный frontend contributor в отдельной ветке/PR.
- MongoDB document ingestion из локального/Yandex Disk контента.
- MinIO тестовые 10 media assets для frontend.
- Arena/Reboot/Impact внешние блоки не делать до core flow.
