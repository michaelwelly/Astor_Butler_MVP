# Yandex VM Deployment Runbook

This is the first production-like path for AERIS on Yandex Cloud: GitHub Actions
syncs the repository to a VM over SSH and runs Docker Compose there.

## What Codex Can Automate

- CI: Maven tests, packaging, Docker image build.
- Secret/artifact guard: block tracked `.env`, `.env.*`, `target/**`, `.codex*`.
- Deploy workflow: copy source to a prepared VM and run Compose.
- Runtime switch to YandexGPT through `ASTOR_MODEL_PROVIDER=yandex`.

## What Must Be Done Manually

Yandex Cloud console and billing operations require a logged-in human session.
Do not store billing links, browser sessions, OAuth tokens, or cloud keys in git.

Manual Yandex Cloud steps:

1. Open the billing account in the Yandex Cloud console.
2. Create or choose a cloud and folder.
3. Create an Ubuntu 24.04 VM.
4. Recommended first AERIS size:
   - 4 vCPU / 16 GB RAM for a paid MVP stand;
   - 8 vCPU / 32 GB RAM if local LLM, Scylla, Neo4j, Grafana, and heavier media
     flows run on the same machine.
5. Add your SSH public key.
6. Open inbound firewall ports:
   - `22` for SSH from your IP;
   - `80` and `443` for public HTTP/TLS;
   - `8089` only temporarily for direct health checks, then close behind nginx.
7. Create a Yandex AI Studio API key or IAM token.
8. Create Object Storage buckets later for production media; local MinIO is still
   acceptable for the first VM smoke.

## VM Bootstrap

Run on the VM:

```bash
sudo apt-get update
sudo apt-get install -y ca-certificates curl git rsync
sudo install -m 0755 -d /etc/apt/keyrings
sudo curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
sudo chmod a+r /etc/apt/keyrings/docker.asc
echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/ubuntu $(. /etc/os-release && echo "$VERSION_CODENAME") stable" \
  | sudo tee /etc/apt/sources.list.d/docker.list >/dev/null
sudo apt-get update
sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
sudo usermod -aG docker "$USER"
sudo mkdir -p /opt/astor-butler
sudo chown -R "$USER:$USER" /opt/astor-butler
```

Log out and back in after adding the user to the `docker` group.

## Server Environment

Create `/opt/astor-butler/.env.production` on the VM. Keep it out of git.

```bash
SPRING_PROFILES_ACTIVE=prod
API_GATEWAY_PUBLIC_PORT=80
AERIS_BOT_PUBLIC_PORT=8089
SMART_SOLUTION_BOT_PUBLIC_PORT=8090

POSTGRES_DB=aether
POSTGRES_USER=astor
POSTGRES_PASSWORD=<strong-password>
REDIS_PASSWORD=<optional-redis-password>
MONGO_USER=astor
MONGO_PASSWORD=<strong-password>
MONGO_DB=aether

S3_ACCESS_KEY=<minio-access-key>
S3_SECRET_KEY=<minio-secret-key>
S3_BUCKET_MEDIA=astor-media
S3_BUCKET_DOCUMENTS=astor-documents
S3_PUBLIC_ENDPOINT=https://<media-domain-or-vm>

TELEGRAM_BOT_ENABLED=true
TELEGRAM_PROXY_TYPE=NO_PROXY
TELEGRAM_PROXY_HOST=
TELEGRAM_PROXY_PORT=0
AERIS_ASTOR_BUTLER_BOT_TOKEN=<telegram-token>
AERIS_ASTOR_BUTLER_BOT_USERNAME=<bot-username>
AERIS_ASTOR_BUTLER_ADMIN_CHAT_ID=<admin-chat-id>
AERIS_ASTOR_BUTLER_STAFF_CHAT_ID=<staff-chat-id>
AERIS_ASTOR_BUTLER_SYSTEM_CHAT_ID=<system-chat-id>

# Separate Smart Solution Telegram bot.
# Must be a different BotFather token from AERIS to avoid Telegram long polling conflicts.
SMART_SOLUTION_BOT_ENABLED=true
SMART_SOLUTION_BOT_TOKEN=<smart-solution-telegram-token>
SMART_SOLUTION_BOT_USERNAME=<smart-solution-bot-username>
SMART_SOLUTION_OPS_CHAT_ID=<smart-solution-team-chat-id>
SMART_SOLUTION_OWNER_MENTION=@michaelwelly
SMART_SOLUTION_OWNER_USERNAME=michaelwelly
SMART_SOLUTION_OWNER_USER_ID=<optional-telegram-user-id>
SMART_SOLUTION_REDIS_KEY_PREFIX=smart-solution
SMART_SOLUTION_GROUP_QA_ENABLED=true
SMART_SOLUTION_GROUP_QA_LLM_ENABLED=true
SMART_SOLUTION_GROUP_INTAKE_ENABLED=true
SMART_SOLUTION_MODEL_PROVIDER=yandex
SMART_SOLUTION_YANDEX_MODEL=yandexgpt-5-lite
SMART_SOLUTION_YANDEX_QUALITY_MODEL=yandexgpt-5.1

ASTOR_MODEL_PROVIDER=yandex
YANDEX_FOLDER_ID=<folder-id>
YANDEX_API_KEY=<api-key>
YANDEX_MODEL=gpt://<folder-id>/yandexgpt-5-lite/latest
YANDEX_QUALITY_MODEL=gpt://<folder-id>/yandexgpt-5.1/latest
YANDEX_MAX_TOKENS=256
YANDEX_TEMPERATURE=0.1
ASTOR_UNDERSTANDING_LLM_ENABLED=true

JWT_SECRET=<long-random-secret>
```

