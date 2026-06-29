# C3FLEX / Astor Butler — Frontend Production Plan

Status: implemented (frontend-only). Aligns with `docs/contracts/FRONTEND_BACKEND_CONTRACTS.md` (`contract-first v0.1`).

Scope respected: only `frontend/**` and `design-system/**` were touched. No backend endpoints, no `src/main/**`, no `docker-compose.yml`, no `.env*`, no `docs/FSM_*`, no `docs/architecture/ARCHITECTURE.md`. No video binaries in git.

## What was built

### 1. Contract-aligned video catalog
`frontend/lib/video-catalog.ts` defines the contract shape (§3): `videoId, slug, title,
description, shortDescription, tags, category, featured, durationSeconds, orientation,
status, poster, sources[], cta`. `status` uses backend-compatible values `READY / DRAFT /
ARCHIVED`. The 30 existing cases in `lib/portfolio.ts` are mapped to this shape by a
deterministic adapter; any case can override fields via the optional fields added to
`PortfolioCase`. When the backend ships `GET /api/content/c3flex/videos`, its `items[]`
replaces `catalogVideos` 1:1.

### 2. Preview cards
`components/ui/VideoCard.tsx` now shows poster, duration, tags, short description, and a
`featured` / `status` badge. UI does not assume exactly 30 items.

### 3. Adaptive player
`components/ui/VideoPlayer.tsx` is mobile-first, reserves aspect-ratio by `orientation`
(no layout jumps), supports portrait/landscape, picks a source by viewport (`selectSource`),
adds a fullscreen toggle, and uses `playsInline`.

### 4. Chat widget + Web Chat payload
`components/ui/ChatWidget.tsx` collapses to a compact macOS-Spotlight-style input and expands
to full chat. Each message is sent via `lib/web-chat.ts`, which builds a body compatible with
the existing `POST /api/messages` (§4): `channel, externalUserId, chatId, text, correlationId,
payload.{site, sessionId, page, referrer, utm, selectedVideo, viewport, consent}`. Supporting
libs: `lib/session.ts` (anonymous session/chat id), `lib/utm.ts` (first-touch UTM + referrer),
`lib/consent.ts` (privacy consent state). `app/api/chat/route.ts` remains a **frontend mock**
that now accepts the contract body and returns a contract-shaped response.

### 5. Login placeholders
`components/auth/LoginPanel.tsx` + `ProviderButton.tsx` offer Google and Yandex only (§5),
backed by `lib/auth-api.ts` whose request matches `POST /api/auth/login`. No real OAuth runs;
sign-in is optional and not required for a first lead.

### 6. Consent UI
`components/ui/ConsentNotice.tsx` shows a short privacy notice before any chat/contact data is
sent (§6), policy version `2026-06-02-local`. Consent evidence travels inside the Web Chat
payload until the backend persists anonymous consent.

## Video assets — object storage mapping (metadata only)

Binaries are NOT in git. Source files currently live on Yandex.Disk
(`https://disk.yandex.ru/d/oopTdDN0CTuIig`). Target object storage: bucket `astor-media`
(per README), exposed to the frontend only as `publicUrl` / `signedUrl` (§2).

Frontend resolution order (`lib/video-catalog.ts → resolveMediaUrl`):

1. absolute `http(s)` URL (dev sample / CDN) → used as-is;
2. local public asset path `/portfolio/...` → used as-is;
3. bare object key → prefixed with `NEXT_PUBLIC_MEDIA_BASE_URL`.

Suggested object-key convention for the 30 items:

```
content/c3flex/videos/<slug>/source.mp4      # original
content/c3flex/videos/<slug>/mobile.mp4      # 720p portrait/landscape rendition
content/c3flex/videos/<slug>/desktop.mp4     # 1080p rendition
content/c3flex/posters/<slug>.jpg            # poster
```

`<slug>` equals the catalog `slug` (currently the case `id`). To go live with real media:
set `NEXT_PUBLIC_MEDIA_BASE_URL`, upload files under the keys above, then either set each
case's `video`/`image` to the object key or replace `catalogVideos` with the backend response.

## Environment variables (frontend)

```
NEXT_PUBLIC_MEDIA_BASE_URL     # object storage / CDN base for bare object keys
NEXT_PUBLIC_WEB_CHAT_ENDPOINT  # defaults to /api/chat (mock); set to gateway /api/messages
NEXT_PUBLIC_AUTH_LOGIN_ENDPOINT# optional; enables real OAuth start
NEXT_PUBLIC_LEAD_ENDPOINT      # existing lead endpoint (unchanged)
```

## Handoff to Codex (backend next targets, per contract §9)

1. `GET /api/content/c3flex/videos` service-backed.
2. Stable web session id instead of the temporary numeric `chatId`.
3. Web channel persistence for site messages.
4. Site lead / admin notification projection.
5. Keycloak / OAuth2 resource server.
6. Anonymous consent persistence + link-to-user.
