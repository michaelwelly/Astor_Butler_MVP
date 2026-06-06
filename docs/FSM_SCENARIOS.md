# Рабочие FSM-сценарии Astor Butler MVP

Этот документ описывает фактическую рабочую FSM-модель, которая сейчас есть в коде. Telegram остается транспортом и UI, а источником истины по состояниям является backend FSM.

Главная диаграмма ниже написана в Mermaid. GitHub умеет рендерить Mermaid прямо в Markdown, поэтому этот файл можно читать как один self-contained blueprint для человека и для LLM.

Для удобного просмотра крупными блоками в браузере есть отдельный HTML-viewer:

```text
docs/FSM_SCENARIOS_VIEWER.html
```

Дополнительная UML-диаграмма в формате PlantUML лежит рядом:

```text
docs/FSM_WORKING_SCENARIOS_UML.puml
```

Ее можно открыть в IntelliJ IDEA через PlantUML plugin или вставить в любой PlantUML renderer. Но основной рабочий файл для чтения - этот Markdown.

## Главная идея

Все входящие сообщения идут через `MessageGatewayService`. Это единая точка входа для Telegram, будущего web-чата и smoke-тестов через `POST /api/messages`.

Порядок маршрутизации:

1. Сохраняем входящее сообщение и профиль Telegram в PostgreSQL.
2. Публикуем событие в outbox/Kafka `astor.user.events`; admin/analytics chat получает человекочитаемую проекцию для наблюдаемости.
3. Если это служебный admin/analytics chat, сохраняем сообщение и не запускаем гостевой FSM.
4. Если это первое касание или гость еще не дал контакт/согласие, запускаем `FirstTouchScenario`.
5. Если гость уже в системе, он находится в базовом состоянии `READY_FOR_DIALOG`, которое в продуктовой логике играет роль `MainMenuScenario`.
6. Из `MainMenuScenario` запускаются конкретные сценарии: бронь стола, будущая бронь мероприятия, меню, менеджер, изменение брони.
7. Каждый завершенный сценарий возвращает гостя в `READY_FOR_DIALOG`.
8. `AI_FALLBACK` остается safety-net для неизвестных сообщений, а не основной дорогой диалога.

## Визуальная карта FSM

