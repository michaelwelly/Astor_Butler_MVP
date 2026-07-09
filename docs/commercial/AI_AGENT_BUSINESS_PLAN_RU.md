# Astor Butler: AI-agent strategy and business plan

Дата: 2026-07-07

Статус: рабочий стратегический документ. Числа по рынку, Selectel, OpenAI/API и конкурентам являются оценками на дату документа и должны перепроверяться перед коммерческим предложением, договором или финансовым планом.

## 1. Главная идея

Astor Butler не должен быть "оберткой над LLM" или обычным чат-ботом.

Целевая формула:

```text
HoReCa AI-agent platform:
LLM понимает речь гостя,
FSM принимает бизнес-решения,
domain services выполняют действия,
люди подтверждают чувствительные операции,
event trail сохраняет историю.
```

Коротко:

```text
LLM understands.
FSM decides.
Domain services act.
Humans confirm sensitive things.
Events remember everything.
```

Это отличает Astor Butler от простых Telegram-ботов, конструкторов сценариев и "свободных" AI-ассистентов, которые могут красиво отвечать, но не держат операционный процесс ресторана.

## 2. LLM и AI-agent: разница

LLM - языковая модель. Она хорошо:

- понимает свободный текст;
- извлекает намерения и сущности;
- формулирует ответы;
- суммаризирует переписки;
- помогает с RAG по меню, правилам и документам.

Но LLM сама по себе не должна:

- подтверждать бронь;
- обещать наличие столика;
- менять заказ;
- принимать платежи, ставки или отмены;
- быть source of truth для состояния гостя;
- хранить юридически значимое состояние.

AI-agent - это система вокруг модели:

- получает входящие сообщения;
- читает контекст гостя и заведения;
- выбирает сценарий;
- вызывает инструменты;
- проверяет бизнес-правила;
- сохраняет состояние;
- эскалирует человеку;
- логирует каждое важное решение.

Для Astor Butler правильная архитектура:

```text
Telegram / voice / web input
 -> transport normalization / STT
 -> SemanticRouter
 -> ScenarioRouter / FSM
 -> domain services
 -> PostgreSQL / Redis / Kafka / pgvector / MinIO
 -> response builder
 -> Telegram / admin / hostess output
```

## 3. Целевая AI-архитектура

### 3.1. Provider-agnostic ModelGateway

Проект должен быть независим от конкретной LLM.

```text
Astor Butler
 -> ModelGateway
    -> OpenAIModelProvider
    -> OllamaModelProvider
    -> VllmModelProvider
    -> LocalRulesProvider
    -> StubProvider for tests
```

Это позволяет:

- использовать OpenAI там, где это юридически и технически возможно;
- держать локальный fallback;
- переключаться на другие cloud/local провайдеры;
- не переписывать FSM и domain layer при смене модели.

### 3.2. SemanticRouter

LLM не должна "просто отвечать". Первый целевой контракт - понять, что хочет гость.

Пример результата:

```json
{
  "intent": "TABLE_BOOKING",
  "entities": {
    "date": "2026-07-08",
    "time": "20:00",
    "partySize": 6
  },
  "confidence": 0.91,
  "proposedAction": "CONTINUE_SCENARIO",
  "safetyFlags": []
}
```

FSM затем валидирует, можно ли выполнить предложенный переход.

### 3.3. Tool layer

Агенту нужны инструменты, но они должны быть Java/domain-инструментами, а не прямыми правами LLM на базу.

Примеры tools:

- `checkTableAvailability`;
- `createReservationDraft`;
- `sendHostessApproval`;
- `getMenuAssets`;
- `searchVenueContent`;
- `summarizeGuestRequest`;
- `handoffToManager`;
- `retrieveMenuContext`;
- `createEventBookingLead`.

Модель предлагает действие. Domain service проверяет и выполняет.

### 3.4. Model routing

Не каждый запрос должен идти в лучшую и дорогую модель.

Целевой routing:

```text
CLASSIFY_INTENT       -> rules/local/nano
EXTRACT_SLOTS         -> local/mini
GUEST_REPLY           -> mini
RAG_ANSWER            -> mini + pgvector context
HARD_CASE             -> flagship/reasoning model
MANAGER_SUMMARY       -> mini/flagship
SAFETY_OR_CONFLICT    -> flagship + human handoff
```

Практическая цель:

```text
60-80% сообщений: FSM/rules/cache/local model
15-30% сообщений: недорогая cloud model
1-5% сообщений: лучшая reasoning/flagship model
```

## 4. Технологии, которые стоит добавить

