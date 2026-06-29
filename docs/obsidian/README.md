# Repo-Owned Obsidian Memory

Эта папка - переносимая проектная память внутри репозитория. Она нужна, чтобы Codex, Claude и человек читали один и тот же контекст при работе с Astor Butler.

Внешний Obsidian vault остается локальной рабочей базой:

```text
/Users/michaelwelly/Obsidian/Astor_Butler_Knowledge
```

Но для командной работы, переезда на сервер и восстановления контекста используем именно `docs/obsidian/**`.

## Что Читать Перед Работой

1. `docs/obsidian/01_Project/Project_Context.md`
2. `docs/obsidian/01_Project/Work_Plan.md`
3. `docs/obsidian/01_Project/NEXT_CHAT_HANDOFF.md`
4. `docs/obsidian/04_Tech/Tech_Decisions.md`
5. `docs/obsidian/03_FSM/FSM_Index.md`
6. `docs/obsidian/05_Yana/Yana_Request_Draft.md`

## Правило Приоритета

Если документы спорят друг с другом:

1. `docs/FSM_SCENARIOS_VIEWER.html`
2. `docs/architecture/ARCHITECTURE.md`
3. `docs/fsm/FSM_SCENARIOS.md`
4. `docs/obsidian/**`
5. `docs/archive/**`

Архивные документы нельзя использовать как актуальную постановку задачи без повторного подтверждения.

## Как Обновлять

- После важных решений обновлять ближайший файл в `docs/obsidian/**`.
- Если решение меняет архитектуру или FSM, сначала обновлять viewer/architecture, потом память.
- Не копировать сюда `.env`, локальные аудио, скринкасты, IDE-кэши и временные Codex-артефакты.