```mermaid
flowchart TD
    Start((Новое сообщение)) --> Gateway["MessageGatewayService<br/>единая точка входа"]

    subgraph Intake["Общее правило входа"]
        Gateway --> Store["Сохранить входящее сообщение<br/>PostgreSQL + outbox/Kafka"]
        Store --> ProjectionTap["Событие наблюдаемости<br/>astor.user.events"]
        ProjectionTap --> AdminProjection["Admin / Analytics chat<br/>человекочитаемая проекция"]
        Store --> ServiceCheck{"Служебный admin/analytics chat?"}
        ServiceCheck -- "да" --> AdminOnly["Только сохранить и наблюдать<br/>гостевой FSM не запускать"]
        ServiceCheck -- "нет" --> GuestGate{"Гость прошел first touch?"}
    end

    subgraph FirstTouch["Первое касание / FirstTouchScenario"]
        Unknown["UNKNOWN<br/>состояния еще нет"] --> Consent["CONSENT_REQUIRED<br/>нужен контакт + согласие"]
        Consent -- "/start" --> Consent
        Consent -- "текст без контакта" --> Nudge["PRE_AUTH_CONSENT_NUDGE<br/>вернуть к кнопке контакта"]
        Nudge --> Consent
        Consent -- "Telegram contact" --> Ready["READY_FOR_DIALOG<br/>можно вести сценарии"]
    end

    subgraph MainMenu["Базовое состояние / MainMenuScenario"]
        Ready --> Home["MAIN_MENU / READY_FOR_DIALOG<br/>дом сценариев"]
        Home --> MenuRender["Показать основные действия<br/>бронь стола / меню / менеджер / событие"]
        MenuRender --> ScenarioRouter{"Какой сценарий хочет гость?"}
        ScenarioRouter -- "стол / посадка / на двоих" --> Intent
        ScenarioRouter -- "банкет / день рождения / корпоратив" --> EventBooking["EVENT_BOOKING<br/>будущий сценарий мероприятия"]
        ScenarioRouter -- "меню / карта бара" --> MenuAssets["MENU_ASSETS<br/>будущий сценарий меню"]
        ScenarioRouter -- "менеджер / человек" --> ManagerHelp["MANAGER_HELP<br/>ручное подключение команды"]
        ScenarioRouter -- "непонятно" --> Ai
    end

    subgraph TableBooking["Бронь стола / TableBookingScenario"]
        Intent{"Intent: бронь стола?"}
        Intent -- "нет даты" --> Date["TABLE_BOOKING_COLLECT_DATE<br/>спросить дату"]
        Intent -- "есть дата, нет времени" --> Time["TABLE_BOOKING_COLLECT_TIME<br/>спросить время"]
        Intent -- "есть дата/время, нет гостей" --> Party["TABLE_BOOKING_COLLECT_PARTY_SIZE<br/>спросить гостей"]
        Intent -- "дата + время + гости" --> Draft["Сохранить booking draft<br/>Redis TTL"]

        Date --> Time
        Time --> Party
        Party --> Draft
        Draft --> Plan["Отправить AERIS PLAN.pdf<br/>classpath:booking/aeris-plan.pdf"]
        Plan --> WaitTable["TABLE_BOOKING_WAIT_TABLE_SELECTION<br/>ждем номер стола / зону / выбери сам"]

        WaitTable -- "повтор intent или неясный выбор" --> Plan
        WaitTable -- "стол 17 / 17 / vip / бар / выбери сам" --> CreateOrder["Создать order + HELD hold<br/>PostgreSQL"]
        WaitTable -- "выбран стол, но draft потерян" --> Date

        CreateOrder --> Hostess["TABLE_BOOKING_WAIT_HOSTESS_CONFIRMATION<br/>карточка хостес с кнопками Да/Нет"]
        Hostess -- "Да" --> Confirmed["TABLE_BOOKING_CONFIRMED<br/>order + hold CONFIRMED<br/>гостю красивый ордер"]
        Hostess -- "Нет" --> Rejected["TABLE_BOOKING_REJECTED<br/>hold RELEASED<br/>гостю вежливый отказ"]
        Rejected -- "гость выбирает другой вариант" --> Change["TABLE_BOOKING_CHANGE_REQUESTED<br/>обновить draft"]
        Change --> Plan
        Hostess -- "отмена" --> Cancelled["TABLE_BOOKING_CANCELLED<br/>закрыть бронь и release holds"]
        Confirmed --> ReturnHome["Вернуть гостя в READY_FOR_DIALOG"]
        Rejected --> ReturnHome
        Cancelled --> ReturnHome
    end

    subgraph Fallback["AI fallback / ручная помощь"]
        Ai["AI_FALLBACK<br/>непонятный текст или LLM-проблема"]
        Alert["Admin alert<br/>если включен TELEGRAM_ADMIN_CHAT_ID"]
        Ai --> Alert
    end

    GuestGate -- "нет / UNKNOWN" --> Consent
    GuestGate -- "CONSENT_REQUIRED" --> Consent
    GuestGate -- "да / READY_FOR_DIALOG" --> Home
    GuestGate -- "внутри активного сценария" --> ScenarioState{"Текущее FSM-состояние"}
    ScenarioState -- "TABLE_BOOKING_*" --> Intent
    ScenarioState -- "AI_FALLBACK" --> Home

    ReturnHome --> Home
    EventBooking --> Home
    MenuAssets --> Home
    ManagerHelp --> Home
    Ai -- "следующее сообщение гостя" --> Gateway

    CreateOrder --> ProjectionTap
    Confirmed --> ProjectionTap
    Rejected --> ProjectionTap

    classDef state fill:#E8F5E9,stroke:#2E7D32,stroke-width:1px,color:#1B5E20;
    classDef decision fill:#FFF8E1,stroke:#F9A825,stroke-width:1px,color:#3E2723;
    classDef storage fill:#E3F2FD,stroke:#1565C0,stroke-width:1px,color:#0D47A1;
    classDef terminal fill:#FCE4EC,stroke:#AD1457,stroke-width:1px,color:#880E4F;

    class Unknown,Consent,Ready,Home,Date,Time,Party,WaitTable,Hostess,Ai state;
    class ServiceCheck,GuestGate,ScenarioRouter,ScenarioState,Intent decision;
    class Store,ProjectionTap,AdminProjection,Draft,CreateOrder,Plan,MenuRender storage;
    class Confirmed,Rejected,Cancelled,AdminOnly,ReturnHome terminal;
```

## Перевод состояний на русский

| Состояние | По-русски | Что делает система |
| --- | --- | --- |
| `UNKNOWN` | Состояния еще нет | Redis не знает гостя; backend создает начальное состояние. |
| `CONSENT_REQUIRED` | Нужен контакт и согласие | Просим нажать кнопку контакта; без этого не ведем бронирование. |
| `READY_FOR_DIALOG` | Главное меню / дом сценариев | Гость прошел первый контакт; отсюда запускаются сценарии и сюда они возвращаются после завершения. |
| `AI_FALLBACK` | Непонятный текст или LLM-проблема | Последняя страховка, а не основной путь: отвечаем безопасно, при необходимости шлем alert админу. |
| `TABLE_BOOKING_COLLECT_DATE` | Нужна дата | Спрашиваем дату брони. |
| `TABLE_BOOKING_COLLECT_TIME` | Нужно время | Спрашиваем время брони. |
| `TABLE_BOOKING_COLLECT_PARTY_SIZE` | Нужно количество гостей | Спрашиваем, на сколько гостей бронировать. |
| `TABLE_BOOKING_WAIT_TABLE_SELECTION` | Ждем выбор стола | PDF плана уже отправлен, ждем номер стола, зону или "выбери сам". |
| `TABLE_BOOKING_WAIT_HOSTESS_CONFIRMATION` | Ждем хостес | Заявка и hold созданы, в чат хостес ушла карточка с кнопками `Да`/`Нет`. |
| `TABLE_BOOKING_CONFIRMED` | Бронь подтверждена | Хостес нажала `Да`, hold стал подтвержденным, гостю отправляется красивый ордер. |
| `TABLE_BOOKING_REJECTED` | Бронь отклонена | Хостес нажала `Нет`, hold освобожден, гостю отправляется вежливый отказ и предложение выбрать другой вариант. |
| `TABLE_BOOKING_CHANGE_REQUESTED` | Гость хочет изменить бронь | Пересобираем draft и снова идем к выбору/подтверждению. |
| `TABLE_BOOKING_CANCELLED` | Бронь отменена | Освобождаем активные holds и закрываем сценарий. |

