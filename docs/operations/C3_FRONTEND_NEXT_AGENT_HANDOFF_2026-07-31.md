# C3AG Frontend Next Agent Handoff 2026-07-31

## Production State

- Production preview is deployed on `http://51.250.31.97:3001`.
- VM container: `c3_agency_frontend`.
- Compose service: `c3-agency-frontend`.
- Latest frontend image was rebuilt on the VM and the container is `healthy`.
- Backend/Telegram services were not touched during the frontend deploy.

## What Changed

- Splash now has a two-stage visual flow:
  - before click: content-machine background;
  - after click: sand/light transition.
- Light theme uses a minimal sand/light site texture.
- Dark theme uses the darker content-machine visual direction.
- Splash sound is a short four-hit handpan/hang motif.
- New generated backgrounds live in `frontend/public/hero-backgrounds/`.
- Production CSS references all four selected background files.

## Verification Already Done

- Local:
  - `npm run lint` passed with existing warnings only.
  - `npm run build` passed.
  - `git diff --check` passed.
- VM:
  - `docker compose --profile frontend up -d --build c3-agency-frontend` completed.
  - `c3_agency_frontend` reported `healthy`.
  - `/`, `/reels`, `/events`, `/reclama`, `/studio` returned `200`.
  - hero background PNGs returned `200 image/png`.
  - `/portfolio/segreto_hero.mp4` returned `200 video/mp4`.
  - `/api/yadisk?resolve=1&path=/AI/Morgan Barbie.mp4` returned a signed Yandex Disk URL.
  - Signed Yandex Disk URL supports range playback: byte-range GET returned `206 Partial Content` with `Access-Control-Allow-Origin: *`.

## Content Display Notes

- `frontend/data/videos.json` currently has 197 live records using `yadisk:*` and `yadisk-poster:*`.
- `featuredCount` is currently `0`; product rows still render, but there is no curated featured subset.
- Cards intentionally do not autoplay Yandex Disk archive masters because they are heavy camera masters. They show poster stills; full video resolves and plays in `ReelsPlayer` after click.
- `DeviceHero` intentionally limits live screens to 3 and uses posters for the rest to keep memory/CPU under control.
- If the desired behavior is "every card previews video like localhost", the right production step is to generate lightweight web renditions/previews and fill `previewUrl` or hosted `src` with Object Storage/CDN URLs.

## Do Not Accidentally Commit

- Backend files are currently dirty from a separate backend-agent flow. Keep them out of frontend commits unless that agent explicitly asks.
- Also keep out of the frontend commit:
  - `graphify-out/**`;
  - `output/**`;
  - `scripts/e2e_butler_yandex_smoke.mjs` unless backend-agent owns it;
  - `.env`, `.env.*`, `target/**`, `.codex*`.

## Useful Commands

```bash
rsync -az --delete --exclude node_modules --exclude .next --exclude .env --exclude .env.local --exclude .env.production \
  -e "ssh -p 2222 -i /Users/michaelwelly/.ssh/astor_yandex_vm_ed25519" \
  /Users/michaelwelly/IdeaProjects/Astor_Butler_MVP/frontend/ \
  ubuntu@51.250.31.97:/opt/astor-butler/frontend/

ssh -p 2222 -i /Users/michaelwelly/.ssh/astor_yandex_vm_ed25519 ubuntu@51.250.31.97 \
  'cd /opt/astor-butler && docker compose --env-file .env.production -f docker-compose.yml -f docker-compose.prod.yml --profile frontend up -d --build c3-agency-frontend'
```
