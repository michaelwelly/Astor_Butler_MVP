# C3AG: ограниченный frontend-deploy для Егора

Дата: 2026-08-03

Цель: дать Егору доступ только к выкладке публичного C3AG frontend на production VM без доступа к backend, секретам, базе данных, Telegram bot, Redis, MinIO или VPN.

## Production Target

Публичный preview:

```text
http://51.250.31.97:3001
```

VM:

```text
51.250.31.97
ssh port: 2222
frontend service: c3-agency-frontend
frontend container: c3_agency_frontend
deploy path: /opt/astor-butler
```

## Репозиторий и frontend paths

Егор работает с frontend-частью проекта:

```text
frontend/
frontend/app/
frontend/components/
frontend/lib/
frontend/public/
frontend/Dockerfile
frontend/next.config.ts
```

Операционный wrapper также использует только compose-файлы из `main`:

```text
docker-compose.yml
docker-compose.prod.yml
```

## Политика branch/ref

Production wrapper принимает только:

```text
main
```

Произвольные branch, tag, SHA или shell-команды отклоняются. Если нужно выкатить ветку или конкретный SHA, сначала нужно отдельное решение владельца проекта и обновление wrapper policy.

## SSH-доступ Егора

На VM создан отдельный пользователь:

```text
egor-c3deploy
```

Команды для Егора:

```bash
ssh -p 2222 egor-c3deploy@51.250.31.97 status
ssh -p 2222 egor-c3deploy@51.250.31.97 deploy main
ssh -p 2222 egor-c3deploy@51.250.31.97 rollback latest
```

Если SSH-клиент не подхватывает нужный ключ автоматически:

```bash
ssh -i <egor-private-key> -p 2222 egor-c3deploy@51.250.31.97 status
```

Разрешённые команды:

```text
help
version
status
deploy main
rollback latest
```

## Что делает deploy wrapper

Root-owned wrapper:

```text
/usr/local/sbin/c3ag-frontend-deploy
```

Forced-command gate:

```text
/usr/local/bin/c3ag-frontend-deploy-gate
```

Deploy flow:

1. Проверяет, что ref ровно `main`.
2. Клонирует чистый `main` из GitHub во временную директорию.
3. Делает backup текущих `frontend/`, `docker-compose.yml`, `docker-compose.prod.yml`.
4. Синхронизирует только `frontend/`.
5. Обновляет только `docker-compose.yml` и `docker-compose.prod.yml`.
6. Запускает:

```bash
docker compose --env-file .env.production \
  -f docker-compose.yml \
  -f docker-compose.prod.yml \
  --profile frontend \
  up -d --build c3-agency-frontend
```

7. Проверяет локальный health:

```text
http://127.0.0.1:3001/
```

8. Пишет результат в:

```text
/var/log/c3ag-frontend-deploy.log
```

Wrapper не пересоздаёт backend, bot, DB, Redis, MinIO или VPN-сервисы.

## Health verification

После deploy:

```bash
ssh -p 2222 egor-c3deploy@51.250.31.97 status
```

Ожидаемо:

```text
service=c3-agency-frontend
allowed_ref=main
c3_agency_frontend ... Up ... healthy
health=ok url=http://127.0.0.1:3001/
```

Внешняя проверка:

```bash
curl -I http://51.250.31.97:3001/
```

Ожидаемый HTTP status: `200`.

## Rollback

Если свежий deploy сломал frontend:

```bash
ssh -p 2222 egor-c3deploy@51.250.31.97 rollback latest
```

Rollback восстанавливает последний backup `frontend/` и compose-файлов, затем пересобирает только `c3-agency-frontend` и снова выполняет healthcheck.

## Запрещённые зоны

Егор не должен получать доступ к:

```text
/opt/astor-butler/.env.production
backend source/runtime secrets
PostgreSQL
Redis
Mongo/MinIO/Kafka/Neo4j/Scylla
Telegram bot tokens/chats
WireGuard/VPN config
Docker socket или docker group
общий sudo/shell на VM
```

Нельзя коммитить:

```text
.env
.env.*
target/**
.codex*
frontend/node_modules/**
frontend/.next/**
WireGuard configs
VM private keys
runtime logs with secrets
```

## Проверенный security state

Проверено 2026-08-03:

- `egor-c3deploy` существует отдельным OS-пользователем;
- пользователь не состоит в `sudo` и `docker`;
- SSH key ограничен forced command;
- `authorized_keys`, gate и wrapper принадлежат `root:root`;
- `sudoers` разрешает только `/usr/local/sbin/c3ag-frontend-deploy *`;
- gate отклоняет произвольную команду вроде чтения `.env.production`;
- прямое чтение `/opt/astor-butler/.env.production` от `egor-c3deploy` запрещено;
- прямой `docker ps` от `egor-c3deploy` запрещён;
- `status` показывает healthy frontend на `127.0.0.1:3001`;
- VM может читать `origin/main` из GitHub для deploy wrapper.

## Rollback доступа

Удалить доступ Егора:

```bash
sudo rm -f /etc/sudoers.d/egor-c3deploy-frontend
sudo rm -f /usr/local/bin/c3ag-frontend-deploy-gate
sudo rm -f /usr/local/sbin/c3ag-frontend-deploy
sudo userdel -r egor-c3deploy
```

Если нужно оставить пользователя, но временно выключить SSH:

```bash
sudo mv /home/egor-c3deploy/.ssh/authorized_keys /home/egor-c3deploy/.ssh/authorized_keys.disabled
```