Уже сильная база проекта:

- Java 21;
- Spring Boot;
- Telegram transport;
- FSM как single source of truth;
- PostgreSQL;
- Redis;
- Kafka/outbox;
- pgvector direction;
- MinIO/S3 direction;
- Neo4j/scenario graph direction;
- ModelGateway direction.

Практически добавить/усилить:

- `SemanticRouter`;
- `SemanticDecision` DTO;
- provider registry для `ModelGateway`;
- tool calling contract;
- prompt/version registry;
- golden dataset для intents/entities;
- LLM eval tests;
- per-venue AI budget;
- usage accounting по tokens/calls/latency;
- PII masking перед внешними провайдерами;
- provider fallback policy;
- admin UI для просмотра agent decisions.

Для масштабирования на заведения:

- `tenant_id` / `organization_id`;
- `venue_id` почти во все доменные сущности;
- `venues`;
- `venue_settings`;
- `venue_opening_hours`;
- `venue_tables`;
- `venue_menus`;
- `venue_policies`;
- `venue_integrations`;
- `venue_ai_profile`.

Для интеграций HoReCa:

- iiko adapter;
- r_keeper adapter;
- SBIS adapter;
- POS/booking integration ports;
- Telegram/WhatsApp/web chat adapters;
- admin/hostess dashboard.

## 5. Чем Astor Butler отличается от рынка

Astor Butler не должен конкурировать как "чат-бот за 5 000 рублей".

Позиционирование:

```text
AI-host / AI guest manager для ресторана,
который встроен в реальные процессы заведения.
```

Отличия:

- FSM-governed, а не свободный LLM-chat;
- HoReCa-native сценарии: столы, банкеты, депозиты, меню, подрядчики, хостес, менеджер;
- human-in-the-loop для подтверждений;
- память заведения: меню, афиша, правила, документы, preference map;
- admin/system/hostess chats как operational control plane;
- event trail для аналитики и разбора ошибок;
- модель "soft-governance": агент не командует, а снижает хаос.

Главная коммерческая мысль:

```text
Astor Butler продает не кнопку и не генерацию текста.
Он продает управляемое гостевое внимание.
```

## 6. Масштабирование на другие заведения

Нужно отделить platform core от venue configuration.

Общая платформа:

- transport adapters;
- FSM runtime;
- ScenarioRouter;
- SemanticRouter;
- ModelGateway;
- booking domain contracts;
- media/RAG pipeline;
- admin notifications;
- consent/audit/event trail;
- integration ports.

Настройки заведения:

- название, адрес, timezone;
- часы работы;
- столы, зоны, планы;
- меню;
- правила отмены и депозитов;
- tone of voice;
- staff/admin/hostess chats;
- активные сценарии;
- интеграции;
- языки;
- escalation policy.

Целевая сущность:

```text
VenueAiProfile
- venueId
- brandVoice
- greetingStyle
- allowedPromises
- escalationRules
- bookingPolicy
- menuRetrievalScope
- sensitiveActionPolicy
- defaultLanguage
```

Путь масштабирования:

1. Довести AERIS как эталонный vertical slice.
2. Вынести AERIS-specific правила в venue config.
3. Добавить `venue_id` в таблицы, события и индексы.
4. Сделать onboarding script: меню, план зала, часы, чаты, tone.
5. Подключить второе заведение.
6. Найти и убрать оставшийся hardcode.
7. После 3-5 заведений делать полноценный onboarding/dashboard.

## 7. Инфраструктура Selectel: оценка бюджета

Оценки по публичным ценам Selectel на дату документа:

```text
vCPU: около 735 руб/мес
RAM: около 267 руб/GB/мес
local SSD: около 12.5 руб/GB/мес
public IP: около 190 руб/мес
backup: около 4.5 руб/GB/мес
```

### 7.1. MVP / пилот на одной машине

```text
4 vCPU
16 GB RAM
160 GB SSD
public IP
100 GB backup
50 GB S3/Vault

Итого: примерно 10 000 руб/мес
```

Подходит для одного заведения и разработки без локальной GPU.

### 7.2. Нормальный production для одного или нескольких заведений

```text
8 vCPU
32 GB RAM
300 GB SSD
public IP
200 GB backup
100 GB S3/Vault

Итого: примерно 19 000-20 000 руб/мес
```

Это разумный стартовый production baseline без локальной LLM/GPU.

### 7.3. Вариант с managed DB

