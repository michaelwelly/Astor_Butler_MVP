# C3FLEX / Astor Butler Frontend Handoff For Egor

Дата: 2026-07-22  
Владелец фронтенда: Егор + Claude  
Backend / infrastructure: Codex + Michael

## 1. Что это за проект

Репозиторий:

```text
/Users/michaelwelly/IdeaProjects/Astor_Butler_MVP
```

Внутри одного репозитория живут два продуктовых контура:

- `Astor Butler` - backend/FSM/Telegram/Web assistant для HoReCa.
- `C3FLEX.com`, будущий `C3AG.ru` - production-сайт команды видеопродакшена.

Егор работает в основном с frontend-контуром:

```text
frontend/**
design-system/**
docs/frontend/**
docs/contracts/**
docs/content/**
```

Backend-код без согласования не трогать:

```text
src/main/**
docker-compose.yml
.env*
docs/FSM_SCENARIOS_VIEWER.html
docs/architecture/**
```

## 2. Главная цель фронтенда

C3FLEX/C3AG - не классический лендинг, а video-first production portfolio:

- premium dark visual style;
- видео как главный контент;
- быстрый просмотр работ;
- адаптация под мобильный экран;
- мини-бриф/lead form;
- чат-виджет с Astor Butler;
- готовность к подключению backend API и Yandex Object Storage.

Публичный нейминг сейчас:

```text
C3FLEX.com
```

Будущий домен:

```text
C3AG.ru
```

## 3. Что прочитать перед работой

Обязательный минимум:

- `frontend/README.md`
- `docs/frontend/C3AG_FRONTEND_TZ.md`
- `docs/contracts/FRONTEND_BACKEND_CONTRACTS.md`
- `docs/contracts/API_CONTRACT.md`
- `docs/content/MEDIA_PIPELINE.md`
- `design-system/c3flex/MASTER.md`

Если нужно понять backend/инфраструктуру:

- `docs/architecture/ARCHITECTURE.md`
- `docs/operations/PRODUCTION_DEPLOYMENT_PLAN.md`

## 4. Локальный запуск

Требования:

- Node.js 20+
- npm

Команды:

```bash
cd frontend
npm install
npm run dev
```

Открыть:

```text
http://localhost:3001
```

Порт `3000` не использовать: он зарезервирован под Grafana в backend Docker Compose.

Проверки перед PR:

```bash
cd frontend
npm run lint
npm run build
```

Если Claude/песочница не может скачать native SWC binary, это нормально для sandbox. На рабочей машине Егора команды должны проходить локально.

## 5. Git workflow

Работать в отдельной ветке:

```bash
git checkout main
git pull origin main
git checkout -b frontend/egor-c3flex-<short-task>
```

Коммиты делать маленькими и понятными:

```bash
git add frontend docs/frontend docs/contracts docs/content design-system
git commit -m "Update C3FLEX video catalog UI"
git push origin frontend/egor-c3flex-<short-task>
```

После работы открыть PR в GitHub на `main`.

Важно:

- не пушить напрямую в `main`;
- не коммитить `.env`, `.env.local`, `.next`, `node_modules`, media originals;
- не коммитить большие видео;
- не менять backend-контракты без отдельного согласования.

## 6. Env для frontend

Frontend читает только public-переменные:

```env
NEXT_PUBLIC_API_BASE_URL=
NEXT_PUBLIC_MEDIA_BASE_URL=
NEXT_PUBLIC_WEB_CHAT_ENDPOINT=
NEXT_PUBLIC_AUTH_LOGIN_ENDPOINT=
NEXT_PUBLIC_LEAD_ENDPOINT=
```

Локально можно создать:

```text
frontend/.env.local
```

Пример:

```env
NEXT_PUBLIC_API_BASE_URL=http://localhost:8089
NEXT_PUBLIC_WEB_CHAT_ENDPOINT=http://localhost:8089/api/messages
NEXT_PUBLIC_MEDIA_BASE_URL=https://media.c3ag.ru
NEXT_PUBLIC_LEAD_ENDPOINT=http://localhost:8089/api/messages
```

Файл `.env.local` не должен попадать в git.

## 7. Backend contracts

Frontend не ходит напрямую в:

- PostgreSQL;
- Redis;
- Kafka/Redpanda;
- MinIO/Yandex Object Storage;
- MongoDB;
- ScyllaDB;
- Neo4j.

Все идет через backend/API Gateway.

Главные контракты:

### Video catalog

```http
GET /api/content/c3flex/videos?category=&tag=&featured=&limit=30
```

Frontend должен уметь жить с mock/local data, пока endpoint не готов, но форма данных должна совпадать с `docs/contracts/FRONTEND_BACKEND_CONTRACTS.md`.

