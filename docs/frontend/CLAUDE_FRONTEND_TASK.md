# Claude Frontend Task

Use this prompt in Claude App after the project folder and context files are connected.

## When To Start Claude

Start Claude now with a planning step first: frontend architecture and UI component map.

Codex has confirmed the backend boundaries:

- video metadata contract;
- web chat payload contract;
- auth/consent payload contract;
- object storage URL strategy.

These boundaries are collected in:

- `docs/contracts/FRONTEND_BACKEND_CONTRACTS.md`

After Claude returns the file/component plan and the user approves it, Claude may implement frontend-only changes against this contract.

## Prompt

```text
You work in Astor_Butler_MVP.

Read first:
- CLAUDE.md
- docs/frontend/CLAUDE_PROJECT_PACK.md
- docs/contracts/FRONTEND_BACKEND_CONTRACTS.md
- design-system/c3flex/MASTER.md
- README.md

Task: prepare the frontend production plan for C3FLEX/Astor Butler site.

Scope:
- frontend/**
- design-system/**

Do not edit:
- src/main/**
- docker-compose.yml
- .env, .env.*
- docs/FSM_*
- docs/architecture/ARCHITECTURE.md
- backend/database/infra files

Goal:
The site must support a portfolio/media experience with 30 videos and a compact chat widget connected later to backend/Web Chat API.

Design/UX requirements:
1. Video catalog with 30 items.
2. Each item has preview card:
   - poster;
   - title;
   - short description;
   - tags;
   - duration;
   - status/featured flag if useful.
3. Adaptive video player:
   - mobile-first;
   - responsive to user screen;
   - supports portrait/landscape media;
   - fullscreen-friendly;
   - no layout jumps.
4. Chat widget:
   - can expand into normal chat;
   - can collapse into a compact macOS Spotlight/Search-like input;
   - collects context for backend:
     - current page;
     - referrer;
     - UTM;
     - selected video;
     - typed message;
     - timestamp;
     - anonymous/session id placeholder.
5. Login UI placeholders:
   - Google;
   - Yandex.
6. Consent UI:
   - short privacy notice before sending lead/chat/contact data.

Important:
Videos are not stored in git.
Use metadata and object storage URL placeholders.

First response required:
Give a file/component plan only.
List files you propose to edit/create.
Wait for approval before implementation.
```

## Expected Claude Output

Claude should return:

- component tree;
- route/page changes;
- data model TypeScript interfaces;
- mock data format;
- responsive/player strategy;
- chat widget state machine;
- list of files to edit/create;
- risks/questions for Codex backend.
