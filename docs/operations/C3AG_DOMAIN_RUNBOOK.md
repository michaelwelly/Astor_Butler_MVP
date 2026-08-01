# C3AG Domain Runbook

Дата: 2026-07-30

Цель: купить `c3ag.ru`, направить домен на Yandex VM и подготовить C3 Agency frontend + AERIS backend к публичному запуску.

## Current Target

Yandex VM:

```text
astor-butler-aeris-mvp
public IPv4: 51.250.31.97
```

Yandex Cloud check:

```bash
yc vpc address get e2l09tnk86c131ojd26s --format json
```

Confirmed:

- address: `51.250.31.97`;
- type: `EXTERNAL`;
- zone: `ru-central1-b`;
- used by VM.

## Buy Domain

Primary target:

```text
c3ag.ru
```

Recommended registrars:

- REG.RU: `https://www.reg.ru/domain/new/`
- RU-CENTER / NIC.RU: `https://www.nic.ru/catalog/domains/`

Before payment:

1. Search for `c3ag.ru`.
2. Confirm the exact domain is available.
3. Buy for 1 year first.
4. Do not buy hosting or site builder add-ons.
5. Keep registrar DNS enabled unless we later move DNS to Yandex Cloud DNS.

## DNS Records

Create these records in the registrar DNS panel:

| Host | Type | Value | TTL |
| --- | --- | --- | ---: |
| `@` | `A` | `51.250.31.97` | `300` |
| `www` | `CNAME` | `c3ag.ru.` | `300` |
| `api` | `A` | `51.250.31.97` | `300` |

Optional later:

| Host | Type | Value | TTL |
| --- | --- | --- | ---: |
| `swagger` | `A` | `51.250.31.97` | `300` |
| `grafana` | `A` | `51.250.31.97` | `300` |

Do not expose `swagger` or `grafana` publicly until access control/TLS is ready.

## DNS Checks

After saving records:

```bash
dig +short c3ag.ru A
dig +short www.c3ag.ru
dig +short api.c3ag.ru A
```

Expected:

```text
51.250.31.97
```

Propagation can take from minutes to several hours. With TTL `300`, changes usually appear quickly after registrar DNS accepts them.

## Server Work After DNS

Once DNS resolves:

1. Add nginx/Caddy routing for:
   - `c3ag.ru` and `www.c3ag.ru` -> C3 Agency frontend container;
   - `api.c3ag.ru` -> backend/api-gateway.
2. Issue TLS certificates.
3. Close direct public debug ports where possible:
   - keep `80/443` public;
   - restrict `8089` after frontend/API gateway is stable.
4. Add C3 frontend Docker service and healthcheck.
5. Smoke:
   - `https://c3ag.ru`;
   - `https://api.c3ag.ru/actuator/health`;
   - frontend lead form to backend API.

## Temporary VM Preview

Until DNS/TLS is attached, the C3 Agency frontend is available directly on the VM:

```text
http://51.250.31.97:3001
```

Runtime:

- Docker Compose service: `c3-agency-frontend`;
- container: `c3_agency_frontend`;
- public port: `3001`;
- backend smoke stays on `http://51.250.31.97:8089`.

Validated on 2026-07-30:

- local Next production build: green;
- Docker image build: green;
- local container health: green;
- VM container health: green;
- external `GET /`: `200`, title was the pre-rename `C3FLEX.com — видео-продакшн с характером`;
- external hero video `/portfolio/segreto_hero.mp4`: `200`.

Updated on 2026-07-31:

- domains `c3ag.ru` and `c3ag.online` are bought and are waiting for REG.RU administrator identification;
- final frontend brand is `C3AG.ru`;
- production media bucket is `c3ag-media`;
- logo smoke URL: `https://storage.yandexcloud.net/c3ag-media/brand/c3ag-logo.svg`;
- Docker production default media base: `https://storage.yandexcloud.net/c3ag-media`;
- the frontend release includes a persisted light/dark theme toggle.

Temporary security-group rule:

- `c3-frontend-public-3001`: ingress TCP `3001` from `0.0.0.0/0`.

After domain/TLS is ready, close direct `3001` and route public traffic through `80/443`.

## Telegram API Egress Check

Yandex support answer on 2026-08-01:

- if the VM has a public IP, NAT Gateway is not required for outbound HTTPS to `api.telegram.org:443`;
- ensure outbound TCP `443`, DNS UDP/TCP `53`, local firewall and OS routing;
- metadata service `169.254.169.254:80` is separate from public internet egress;
- DDoS protection is a separate topic and does not replace application-level rate limits.

Safe VM check without printing bot token:

```bash
getent ahostsv4 api.telegram.org
getent ahostsv6 api.telegram.org
curl -4 -sS -o /tmp/tg-root-v4.txt \
  -w 'v4_https_status=%{http_code} remote=%{remote_ip} time=%{time_total}\n' \
  --connect-timeout 5 --max-time 10 https://api.telegram.org/
```

