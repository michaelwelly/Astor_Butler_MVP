# Next Session Backlog

## Context

The next session should continue from the saved MVP foundation:

- PR #5 merged the first C3FLEX.com frontend scaffold into `main`.
- Frontend app lives in `frontend/` and runs on `http://localhost:3001` because Grafana uses `3000`.
- Backend Swagger runs on `http://localhost:8088/swagger-ui/index.html`.
- API Contract v0 exists and Swagger contains non-empty paths.
- Telegram/FSM message gateway now has a visible vertical slice: `/start`, contact request, AI-assisted reply and fallback-to-admin.
- `POST /api/messages` exposes the same FSM gateway for future web chat.
- `Consent Vault API` is reserved early for policy, grant, revoke and export flows.
- MongoDB document store is populated with diploma, pitch, one-pager, conference and project-memory materials.
- Capability modules are mapped to the eight emotional axes.
- Docker Compose has PostgreSQL, Redis, MongoDB, Kafka/Redpanda, Prometheus, Grafana and S3-compatible MinIO.

## Backend API Expansion

Update the API list to cover backend and infrastructure needs for future microservice decomposition:

- service discovery and internal health;
- load-balancer readiness;
- retry/idempotency contracts;
- gRPC/service-to-service boundaries;
- Kafka topics and event schemas;
- notification routes and delivery states;
- media ingestion and S3 object lifecycle;
- document search and AI Adapter lookup;
- cache warm-up and invalidation;
- K6 load-test endpoints and scenarios.

All API endpoints must keep:

- DTO/schema-first OpenAPI contracts;
- standard `2xx/3xx/4xx/5xx` response families;
- `ApiErrorResponse` for all errors;
- enum models for statuses, roles, event types and delivery states;
- test-first development: GDD/TDD before business logic.

## Test Strategy

Development order:

1. Contract tests for API request/response schemas.
2. Unit tests for DTO validation and domain services.
3. Integration tests for PostgreSQL, Redis, MongoDB and S3.
4. Kafka contract tests for event payloads.
5. K6 load tests for idempotent `GET` endpoints and safe read paths.
6. Load-test report for local API readiness before preprod planning.

## Media Content Pipeline

Yandex Disk source:

```text
https://disk.yandex.ru/d/oopTdDN0CTuIig
```

The linked folder contains source works split by folders. It is the primary content source for the frontend and S3 media store.

Planned pipeline:

1. Download/sync source folders locally.
2. Classify files by media type: video, photo, document, presentation, text.
3. Upload originals to S3-compatible storage.
4. Store metadata in PostgreSQL/MongoDB.
5. Create Redis cache keys for menus, previews, landing blocks and frequently used media references.
6. Add compression/transcoding plan:
   - image derivatives;
   - video preview/poster;
   - mp4/webm/adaptive streaming target;
   - document previews.
7. Expose media metadata through Swagger API.
8. Feed frontend with stable CDN-ready URLs.

## Frontend Preparation

Current frontend baseline:

- Next.js/React/TypeScript scaffold is merged.
- Current UI is accepted only as baseline, not final visual direction.
- Portfolio data is still mock/sample-based.
- Lead form is demo-mode until backend endpoint is selected.
- Next redesign work should happen from a new branch, for example `frontend/c3flex-redesign`.

Prepare frontend contract for the next launch session:

- C3FLEX.com visual redesign;
- real 3-video sample mapping: Event Stories, Reels & Product Content, Commercials;
- media card/detail behavior;
- lead capture;
- backend adapter to Swagger/OpenAPI where endpoints exist;
- System Design/JavaGuru promo section if it supports sales narrative.

Frontend should use generated API clients from Swagger once DTO contracts stabilize.

## Notion / Homework Delivery

Create a Notion page for System Design homework:

- sequence diagram for designed API;
- API list in readable table format;
- functional requirements schema;
- technology descriptions mapped into non-functional requirements;
- links to architecture docs, Swagger and GitHub;
- session time tracking for the three work sessions.

## Product Narrative To Preserve

Core thesis:

```text
Astor Butler is a soft-governance digital butler for premium hotels, stadiums and city-scale cultural/event networks.
```

Inside the hotel:

- Memory Engine;
- Preference Map;
- Smart Tip;
- Quiet Guide;
- Hidden Heart;
- Safe Play;
- Slot Keeper;
- Panic Exit.

Outside the hotel:

- Direct Channel Hub;
- Arena Reboot Engine;
- Consent Vault;
- Impact Meter.

Market wedge:

- premium hotels;
- stadium/festival legacy;
- Doha luxury segment;
- Russian flagship hotels;
- digital-native guests aged 25-45;
- zero-friction service.

Execution model:

- 30-day VIP hotel pilot;
- media/catharsis event;
- three-hotel plus stadium city triangle;
- flagship expansion;
- 12 RU hotels plus 2 GCC locations;
- recurring revenue through subscription plus event kit.

## Team / Backlog

Create Jira backlog tasks for:

- backend API contracts;
- Telegram/FSM vertical slice tests;
- Consent Vault persistence design;
- service/gRPC boundaries;
- Kafka topics/events;
- S3 media pipeline;
- document store API;
- frontend generated client integration;
- K6 load tests;
- integration QA ownership;
- DevOps CI/CD and monitoring;
- senior backend review;
- sales/hospitality lead tasks;
- legal/compliance for Qatar and data processing.
