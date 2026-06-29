# FSM Index

## Цель

Описать управляемые сценарии общения AI-менеджера с клиентом ресторана/event-площадки.

## Первый крупный сценарий

Бронирование даты под мероприятие:

1. Приветствие и определение намерения.
2. Сбор типа мероприятия.
3. Сбор даты и времени.
4. Сбор количества гостей.
5. Сбор бюджета или диапазона.
6. Сбор формата: банкет, фуршет, кофе-брейк, смешанный формат.
7. Сбор пожеланий по меню.
8. Сбор технических требований.
9. Сбор данных клиента.
10. Подтверждение структуры заявки.
11. Передача менеджеру или формирование документа бронирования.

## Нужно прописать

- состояния;
- события;
- переходы;
- обязательные поля;
- fallback-сценарии;
- что делать при неясном ответе;
- когда подключается живой менеджер.

## Specs

`/Users/michaelwelly/Obsidian/Astor_Butler_Knowledge/03_FSM/EVENT_BOOKING_FSM.md`

`/Users/michaelwelly/Obsidian/Astor_Butler_Knowledge/03_FSM/FIRST_TOUCH_FSM.md`

Старые standalone-документы `TABLE_BOOKING_FSM.md` и `FSM_WORKING_SCENARIOS_UML.puml` архивированы в:

`/Users/michaelwelly/IdeaProjects/Astor_Butler_MVP/docs/archive/2026-06-29-docs-cleanup/`

Русское описание текущего runtime flow:

`/Users/michaelwelly/IdeaProjects/Astor_Butler_MVP/docs/fsm/FSM_SCENARIOS.md`

Крупный визуальный viewer всей FSM-инфраструктуры:

`/Users/michaelwelly/IdeaProjects/Astor_Butler_MVP/docs/FSM_SCENARIOS_VIEWER.html`

Статус 2026-06-16:

- `PreferenceScenario` добавлен как сценарий `8.1 Preference`: гость явно просит что-то запомнить, система сохраняет только guest-provided preference и возвращает гостя в `READY_FOR_DIALOG`.
- `ConciergeScenario` добавлен как сценарий `16.1 Concierge`: гость просит передать команде конкретное сервисное действие, система создает `concierge_request`, отправляет карточку команде/admin и не обещает выполнение без подтверждения людей.
- `FSM_SCENARIOS_VIEWER.html` и `FSM_SCENARIOS.md` синхронизированы по этим сценариям: registry, intake matrix, MainMenu routes, Mermaid flow и предкодовые тесты.
- Следующий кодовый фокус: наполнить сценарии глубокой бизнес-логикой и ручными Telegram smoke-test cases, двигаясь bottom-up от БД/API к FSM/Telegram.

Статус 2026-06-17:

- В viewer и `FSM_SCENARIOS.md` добавлен отдельный слой `1.3 Guest Input Understanding`.
- `GuestInputUnderstandingService` стоит перед `ScenarioRouter` и нормализует живой ввод гостя в `UnderstoodInput`: intent, confidence, slots, candidates.
- Первый golden corpus для проверки фраз без Telegram лежит в `src/main/resources/understanding/guest-input-golden-corpus.jsonl`.
- Corpus расширен первым набором для брони стола: дата, время, количество гостей, стол/зона, full intent и несколько menu/guide/safe-play кнопок.
- Цель слоя: меньше ручных Telegram-прогонов и меньше сценарных `if` внутри каждого FSM-сценария.
- Добавлен runtime knowledge слой для понимания ввода:
  - `intent_examples`;
  - `intent_example_embeddings`;
  - `intent_understanding_misses`.
- `GuestInputUnderstandingService` теперь может повышать confidence через approved examples / pgvector, но FSM остается source of truth по допустимому переходу.
- Natasha используется как runtime Russian NLU service; Duckling оставлен только как архивный spike и не поднимается Docker Compose.

Статус 2026-06-25:

- Natasha описана в viewer и архитектуре как runtime Russian NLU toolchain перед FSM; Duckling зафиксирован как archived spike.
- В код добавлен общий `RussianNluAdapter` contract; Duckling adapter оставлен выключенным для сравнения, но удален из Docker Compose runtime.
- `GuestInputUnderstandingService` теперь принимает внешние NLU slots, но оставляет маршрутизацию и side effects за FSM.
- Табличная бронь усилена через универсальное понимание коротких ответов: `8` в состоянии сбора времени -> `20:00`, `На 2` в состоянии гостей -> party size, `7` в выборе стола -> table number.
- Фраза "Хочу стол завтра в восемь вечера на двоих, тихий стол в винной комнате" теперь маршрутизируется в `TABLE_BOOKING`, а "винной комнате" считается seating preference, не запросом винной карты.

Статус 2026-06-26:

- Начат рефактор table booking под фактическую FSM Implementation Plan.
- `TableBookingScenario` теперь должен отвечать за orchestration, а не за распознавание всех фраз гостя.
- Слой понимания (`UnderstoodInput`) передается из `ScenarioRouter` внутрь сценария.
- `TableBookingDraftMerger` заполняет draft из slots + stored state + fallback parsing.
- `TableBookingStepRegistry` определяет следующий шаг алгоритма брони:
  1. отправить план / выбрать стол или зону;
  2. собрать дату;
  3. собрать время;
  4. собрать количество гостей;
  5. создать заявку хостес и вернуть гостя в главное меню.
