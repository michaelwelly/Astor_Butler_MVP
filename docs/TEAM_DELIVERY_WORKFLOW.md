# Team Delivery Workflow

## Goal

Astor Butler needs a team-facing documentation and task-management loop before active implementation by several developers.

Target flow:

```text
Voice note / Markdown draft
  -> structured project documentation
  -> Confluence pages
  -> Jira epics/tasks/subtasks
  -> GitHub branches / PRs
  -> CI status back to Jira
```

## Tools

- Confluence - team documentation, architecture decisions, onboarding, API contracts, runbooks.
- Jira - backlog, epics, implementation tasks, QA tasks, release planning.
- GitHub - code, pull requests, GitHub Actions, branch and commit history.
- Markdown - source format for fast documentation capture and agent-generated drafts.

## Confluence Spaces

Recommended spaces/pages:

- `Astor Butler / Overview`
  - project concept;
  - MVP scope;
  - glossary;
  - links to GitHub, Notion, presentations and diploma materials.
- `Astor Butler / Architecture`
  - high-level architecture;
  - API contract;
  - data layer;
  - FSM;
  - capability modules;
  - integration boundaries.
- `Astor Butler / Backend`
  - Java/Spring conventions;
  - package map;
  - JDBC/Liquibase decisions;
  - test strategy;
  - observability.
- `Astor Butler / Frontend`
  - manager web app;
  - promo/lead-gen frontend;
  - generated API client usage;
  - UI flows.
- `Astor Butler / Operations`
  - Docker Compose;
  - profiles: `test`, `preprod`, `prod`;
  - CI/CD;
  - incident and support runbooks.

## Jira Structure

Recommended issue hierarchy:

- Epic: `MVP Architecture Foundation`
  - API contract skeleton.
  - Global error contract.
  - Swagger/OpenAPI generated client readiness.
  - Docker Compose test environment.
- Epic: `Document Store and Knowledge Base`
  - MongoDB document registry.
  - Document ingestion from Downloads/Obsidian/repo docs.
  - Academic work summary.
  - AI Adapter document lookup contract.
- Epic: `User/Auth Domain`
  - Telegram login verifier.
  - Keycloak JWT integration.
  - User profile CRUD.
  - Role and permission mapping.
- Epic: `Booking Domain`
  - booking DTOs;
  - JDBC schema;
  - status transitions;
  - manager notes.
- Epic: `FSM Core`
  - safe exit;
  - idempotency guard;
  - Redis persistence;
  - event normalization.
- Epic: `Capability Extensions`
  - Memory Engine;
  - Preference Map;
  - Smart Tip;
  - Quiet Guide;
  - Hidden Heart;
  - Safe Play;
  - Slot Keeper;
  - Panic Exit.

## Markdown-to-Jira Contract

Voice notes should be converted into Markdown first. Each task block should follow this shape:

```markdown
## <Task title>

Type: Story | Task | Bug | Spike
Epic: <Epic name>
Priority: P0 | P1 | P2 | P3
Owner: Backend | Frontend | DevOps | QA | Product

### Context
Why this task exists.

### Scope
What should be done.

### Acceptance Criteria
- [ ] Observable result 1
- [ ] Observable result 2

### Links
- API:
- Docs:
- Code:
```

This format is intentionally simple so it can later be converted into Jira issues through Jira API, CSV import, Atlassian automation, or an agent connector.

## GitHub Linkage

Branch naming:

- `codex/<task-key>-short-name`
- `feature/<task-key>-short-name`
- `fix/<task-key>-short-name`

Commit message format:

```text
<TASK-KEY>: <imperative summary>
```

PR description should include:

- Jira task link;
- scope;
- tests;
- Swagger/API contract changes;
- database migration changes;
- deployment notes.

## Current Status

- API contract skeleton exists in code.
- Swagger has non-empty paths and common error schema.
- Confluence/Jira are not connected yet.
- Next practical step is to export `docs/*.md` into the initial Confluence space and create Jira epics from this document.
