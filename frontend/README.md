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

The current lead form works in demo mode until `NEXT_PUBLIC_LEAD_ENDPOINT` is defined. The portfolio dataset lives in `lib/portfolio.ts`; replace temporary media with public Yandex Object Storage URLs from `c3ag-media` after curation.

## Lightweight Preview

`preview.html` is a dependency-free visual preview for environments where frontend packages cannot be installed yet. The Next.js application remains the source of truth.
