# Telegram Webhook Routing Plan

Дата: 2026-08-02

Статус: implementation-ready plan, не деплоить без отдельного подтверждения. DNS records, certificates, ALB resources and Telegram `setWebhook` are not created by this document.

## Goal

Move Telegram inbound updates from long polling to HTTPS webhook when domain/TLS/routing are ready.

Target public endpoint:

```text
https://c3ag.online/telegram/webhook
```

Alternative if a dedicated API subdomain is approved:

```text
https://api.c3ag.online/telegram/webhook
```

Use exactly one verified production hostname in runtime config. Do not set webhook to a bare IP address.

## Official References

- Telegram Bot API `setWebhook`: `https://core.telegram.org/bots/api#setwebhook`
  - Telegram sends HTTPS POST requests with serialized `Update` objects to the configured URL.
  - Telegram retries unsuccessful webhook deliveries.
  - `secret_token` can be used so the server can verify `X-Telegram-Bot-Api-Secret-Token`.
- Yandex Certificate Manager domain validation: `https://yandex.cloud/en/docs/certificate-manager/concepts/challenges`
  - Let's Encrypt certificates require HTTP or DNS domain ownership verification.
- Yandex Certificate Manager service integration: `https://yandex.cloud/en/docs/certificate-manager/concepts/services`
  - Certificate Manager certificates can be used by Yandex Application Load Balancer.
- Yandex Application Load Balancer: `https://yandex.cloud/en/docs/application-load-balancer/`
  - ALB can terminate TLS and route HTTP traffic to backend groups.

## Current Verified State

Read-only checks on 2026-08-02:

- `c3ag.online` resolves to the VM public IPv4:

```text
c3ag.online A -> 51.250.31.97
NS -> ns1.reg.ru, ns2.reg.ru
```

- `api.c3ag.online` and `telegram.c3ag.online` do not currently resolve.
- Yandex Cloud DNS in the active folder has only auto-created private zones:
  - `10.in-addr.arpa.`
  - `internal.`
- There is no Yandex Application Load Balancer in the active folder.
- There is no Certificate Manager certificate in the active folder.
- VM:
  - name: `astor-butler-aeris-mvp`;
  - public IPv4: `51.250.31.97`;
  - attached SG: `astor-butler-mvp-sg` / `enptafg8to48sg6ipr7f`.
- Security group ingress already allows public TCP `80` and `443`.
- On the VM, port `80` is served by `astor_api_gateway`.
- On the VM, port `443` is not listening.
- `http://c3ag.online/` currently returns a redirect to `http://c3ag.online:8080/swagger-ui/index.html`.
- `https://c3ag.online/` currently fails to connect.
- C3 frontend is still directly exposed at `http://51.250.31.97:3001`.
- Backend app is exposed at `http://51.250.31.97:8089`.
- The backend currently uses `TelegramLongPollingBot` with `TelegramBotsApi(DefaultBotSession)`.
- No `/telegram/webhook` controller exists yet.
- Telegram outbound reachability from the VM still fails:
  - DNS resolves `api.telegram.org`;
  - IPv4 `149.154.166.110:443` times out;
  - IPv6 is not usable from the VM path.

## Critical Clarification

Webhook changes only the inbound delivery mechanism:

```text
Telegram servers -> HTTPS webhook -> Astor backend
```

It reduces or removes long polling from the VM, but it does not remove outbound Telegram API calls from Astor:

```text
Astor backend -> https://api.telegram.org -> sendMessage / answerCallbackQuery / sendPhoto / getFile / setWebhook / deleteWebhook
```

Therefore, webhook can receive updates even if polling is not used, but normal bot replies, management calls and media/file operations still require working outbound HTTPS to Telegram Bot API, unless responses are limited to the webhook HTTP response pattern supported by the chosen Telegram library and scenario. Current code sends through `telegramBot.execute(...)`, so outbound Telegram egress remains required.

## Is A DNS Change Enough?

