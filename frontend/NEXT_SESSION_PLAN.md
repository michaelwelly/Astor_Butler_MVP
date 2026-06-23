# C3FLEX / Astor Butler — план на следующую сессию

Обновлено: после паса «каталог-карусель + хедер-авторизация + eslint + a11y».
Границы те же: только `frontend/**` и `design-system/**`. Контракт —
`docs/FRONTEND_BACKEND_CONTRACTS.md`. Backend/FSM/infra/.env/docker — Codex.

## Уже сделано (закрыто)

- Контрактная модель каталога (`lib/video-catalog.ts`), 30 кейсов через адаптер.
- Превью-карточки: теги, краткое описание, бейджи featured/status.
- Адаптивный плеер: ориентация, выбор source по вьюпорту, fullscreen, пустое состояние.
- Чат: Spotlight ↔ полный чат, payload под `POST /api/messages`, consent-гейт.
- Login Google/Yandex — реальный OAuth-редирект + `/auth/callback`, фирменные кнопки.
- Хедер-авторизация: «Войти» (дропдаун) / «вошёл как…» + «Выйти» (`useAuth`).
- Каталог: карусель 4-на-страницу + свайп, остальное — модалка «Архив».
- Постер-заглушка `_poster-fallback.svg` + `onError` на карточках.
- ESLint flat config (`eslint.config.mjs`), `npm run lint` гоняется (0 ошибок).
- a11y: focus-visible, prefers-reduced-motion (CSS + MotionConfig), контраст `--muted`,
  `overflow-x` guard, `role=dialog`/`aria-modal`/Escape на модалках.

## Приоритет A — контент и финальная проверка

1. **Реальные данные 30 видео** (ждём имена/хронометраж/ориентацию с Яндекс.Диска):
   заполнить в `lib/portfolio.ts` `slug/tags/orientation/status/shortDescription` и
   object-keys по схеме из `FRONTEND_PRODUCTION_PLAN.md`. Залить медиа в `astor-media`,
   задать `NEXT_PUBLIC_MEDIA_BASE_URL`.
2. **Локальный `npm run build`** на маке (в песочнице не идёт: нет swc-linux + сети).
   Поймать то, что не видит `tsc`/eslint.
3. **Визуальная a11y-верификация** на реальном 375px: contrast-tool по тёмной теме,
   таб-порядок, отсутствие горизонтального скролла, focus-trap внутри модалок
   (сейчас Escape + видимый focus есть, зацикливание Tab — нет).

## Приоритет B — решения по UX

4. **Два чат-виджета** (inline в секции + плавающий Spotlight): после просмотра мобильного
   UX решить — оставить оба входа или объединить в один общий стейт/контекст.
5. **next/image** (опционально): убрать 10 lint-warnings `no-img-element`. Затрагивает и
   нетронутые файлы (ParallaxSection, SplashGate, CaseCard) — делать осознанно, разом.
6. Логаут/«вошёл как…» — проверить реальный вид после поднятия Keycloak.

## Приоритет C — интеграция (как только Codex поднимет бэкенд)

7. Чат → реальный `POST /api/messages` (`NEXT_PUBLIC_WEB_CHAT_ENDPOINT`), убрать dev-поле `turn`.
8. Каталог → `GET /api/content/c3flex/videos` (заменить `catalogVideos` ответом 1:1).
9. OAuth end-to-end (Keycloak + Google/Yandex), `/auth/callback` → `/api/auth/me`, `/api/auth/logout`.
10. Consent → серверная персистенция анонимного согласия.

## Открытые вопросы к Michael

- (ждём) два чат-виджета: оставить оба или объединить?
- Нужен ли отдельный экран/роут `/archive`, или модалки достаточно?
- Брендинг кнопок входа ок (Google белая / Yandex красная) или докрутить под гайды?
