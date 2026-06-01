# C3FLEX.com Frontend

Public video-first portfolio and lead-generation frontend for the C3FLEX.com product contour.

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

The current lead form works in demo mode until `NEXT_PUBLIC_LEAD_ENDPOINT` is defined. The portfolio dataset lives in `lib/portfolio.ts`; replace its three sample video URLs with public MinIO URLs from `astor-media/raw/` after local curation.

## Lightweight Preview

`preview.html` is a dependency-free visual preview for environments where frontend packages cannot be installed yet. The Next.js application remains the source of truth.
