# C3AG.ru Frontend

Public video-first portfolio and lead-generation frontend for the C3AG.ru product contour.

## Local Run

Requirements:

- Node.js 20+;
- npm.

Install and start:

```powershell
cd frontend
npm install
npm run dev
```

Open:

```text
http://localhost:3001
```

Port `3000` is reserved for local Grafana in the backend Docker Compose stack.

## Backend Connection

Use `.env.local` only when custom frontend values are needed.

The chat widget works in demo mode while `NEXT_PUBLIC_WEB_CHAT_ENDPOINT=/api/chat`.
For production Telegram operator notifications, build with:

```text
NEXT_PUBLIC_WEB_CHAT_ENDPOINT=http://51.250.31.97:8089/api/messages
```

The backend must allow the frontend origin through `ASTOR_WEB_ALLOWED_ORIGINS`
and must have Telegram analytics/operator delivery configured through
`TELEGRAM_BOT_ENABLED`, `TELEGRAM_BOT_TOKEN`, and `TELEGRAM_ANALYTICS_CHAT_ID`
or `TELEGRAM_ADMIN_CHAT_ID`.

The portfolio dataset lives in `lib/portfolio.ts`; replace temporary media with
public Yandex Object Storage URLs from `c3ag-media` after curation.

## Clio Chat Persona

The frontend display assistant is `Clio`. User-facing copy and deterministic
demo replies live in `lib/clio-persona.ts`; backend/FSM identities are not
renamed by this frontend layer.

## Lightweight Preview

`preview.html` is a dependency-free visual preview for environments where frontend packages cannot be installed yet. The Next.js application remains the source of truth.
