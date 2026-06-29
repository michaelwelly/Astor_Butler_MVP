# Frontend → Codex handoff report

Date: 2026-06-23
Scope touched: `frontend/**` + `design-system/**` only.
NOT touched: `src/main/**`, `docker-compose.yml`, `.env*`, `docs/FSM_*`,
`docs/architecture/ARCHITECTURE.md`, backend/DB/infra, Liquibase, backend tests, `.next`, `node_modules`.
No video binaries committed.

Verification: `tsc --noEmit` clean; `npm run lint` clean (0 errors, 10 warnings, all
pre-existing `@next/next/no-img-element` project convention). `next build` NOT run in the
sandbox (needs `swc-linux-arm64` + npm network; only `swc-darwin-arm64` installed) — run on a
Mac.

## What the frontend now expects from the backend

All shapes follow `docs/contracts/FRONTEND_BACKEND_CONTRACTS.md`. The frontend is contract-first and
mock-backed; swapping in real endpoints is config-only.

1. **Video catalog — `GET /api/content/c3flex/videos`** (§3)
   - Frontend mock: `lib/video-catalog.ts` (`catalogVideos`), shape matches `items[]` 1:1
     (`videoId, slug, title, description, shortDescription, tags, category, featured,
     durationSeconds, orientation, status READY|DRAFT|ARCHIVED, poster, sources[], cta`).
   - To go live: return `items[]`; frontend replaces `catalogVideos`. UI already tolerates
     1 source, missing poster, and ≠30 items.

2. **Web chat — `POST /api/messages`** (§4)
   - Frontend already builds the full body in `lib/web-chat.ts`:
     `channel:"WEB", externalUserId, chatId, text, correlationId,
     payload{ site, sessionId, page, referrer, utm, selectedVideo, viewport, consent }`.
   - Posts to `/api/chat` mock today. Set `NEXT_PUBLIC_WEB_CHAT_ENDPOINT` to the gateway path
     to switch.
   - ⚠ The widget adds ONE dev-only top-level field `turn` (int) so the local mock can drive
     its guided script. It sits OUTSIDE `payload`. **Backend must ignore unknown top-level
     fields** — remove reliance on `turn` once real.
   - `chatId` is a temporary local numeric id (`lib/session.ts`); replace with a real web
     session strategy.

3. **OAuth — `POST /api/auth/login`, `GET /api/auth/me`, `POST /api/auth/logout`** (§5)
   - Frontend offers ONLY Google + Yandex. It performs a REAL redirect to
     `${NEXT_PUBLIC_API_BASE_URL}/oauth2/authorization/keycloak?kc_idp_hint=<provider>` and
     returns at `${origin}/auth/callback`, which calls `GET /api/auth/me` (cookie,
     `credentials: include`).
   - Optional: set `NEXT_PUBLIC_AUTH_LOGIN_ENDPOINT` and return `{ authorizationUrl }` to use
     the POST-start contract instead.
   - Needs: Keycloak realm with Google/Yandex IdP federation; implement `/api/auth/me`
     (returns `{subject, roles, claims{name,email,...}}`) and `/api/auth/logout`.
   - Tokens stay server-side (httpOnly cookie). Frontend never stores JWT.

4. **Consent — `GET /api/consents/policy/current`, `POST /api/consents`** (§6)
   - Privacy notice gates chat send. Policy version `2026-06-02-local`.
   - Consent evidence currently rides inside the chat `payload.consent`. Needs anonymous
     consent persistence + link-to-user after login.

5. **Media/object storage** (§2)
   - Frontend never builds S3 paths. `lib/video-catalog.ts → resolveMediaUrl`: absolute URL →
     as-is; `/local` path → as-is; bare object key → prefixed with `NEXT_PUBLIC_MEDIA_BASE_URL`.
   - Suggested keys: `content/c3flex/videos/<slug>/{source,mobile,desktop}.mp4`,
     `content/c3flex/posters/<slug>.jpg`.

## Env the frontend reads

```
NEXT_PUBLIC_MEDIA_BASE_URL        # object storage / CDN base for bare object keys
NEXT_PUBLIC_WEB_CHAT_ENDPOINT     # default /api/chat (mock) → gateway /api/messages
NEXT_PUBLIC_AUTH_LOGIN_ENDPOINT   # optional; enables POST /api/auth/login start
NEXT_PUBLIC_API_BASE_URL          # gateway base for auth + media
NEXT_PUBLIC_LEAD_ENDPOINT         # existing lead endpoint (unchanged)
```

## Codex backend next targets (contract §9)

1. `GET /api/content/c3flex/videos` service-backed.
2. Stable web session id instead of numeric `chatId`.
3. Web channel persistence for site messages (+ ignore the dev `turn` field).
4. Site lead / admin-chat projection.
5. Keycloak/OAuth2 resource server with Google + Yandex federation; `/api/auth/me`, logout.
6. Anonymous consent persistence + link-to-user.

## Frontend files (this and prior passes)

New: `lib/{video-catalog,session,utm,consent,web-chat,auth-api}.ts`,
`hooks/useAuth.ts`, `components/auth/{LoginPanel,ProviderButton,AuthMenu}.tsx`,
`components/ui/ConsentNotice.tsx`, `app/auth/callback/page.tsx`,
`public/portfolio/_poster-fallback.svg`, `eslint.config.mjs`,
`FRONTEND_PRODUCTION_PLAN.md`, `NEXT_SESSION_PLAN.md`, `CODEX_HANDOFF_REPORT.md`.
Changed: `lib/portfolio.ts`, `app/api/chat/route.ts`, `app/globals.css`,
`components/home-page.tsx`, `components/layout/Navigation.tsx`,
`components/sections/FeaturedCatalog.tsx`,
`components/ui/{ChatWidget,VideoPlayer,VideoCard,VideoOverlay}.tsx`, `package.json`.
