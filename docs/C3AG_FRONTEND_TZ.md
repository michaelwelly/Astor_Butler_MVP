# C3AG Frontend Technical Brief

Status: checked against the Notion TZ on 2026-05-31.

Source: https://www.notion.so/C3AG-36ea7c019f19803e8fcce085b4db7f88

## Goal

C3AG is an immersive production portfolio and lead-generation website. It is not a classic corporate landing page. The first frontend session should build a Netflix-like visual experience where video works are the main content, and text only supports navigation, filtering and conversion.

The website must:

- segment the visitor by production direction;
- show relevant cases with video-first browsing;
- create an emotional premium production feeling;
- collect leads through a short brief form;
- prepare the frontend for future connection to Astor Butler backend APIs.

## Product Positioning

Public product name is currently not fully locked. The Notion page uses C3AG and also mentions `3cFLEX` / Netflix-like mechanics. For implementation we use:

- repository and project context: `Astor Butler MVP`;
- frontend experience name: `C3AG`;
- interaction pattern: Netflix-like catalog of production works;
- optional internal codename: `3cFLEX`.

This naming should be confirmed before public release.

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

Directions from Notion:

- Event Stories;
- Reels & Product Content;
- Commercials.

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
- local MinIO stores only 10 representative sample files for frontend/API work;
- full S3 migration is deferred to cloud S3/Object Storage when storage is available.

Current expected local sample:

- bucket: `astor-media`;
- prefix: `raw/`;
- sample count: 10 files;
- sample size: about 960 MiB.

For the frontend session, use the sample as the first video dataset. Do not copy the full 10+ GB media set into local MinIO.

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

Open question: exact CRM is not fixed yet.

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

## Detected Inconsistencies To Confirm

1. Public naming: should the site brand be `C3AG`, `3cFLEX`, or another visible name?
2. Frontend language: Notion says JS/TS optional; project recommendation is TypeScript.
3. Backend contract: Notion allows REST/GraphQL; project recommendation is REST/OpenAPI first.
4. Video delivery: MVP should use mp4/webm from S3/MinIO; adaptive streaming/HLS is a production target.
5. CMS/admin: Notion says Headless CMS/WordPress; project recommendation is custom backend/admin, no WordPress.
6. CRM: provider is not fixed; Telegram/email notification can be the first integration.
7. Case taxonomy: Notion has 3 directions; media folders need mapping to these directions.
8. Public rights: confirm which 10 sample files and which of the 102 originals are allowed for public portfolio display.
9. Sound: autoplay must start muted; sound can be enabled by user action.

## Prompt For Next Frontend Codex Session

Use this prompt when starting the separate frontend-building Codex account:

```text
Мы строим C3AG frontend для проекта Astor Butler MVP.

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
- sections: Event Stories, Reels & Product Content, Commercials;
- case detail page;
- service packages;
- lead mini-brief form;
- integration-ready API layer for backend REST/OpenAPI.

Важно:
- WordPress не использовать;
- backend business logic не тащить во frontend;
- локально backend работает через Spring profile local;
- Docker Compose держит только инфраструктуру;
- медиа: в Mongo metadata по 102 файлам, в MinIO только 10 sample files under bucket astor-media/raw/;
- не копировать весь Yandex Disk в MinIO.

Перед кодом проверь текущий API/Swagger:
http://localhost:8080/swagger-ui/index.html
```
