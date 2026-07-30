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
- external `GET /`: `200`, title `C3FLEX.com — видео-продакшн с характером`;
- external hero video `/portfolio/segreto_hero.mp4`: `200`.

Temporary security-group rule:

- `c3-frontend-public-3001`: ingress TCP `3001` from `0.0.0.0/0`.

After domain/TLS is ready, close direct `3001` and route public traffic through `80/443`.

## Status

- Domain purchase: activation in progress at REG.RU.
- DNS records: pending registrar panel.
- Server TLS/routing: pending DNS propagation.
- VM frontend preview: running on `http://51.250.31.97:3001`.
- Telegram proxy: intentionally postponed until call with Egor.
