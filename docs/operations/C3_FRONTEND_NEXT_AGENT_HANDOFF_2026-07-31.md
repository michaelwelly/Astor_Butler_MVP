# C3AG Frontend Next Agent Handoff 2026-07-31

## Production State

- Production preview is deployed on `http://51.250.31.97:3001`.
- VM container: `c3_agency_frontend`.
- Compose service: `c3-agency-frontend`.
- Latest frontend image was rebuilt on the VM and the container is `healthy`.
- Backend AERIS was rebuilt after the frontend deploy to add YandexGPT understanding audit and the E2E smoke script.

## What Changed

- Splash now has a two-stage visual flow:
  - before click: content-machine background;
  - after click: sand/light transition.
- Light theme uses a minimal sand/light site texture.
- Dark theme uses the darker content-machine visual direction.
- Splash sound is a short four-hit handpan/hang motif.
- New generated backgrounds live in `frontend/public/hero-backgrounds/`.
- Production CSS references all four selected background files.
- The hero CTA is now `ФЛЕКСИТЬ`; the light/dark theme toggle lives beside it in the hero instead of the header.
- The archive modal scroll is contained in `.archive-modal-body`, so the page background no longer scrolls while browsing the archive.
- The Butler chat has premium AI assistant styling and uses the Astor Butler logo.
- The lower Astor Butler block is now a sales assistant surface, not an implementation explainer:
  - removed the Iris booking CTA from the C3AG site;
  - removed visible `WEB -> Postgres -> RAG -> YandexGPT` copy from the UI;
  - added Telegram-like quick buttons for C3 RИИLS, C3 REПОРТАЖ, C3 RECLAMA, C3 ФILM and C3 ЫI;
  - product buttons call the floating Butler chat with sales/scenario prompts.
- The generated preview background is now used as a persistent subtle site layer under lower sections.
- Backend web fast reply now says `C3AG`, not `C3FLEX`.
- LLM understanding calls now write `model_interaction_audit` rows with Yandex usage metadata.
- `scripts/e2e_butler_yandex_smoke.mjs` covers Yandex probe, web message, Telegram contact bootstrap, Telegram message, optional DB assertions and token-cost estimate.

## Verification Already Done

- Local:
  - `npm run lint` passed with existing warnings only.
  - `npm run build` passed.
  - `git diff --check` passed after Butler CTA/background changes.
  - targeted Maven tests passed: `MessageControllerTest`, `MessageGatewayServiceTest`, `GuestInputUnderstandingServiceTest`, `YandexModelGatewayTest`.
  - `git diff --check` passed.
- VM:
  - `docker compose --profile frontend up -d --build c3-agency-frontend` completed.
  - `docker compose --profile app build/up aeris-astor-butler-bot` completed.
  - `c3_agency_frontend` reported `healthy`.
  - `aeris_astor_butler_bot` reported `healthy`; `/actuator/health` returned `UP`.
  - `/`, `/reels`, `/events`, `/reclama`, `/studio` returned `200`.
  - `/podcast`, `/wedding`, `/film`, `/ai`, `/sitemap.xml`, `/ab-logo.jpg` returned `200`.
  - hero background PNGs returned `200 image/png`.
  - `/portfolio/segreto_hero.mp4` returned `200 video/mp4`.
  - `/api/yadisk?resolve=1&path=/AI/Morgan Barbie.mp4` returned a signed Yandex Disk URL.
  - Signed Yandex Disk URL supports range playback: byte-range GET returned `206 Partial Content` with `Access-Control-Allow-Origin: *`.
  - Butler E2E smoke saved messages and audit:
    - direct YandexGPT Lite probe: 242 tokens, about `0.0484 RUB`;
    - backend LLM audit rows: 841 total tokens, about `0.1682 RUB`;
    - Postgres counts for run `butler-e2e-20260731122300`: `telegram_messages=3`, `web_messages=2`, `model_interaction_audit=2`.

## Content Display Notes

- `frontend/data/videos.json` currently has 197 live records using `yadisk:*` and `yadisk-poster:*`.
- `featuredCount` is currently `0`; product rows still render, but there is no curated featured subset.
- Cards intentionally do not autoplay Yandex Disk archive masters because they are heavy camera masters. They show poster stills; full video resolves and plays in `ReelsPlayer` after click.
- `DeviceHero` intentionally limits live screens to 3 and uses posters for the rest to keep memory/CPU under control.
- If the desired behavior is "every card previews video like localhost", the right production step is to generate lightweight web renditions/previews and fill `previewUrl` or hosted `src` with Object Storage/CDN URLs.

## Do Not Accidentally Commit

- Keep local/runtime artifacts out of commits:
  - `output/**`;
  - `.env`, `.env.*`, `target/**`, `.codex*`.
- `graphify-out/**` may be committed when intentionally refreshed after code changes.

## Useful Commands

```bash
rsync -az --delete --exclude node_modules --exclude .next --exclude .env --exclude .env.local --exclude .env.production \
  -e "ssh -p 2222 -i /Users/michaelwelly/.ssh/astor_yandex_vm_ed25519" \
  /Users/michaelwelly/IdeaProjects/Astor_Butler_MVP/frontend/ \
  ubuntu@51.250.31.97:/opt/astor-butler/frontend/

ssh -p 2222 -i /Users/michaelwelly/.ssh/astor_yandex_vm_ed25519 ubuntu@51.250.31.97 \
  'cd /opt/astor-butler && docker compose --env-file .env.production -f docker-compose.yml -f docker-compose.prod.yml --profile frontend up -d --build c3-agency-frontend'
```