- Target: дальше расширять corpus/NLU, а не добавлять все варианты "на одного/двоих/троих" внутрь сценария.

Hotfix 2026-06-26:

- Заявка на бронь не должна создаваться без времени. Если есть дата, но нет времени, следующий state остается `TABLE_BOOKING_COLLECT_TIME`.
- Duckling ISO date (`2026-07-03`) обрабатывается как ISO date, а не как локальный шаблон `07.03.2027`.
- Русские weekdays ("на пятницу") считаются относительно текущей даты/зоны, не через случайный numeric fallback.
- Любой free-text на шаге seating preference сохраняется в заявку как пожелание, даже если NLU не распознал слот.
- В Staff Chat дата и время показываются раздельно: `Дата` + `Время`, чтобы не маскировать midnight fallback.

Hotfix 2026-06-27:

- В table booking введен `BookingTimeProvider` как единая точка текущей даты/времени в зоне AERIS (`Asia/Yekaterinburg`).
- `TABLE_BOOKING_COLLECT_DATE` теперь отдает Telegram reply-кнопки на 21 день от сегодняшнего дня, но продолжает принимать свободный текст: "сегодня", "завтра", "на пятницу", `03.07`.
- Weekday semantics: "на пятницу" означает ближайшую пятницу в текущей неделе заведения, включая сегодня, если сегодня пятница.
- `TABLE_BOOKING_COLLECT_TIME` отдает reply-кнопки времени с шагом 1 час: сегодня от ближайшего целого часа, будущие даты от 12:00.
- Кнопки даты/времени идут через тот же Telegram text pipeline, что и ручной ввод/STT, поэтому FSM/NLU остаются единственным маршрутом сценария.
- Для быстрого ручного теста AERIS bot container можно остановить и запускать Spring Boot из IDEA, оставляя PostgreSQL/Redis/Kafka/MinIO/Mongo/Scylla/Neo4j/NLU/LLM в Docker.
- После выбора времени временная клавиатура должна исчезать перед вопросом о количестве гостей.
- Пожелания посадки больше не являются финальным обязательным шагом. Они собираются на уровне плана/стола/зоны: "у окна", "винная комната", "тихий стол".
- После создания заявки FSM гостя возвращается в `READY_FOR_DIALOG`; ожидание хостес остается статусом order lifecycle, не блокирующим диалог.

Статус 2026-06-15:

- `FSM_SCENARIOS_VIEWER.html` принят как актуальный визуальный source of truth для карты FSM, нумерации сценариев и предкодового тест-плана.
- `FSM_SCENARIOS.md` остается текстовым companion-документом и должен синхронизироваться с viewer.
- Service chats не запускают guest FSM: admin, analytics, hostess и новый system chat.
- Новый system chat: `TELEGRAM_SYSTEM_CHAT_ID=-5403153261`.
- Telegram message cleanup временно отключен полностью; UX-чистка будет проектироваться как session policy через Redis/timeline, а не через немедленный `DeleteMessage`.

Implementation contract перед глубоким написанием FSM:

`/Users/michaelwelly/IdeaProjects/Astor_Butler_MVP/docs/fsm/FSM_IMPLEMENTATION_PLAN.md`

Фиксирует:

- priority order входящих сообщений;
- единый `FsmScenario` contract;
- `IntentPlan` / composite intent policy;
- target states и draft storage policy;
- recovery policy;
- manual test checklist.

В viewer и `FSM_SCENARIOS.md` добавлен product layer по 8 дипломным осям боли:

- Memory Engine / идентичность;
- Preference Map / персонализация;
- Smart Tip / благодарность;
- Quiet Guide / инфо-поддержка;
- Hidden Heart / социальный вклад;
- Safe Play / игровой опыт;
- Slot Keeper / управление временем;
- Panic Exit / безопасность.

Legacy используется как продуктовая память, но не как архитектура для копирования:

- `auction/*` -> `ArtAuctionScenario`;
- `charity/*` -> `HiddenHeartScenario`;
- `tip/*` -> `SmartTipScenario`;
- `poster/*` -> `QuietGuideScenario`;
- `merch/*` -> future commerce/order boundary;
- `feedback/*` -> future feedback / manager help.

Текущий договор до кода:

- сначала согласовать `FSM_SCENARIOS.md` и viewer глазами;
- `MainMenuScenario` уже есть как базовый вход в продуктовые ветки;
- 2026-06-15 введен `FsmScenario` contract и `ScenarioRouter`;
- после этого новые домены пишутся как отдельные сценарии с focused tests и подключаются к router-у, а не напрямую в `MessageGatewayService`.

Runtime LLM contract:

`/Users/michaelwelly/IdeaProjects/Astor_Butler_MVP/src/main/resources/fsm/table-booking-llm-contract.md`
