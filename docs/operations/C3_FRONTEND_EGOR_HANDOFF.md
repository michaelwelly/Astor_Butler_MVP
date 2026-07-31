# C3 Agency Frontend Handoff For Egor

Date: 2026-07-30

Owner handoff: Egor

## Current Preview

Temporary VM preview:

```text
http://51.250.31.97:3001
```

Current state:

- frontend is running on the Yandex VM as Docker container `c3_agency_frontend`;
- container healthcheck is green;
- desktop and mobile open after click/tap on the intro splash;
- domain `c3ag.ru` is bought/activating, DNS/TLS will be attached later;
- Telegram and proxy work are postponed until the separate call.

## Scope For This Handoff

This handoff is for frontend work only:

- C3 Agency / C3AG.ru public preview;
- responsive UI/UX review;
- Dockerized Next.js production path;
- future design iteration readiness.

Do not treat the current visual state as final. Additional design, text and structure edits will arrive later.

## Repository Area

Frontend app:

```text
frontend/
```

Important files:

```text
frontend/app/
frontend/components/
frontend/lib/
frontend/public/
frontend/Dockerfile
frontend/.dockerignore
frontend/next.config.ts
frontend/FRONTEND_PRODUCTION_PLAN.md
docs/operations/C3AG_DOMAIN_RUNBOOK.md
```

## Local Commands

Install and run locally:

```bash
cd frontend
npm ci
npm run dev
```

Local app:

```text
http://localhost:3001
```

Production build:

```bash
cd frontend
npm run build
```

Docker build:

```bash
docker build -t c3-agency-frontend:local frontend
```

## Runtime Notes

Default runtime values:

```text
NEXT_PUBLIC_SITE_URL=http://localhost:3001
NEXT_PUBLIC_API_BASE_URL=http://localhost:8089
NEXT_PUBLIC_WEB_CHAT_ENDPOINT=/api/chat
NEXT_PUBLIC_MEDIA_BASE_URL=https://storage.yandexcloud.net/c3ag-media
NEXT_PUBLIC_BRAND_LOGO_URL=https://storage.yandexcloud.net/c3ag-media/brand/c3ag-logo.svg
```

Chat is intentionally mocked through `/api/chat` for now. Later it should move to backend `/api/messages` after the web-chat persistence contract is confirmed.

## What To Review First

1. Desktop first viewport:
   - logo;
   - top navigation;
   - intro splash transition;
   - hero video readability;
   - chat input position.
2. Mobile first viewport:
   - intro tap behavior;
   - header/menu spacing;
   - hero crop;
   - chat input position;
   - text readability.
3. Portfolio sections:
   - card spacing;
   - duration/category labels;
   - poster/video fallback;
   - detail overlay behavior.
4. Interaction:
   - splash click/tap;
   - menu;
   - video cards;
   - chat expand/send mock path;
   - login placeholder.

## Known Follow-Ups

- Design revisions will arrive later; keep current changes easy to iterate.
- Real video/poster assets should replace temporary and mixed sample media.
- Product order on the homepage is fixed as Reels, Events/reportage, Advertising, then the rest.
- Public logo is already smoke-tested in Yandex Object Storage; use the same bucket for future media.
- The header now has a light/dark theme toggle; it persists in browser local storage.
- Public domain/TLS will replace the temporary `51.250.31.97:3001` URL.
- Direct public port `3001` should be closed after reverse proxy on `80/443` is ready.
- `npm ci` currently reports high-severity dependency audit warnings; review before paid production traffic.
- Replace mock chat with backend `/api/messages` after the product owner accepts the web-chat contract.

## Guardrails

- Do not commit `.env`, `.env.*`, secrets or VM keys.
- Do not commit `node_modules`, `.next`, `.vercel`, `target`, `.codex*` or output artifacts.
- Do not break the current Docker production path.
- Keep frontend changes scoped under `frontend/**` unless a compose/proxy change is explicitly requested.
- If a design choice is unclear, write a TODO and keep the current preview stable.

## Definition Of Done For Egor's First Pass

- Preview still opens on desktop and mobile.
- No obvious text overlap or clipped primary controls.
- Intro splash opens reliably by click/tap.
- Chat mock does not block browsing.
- Docker production build still succeeds.
- All requested design uncertainties are written down as follow-up tasks.