Observed on 2026-08-01 from VM `51.250.31.97`:

- DNS works: `api.telegram.org` resolves to IPv4 `149.154.166.110` and IPv6 `2001:67c:4e8:f004::9`;
- direct outbound HTTPS to `api.telegram.org:443` times out over IPv4;
- IPv6 connection fails immediately because Yandex Cloud security groups are IPv4-only and this VM path is not a working IPv6 egress path;
- no network rules were changed during the production C3AG rollout.

Additional VPC evidence on 2026-08-01:

- VM: `astor-butler-aeris-mvp` / `epdjiqbfc08tufap5v8p`;
- private IPv4: `10.129.0.32`;
- public IPv4: `51.250.31.97`, one-to-one NAT;
- subnet: `default-ru-central1-b`, `10.129.0.0/24`;
- network: `default`;
- attached security group: `astor-butler-mvp-sg` / `enptafg8to48sg6ipr7f`;
- security group egress already includes TCP `0.0.0.0/0` and UDP `0.0.0.0/0`;
- no custom route table is attached/listed for the network;
- VM OS route: `default via 10.129.0.1 dev eth0`;
- DNS resolver: `10.129.0.2`;
- `ufw` is inactive and host `iptables OUTPUT` policy is `ACCEPT`;
- general outbound HTTPS works from host:
  - `https://ya.ru/`: HTTP `302`;
  - `https://yandex.ru/`: HTTP `302`;
  - `https://storage.yandexcloud.net/`: HTTP `200`;
- general outbound HTTPS works from `aeris_astor_butler_bot` container to Yandex;
- `https://api.telegram.org/` still times out from both host and container.

Conclusion: this is not a missing NAT Gateway when the VM keeps public IPv4 `51.250.31.97`; the remaining gap is outbound reachability/firewall/routing or remote reachability to Telegram API from this VM/network path. Until resolved, `WebLeadNotificationService` can form and persist operator notifications, but actual Telegram delivery may fail or remain unverified.

No clear missing Yandex security-group egress rule, subnet default route, public-IP topology issue, DNS issue, or VM firewall block was identified. Do not apply route changes or DNS overrides blindly. The next safe step is to send the evidence above to Yandex support and ask them to check Telegram API reachability from this public IP / zone / network path, or to confirm whether provider-side filtering/routing toward Telegram exists.

## 2026-08-01 Production Rollout And Load Smoke

Deployed from local worktree to `/opt/astor-butler` using `rsync` with `.env*`, `.git`, `target`, `frontend/node_modules`, `frontend/.next`, `.codex*`, `.idea` and `graphify-out` excluded.

Rebuilt/recreated only:

```bash
docker compose --env-file .env.production \
  -f docker-compose.yml -f docker-compose.prod.yml \
  --profile frontend --profile telegram \
  up -d --build c3-agency-frontend aeris-astor-butler-bot
```

Production checks:

- `GET http://51.250.31.97:3001/`: `200`;
- `GET /film`, `/wedding`, `/podcast`: `200`;
- `GET /product-covers/film.jpg` and `/clio-avatar.jpg`: `200`;
- backend `GET http://51.250.31.97:8089/actuator/health`: `UP`;
- containers `c3_agency_frontend` and `aeris_astor_butler_bot`: `healthy`;
- visual screenshots: desktop dark/light home, mobile dark film, mobile light wedding;
- one marked WEB E2E message returned `WEB_LEAD_RECEIVED`, persisted `web_messages` as `IN=1` and `OUT=1`, and formed `ADMIN_ALERT`.

Controlled production k6 smoke:

```bash
C3AG_BASE_URL=http://51.250.31.97:3001 \
C3AG_BACKEND_URL=http://51.250.31.97:8089 \
C3AG_K6_VUS=2 \
C3AG_K6_DURATION=5m \
k6 run scripts/k6_c3ag_prod_smoke.js
```

Result:

- duration: 5 minutes;
- read-only only: frontend pages/assets and backend health, no chat POST flood;
- HTTP requests: `623`;
- failed requests: `0.00%`;
- checks: `1246/1246`, `100%`;
- `http_req_duration p95`: `536.5ms`;
- no interrupted iterations;
- post-load smoke kept frontend/backend healthy and recent logs showed no new error/exception/fatal entries.

## Status

- Domain purchase: activation in progress at REG.RU.
- DNS records: pending registrar identification / DNS panel.
- Server TLS/routing: pending DNS propagation.
- VM frontend preview: running on `http://51.250.31.97:3001`.
- Telegram API egress: DNS works, direct HTTPS to `api.telegram.org:443` currently times out from the VM; keep investigating outbound firewall/routing/Telegram reachability before relying on Telegram operator notifications.
