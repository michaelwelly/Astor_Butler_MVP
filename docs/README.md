# Astor Butler Docs Map

Эта папка делит документацию на рабочую память проекта, публичные материалы, технические контракты и архив.

## Source Of Truth

- `docs/FSM_SCENARIOS_VIEWER.html` - главный визуальный источник истины по FSM-сценариям, нумерации и переходам.
- `docs/fsm/FSM_SCENARIOS.md` - текстовый companion к viewer. Он не должен спорить с viewer.
- `docs/architecture/ARCHITECTURE.md` - архитектурные границы, runtime-слои, Model Gateway, NLU/RAG/VLM и инфраструктура.
- `docs/fsm/FSM_IMPLEMENTATION_PLAN.md` - план переноса визуальной FSM-карты в код и тесты.

## Repo Memory

- `docs/obsidian/` - commit-friendly проектная память для Codex, Claude и ручной работы.
- Внешний Obsidian vault остается локальной рабочей базой: `/Users/michaelwelly/Obsidian/Astor_Butler_Knowledge`.
- При конфликте приоритет такой: `docs/FSM_SCENARIOS_VIEWER.html` -> `docs/architecture/ARCHITECTURE.md` -> `docs/fsm/FSM_SCENARIOS.md` -> `docs/obsidian/**` -> архив.

## Package Index

- `docs/architecture/` - архитектурные решения, модель данных, локальные БД и runtime-слои.
- `docs/contracts/` - API, Kafka/outbox, платежные и frontend/backend-контракты.
- `docs/content/` - медиа, MinIO, RAG-источники, AERIS channel ingest.
- `docs/frontend/` - handoff/ТЗ/пакеты задач для Claude и фронтенд-ветки.
- `docs/fsm/` - текстовые FSM companion-документы, implementation plan и доменные сценарии.
- `docs/operations/` - запуск, деплой, нагрузка, командный workflow и ближайший backlog.
- `docs/public/` - публичные/внешние материалы, которые можно переносить в Notion/GitHub Pages.
- `docs/research/` - spike, исследование NLU, дипломные и продуктовые черновики.
- `docs/obsidian/` - переносимая проектная память для агентов и человека.
- `docs/analytics/` - отчеты ручных и нагрузочных прогонов.
- `docs/archive/` - исторические документы, которые больше не управляют реализацией.

## Backend Contracts

- `docs/contracts/API_CONTRACT.md`
- `docs/contracts/FRONTEND_BACKEND_CONTRACTS.md`
- `docs/architecture/DATABASE_MODEL.md`
- `docs/contracts/KAFKA_TOPICS.md`
- `docs/contracts/KAFKA_OUTBOX.md`
- `docs/fsm/TABLE_BOOKING.md`
- `docs/content/MEDIA_PIPELINE.md`
- `docs/content/AERIS_CHANNEL_INGEST.md`

## Public / External Docs

- `docs/LICENSE`
- `docs/policy.html`
- `docs/guest-guide.html`
- `docs/staff-guide.html`
- `docs/public/notion-knowledge-base-refresh-2026-06-15.md`

## Analytics

- `docs/analytics/` - отчеты ручных и нагрузочных прогонов, weekend stands, perf notes.
- `docs/operations/LOAD_TESTING.md` - как запускать и читать нагрузочные тесты.

## Archive

- `docs/archive/` - исторические документы, которые больше не являются источниками истины.
- Архив не используется для планирования новой реализации, но помогает восстановить ход решений.