```text
App VM 4 vCPU / 16 GB / 120 GB: примерно 8 000-9 000 руб/мес
Managed PostgreSQL 2 vCPU / 8 GB / 100 GB: примерно 7 000 руб/мес
Redis small или на app VM: 0-2 200 руб/мес
S3/backups/IP: 800-1 500 руб/мес

Итого: примерно 17 000-20 000 руб/мес
```

### 7.4. Локальная LLM/GPU

Постоянный GPU для одного ресторана на старте экономически не нужен.

Пример L4 24 GB:

```text
GPU L4 24 GB: примерно 36 000 руб/мес
8 vCPU: примерно 5 900 руб/мес
32 GB RAM: примерно 8 600 руб/мес
200 GB SSD: примерно 2 500 руб/мес
IP/backups: 500-1 000 руб/мес

Итого: примерно 53 000-55 000 руб/мес
```

Вывод:

```text
Для пилота лучше: CPU server + cloud/API LLM + local fallback.
GPU брать почасово или позже, когда есть стабильная нагрузка и понятная экономика.
```

## 8. Сколько заведений выдержит одна машина

Для машины `8 vCPU / 32 GB RAM / 300 GB SSD`, если OpenAI/LLM работает через API, а не локально:

```text
Комфортно:        5-10 заведений
С осторожностью: 10-20 заведений
Рискованно:      20+ заведений
```

Для бизнес-плана безопасно считать:

```text
1 production node = 10 заведений
```

При росте нужно разделять:

- app nodes;
- PostgreSQL;
- Redis;
- object storage;
- queue/event streaming;
- observability.

## 9. LLM/API расходы

Точная стоимость зависит от провайдера, модели, prompt design и процента сообщений, которые доходят до LLM.

Для одного ресторана уровня AERIS:

```text
20 столов
выручка около 3 млн руб/мес
примерно 800-1500 гостей/мес
примерно 300-1500 bot-dialogs/мес
```

Реалистичная оценка:

```text
Экономный model routing: 1 000-5 000 руб/мес на заведение
С запасом:               5 000-15 000 руб/мес на заведение
Плохой вариант без routing: может быть существенно дороже
```

Архитектурное правило:

```text
Не каждое сообщение идет в дорогую модель.
FSM/rules/cache/RAG-first, LLM only where useful.
```

Нужно добавить:

- учет tokens/calls по `venue_id`;
- месячный budget по заведению;
- premium-calls limit;
- fallback при превышении бюджета;
- отчеты по AI usage.

## 10. РФ, блокировки и провайдеры

Нельзя проектировать бизнес так, будто один внешний LLM-провайдер всегда доступен.

Нужна стратегия:

```text
ProviderStrategy:
- primary: разрешенный cloud provider where legally available
- fallback: local/open-weight model
- emergency: FSM-only mode
```

Важно:

- не держать бизнес-логику в LLM;
- не делать OpenAI единственной runtime-зависимостью;
- иметь local provider через Ollama/vLLM;
- иметь provider abstraction;
- для юридических вопросов по доступности провайдеров и географии клиентов нужна отдельная проверка.

## 11. Туннели, деплой и безопасность

Для Telegram webhook нужен публичный HTTPS endpoint:

```text
Telegram -> HTTPS domain -> Nginx/Caddy -> Spring Boot
```

Для разработки и администрирования:

- Tailscale;
- WireGuard;
- Cloudflare Tunnel;
- SSH по ключу;
- Caddy/Nginx reverse proxy.

Нельзя открывать наружу:

- PostgreSQL;
- Redis;
- Kafka/Redpanda;
- MinIO admin;
- internal actuator без защиты.

Минимально наружу:

```text
443 HTTPS
22 SSH только по ключу/VPN
```

## 12. Конкуренты и рынок

Рынок делится на несколько категорий.

### 12.1. Дешевые системы бронирования и ресторанные CRM

Примеры:

- Restoplace;
- ReMarked;
- ресторанные booking/CRM/loyalty systems.

Обычно это:

- виджеты бронирования;
- схема зала;
- CRM/лояльность;
- депозиты;
- POS-интеграции.

Цены часто находятся в диапазоне нескольких тысяч рублей в месяц за отдельные функции.

Вывод:

```text
Astor Butler не должен продаваться как "еще один виджет бронирования".
```

### 12.2. Конструкторы чат-ботов

Примеры:

- BotHelp;
- Salebot;
- Manychat;
- Tidio.

Обычно это:

- сценарии;
- рассылки;
- интеграции;
- AI-addons;
- CRM-lite.

Цены могут быть низкими, но ресторан сам собирает операционную логику или платит интегратору.

Вывод:

