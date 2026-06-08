# Astor Butler Project Instructions

## graphify

This project has a knowledge graph at `graphify-out/` with cross-file relationships.

Rules:

- For codebase questions, first run `graphify query "<question>"` when `graphify-out/graph.json` exists.
- Use `graphify path "<A>" "<B>"` for relationships and `graphify explain "<concept>"` for focused concepts.
- These return a scoped subgraph, usually much smaller than raw grep output.
- Dirty `graphify-out/` files are expected after hooks or incremental updates; dirty graph files are not a reason to skip graphify.
- Only skip graphify if the task is about stale/incorrect graph output, or the user explicitly says not to use it.
- If `graphify-out/wiki/index.md` exists, use it for broad navigation instead of raw source browsing.
- Read `graphify-out/GRAPH_REPORT.md` only for broad architecture review or when query/path/explain do not surface enough context.
- After modifying code, run `graphify update .` or rebuild the relevant graph to keep it current.

Local CLI path in this workstation:

```text
/Users/michaelwelly/.codex/tools/graphify-venv/bin/graphify
```

## Repository

Работаю в репозитории:

```text
/Users/michaelwelly/IdeaProjects/Astor_Butler_MVP
```

Краткий контекст проекта:

Astor Butler MVP — Java 21 + Spring Boot монолит для Telegram-бота с FSM-архитектурой. Telegram выступает как транспорт/UI, а FSM является single source of truth. Архитектурная схема лежит в `docs/ARCHITECTURE.md`. В README описана концепция soft-governance tool для HoReCa, с Telegram, PostgreSQL, Redis, Docker и целевой инфраструктурой в Yandex Cloud.

## Git Hygiene

- Ветка может быть грязной. Не откатывать чужие изменения без явной просьбы.
- Не коммитить `.env`, `target/**`, локальные `.codex*` артефакты и IDE-мусор без отдельного решения.
- Локальные `.idea/dataSources.xml` и `.idea/misc.xml` не трогать без явного решения.
- Перед push или PR внимательно проверять diff на секреты и build artifacts.

## Local Artifacts

Untracked артефакты Codex не являются production-кодом:

- `.codex/`
- `.codex_audio_chunks/`
- `.codex_frames/`
- `.codex_tmp_hr_screening.wav`
- `.codex_transcripts/`
- `.codex_whisper_chunks/`
- `.codex_whisper_out/`

Материалы HR screening в `.codex_transcripts/` относятся к отдельной рабочей сессии и не связаны напрямую с production-кодом Astor Butler.

## Project Memory

Локальный Obsidian vault:

```text
/Users/michaelwelly/Obsidian/Astor_Butler_Knowledge
```

Это не production-репозиторий и не должно попадать в git проекта.

Перед началом содержательной работы по Astor Butler читать:

- `/Users/michaelwelly/Obsidian/Astor_Butler_Knowledge/01_Project/Project_Context.md`
- `/Users/michaelwelly/Obsidian/Astor_Butler_Knowledge/01_Project/Work_Plan.md`
- `/Users/michaelwelly/Obsidian/Astor_Butler_Knowledge/01_Project/NEXT_CHAT_HANDOFF.md`
- `/Users/michaelwelly/Obsidian/Astor_Butler_Knowledge/04_Tech/Tech_Decisions.md`
- `/Users/michaelwelly/Obsidian/Astor_Butler_Knowledge/03_FSM/FSM_Index.md`
- `/Users/michaelwelly/Obsidian/Astor_Butler_Knowledge/05_Yana/Yana_Request_Draft.md`

После важных решений обновлять соответствующие Markdown-заметки в этом vault.
