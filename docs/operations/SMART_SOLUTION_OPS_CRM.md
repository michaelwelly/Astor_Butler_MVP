# Smart Solution Ops CRM

Дата: 2026-07-23

## Идея

Smart Solution Ops CRM - внутренний операционный контур поверх Astor Butler.

Telegram остается быстрым интерфейсом для команды, но источник истины по проектам, задачам, ответственным, срокам и пайплайну хранится структурно в backend.

## Первый слой

Первый backend slice добавляет:

- `ops_projects` - карточки проектов и запусков;
- `ops_tasks` - задачи, ответственные, сроки и ссылки на deliverables;
- `ops_calls` - расписание проектных созвонов;
- `ops_artifacts` - ссылки на презентации, брифы, договоры, видео, дизайн и отчеты;
- REST API `/api/ops/**`;
- Telegram-ready digest проекта через `/api/ops/projects/{id}/digest`.

## Вертикали

`OpsProjectVertical`:

- `HORECA`;
- `PRINTING`;
- `VIDEO_PRODUCTION`;
- `MEDICINE`;
- `WEBSITE`;
- `AI_PROJECT`;
- `MARKETING`;
- `INTERNAL`;
- `OTHER`.

## Пайплайн

`OpsProjectStage`:

- `INTAKE`;
- `BRIEFING`;
- `PLANNING`;
- `PRODUCTION`;
- `REVIEW`;
- `LAUNCH`;
- `SUPPORT`;
- `DONE`.

## Статусы проекта

`OpsProjectStatus`:

- `DRAFT`;
- `ACTIVE`;
- `BLOCKED`;
- `WAITING_CLIENT`;
- `WAITING_TEAM`;
- `READY_TO_LAUNCH`;
- `LAUNCHED`;
- `ARCHIVED`.

## Telegram UX target

Первый Telegram command layer подключен для service chats через `OpsTelegramCommandService`.

Runtime настройка:

```text
TELEGRAM_OPS_CHAT_ID=<team chat id>
TELEGRAM_OPS_GROUP_QA_ENABLED=true
TELEGRAM_OPS_OWNER_MENTION=@michaelwelly
TELEGRAM_OPS_OWNER_USERNAME=michaelwelly
```

Yandex AI Studio для Smart Solution:

```text
SMART_SOLUTION_MODEL_PROVIDER=yandex
SMART_SOLUTION_YANDEX_MODEL=yandexgpt-5-lite
SMART_SOLUTION_YANDEX_QUALITY_MODEL=yandexgpt-5.1
SMART_SOLUTION_GROUP_QA_LLM_ENABLED=true
YANDEX_FOLDER_ID=<folder id>
YANDEX_API_KEY=<api key>
```

`yandexgpt-5.1` используется как quality/Pro модель для ответов по группе. Если нужен Alice-style assistant, модель можно переключить на `aliceai-llm`, но это все равно будет LLM внутри наших каналов, а не управление публичной выдачей Алисы или Яндекс Карт.

Если отдельный ops-chat не задан, `telegram.ops.chat-id` по умолчанию берет `TELEGRAM_HOSTESS_CHAT_ID`.

Поддержанные команды чтения:

```text
/ops
/projects
/project MED
/tasks VIDEO
/summary MED
/calls VIDEO
/artifacts MED
/summary ADS
```

Поддержанные команды записи:

```text
/newproject SITE "Smart_Soultion.com" WEBSITE @owner
/status IZI WAITING_CLIENT 70% ожидает оплату
/task MED "Подготовить презентацию" @owner 25.07
/call VIDEO "Созвон по запуску" 24.07 15:00 @owner
/artifact MED "Презентация" https://... PRESENTATION @owner
```

Команды записи идут через `OpsProjectService`; Telegram не меняет CRM-состояние напрямую.

Seeded стартовые проекты:

