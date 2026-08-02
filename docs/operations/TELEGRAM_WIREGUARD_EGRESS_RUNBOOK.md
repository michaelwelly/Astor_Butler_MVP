# Telegram WireGuard Egress Runbook

Дата: 2026-08-02

Статус: production VM configured, secret values are not stored in this repository.

## Goal

Restore Telegram Bot API reachability from the Yandex VM without routing public website, SSH, databases, deployment traffic or arbitrary VM traffic through VPN.

The supplied WireGuard config is a full tunnel:

```text
AllowedIPs = 0.0.0.0/0, ::/0
```

Because of that, it must not be applied with host-level `wg-quick up`. Host-level full tunnel would change the VM default route and could break SSH, website routing and deployments.

## Implemented Safe Shape

The VM uses a dedicated Linux network namespace:

```text
namespace: astor-tg-wg
host veth: astorwg-host 10.233.200.1/30
namespace veth: astorwg-ns 10.233.200.2/30
proxy: tinyproxy on 10.233.200.2:8888
systemd unit: astor-telegram-wg-proxy.service
secret dir: /etc/astor-telegram-wg, mode 0700
WireGuard raw config: /etc/astor-telegram-wg/wg0.raw.conf, mode 0600
```

Only clients explicitly configured to use:

```text
HTTP proxy http://10.233.200.2:8888
```

send traffic through the VPN namespace. The host default route remains:

```text
default via 10.129.0.1 dev eth0
```

## Runtime Env

Production `.env.production` on the VM was configured with:

```text
TELEGRAM_PROXY_TYPE=HTTP
TELEGRAM_PROXY_HOST=10.233.200.2
TELEGRAM_PROXY_PORT=8888
TELEGRAM_BOT_ENABLED=false
AERIS_ASTOR_BUTLER_BOT_ENABLED=false
```

Polling remains disabled intentionally. This avoids processing old pending Telegram updates or sending startup messages without a separate production activation decision.

When the operator is ready to activate polling, first decide pending-update policy, then set the relevant runtime flag(s) and restart only the AERIS bot service.

## Tests Completed

Read-only TLS test through the scoped proxy:

```text
curl --proxy http://10.233.200.2:8888 https://api.telegram.org/
```

Result on 2026-08-02:

```text
TLSv1.3 connected
certificate verified for api.telegram.org
HTTP/2 302 -> https://core.telegram.org/bots
curl_exit=0
```

Direct host route without proxy still reproduces the original issue:

```text
/dev/tcp/api.telegram.org/443 -> timeout exit 124
```

Token-safe Bot API smoke through the proxy:

```text
getMe_ok=true
bot_username=astor_butler_bot
```

No `sendMessage`, webhook registration or bot messages were sent.

Public regression checks after the VPN proxy:

```text
frontend http://51.250.31.97:3001/ -> 200
backend http://127.0.0.1:8089/actuator/health -> UP
c3_agency_frontend -> healthy
aeris_astor_butler_bot -> healthy
host default route -> unchanged via eth0
```

## Verification Commands

Do not print tokens.

```bash
systemctl is-active astor-telegram-wg-proxy.service
sudo ip netns exec astor-tg-wg wg show
ip route show default
curl -I --proxy http://10.233.200.2:8888 https://api.telegram.org/
timeout 8 bash -lc '</dev/tcp/api.telegram.org/443'; echo "$?"
curl -sS http://127.0.0.1:8089/actuator/health
curl -sSI http://51.250.31.97:3001/
```

## Activation Checklist For Polling

Before enabling the production bot:

1. Confirm no other environment is polling with the same BotFather token.
2. Decide pending update policy. Current long-polling code does not explicitly drop old updates.
3. Keep `ASTOR_OPERATIONAL_PREVIEW_ENABLED` and system notification settings conservative if startup side effects are not desired.
4. Set:

```text
TELEGRAM_BOT_ENABLED=true
AERIS_ASTOR_BUTLER_BOT_ENABLED=true
TELEGRAM_PROXY_TYPE=HTTP
TELEGRAM_PROXY_HOST=10.233.200.2
TELEGRAM_PROXY_PORT=8888
```

5. Restart only:

```bash
docker compose --env-file .env.production \
  -f docker-compose.yml -f docker-compose.prod.yml \
  up -d --no-build --force-recreate aeris-astor-butler-bot
```

6. Watch logs for `Telegram bot registered`.
7. Send one marked manual Telegram message only after confirming the bot is the only active consumer.

## Rollback

Stop VPN proxy and remove scoped runtime effects:

```bash
sudo systemctl disable --now astor-telegram-wg-proxy.service
sudo /usr/local/sbin/astor-telegram-wg-proxy stop
```

Restore Telegram direct/no-proxy runtime:

```text
TELEGRAM_PROXY_TYPE=NO_PROXY
TELEGRAM_PROXY_HOST=
TELEGRAM_PROXY_PORT=0
TELEGRAM_BOT_ENABLED=false
AERIS_ASTOR_BUTLER_BOT_ENABLED=false
```

Then restart the backend only if env was changed for a running bot.

To remove installed secret material entirely:

```bash
sudo rm -f /etc/systemd/system/astor-telegram-wg-proxy.service
sudo rm -f /usr/local/sbin/astor-telegram-wg-proxy
sudo rm -rf /etc/astor-telegram-wg
sudo systemctl daemon-reload
```

Do not commit the WireGuard config, private key, `.env.production`, backups or VM temporary files.
