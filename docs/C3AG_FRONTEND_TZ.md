# C3FLEX.com Frontend Technical Brief

Status: checked against the Notion TZ on 2026-05-31.
Decisions updated on 2026-05-31 after product clarification.

Source: https://www.notion.so/C3AG-36ea7c019f19803e8fcce085b4db7f88

## Goal

C3FLEX.com is an immersive production portfolio and lead-generation website based on the C3AG materials. It is not a classic corporate landing page. The first frontend session should build a Netflix-like visual experience where video works are the main content, and text only supports navigation, filtering and conversion.

The website must:

- segment the visitor by production direction;
- show relevant cases with video-first browsing;
- create an emotional premium production feeling;
- collect leads through a short brief form;
- prepare the frontend for future connection to Astor Butler backend APIs.

## Product Positioning

Public product name is fixed as `C3FLEX.com`.

The Notion page uses C3AG and also mentions `3cFLEX` / Netflix-like mechanics. For implementation we use:

- repository and project context: `Astor Butler MVP`;
- public site name: `C3FLEX.com`;
- source/spec context: `C3AG`;
- interaction pattern: Netflix-like catalog of production works;
- internal short name: `C3FLEX`.

## Frontend Scope

Primary stack:

- Next.js;
- React;
- TypeScript by default;
- GSAP;
- Framer Motion;
- Lenis smooth scroll;
- ESLint.

The first screen:

- fullscreen intro/showreel;
- motion typography;
- dark premium visual system;
- muted autoplay by default because browsers block sound autoplay;
- explicit sound toggle if audio is needed.

Core sections:

- main video catalog;
- direction selector;
- direction page;
- project/case detail;
- service/packages page;
- contact and mini-brief form.

Top-level directions:

- Event Stories;
- Reels & Product Content;
- Commercials.

Yandex Disk folders under `ПОРТФОЛИО / VIDEO C3AG` are treated as nested production categories inside these three top-level directions. The production version can expose deeper category trees, but the local MVP starts with one video per top-level direction.

Visual behavior:

- video cards with autoplay preview;
- horizontal rows or sections similar to Netflix content shelves;
- fullscreen transitions;
- hover preview;
- smooth scroll;
- lazy loading;
- responsive mobile-first layout.

Reference mood:

- A24;
- Stink Films;
- Apple-level motion restraint;
- fashion/editorial production websites;
- premium dark interface.

## Content And Media

Current local decision:

- originals remain in `/Users/michaelwelly/Yandex.Disk.localized`;
- MongoDB stores metadata for the full set of 102 media/documents;
- all 102 files are allowed for public display after curation;
- local MinIO stores only 3 representative videos for frontend/API work, one per top-level direction;
- full S3 migration is deferred to cloud S3/Object Storage when storage is available.

Current expected local sample:

- bucket: `astor-media`;
- prefix: `raw/`;
- sample count: 3 videos;
- sample rule: one video for Event Stories, one for Reels & Product Content, one for Commercials.

For the frontend session, use the 3-video sample as the first video dataset. Do not copy the full 10+ GB media set into local MinIO.

## Backend Alignment

The Notion TZ mentions `Headless CMS or WordPress`, but project architecture overrides this:

- WordPress is not used;
- backend is Astor Butler Java 21 + Spring Boot;
- REST/OpenAPI is the first contract for frontend;
- GraphQL can be revisited later, after stable REST contracts;
- content/case metadata is served from backend APIs;
- media files are served from S3-compatible storage;
- MongoDB stores document-like metadata and import manifests;
- PostgreSQL remains the primary relational system for users, roles, bookings, statuses, timelines and posts;
- Redis stores hot context, FSM, idempotency and cached menus;
- Kafka/Redpanda is prepared for async audit, notification and analytics events.

The frontend must not embed backend business logic. It consumes generated or documented API contracts.

## Lead Form

Mini-brief fields from Notion:

- project type;
- task description;
- needed services;
- format;
- deadline;
- budget;
- name/contact.

Initial integrations:

- Telegram notification;
- email notification if available;
- CRM adapter later.

CRM is intentionally not fixed for the first frontend session. Telegram/email are enough for the first lead flow; CRM selection moves to a later backend/integration session.

## Service Packages

Notion lists the following commercial packages:

- Reels/Product A: 10 reels, idea and structure, shooting, edit, 1 revision, 85,000 RUB;
- Reels/Product B: shooting, edit, 1 revision, 65,000 RUB;
- Event stories: 11,000 RUB/hour, minimum 2 hours, minimum check 22,000 RUB;
- Podcast up to 1 hour: 68,000 RUB.

These can be shown as editable content cards, not hardcoded business logic.

## Local Environment Contract

Only one local backend profile remains:

- Spring profile: `local`;
- config files: `application.yaml`, `application-local.yaml`;
- env template in git: `.env.example`;
- real credentials: local `.env`, ignored by git.

Local Docker Compose is infrastructure only:

- PostgreSQL;
- Redis;
- MongoDB;
- Kafka-compatible Redpanda;
- MinIO;
- Prometheus;
- Grafana;
- optional Ollama through Compose profile `ai`.

Spring Boot is started locally from IDE or Maven:

```bash
cp .env.example .env
docker compose up -d postgres redis mongo kafka minio minio-init prometheus grafana
scripts/run_local_app.sh
```

Swagger:

```text
http://localhost:8080/swagger-ui/index.html
```

## Resolved Decisions And Remaining Checks

1. Public naming: `C3FLEX.com`.
2. Public rights: all 102 files can be publicly displayed after curation.
3. Local sample: 3 videos, one per top-level direction.
4. Category model: Event Stories, Reels & Product Content, Commercials as top-level branches; Yandex Disk folders become nested production categories.
5. CRM: not fixed; first version sends leads to Telegram/email.
6. Frontend language: Notion says JS/TS optional; project recommendation remains TypeScript.
7. Backend contract: Notion allows REST/GraphQL; project recommendation is REST/OpenAPI first.
8. Video delivery: MVP should use mp4/webm from S3/MinIO; adaptive streaming/HLS is a production target.
9. CMS/admin: Notion says Headless CMS/WordPress; project recommendation is custom backend/admin, no WordPress.
10. Sound: autoplay must start muted; sound can be enabled by user action.

## Prompt For Next Frontend Codex Session

Use this prompt when starting the separate frontend-building Codex account:

```text
Мы строим C3FLEX.com frontend на основе C3AG материалов внутри проекта Astor Butler MVP.

Открой репозиторий /Users/michaelwelly/IdeaProjects/Astor_Butler_MVP и прочитай:
- docs/C3AG_FRONTEND_TZ.md
- docs/ARCHITECTURE.md
- docs/API_CONTRACT.md
- docs/MEDIA_PIPELINE.md
- README.md

Задача следующей сессии: спроектировать и начать frontend как Netflix-like immersive production portfolio:
- Next.js + React + TypeScript;
- GSAP + Framer Motion + Lenis;
- dark premium visual style;
- video-first catalog;
- public name: C3FLEX.com;
- sections: Event Stories, Reels & Product Content, Commercials;
- локальный MVP: одно видео на каждую верхнюю секцию;
- production taxonomy: папки Яндекс.Диска внутри ПОРТФОЛИО/VIDEO C3AG становятся вложенными категориями;
- case detail page;
- service packages;
- lead mini-brief form;
- integration-ready API layer for backend REST/OpenAPI.

Важно:
- WordPress не использовать;
- backend business logic не тащить во frontend;
- локально backend работает через Spring profile local;
- Docker Compose держит только инфраструктуру;
- медиа: в Mongo metadata по 102 файлам, все можно показывать публично после курации, в MinIO для локальной разработки только 3 sample videos under bucket astor-media/raw/;
- не копировать весь Yandex Disk в MinIO.

Перед кодом проверь текущий API/Swagger:
http://localhost:8080/swagger-ui/index.html
```