### Web chat

```http
POST /api/messages
```

Для C3FLEX сайт отправляет:

- `channel: "WEB"`;
- текст пользователя;
- `payload.sessionId`;
- текущую страницу;
- selected video;
- viewport;
- UTM/referrer;
- consent evidence.

Backend сам выдает стабильный synthetic `chatId`.

### Lead form

На MVP lead form может использовать тот же `/api/messages`, пока нет отдельного endpoint.

Смысл: frontend собирает mini-brief, backend отправляет событие команде/админу и сохраняет audit.

## 8. Media strategy

Большие видео не хранятся в git.

Production target:

```text
Yandex Object Storage + CDN
```

Рекомендуемая структура bucket:

```text
astor-media-prod/
  c3flex/
    videos/
      <slug>/
        source.mp4
        desktop.mp4
        mobile.mp4
    posters/
      <slug>.jpg
  aeris/
    menu/
    hall-plan/
    interior/
```

Frontend не должен собирать S3 paths сам. Backend возвращает:

- `publicUrl`;
- `signedUrl`, если нужно;
- `poster.publicUrl`;
- `sources[]`.

Локально можно использовать mock URLs или маленький sample media set.

## 9. Что можно менять свободно

Можно:

- менять React components;
- улучшать layout, motion, mobile UX;
- переписывать video player;
- добавлять skeleton/loading/error states;
- менять локальные mock данные;
- добавлять frontend-only helper libraries;
- улучшать accessibility;
- редактировать `docs/frontend/**` и frontend handoff notes.

Нужно согласовать:

- изменение API shape;
- новые backend endpoints;
- изменение auth flow;
- изменение storage URL strategy;
- изменение доменной логики lead/chat;
- добавление тяжелых зависимостей.

## 10. UX ориентиры

Frontend должен ощущаться как production studio, не SaaS dashboard:

- video first;
- no generic stock feel;
- premium dark visual language;
- restrained motion;
- strong mobile experience;
- fast path to contact/brief;
- chat widget should not fight video browsing.

Для двух чат-входов:

- inline chat в секции нужен для контекста;
- floating/spotlight chat нужен как быстрый контакт;
- если UX перегружен, объединить входы в один predictable widget.

## 11. Deployment model

Первый cloud target:

```text
Yandex VM
  Docker Compose backend stack
  nginx/api-gateway
  frontend build or separate Node process
  Object Storage/CDN for media
```

Предпочтительно:

```text
c3ag.ru         -> frontend
api.c3ag.ru     -> backend/API gateway
media.c3ag.ru   -> CDN -> Yandex Object Storage
```

Минимальный frontend deploy на сервере:

```bash
cd Astor_Butler_MVP/frontend
npm ci
npm run build
npm run start
```

Production позже можно вынести в:

- Vercel;
- Yandex Cloud static hosting/CDN;
- Docker image behind nginx.

Решение по финальному hosting mode принимаем после первого Yandex deploy backend.

## 12. Claude instructions

При работе с Claude дать ему такой режим:

```text
You are working only on C3FLEX/C3AG frontend inside Astor_Butler_MVP.

Read first:
- docs/frontend/EGOR_FRONTEND_HANDOFF.md
- docs/frontend/C3AG_FRONTEND_TZ.md
- docs/contracts/FRONTEND_BACKEND_CONTRACTS.md
- design-system/c3flex/MASTER.md
- frontend/README.md

You may edit:
- frontend/**
- design-system/**
- docs/frontend/**

Do not edit without explicit approval:
- src/main/**
- docker-compose.yml
- .env*
- docs/FSM_SCENARIOS_VIEWER.html
- docs/architecture/**

Work in a separate branch and open a PR.
Do not commit node_modules, .next, media originals, secrets or local env files.
Keep frontend aligned with documented backend contracts.
```

## 13. PR checklist

Перед PR:

- `npm run lint` passed;
- `npm run build` passed;
- mobile layout checked;
- desktop layout checked;
- no `.env.local`;
- no `node_modules`;
- no `.next`;
- no large video files;
- API calls match `docs/contracts/FRONTEND_BACKEND_CONTRACTS.md`;
- changed docs if contracts/UX assumptions changed.

## 14. Current known backlog

- заменить mock video catalog на backend endpoint;
- подключить реальные media URLs из Yandex Object Storage;
- проверить два chat entry points на мобильном UX;
- довести lead form до backend/admin notification;
- подготовить C3AG.ru production domain wiring;
- улучшить accessibility and performance;
- решить hosting mode: VM nginx/Node, Vercel или Yandex static/CDN.