```text
Astor Butler должен продавать готовое внедрение, а не конструктор.
```

### 12.3. AI-host / restaurant AI platforms

Примеры международного типа:

- Slang AI;
- Popmenu AI;
- Owner.com;
- OpenTable guest management / reservations.

Обычно это уже ближе к ценам:

```text
$149-$599+ в месяц
или setup + monthly subscription
```

Вывод:

```text
Astor Butler должен позиционироваться ближе к AI-host / guest manager,
а не к дешевому chatbot builder.
```

## 13. Коммерческая модель: не три тарифа, а внедрение + поддержка

Три тарифа `Start / Pro / Premium` не подходят, если ценность продукта - в рабочей интеграции.

Лучше:

```text
Разовое внедрение + единая поддержка.
```

Рекомендуемая модель:

```text
Внедрение: 150 000-500 000 руб разово
Поддержка: 10 000 руб/мес
Дополнительные доработки: отдельно
AI overage: отдельно или по лимиту
```

Для первого публичного кейса:

```text
100 000-150 000 руб
или сниженная цена за право использовать кейс
```

Для обычного заведения:

```text
250 000 руб setup
10 000 руб/мес support
```

Для сложных заведений:

```text
350 000-500 000+ руб setup
10 000-30 000 руб/мес support/SLA
```

### 13.1. Что входит во внедрение

- Telegram-agent заведения;
- tone of voice;
- меню/FAQ/RAG;
- бронирование столов;
- event/banquet lead flow;
- confirmation через хостес/менеджера;
- интеграция с iiko/r_keeper/SBIS или выбранной системой;
- карта зала/столы/зоны;
- правила заведения;
- admin/hostess/system notifications;
- базовая аналитика;
- обучение команды;
- сопровождение после запуска.

### 13.2. Что покрывает поддержка 10 000 руб/мес

- хостинг;
- мониторинг;
- резервные копии;
- мелкие исправления;
- обновления ядра;
- контроль логов;
- поддержку интеграции;
- лимит AI usage.

Ограничения нужно прописать:

```text
Поддержка включает:
- до N диалогов/мес;
- до N AI tokens/мес;
- до 1 часа мелких правок/мес;
- best-effort SLA.

Сверх лимита:
- AI usage по факту;
- доработки по ставке;
- срочная поддержка отдельно.
```

## 14. Юнит-экономика

### 14.1. Если продавать только подписку 10 000 руб/мес

Формула:

```text
Server: 20 000 руб/мес
LLM per venue: 3 000 руб/мес
Other: 5 000 руб/мес
Revenue per venue: 10 000 руб/мес
```

```text
profit = N * 10 000 - 20 000 - N * 3 000 - 5 000
profit = N * 7 000 - 25 000
```

Точка безубыточности:

```text
25 000 / 7 000 = около 4 заведений
```

Оценка:

```text
1 заведение:  убыток/пилот
3 заведения:  почти ноль
4 заведения:  окупаемость
5 заведений:  первые деньги
10 заведений: около 45 000 руб/мес валовой прибыли
20 заведений: около 115 000 руб/мес валовой прибыли
```

Вывод:

```text
10 000 руб/мес как единственная цена слаба.
Она хороша как support retainer после оплаченного внедрения.
```

### 14.2. Модель setup + support

Пример:

```text
Setup: 250 000 руб
Support: 10 000 руб/мес
```

Если внедрение занимает 100 часов:

```text
250 000 / 100 = 2 500 руб/час
```

Recurring:

```text
10 заведений * 10 000 = 100 000 руб/мес
20 заведений * 10 000 = 200 000 руб/мес
```

Основная маржа на старте - внедрения.
Поддержка покрывает hosting/AI/операционную стабильность и создает recurring base.

## 15. Event/festival direction

Ивенты и фестивали - сильный acquisition channel для Astor Butler.

Позиционирование:

```text
Astor Butler Event AI Host
```

Агент для события:

- расписание;
- меню/бар/цены;
- карта площадки;
- VIP/столы;
- ответы на FAQ;
- lost and found / помощь;
- вызов менеджера;
- промо-механики;
- сбор лидов;
- post-event summary.

### 15.1. Нагрузка на 2000 человек

Допущения:

```text
20% активных: 400 пользователей
40% активных: 800 пользователей
70% активных: 1400 пользователей
90% активных: 1800 пользователей
```

Сообщения:

```text
800 пользователей * 6 сообщений = 4 800 входящих
1400 пользователей * 8 сообщений = 11 200 входящих
1800 пользователей * 10 сообщений = 18 000 входящих
```

