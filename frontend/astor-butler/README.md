# Astor Butler — Presentation Site

Анимированный презентационный сайт Astor Butler: брендовая книга + демонстрация сценариев.
Чистая статика: HTML/CSS/vanilla JS, без сборки и зависимостей. Отдельный бренд, не смешан с C3FLEX Next.js-приложением (`frontend/`).

## Структура

```
frontend/astor-butler/
├── index.html          # основной сайт (11 глав: hook → story → scenarios → comparison → CTA)
├── css/style.css       # вся стилистика (dark + gold, Playfair Display + Inter — синхронно с C3FLEX)
├── js/main.js          # курсор-ключ, рябь, дверь, scroll reveal, маскот Butler, chat UI
├── js/widget.js        # transport layer виджета: submitMessage(payload), mock/backend режимы
├── assets/             # favicon.svg, og-image.png
└── docs/               # коммерческий пакет как HTML-страницы
    ├── offer.html      # КП (из docs/commercial/COMMERCIAL_OFFER_RU.md)
    ├── comparison.html # сравнение (из docs/commercial/BENCHMARK_COMPARISON_RU.md)
    ├── brand.html      # бренд-гайд (из docs/commercial/BRAND_GUIDE_RU.md)
    └── docs.css
```

## Локальный запуск

Любой статический сервер, например:

```bash
cd frontend/astor-butler
python3 -m http.server 8090
# → http://localhost:8090
```

Или просто открыть `index.html` в браузере (Google Fonts требует сеть; без сети — системные fallback-шрифты).

## Деплой

Сайт хостится где угодно: nginx на Selectel, GitHub Pages, любой static hosting.
Достаточно отдать папку `frontend/astor-butler/` как document root. Никакой сборки.

nginx пример:

```nginx
server {
  server_name astorbutler.example;
  root /var/www/astor-butler;
  index index.html;
}
```

## Подключение backend (когда контейнеры будут развернуты)

Chat widget сейчас работает в mock-режиме. Для боевого режима:

1. Открыть `js/widget.js`.
2. Задать endpoint:

```js
window.AstorChatConfig = {
  endpoint: "https://<backend-host>/api/messages",
  channel: "WEB",
  site: "astor-butler-commercial",
};
```

3. `submitMessage(payload)` начнет отправлять POST JSON:

```json
{
  "channel": "WEB",
  "text": "…",
  "payload": {
    "sessionId": "web-…",
    "site": "astor-butler-commercial",
    "pageContext": "commercial_landing",
    "sentAt": "ISO-8601"
  }
}
```

Ожидаемый ответ от текущего backend `MessageController`: `{ "text": "…", "nextState": "…" }`.

4. Telegram-кнопки: заменить placeholder `https://t.me/astor_butler_bot` на реальный username бота (2 места в `index.html`).

## Чеклист проверки

- [ ] Hero открывается «дверью», ключ-курсор на desktop
- [ ] Рябь при движении мыши
- [ ] Маскот Butler появляется после hero и меняет позу/подпись по главам
- [ ] Guest journey читается как история (9 шагов, живые фрагменты диалога)
- [ ] Сравнение — 3 колонки, Astor Butler по центру
- [ ] Chat widget появляется на финальной секции, mock-ответы работают
- [ ] Telegram CTA в hero и в финале
- [ ] Ссылки на docs/offer.html, comparison.html, brand.html работают
- [ ] Mobile: нет горизонтального скролла, курсор-эффекты отключены
- [ ] prefers-reduced-motion: анимации отключаются, контент виден
- [ ] Keyboard: skip-link, focus-visible, Esc закрывает чат