## GitHub Secrets

Add these repository secrets before running `Deploy to Yandex VM`:

```text
YANDEX_VM_HOST
YANDEX_VM_USER
YANDEX_VM_SSH_KEY
YANDEX_VM_SSH_PORT
YANDEX_VM_DEPLOY_PATH
```

`YANDEX_VM_SSH_PORT` and `YANDEX_VM_DEPLOY_PATH` are optional in practice; the
workflow defaults to `22` and `/opt/astor-butler`.

## First Deploy

In GitHub Actions, run:

```text
Deploy to Yandex VM
compose_profiles = telegram nlu
smoke_url = http://127.0.0.1:8089/actuator/health
```

To deploy AERIS and the separate Smart Solution bot together:

```text
Deploy to Yandex VM
compose_profiles = telegram smart-solution nlu
smoke_url = http://127.0.0.1:8089/actuator/health
extra_smoke_urls = http://127.0.0.1:8090/actuator/health
```

The workflow runs:

```bash
docker compose --env-file .env.production \
  -f docker-compose.yml \
  -f docker-compose.prod.yml \
  --profile telegram \
  --profile smart-solution \
  --profile nlu \
  up -d --build
```

To deploy only Smart Solution bot on the already prepared VM:

```text
Deploy to Yandex VM
compose_profiles = smart-solution
smoke_url = http://127.0.0.1:8090/actuator/health
```

## Local Dry Run

From a machine that has a safe `.env.production`:

```bash
docker compose --env-file .env.production \
  -f docker-compose.yml \
  -f docker-compose.prod.yml \
  --profile telegram \
  --profile smart-solution \
  --profile nlu \
  config >/tmp/astor-compose-prod.yaml
```

Then start only after reviewing the rendered config:

```bash
docker compose --env-file .env.production \
  -f docker-compose.yml \
  -f docker-compose.prod.yml \
  --profile telegram \
  --profile smart-solution \
  --profile nlu \
  up -d --build
```

Smart Solution group behavior:

- normal ops commands still work: `/ops`, `/projects`, `/summary MED`, `/tasks VIDEO`;
- group questions are answered from Ops CRM + project memory through LLM/RAG;
- if the bot cannot answer confidently, it tags `SMART_SOLUTION_OWNER_MENTION`;
- an owner reply to the original question is saved into `SMART_SOLUTION_GROUP_MEMORY`;
- `ASTOR_REDIS_KEY_PREFIX=smart-solution` keeps FSM/idempotency keys separate from AERIS.

## YandexGPT Smoke

Use the existing probe after setting `YANDEX_FOLDER_ID` and `YANDEX_API_KEY`:

```bash
node scripts/probe_yandex_understanding.mjs
```

In the app runtime, `ASTOR_MODEL_PROVIDER=yandex` activates
`YandexModelGateway`. FSM remains the business authority; YandexGPT only
classifies, extracts slots, summarizes, or drafts text behind `ModelGateway`.

## Production Smoke 2026-07-30

Manual deploy command used from the operator machine:

```bash
rsync -az --delete \
  --exclude='/.git/' \
  --exclude='/.env' \
  --exclude='/.env.*' \
  --exclude='/target/' \
  --exclude='/.codex*/' \
  --exclude='/frontend/node_modules/' \
  --exclude='/frontend/.next/' \
  --exclude='/output/' \
  --exclude='/graphify-out/' \
  -e 'ssh -i ~/.ssh/astor_yandex_vm_ed25519 -p 2222 -o BatchMode=yes' \
  ./ ubuntu@51.250.31.97:/opt/astor-butler/

ssh -i ~/.ssh/astor_yandex_vm_ed25519 -p 2222 ubuntu@51.250.31.97 \
  'cd /opt/astor-butler && docker compose --env-file .env.production -f docker-compose.yml -f docker-compose.prod.yml --profile telegram up -d --build aeris-astor-butler-bot'
```

Smoke result:

- container: `aeris_astor_butler_bot Up (healthy)`;
- health: `UP`;
- readiness: `ready=true`;
- OpenAPI paths: `117`;
- YandexGPT Lite probe: 200 total tokens, about 1.3s;
- REST `/api/messages` contact + booking smoke created reservation `#1`;
- database `aether` confirmed rows in `telegram_profiles`, `telegram_messages`, `table_reservation_orders`.

Smart Solution Ops:

- project `RESTO` set to `READY_TO_LAUNCH`, `85%`;
- artifact `AERIS VM YandexGPT booking smoke 2026-07-30` created;
- next task created for Germany proxy, `c3ag.ru`, and C3 frontend container.

## Next Hardening

- Put nginx/Caddy TLS in front of `api-gateway`.
- Move media from MinIO-on-VM to Yandex Object Storage.
- Add backups for PostgreSQL and MinIO/Object Storage metadata.
- Add production logging retention.
- Add deployment rollback commands after the first successful smoke.
