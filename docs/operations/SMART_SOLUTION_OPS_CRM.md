# Smart Solution Ops CRM

Дата: 2026-07-23

## Идея

Smart Solution Ops CRM - внутренний операционный контур поверх Astor Butler.

Telegram остается быстрым интерфейсом для команды, но источник истины по проектам, задачам, ответственным, срокам и пайплайну хранится структурно в backend.

## Первый слой

Первый backend slice добавляет:

- `ops_projects` - карточки проектов и запусков;
- `ops_tasks` - задачи, ответственные, сроки и ссылки на deliverables;
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
```

Если отдельный ops-chat не задан, `telegram.ops.chat-id` по умолчанию берет `TELEGRAM_HOSTESS_CHAT_ID`.

Поддержанные команды чтения:

```text
/ops
/projects
/project AERIS_LAUNCH
/tasks AERIS_LAUNCH
/summary AERIS_LAUNCH
```

Поддержанные команды записи:

```text
/status AERIS_LAUNCH READY_TO_LAUNCH 90% waiting DNS
/task AERIS_LAUNCH "Подготовить презентацию" @owner 25.07
```

Команды записи идут через `OpsProjectService`; Telegram не меняет CRM-состояние напрямую.

Свободный ввод через AI:

```text
Что у нас по запуску ресторана?
Кто отвечает за презентацию?
Какие задачи горят до пятницы?
Собери статус для команды на сегодня.
```

## Архитектурная граница

- Telegram - транспорт и быстрый UI.
- `ops_projects` / `ops_tasks` - источник истины для CRM-состояния.
- AI помогает формулировать, суммаризировать и находить информацию.
- Статусы, сроки, ответственные и переходы пайплайна меняются через доменный сервис/API.
