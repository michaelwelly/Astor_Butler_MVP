# Claude Project Pack

Use this file as the quick context pack when connecting Claude App to the local folder:

```text
/Users/michaelwelly/IdeaProjects/Astor_Butler_MVP
```

## What To Add To Claude Project

In Claude App, choose **Use an existing folder** and select the repository folder.

Then add these files as high-priority project context:

```text
CLAUDE.md
README.md
docs/CLAUDE_PROJECT_PACK.md
docs/ARCHITECTURE.md
docs/FSM_SCENARIOS.md
docs/FSM_SCENARIOS_VIEWER.html
docs/FSM_IMPLEMENTATION_PLAN.md
docs/KAFKA_TOPICS.md
docs/TABLE_BOOKING.md
docs/DATABASE_MODEL.md
docs/API_CONTRACT.md
docs/FRONTEND_HANDOFF.md
design-system/c3flex/MASTER.md
```

Optional frontend skill context:

```text
.claude/skills/ui-ux-pro-max/SKILL.md
```

Do not add `.env`, `.env.*`, `target/**`, `.idea/**`, `graphify-out/**`, or local Codex artifacts.

## Instructions Field

Paste this into the Claude Project **Instructions** field:

```text
You work in Astor_Butler_MVP.

Default role: frontend/UX/design assistant. Codex owns backend, FSM, infrastructure, Telegram bot logic, databases, Kafka, Docker, and production readiness.

Allowed by default:
- frontend/**
- design-system/**
- frontend assets
- UI/UX copy
- frontend-oriented documentation assigned by the user

Forbidden without explicit user permission:
- src/main/**
- docker-compose.yml
- .env, .env.*
- docs/FSM_SCENARIOS.md
- docs/FSM_SCENARIOS_VIEWER.html
- docs/FSM_WORKING_SCENARIOS_UML.puml
- docs/ARCHITECTURE.md
- docs/KAFKA_TOPICS.md
- docs/TABLE_BOOKING.md
- Liquibase migrations
- backend tests
- database configuration
- Telegram bot scenario logic

Never delete project documentation. If a file seems outdated, propose an update or archive plan.
Never commit secrets, build artifacts, IDE files, or local caches.
Before edits, say which files you will touch and why.
After edits, list changed files and explicitly mention if any forbidden-scope file was touched.

Project priority:
Backend/FSM AERIS comes first. AERIS Telegram bot and C3FLEX/site bot should share infrastructure but follow different scenarios.

If the request is backend/FSM/infra related, pause and recommend handing it to Codex.
If the request is frontend/UX/C3FLEX/design related, work actively.
```

## Fast Work Plans

### Frontend Task Plan

1. Read `CLAUDE.md`.
2. Read `design-system/c3flex/MASTER.md`.
3. Inspect the specific `frontend/**` files related to the task.
4. Propose the minimal edit set.
5. Implement only frontend/design changes.
6. Verify responsive layout and no obvious console/build issues.
7. Report changed files.

### Astor Butler Context Plan

1. Read `README.md`.
2. Read `docs/ARCHITECTURE.md`.
3. Read `docs/FSM_SCENARIOS.md` and use `docs/FSM_SCENARIOS_VIEWER.html` only as visual context.
4. Do not edit FSM/backend files unless explicitly asked.
5. If the needed change is backend logic, write a handoff note for Codex.

### C3FLEX Design Plan

1. Read `design-system/c3flex/MASTER.md`.
2. Keep luxury/premium palette and typography.
3. Preserve performance: avoid heavy decorative work that slows portfolio pages.
4. Keep conversion path visible: project, proof, contact.
5. Prefer clean components over one-off visual hacks.

## Current Skill Notes

The local UI/UX skill found in `.claude/skills/ui-ux-pro-max/SKILL.md` is useful as a reference for:

- accessibility;
- responsive layout;
- typography;
- color contrast;
- landing/portfolio structure;
- charts and dashboards;
- React/Next.js UI review.

Claude App may not execute local CLI scripts automatically in the same way as CLI/IDE tools.
Use the skill markdown and `design-system/c3flex/MASTER.md` as readable context. Codex can run scripts and update project files when needed.
