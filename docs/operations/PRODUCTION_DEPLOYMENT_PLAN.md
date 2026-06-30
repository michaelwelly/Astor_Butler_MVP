# Production Deployment Plan

Дата: 2026-06-23

Цель: вывести Astor Butler / C3FLEX frontend и backend-инфраструктуру на удаленную машину так, чтобы заказчик мог смотреть сайт по домену, а AERIS Telegram bot и site/C3FLEX bot работали на одной инфраструктуре, но по разным сценариям.

## 1. Разделение Работ

### Codex / Backend / Infra

- Production topology.
- Remote VM/provider choice.
- Docker Compose -> k3s migration plan.
- Object Storage для видео и media.
- Web Chat API.
- Site bot events.
- Staff/Admin/System chat notifications для сайта.
- Keycloak + Spring Security + OAuth2.
- Google/Yandex login.
- JWT/session policy.
- Consent/policy для сайта.
- User profile enrichment.
- Data model для video/media metadata.

### Claude / Frontend / UX

- Каталог 30 видео.
- Preview cards для видео.
- Описания, теги, duration.
- Adaptive video player.
- Chat widget, сворачиваемый в compact search-like window.
- Website user context collection.
- Login UI для Google/Yandex.
- Consent UI.

Claude не трогает backend, FSM, Docker, env, infra и source-of-truth docs без явного разрешения.

## 2. Production Target

### MVP VM

Минимально комфортная конфигурация:

```text
8 vCPU
32 GB RAM
300-500 GB SSD/NVMe
Ubuntu 24.04 LTS
Object Storage отдельно
```

Для первого платного стенда можно начать с:

```text
4 vCPU
16 GB RAM
200-300 GB SSD/NVMe
Object Storage отдельно
```

Но текущая инфраструктура тяжелая: Postgres, Redis, Kafka/Redpanda, MinIO, Mongo, Scylla, Neo4j, LLM gateway, backend, frontend. Поэтому 8/32 выглядит спокойнее.

### Provider Shortlist

1. Timeweb Cloud
   - проще быстро поднять VDS/S3;
   - хорош для ручного MVP-стенда;
   - удобно для старта без enterprise overhead.

2. Yandex Cloud
   - лучше как production narrative для заказчика;
   - Compute + Object Storage + Managed Kubernetes в одной экосистеме;
   - дороже/сложнее, но солиднее.

3. Selectel
   - сильная инфраструктура;
   - хороший вариант, если нужен российский cloud/provider с большим запасом.

Решение: сначала выбрать provider и бюджет, затем завести VM + Object Storage + домен/TLS.

## 3. Media / 30 Videos

Видео не хранятся в git и не попадают в jar/container image.

Хранение:

- binaries: Object Storage;
- metadata: PostgreSQL/Mongo media catalog;
- previews/posters: Object Storage;
- generated thumbnails: Object Storage;
- optimized variants: Object Storage.

Минимальная metadata-модель:

```text
video_id
slug
title
description
tags
duration_seconds
poster_object_key
source_object_key
mobile_object_key
desktop_object_key
status
created_at
updated_at
```

Для MVP frontend может использовать placeholder JSON/metadata, но production должен получать catalog через backend API.

## 4. Site Bot / Web Channel

Нужно добавить канал:

```text
WEB
```

Он должен идти через тот же MessageGateway/FSM boundary, но иметь отдельные сценарные entrypoints:

- website lead;
- video interest;
- chat question;
- callback/contact request;
- КП request;
- manager handoff.

Site bot обязан отправлять события в:

- Admin Chat;
- Staff/Manager Chat, если это операционный запрос;
- System Chat, если это техническое событие и включен флаг.

## 5. Auth / Security

Production auth target:

- Keycloak как identity provider boundary;
- Spring Security resource server;
- OAuth2 login providers:
  - Google;
  - Yandex;
- JWT:
  - access token: 15 минут;
  - refresh token: 7 дней для MVP;
  - revoke/refresh audit в БД.

Site consent:

- пользователь видит privacy/policy до отправки заявки;
- для OAuth login фиксируем provider, subject, email, consent version, timestamp;
- Telegram consent остается отдельной evidence chain.

## 6. External Bot Enrichment

`@binaryf4f5r7m_bot` можно рассматривать только как внешний источник enrichment, если есть легальный API/договоренный способ доступа.

Правило:

- не парсить приватные данные без явного основания;
- хранить source, timestamp, confidence;
- не смешивать неподтвержденные данные с verified profile fields.

## 7. Deployment Phases

### Phase 0 - Local Stabilization