No, not by itself.

Current `c3ag.online A -> 51.250.31.97` proves basic domain routing to the VM, but Telegram webhook requires a reachable HTTPS URL with a valid certificate. Today:

- DNS points to the VM;
- SG permits `443`;
- no service listens on `443`;
- no Certificate Manager certificate exists;
- no ALB exists;
- no backend webhook endpoint exists.

A DNS change would be enough only after one of these HTTPS routing paths is ready:

1. ALB + Certificate Manager routes `443` to the backend service; or
2. VM-local Caddy/nginx terminates TLS on `443` and proxies `/telegram/webhook` to the backend.

The preferred architecture from the user is Yandex ALB + Certificate Manager.

## Preferred Architecture: Yandex ALB + Certificate Manager

```text
Telegram
  -> https://c3ag.online/telegram/webhook
  -> Yandex Application Load Balancer :443
  -> HTTP router path /telegram/webhook
  -> backend group target VM 10.129.0.32:8089
  -> Spring webhook controller
  -> TelegramRouter / FSM
  -> Telegram Bot API outbound sendMessage
```

Recommended routing:

| Host | Path | Target |
| --- | --- | --- |
| `c3ag.online` | `/telegram/webhook` | backend `8089` |
| `c3ag.online` | `/api/**` | backend/api-gateway, after final API routing decision |
| `c3ag.online` | `/**` | C3 frontend `3001` |

If avoiding mixed frontend/backend host routing, use:

| Host | Path | Target |
| --- | --- | --- |
| `api.c3ag.online` | `/telegram/webhook` | backend `8089` |
| `c3ag.online` | `/**` | C3 frontend |

`api.c3ag.online` is cleaner operationally, but it requires creating a new DNS record and certificate SAN.

## DNS/TLS Requirements

Because `c3ag.online` uses REG.RU name servers, DNS changes must be made at REG.RU unless the domain is delegated to Yandex Cloud DNS later.

For ALB:

1. Create a managed certificate for the exact hostname(s):
   - `c3ag.online`; and optionally
   - `api.c3ag.online`.
2. Complete Certificate Manager domain validation:
   - DNS challenge is usually easiest when DNS is outside Yandex Cloud;
   - HTTP challenge requires temporary HTTP routing that Certificate Manager can reach.
3. Create ALB with public listener `443`.
4. Attach the Certificate Manager certificate to the ALB listener.
5. Route webhook path to backend target group.
6. Update DNS:
   - `c3ag.online A` from `51.250.31.97` to ALB public IPv4 if using apex through ALB;
   - or `api.c3ag.online A/CNAME` to ALB target if using subdomain.
7. Keep old VM direct ports only until smoke passes, then close debug ports where safe.

## Backend Implementation Plan

Add a webhook mode without breaking existing polling mode.

### Configuration

New env/properties:

```text
TELEGRAM_BOT_ENABLED=false
TELEGRAM_WEBHOOK_ENABLED=false
TELEGRAM_WEBHOOK_PUBLIC_URL=https://c3ag.online/telegram/webhook
TELEGRAM_WEBHOOK_SECRET_TOKEN=<runtime secret>
TELEGRAM_WEBHOOK_SET_ON_STARTUP=false
TELEGRAM_WEBHOOK_DROP_PENDING_UPDATES=false
TELEGRAM_WEBHOOK_ALLOWED_UPDATES=message,callback_query
```

Suggested meanings:

- `TELEGRAM_BOT_ENABLED`: legacy polling registration switch.
- `TELEGRAM_WEBHOOK_ENABLED`: enables HTTP controller.
- `TELEGRAM_WEBHOOK_SET_ON_STARTUP`: when true, application calls Telegram `setWebhook` on startup; keep false until egress is fixed and manually smoke-tested.
- `TELEGRAM_WEBHOOK_SECRET_TOKEN`: compared to `X-Telegram-Bot-Api-Secret-Token`.

### Code