Устаревшие алиасы `GREETING`, `CONTACT`, `MENU` еще есть в enum для совместимости Redis-данных. В runtime они канонизируются так:

| Старое состояние | Новое состояние |
| --- | --- |
| `GREETING` | `CONSENT_REQUIRED` |
| `CONTACT` | `CONSENT_REQUIRED` |
| `MENU` | `READY_FOR_DIALOG` |

## Первый контакт

Рабочий путь:

```text
Гость: /start
Astor: просит согласие и Telegram contact
Гость: делится контактом
Astor: сохраняет consent/profile/contact, переводит гостя в READY_FOR_DIALOG
```

Если гость пишет текст до контакта, система не продолжает бронирование. Она коротко возвращает его к кнопке согласия и контакта.

## Главное меню

`READY_FOR_DIALOG` должно быть не пустым состоянием свободного чата, а базовым состоянием `MainMenuScenario`.

Из него запускаются сценарии:

- бронь стола;
- будущая бронь мероприятия;
- меню/карта бара;
- помощь менеджера;
- изменение или отмена брони;
- другие будущие сценарии.

Правило: завершенный сценарий возвращает гостя в `READY_FOR_DIALOG`. Чем больше сценариев описано явно, тем реже система должна попадать в `AI_FALLBACK`.

## Бронь стола

Happy path:

```text
Гость: Хочу забронировать столик завтра на 20:00 на двоих
Astor: сохраняет draft, отправляет AERIS PLAN.pdf, просит выбрать стол/зону
Гость: 17
Astor: создает table_reservation_order и HELD hold
Astor -> чат хостес: карточка заявки с кнопками Да/Нет
Хостес: нажимает Да
Astor: подтверждает order/hold и отправляет гостю красивый ордер
```

Важное поведение:

- план зала отправляется до выбора стола;
- повторная фраза про бронь в состоянии выбора стола снова отправляет план, а не считается номером стола;
- выбор стола считается явным только для ответов вроде `17`, `стол 17`, `vip`, `бар`, `выбери сам`;
- хостес подтверждает только кнопками, свободный текст в чате хостес не подтверждает бронь;
- Redis хранит только runtime draft, а долговечная заявка и holds лежат в PostgreSQL.

## Хранилища

| Слой | Что хранит |
| --- | --- |
| Redis | Текущее FSM-состояние `astor:fsm:telegram:{chatId}:state` и временный table booking draft `astor:booking:table:draft:telegram:{chatId}`. |
| PostgreSQL | Telegram profile/messages/consents, `table_reservation_orders`, `table_reservation_holds`, `venue_tables`. |
| Kafka/outbox | События `astor.user.events` для admin/analytics projection. |
| Telegram | Только UI: сообщения гостя, PDF плана, кнопки контакта и кнопки хостес. |

## Проверка руками

1. Запустить инфраструктуру:

```bash
docker compose up -d
```

2. Запустить Spring Boot из IDEA или через:

```bash
scripts/run_local_app.sh
```

3. Проверить `.env`:

```bash
TELEGRAM_BOT_ENABLED=true
TELEGRAM_BOT_TOKEN=...
TELEGRAM_BOT_USERNAME=...
TELEGRAM_ADMIN_CHAT_ID=...
TELEGRAM_ANALYTICS_CHAT_ID=...
TELEGRAM_HOSTESS_CHAT_ID=-1004291419562
TELEGRAM_BOOKING_PLAN_PDF_PATH=classpath:booking/aeris-plan.pdf
KAFKA_BOOTSTRAP_SERVERS=localhost:9092
KAFKA_USER_EVENTS_TOPIC=astor.user.events
```

4. Написать гостем:

```text
Хочу забронировать столик завтра на 20:00 на двоих
```

Ожидаем:

- гостю приходит `AERIS PLAN.pdf`;
- FSM становится `TABLE_BOOKING_WAIT_TABLE_SELECTION`;
- в admin/analytics chat приходит человекочитаемый Kafka event.

5. Ответить гостем:

```text
17
```

Ожидаем:

- создается order в `table_reservation_orders`;
- создается hold в `table_reservation_holds`;
- чат хостес получает карточку с кнопками `Да`/`Нет`;
- гость получает сообщение, что заявка отправлена хостес.

6. Нажать `Да` в чате хостес.

Ожидаем:

- order получает статус `CONFIRMED`;
- hold получает статус `CONFIRMED`;
- гостю отправляется подтвержденный ордер.