Для backend это не критичная нагрузка, если:

- есть queue/rate limit для Telegram sending;
- медиа отдается из S3/MinIO/CDN;
- меню/афиша кешируются;
- LLM вызывается только там, где нужен смысл;
- admin notifications не спамят чат без backpressure.

### 15.2. AI cost на event

Для 10 000 сообщений, если LLM вызывается в 30% случаев:

```text
3 000 LLM calls
input: 3-6M tokens
output: 0.5-1.2M tokens
```

Грубая оценка:

```text
AI/API cost на event: 2 000-10 000 руб
с запасом на retries/voice/summaries: 10 000-20 000 руб
```

Если все бездумно гнать через дорогую модель, стоимость может стать намного выше. Поэтому для event обязателен model routing.

### 15.3. Pricing events

Рекомендуемая цена:

```text
Малый event до 300 гостей:        30 000-50 000 руб
Средний event 300-1000 гостей:    70 000-120 000 руб
Большой event 1000-3000 гостей:   150 000-300 000 руб
Фестиваль/несколько зон:          от 300 000 руб
```

Для пика 2000 человек:

```text
Оптимально продавать за 150 000-250 000 руб за событие.
```

Event business logic:

```text
Event project -> кейс -> ресторанное внедрение.
```

Даже если 1 из 5 event-клиентов становится постоянным заведением, это хорошая воронка.

## 16. Что нужно сделать в продукте, чтобы это продавать

Первый коммерчески продаваемый vertical slice:

```text
Гость пишет/говорит
 -> агент понимает намерение
 -> FSM проверяет сценарий
 -> domain service создает/обновляет draft/order
 -> хостес/менеджер подтверждает
 -> гость получает ответ
 -> история сохраняется
 -> управляющий видит summary/analytics
```

Минимум для "все или ничего":

- table booking end-to-end;
- event/banquet lead flow;
- menu/FAQ RAG;
- admin/hostess confirmations;
- POS/booking integration boundary;
- AI usage accounting;
- deployment runbook;
- onboarding checklist;
- demo script for restaurateur;
- one real case study.

Что не является первым коммерческим must-have:

- сложные донаты;
- аукционы;
- merch;
- Hidden Heart;
- Smart Tip;
- большая self-service SaaS панель;
- локальный GPU inference в production.

Эти функции могут быть extensions после доказанного core value.

## 17. Риски

Технические:

- LLM latency;
- Telegram rate limits;
- интеграции iiko/r_keeper/SBIS;
- качество извлечения даты/времени;
- backpressure admin notifications;
- хранение медиа;
- восстановление после падений.

Бизнес:

- ресторан воспринимает продукт как "чатбот";
- слишком низкая цена за кастомную работу;
- поддержка 10 000 руб/мес превращается в бесконечные доработки;
- один клиент требует слишком много bespoke logic;
- нет доказанной экономии или роста броней.

Юридические/операционные:

- персональные данные;
- согласия;
- география LLM-провайдеров;
- доступность внешних API;
- договоренности по SLA;
- ответственность за ошибочную бронь.

Меры:

- договор с границами ответственности;
- PII masking;
- human confirmation;
- event trail;
- provider abstraction;
- monthly AI budget;
- clear support limits;
- staging before production.

## 18. Практический следующий план

1. Зафиксировать `SemanticDecision` и `SemanticRouter`.
2. Усилить `ModelGateway` до provider registry.
3. Добавить usage accounting по `venue_id`.
4. Сделать AERIS vertical slice: booking -> hostess confirmation -> guest response -> audit.
5. Описать iiko/r_keeper/SBIS integration ports без жесткой привязки к одному vendor.
6. Подготовить onboarding checklist заведения.
7. Подготовить event AI-host checklist.
8. Сделать commercial demo для ресторатора.
9. После AERIS подключить второе заведение и убрать hardcode.
10. После 3-5 заведений решать, нужна ли self-service панель.

## 19. Рабочая цена

Основной оффер:

```text
Astor Butler Implementation
250 000 руб разово
10 000 руб/мес поддержка
```

Диапазон:

```text
Первый кейс:       100 000-150 000 руб setup
Обычный ресторан:  250 000 руб setup
Сложный проект:    350 000-500 000+ руб setup
Support:           10 000 руб/мес
Event:             150 000-250 000 руб за 2000 человек
```

Финальная позиция:

```text
Astor Butler - не дешевый бот и не универсальная AI-панель.
Это внедрение AI-host в операционную работу ресторана или события.
```