1. Keep `TelegramRouter` as the shared update handler.
2. Add `TelegramWebhookController`:
   - `POST /telegram/webhook`;
   - accepts Telegram `Update`;
   - validates `X-Telegram-Bot-Api-Secret-Token`;
   - rejects missing/invalid token with `401`/`403`;
   - sends update to `TelegramRouter`;
   - returns `200` quickly.
3. Add a sender abstraction if needed:
   - current `TelegramRouter` expects `AbsSender`;
   - webhook mode still needs an `AbsSender`/bot client for replies;
   - outbound `execute(...)` still depends on `api.telegram.org:443`.
4. Make polling and webhook mutually exclusive in production:
   - fail fast or warn if both are enabled for the same token.
5. Add logs/metrics:
   - accepted updates;
   - rejected secret token;
   - route failures;
   - Telegram outbound failures;
   - processing duration.
6. Add tests:
   - valid secret routes update;
   - invalid/missing secret rejected;
   - controller returns quickly;
   - polling registration disabled in webhook profile.

### Telegram Registration

Do not run until HTTPS and egress are green.

Manual command shape:

```bash
curl -sS \
  -X POST "https://api.telegram.org/bot${TELEGRAM_BOT_TOKEN}/setWebhook" \
  -d "url=https://c3ag.online/telegram/webhook" \
  -d "secret_token=${TELEGRAM_WEBHOOK_SECRET_TOKEN}" \
  -d "drop_pending_updates=false"
```

Do not print token. Prefer running through a script that redacts command output.

## Deployment Readiness Checklist

Do not set Telegram webhook until all are true:

- domain owner approves exact host: `c3ag.online` or `api.c3ag.online`;
- DNS control path is confirmed at REG.RU or delegated to Yandex Cloud DNS;
- HTTPS endpoint is live and externally reachable;
- Certificate Manager certificate is issued/valid, or VM-local certificate is valid;
- `/telegram/webhook` exists and returns safe status without token leakage;
- secret token is stored in runtime env only;
- Telegram outbound egress to `api.telegram.org:443` is fixed or a consciously limited inbound-only test is approved;
- current bot polling is disabled for the same token;
- one controlled Telegram update smoke is approved;
- rollback is documented:
  - `deleteWebhook`;
  - restore polling;
  - revert routing/env.

## Support-Ready Evidence

Latest VM Telegram reachability check on 2026-08-02:

```text
DNS api.telegram.org -> 149.154.166.110, 2001:67c:4e8:f004::9
/dev/tcp/api.telegram.org/443 -> timeout exit 124
curl -I https://api.telegram.org/ -> curl exit 28 after IPv4 connect timeout
IPv6 -> Network is unreachable
```

Infrastructure state:

```text
c3ag.online A -> 51.250.31.97
DNS authority -> REG.RU ns1.reg.ru/ns2.reg.ru
Yandex Cloud public DNS zone for c3ag.online -> absent
Yandex ALB -> absent
Certificate Manager certificates -> absent
VM SG ingress 443 -> present
VM listener 443 -> absent
```

## Exact Next Action

Ask the user to choose and confirm one routing target:

```text
Option A: c3ag.online/telegram/webhook
Option B: api.c3ag.online/telegram/webhook
```

Then get explicit approval for the infrastructure step:

1. If using preferred ALB architecture:
   - create ALB, target group, HTTP router, Certificate Manager certificate;
   - provide REG.RU DNS validation record;
   - wait for certificate;
   - update REG.RU A/CNAME to ALB;
   - add backend webhook endpoint;
   - only then set webhook after Telegram egress is resolved.
2. If using faster VM-local TLS:
   - configure Caddy/nginx on VM port `443`;
   - issue certificate via HTTP/DNS challenge;
   - route `/telegram/webhook` to backend;
   - add backend webhook endpoint;
   - only then set webhook after Telegram egress is resolved.

For the current state, DNS alone is not enough. It only proves the domain can point at an ingress target.