- Зафиксировать два bot profiles.
- Проверить AERIS bot и site/C3FLEX bot на общей infra.
- Не собирать production до чистого git tree.

### Phase 1 - Remote Docker Compose

- VM.
- Docker + Compose.
- `.env.production` через secrets vault/manual secure copy.
- Nginx/Caddy reverse proxy.
- TLS.
- Frontend container.
- Backend container.
- Object Storage connection.

### Phase 2 - k3s

- k3s.
- namespaces: `astor-prod`, `astor-observability`.
- sealed/external secrets later.
- ingress controller.
- persistent volumes only for data services when needed.
- S3/Object Storage for media.

### Phase 3 - Production Hardening

- backups;
- monitoring;
- log retention;
- domain DNS;
- deployment checklist;
- rollback plan;
- admin runbook.

## 8. Immediate Next Steps

1. User chooses provider/budget.
2. Codex prepares backend/infra backlog:
   - Web channel;
   - video catalog API;
   - Keycloak integration plan;
   - staff/admin/system site notifications.
3. User launches Claude with `docs/frontend/CLAUDE_FRONTEND_TASK.md`.
4. Claude returns frontend file/component plan.
5. Codex reviews boundaries before frontend implementation starts.

## 9. Windows Server Bootstrap Checklist

Если удаленная машина будет Windows и доступ через AnyDesk, backend-runtime лучше поднимать в WSL2 Ubuntu. Docker Desktop можно использовать для UI, но production-like команды выполняются внутри Ubuntu shell.

### 9.1 Enable WSL2 / Ubuntu

Открыть PowerShell от администратора:

```powershell
wsl --install -d Ubuntu-24.04
wsl --set-default-version 2
```

Если Windows попросит reboot - перезагрузить сервер и снова открыть Ubuntu.

### 9.2 Install Docker Engine Inside Ubuntu

```bash
sudo apt update
sudo apt install -y ca-certificates curl gnupg git jq unzip
sudo install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu $(. /etc/os-release && echo "$VERSION_CODENAME") stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
sudo apt update
sudo apt install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
sudo usermod -aG docker "$USER"
```

Закрыть Ubuntu shell, открыть заново и проверить:

```bash
docker version
docker compose version
```

### 9.3 Clone Project

```bash
mkdir -p ~/projects
cd ~/projects
git clone https://github.com/michaelwelly/Astor_Butler_MVP.git
cd Astor_Butler_MVP
git status
```

Секреты не хранятся в git. `.env` переносится вручную из локальной защищенной копии или собирается по актуальному runbook.

### 9.4 Start Infrastructure

Минимальный production-like стенд для AERIS Telegram bot:

```bash
docker compose --profile ai up -d postgres redis kafka minio mongo scylla neo4j natasha-nlu ollama-1 ollama-2 llm-gateway
docker compose ps
```

Если RAM меньше 24-32 GB, сначала поднять без тяжелых AI/VLM профилей и проверить базовую FSM-инфраструктуру:

```bash
docker compose up -d postgres redis kafka minio mongo scylla neo4j natasha-nlu
```

### 9.5 Pull / Verify Local Models

```bash
docker exec -it astor_ollama_1 ollama pull qwen2.5:1.5b
docker exec -it astor_ollama_1 ollama pull nomic-embed-text
docker exec -it astor_ollama_2 ollama pull qwen2.5:3b
docker exec -it astor_ollama_2 ollama pull nomic-embed-text
```

VLM is not required for baseline booking/RAG. On a stronger server it can be pulled later:

```bash
docker exec -it astor_ollama_2 ollama pull qwen2.5vl:3b
```

### 9.6 Build and Run Backend Bots

```bash
docker compose build aeris-astor-butler-bot
docker compose up -d aeris-astor-butler-bot api-gateway
docker compose logs -f aeris-astor-butler-bot
```

Health checks:

```bash
curl -s http://localhost:8088/actuator/health | jq
curl -s http://localhost:8089/actuator/health | jq
```

### 9.7 Frontend

Frontend production build should run from `frontend/` or its Docker service after Claude merges the frontend branch:

```bash
cd frontend
npm ci
npm run build
npm run start
```

If frontend is containerized in the current compose version, prefer:

```bash
docker compose build frontend
docker compose up -d frontend
```

### 9.8 Resource Target

Recommended for full local AI + infra:

```text
CPU: 8 vCPU+
RAM: 32 GB preferred, 24 GB workable
Disk: 300-500 GB SSD/NVMe
```

Baseline without VLM can start on 16 GB RAM, but Kafka + Scylla + Neo4j + Ollama will be tight.