- `VIDEO` - видео-продакшен, owner `@egor`;
- `MED` - медицина и презентация, owner `@michael`;
- `IZI` - ожидание оплаты перед запуском;
- `RESTO` - статусы запусков ресторанов;
- `PRINT` - типография, макеты и сроки;
- `SITE` - Smart_Soultion.com и внутренняя CRM.
- `ADS` - Яндекс Бизнес, Директ, Карты и внешний growth/adtech контур.

Свободный ввод через AI:

```text
Что у нас по запуску ресторана?
Кто отвечает за презентацию?
Какие задачи горят до пятницы?
Собери статус для команды на сегодня.
Что по Яндекс Директу и рекламной подписке?
```

## Архитектурная граница

- Telegram - транспорт и быстрый UI.
- `ops_projects` / `ops_tasks` - источник истины для CRM-состояния.
- AI помогает формулировать, суммаризировать и находить информацию.
- Статусы, сроки, ответственные и переходы пайплайна меняются через доменный сервис/API.

## Group Q&A loop

For the separate `smart-solution-bot` runtime, group Q&A is enabled by `TELEGRAM_OPS_GROUP_QA_ENABLED=true`.

Flow:

1. Участник пишет вопрос в группе.
2. Бот собирает контекст из Ops CRM и `SMART_SOLUTION_GROUP_MEMORY`.
3. LLM отвечает только если контекст достаточный.
4. Если ответа нет, бот тегает owner mention.
5. Owner отвечает reply на исходный вопрос.
6. Ответ сохраняется в `ops_group_questions` и semantic memory для следующих RAG-ответов.

Production isolation:

- AERIS bot keeps its own token and Redis prefix.
- Smart Solution bot uses `SMART_SOLUTION_BOT_TOKEN`, `SMART_SOLUTION_BOT_USERNAME`, `SMART_SOLUTION_REDIS_KEY_PREFIX=smart-solution`, port `8090` and compose profile `smart-solution`.

## Reverse RAG and sponsored recommendations

Reverse RAG means we intentionally build a clean knowledge package about a restaurant/project for our own assistant:

- structured facts: address, cuisine, hours, menu, average check, booking rules, contacts;
- current offers and launch statuses;
- proof assets: photos, menu links, deck links, reviews, certificates;
- semantic chunks for intents like "where to eat", "book a table", "restaurant for business dinner", "banquet venue".

Inside Smart Solution-owned channels (Telegram bot, site assistant, internal dashboard, future Alice skill) the answer pipeline may use this data to rank the restaurant higher when it is relevant to the user intent.

Important boundary:

- Prompt/RAG enrichment through Yandex AI Studio does not change public Yandex Search, Maps, Navigator, or public Alice ranking.
- Public promotion must go through official channels such as Yandex Business, Maps priority placement, Direct, profile completeness, reviews and content quality.
- Any paid boost inside our own assistant must be transparent and relevant: the bot should mark it as a partner/recommended option and should not hide better matching answers.

## Yandex growth/adtech contour

`ADS` is the dedicated Smart Solution project for external demand generation:

- Yandex Business profile: restaurant card, photos, menu, opening hours, contacts, booking link and reviews;
- Yandex Business / Maps promotion: рекламная подписка, приоритетное размещение, branded priority placement where applicable;
- Yandex Direct: campaigns, geo targeting, keywords, ad creatives, budgets, UTM tags and reports;
- Yandex Maps API on our site/CRM where we need maps, routes, organization search or local context;
- reporting loop: impressions, clicks, calls, routes, bookings, cost per lead and conversion to launch/status events.

Technical boundary:

- Direct API can automate campaign operations and statistics after OAuth/API access is granted.
- Yandex Business/Maps promotion is managed through official Yandex Business products and cabinet flows unless an approved business API is available for the account.
- Smart Solution stores campaign briefs, status, links, budget decisions and performance reports in Ops CRM/RAG, but credentials and tokens stay only in runtime secrets.
