# FSM Infrastructure Blueprint

Документ описывает целевую инфраструктуру FSM-сценариев Astor Butler MVP перед изменениями в коде.

Цель: иметь одну понятную карту системы, по которой человек, LLM и тесты одинаково понимают, что делать с гостем на каждом шаге.

HTML-viewer с крупными диаграммами:

```text
docs/FSM_SCENARIOS_VIEWER.html
```

PlantUML-версия текущей карты:

```text
docs/FSM_WORKING_SCENARIOS_UML.puml
```

## Принципы

1. Telegram - только транспорт и UI.
2. `MessageGatewayService` - единая точка входа.
3. FSM - single source of truth по состояниям и переходам.
4. `READY_FOR_DIALOG` - не пустой чат, а дом сценариев, продуктово это `MainMenuScenario`.
5. Каждый завершенный сценарий возвращает гостя в `READY_FOR_DIALOG`.
6. Admin/analytics chat - наблюдаемость и аудит, а не гостевой сценарий.
7. LLM помогает распознать intent, извлечь сущности и сформулировать текст, но не подтверждает бронь, не создает holds и не меняет бизнес-статусы сам.
8. `AI_FALLBACK` - последняя страховка, а не обычный путь диалога.
9. 8 дипломных осей боли - продуктовый слой над FSM, а не отдельные хаотичные команды.
10. Legacy-сценарии `auction`, `charity`, `tip`, `merch`, `poster`, `feedback` используются как продуктовая память, но переписываются под новый FSM и capability boundaries.

## Общая Карта

```mermaid
flowchart TD
    Guest["Гость<br/>Telegram / будущий web chat"]
    Transport["Transport adapters<br/>TelegramRouter / REST"]
    Gateway["MessageGatewayService<br/>единая точка входа"]

    Intake["Intake layer<br/>profile + message + consent evidence"]
    EventTrail["Outbox + Kafka<br/>astor.user.events"]
    Admin["Admin / Analytics chat<br/>наблюдаемость"]

    FirstTouch["FirstTouchScenario<br/>контакт + consent"]
    MainMenu["MainMenuScenario<br/>READY_FOR_DIALOG<br/>дом сценариев"]
    Router{"Scenario Router<br/>intent + current state"}

    TableBooking["TableBookingScenario<br/>посадка за стол"]
    EventBooking["EventBookingScenario<br/>банкеты / события"]
    MenuAssets["MenuAssetsScenario<br/>меню / карта бара"]
    ManagerHelp["ManagerHelpScenario<br/>подключить команду"]
    ChangeCancel["ChangeCancelScenario<br/>изменить / отменить"]
    SmartTip["SmartTipScenario<br/>чаевые / благодарность"]
    HiddenHeart["HiddenHeartScenario<br/>донейты / социальный вклад"]
    ArtAuction["ArtAuctionScenario<br/>картины / аукцион на событии"]
    QuietGuide["QuietGuideScenario<br/>справки / афиши / контент"]
    SafePlay["SafePlayScenario<br/>мини-сценарии"]
    Impact["ImpactMeterScenario<br/>культурный вклад / KPI"]
    Recovery["RecoveryScenario<br/>непонятно / сбой / ручная помощь"]

    Domain["Domain services<br/>booking / media / consent / notifications"]
    Redis["Redis<br/>FSM state + runtime drafts"]
    Postgres["PostgreSQL<br/>durable facts"]
    Minio["MinIO / resources<br/>PDF / media"]
    Hostess["Hostess chat<br/>Да / Нет"]
    Manager["Manager/Admin<br/>ручная работа"]
    LLM["LLM Adapter<br/>intent/entities/text"]

    Guest --> Transport --> Gateway --> Intake
    Intake --> EventTrail --> Admin
    Gateway --> FirstTouch --> MainMenu
    Gateway --> MainMenu
    MainMenu --> Router

    Router --> TableBooking
    Router --> EventBooking
    Router --> MenuAssets
    Router --> ManagerHelp
    Router --> ChangeCancel
    Router --> SmartTip
    Router --> HiddenHeart
    Router --> ArtAuction
    Router --> QuietGuide
    Router --> SafePlay
    Router --> Impact
    Router --> Recovery

    TableBooking --> Domain --> Hostess
    EventBooking --> Domain --> Manager
    MenuAssets --> Minio
    ManagerHelp --> Manager
    ChangeCancel --> Domain
    SmartTip --> Domain
    HiddenHeart --> Domain
    ArtAuction --> Domain
    QuietGuide --> Domain
    SafePlay --> Domain
    Impact --> Domain
    Recovery --> Manager

    Gateway --> Redis
    Domain --> Postgres
    Domain --> Redis
    Gateway -. "классификация/текст" .-> LLM
    LLM -. "не владеет бизнес-логикой" .-> Gateway

    TableBooking --> MainMenu
    EventBooking --> MainMenu
    MenuAssets --> MainMenu
    ManagerHelp --> MainMenu
    ChangeCancel --> MainMenu
    SmartTip --> MainMenu
    HiddenHeart --> MainMenu
    ArtAuction --> MainMenu
    QuietGuide --> MainMenu
    SafePlay --> MainMenu
    Impact --> MainMenu
    Recovery --> MainMenu
```

## Дипломный Product Layer

Центральная категория исследования: **потребность быть распознанным без давления**.

FSM переводит эту идею в управляемые сценарии: гость не должен угадывать команду, спорить с ботом или проваливаться в ручную помощь. Система сначала распознает, какая ось боли активна, затем переводит гостя в явный сценарий.

| Ось боли | Capability | FSM-сценарий | Смысл для гостя |
| --- | --- | --- | --- |
| Идентичность | `Memory Engine` | `FirstTouchScenario`, `MainMenuScenario` | Меня узнают и помнят контекст. |
| Персонализация | `Preference Map` | `PreferenceScenario` | Можно предложить "как в прошлый раз". |
| Благодарность | `Smart Tip` | `SmartTipScenario` | Я могу красиво поблагодарить команду. |
| Инфо-поддержка | `Quiet Guide` | `MenuAssetsScenario`, `QuietGuideScenario` | Я получаю меню, афиши и справки без спама. |
| Социальный вклад | `Hidden Heart` | `HiddenHeartScenario`, `ArtAuctionScenario` | Я могу участвовать в донате/благотворительности незаметно и достойно. |
| Игровой опыт | `Safe Play` | `SafePlayScenario`, `ArtAuctionScenario` | Я могу участвовать в интерактиве, но всегда выйти. |
| Управление временем | `Slot Keeper` | `TableBookingScenario`, `EventBookingScenario`, `ChangeCancelScenario` | Мое время и слот не теряются. |
| Безопасность | `Panic Exit` | `RecoveryScenario`, `SafeExit` внутри каждого сценария | Я могу остановить сценарий и вернуться домой. |

Внешние extension points:

| Extension point | Где проявляется в FSM |
| --- | --- |
| `Direct Channel Hub` | будущая прямая связка guest <-> PMS/SBIS без обхода FSM |
| `Arena Reboot Engine` | будущие массовые сценарии "отели <-> стадионы" |
| `Consent Vault` | первое касание, отзыв согласия, экспорт данных |
| `Impact Meter` | отчеты о донатах, культурных KPI, аукционах и социальном вкладе |

## Legacy Product Sources

Legacy не копируется как кодовая архитектура, но сохраняется как карта продуктовых идей.

| Legacy area | Новый слой | FSM-сценарий |
| --- | --- | --- |
| `auction/*` | `capability.hiddenheart` + `capability.impact` + `domain.event` | `ArtAuctionScenario` |
| `charity/*` | `capability.hiddenheart` | `HiddenHeartScenario` |
| `tip/*` | `capability.smarttip` | `SmartTipScenario` |
| `merch/*` | `domain.order` + future commerce boundary | `MerchOrderScenario` позже |
| `poster/*` | `domain.content` + `capability.quietguide` | `QuietGuideScenario` |
| `feedback/*` | `domain.timeline` + future feedback capability | `ManagerHelpScenario` / future `FeedbackScenario` |

## Реестр Сценариев

| Сценарий | Статус | Вход | Выход | Возврат |
| --- | --- | --- | --- | --- |
| `FirstTouchScenario` | работает | `/start`, нет состояния, `CONSENT_REQUIRED` | контакт + consent | `READY_FOR_DIALOG` |
| `MainMenuScenario` | целевой следующий слой | гость уже в системе | выбор сценария | остается `READY_FOR_DIALOG` |
| `TableBookingScenario` | частично работает | "хочу столик", "забронировать стол", "на двоих" | заявка хостес, подтверждение/отказ | `READY_FOR_DIALOG` |
| `EventBookingScenario` | целевой | банкет, день рождения, корпоратив, свадьба, выкуп зала | structured event request для менеджера | `READY_FOR_DIALOG` |
| `MenuAssetsScenario` | целевой | "меню", "карта бара", "а что есть по еде" | отправка релевантных файлов/разделов меню | `READY_FOR_DIALOG` |
| `QuietGuideScenario` | целевой | "афиша", "что сегодня", "что будет на неделе" | контент/афиша/справка без спама | `READY_FOR_DIALOG` |
| `SmartTipScenario` | legacy/product source | "оставить чаевые", "поблагодарить официанта" | сумма + подтверждение + future payment boundary | `READY_FOR_DIALOG` |
| `HiddenHeartScenario` | legacy/product source | "донат", "благотворительность", "поддержать проект" | donation draft + confirmation + manager/payment boundary | `READY_FOR_DIALOG` |
| `ArtAuctionScenario` | legacy/product source | "картина", "аукцион", "ставка", event activation | ставка/победитель/социальный вклад | `READY_FOR_DIALOG` |
| `ImpactMeterScenario` | extension point | "сколько собрали", "итоги", "impact" | культурный KPI / отчетность | `READY_FOR_DIALOG` |
| `SafePlayScenario` | целевой позже | игра, квиз, интерактив, аукцион как игровой слой | мини-сценарий с мгновенным выходом | `READY_FOR_DIALOG` |
| `ManagerHelpScenario` | целевой | "позови менеджера", "хочу человека", жалоба | карточка менеджеру/admin | `READY_FOR_DIALOG` или ожидание ответа |
| `ChangeCancelScenario` | целевой | "изменить бронь", "отмена", "не придем" | изменение/отмена active order | `READY_FOR_DIALOG` |
| `RecoveryScenario` | целевой safety-net | неизвестный intent, конфликт, LLM/интеграция недоступна | короткое уточнение или admin alert | `READY_FOR_DIALOG` |

## Состояния

| State | Русское имя | Роль |
| --- | --- | --- |
| `UNKNOWN` | Нет состояния | Redis не знает гостя. |
| `CONSENT_REQUIRED` | Нужен контакт и согласие | Не продолжаем бизнес-сценарии до контакта. |
| `READY_FOR_DIALOG` | Главное меню / дом сценариев | Базовая точка после первого касания и после завершения сценариев. |
| `AI_FALLBACK` | Последняя страховка | Временное состояние для неизвестного/сломавшегося пути. |
| `TABLE_BOOKING_COLLECT_DATE` | Нужна дата | Сбор даты посадки. |
| `TABLE_BOOKING_COLLECT_TIME` | Нужно время | Сбор времени посадки. |
| `TABLE_BOOKING_COLLECT_PARTY_SIZE` | Нужно число гостей | Сбор party size. |
| `TABLE_BOOKING_WAIT_TABLE_SELECTION` | Ждем стол | План отправлен, ждем стол/зону/автовыбор. |
| `TABLE_BOOKING_WAIT_HOSTESS_CONFIRMATION` | Ждем хостес | Заявка и hold созданы, хостес решает кнопками. |
| `TABLE_BOOKING_CONFIRMED` | Подтверждено | Гостю отправлен order, возврат домой. |
| `TABLE_BOOKING_REJECTED` | Отклонено | Предложить другой вариант, затем домой или change. |
| `TABLE_BOOKING_CHANGE_REQUESTED` | Изменение | Пересобрать draft и пройти подтверждение заново. |
| `TABLE_BOOKING_CANCELLED` | Отмена | Освободить holds, вернуться домой. |
| `TIP_COLLECT_AMOUNT` | Сумма чаевых | Smart Tip собирает сумму. |
| `TIP_CONFIRMATION` | Подтверждение чаевых | Гость подтверждает благодарность до payment boundary. |
| `DONATION_COLLECT_AMOUNT` | Сумма доната | Hidden Heart собирает сумму. |
| `DONATION_CONFIRMATION` | Подтверждение доната | Гость подтверждает социальный вклад. |
| `AUCTION_RUNNING` | Аукцион идет | Принимаются ставки по правилам event/auction. |
| `AUCTION_WAIT_BID` | Ждем ставку | Гость вводит сумму или нажимает кнопку. |
| `SAFE_EXIT` | Безопасный выход | Любой сценарий может вернуться домой без давления. |

Старые Redis aliases:

| Старое | Каноническое |
| --- | --- |
| `GREETING` | `CONSENT_REQUIRED` |
| `CONTACT` | `CONSENT_REQUIRED` |
| `MENU` | `READY_FOR_DIALOG` |

## Правила Разговора

Astor должен говорить как спокойный цифровой дворецкий AERIS:

- коротко;
- уверенно;
- не обещать то, что не подтверждено доменным слоем;
- спрашивать один недостающий параметр за раз;
- не спорить с гостем;
- не раскрывать технические детали;
- подтверждать понимание только после структурирования данных;
- для неопределенности использовать уточнение, а не длинную лекцию.

### Базовые тексты

Первое касание:

```text
Нажимая кнопку "Согласиться и поделиться контактом", вы соглашаетесь с политикой обработки персональных данных.
```

Главное меню после контакта:

```text
Спасибо, я на связи. Что сделаем?

• Забронировать стол
• Посмотреть меню
• Позвать менеджера
• Организовать мероприятие
• Изменить или отменить бронь
• Афиша и события
• Чаевые / благодарность
• Благотворительность / аукцион
```

Непонятный intent в Main Menu:

```text
Я пока не уверен, какой сценарий нужен. Выберите, пожалуйста: стол, меню, менеджер или мероприятие.
```

Fallback при технической проблеме:

```text
Я не смог уверенно продолжить сценарий. Передам это администратору, а вы можете написать проще: стол, меню, менеджер или мероприятие.
```

## FirstTouchScenario

```mermaid
flowchart TD
    Unknown["UNKNOWN"] --> Start{"/start или первое касание?"}
    Start -- "да" --> Consent["CONSENT_REQUIRED<br/>попросить contact + consent"]
    Consent -- "текст без контакта" --> Nudge["Коротко вернуть к кнопке контакта"]
    Nudge --> Consent
    Consent -- "Telegram contact" --> Save["Сохранить contact + consent evidence"]
    Save --> Ready["READY_FOR_DIALOG<br/>MainMenuScenario"]
```

Правила:

- не запускать бронь до контакта;
- не собирать дату/стол/меню до consent;
- контакт дает право перейти в `READY_FOR_DIALOG`.

## MainMenuScenario

```mermaid
flowchart TD
    Ready["READY_FOR_DIALOG"] --> Render["Показать / держать главное меню"]
    Render --> Intent{"Intent гостя"}
    Intent -- "стол / посадка / на двоих" --> Table["TableBookingScenario"]
    Intent -- "банкет / корпоратив / день рождения" --> Event["EventBookingScenario"]
    Intent -- "меню / бар / еда / напитки" --> Menu["MenuAssetsScenario"]
    Intent -- "афиша / что сегодня / расписание" --> Guide["QuietGuideScenario"]
    Intent -- "чаевые / поблагодарить" --> Tip["SmartTipScenario"]
    Intent -- "донат / благотворительность" --> Donation["HiddenHeartScenario"]
    Intent -- "картина / аукцион / ставка" --> Auction["ArtAuctionScenario"]
    Intent -- "менеджер / человек / жалоба" --> Manager["ManagerHelpScenario"]
    Intent -- "отмени / измени / перенеси" --> Change["ChangeCancelScenario"]
    Intent -- "непонятно" --> Clarify["Одно уточнение + quick buttons"]

    Table --> Ready
    Event --> Ready
    Menu --> Ready
    Guide --> Ready
    Tip --> Ready
    Donation --> Ready
    Auction --> Ready
    Manager --> Ready
    Change --> Ready
    Clarify --> Ready
```

Правила:

- `READY_FOR_DIALOG` не должен сразу уходить в `AI_FALLBACK`;
- первым делом пробуем явные сценарии;
- если intent слабый, показываем варианты;
- fallback только после повторной неясности или технического сбоя.

## TableBookingScenario

```mermaid
flowchart TD
    Start["Intent: бронь стола"] --> Date{"Есть дата?"}
    Date -- "нет" --> AskDate["Спросить дату"]
    Date -- "да" --> Time{"Есть время?"}
    AskDate --> Time
    Time -- "нет" --> AskTime["Спросить время"]
    Time -- "да" --> Party{"Есть число гостей?"}
    AskTime --> Party
    Party -- "нет" --> AskParty["Спросить гостей"]
    Party -- "да" --> Draft["Сохранить draft в Redis"]
    AskParty --> Draft
    Draft --> Plan["Отправить AERIS PLAN.pdf"]
    Plan --> Select["Ждать стол / зону / выбери сам"]
    Select -- "явный выбор" --> Order["Создать order + HELD hold"]
    Select -- "неясно / повтор intent" --> Plan
    Order --> Hostess["Карточка хостес Да / Нет"]
    Hostess -- "Да" --> Confirm["CONFIRMED + гостю order"]
    Hostess -- "Нет" --> Reject["REJECTED + предложить другой вариант"]
    Confirm --> Home["READY_FOR_DIALOG"]
    Reject --> Home
```

Правила:

- перед выбором стола всегда отправлять план;
- `20:00` не считается номером стола;
- "17", "стол 17", "vip", "бар", "выбери сам" считаются выбором;
- хостес подтверждает только кнопками;
- после `Да`/`Нет` гость получает человекочитаемый результат.

## EventBookingScenario

Целевой сценарий для мероприятий: день рождения, банкет, корпоратив, свадьба, презентация, выкуп зала.

```mermaid
flowchart TD
    Start["Intent: мероприятие"] --> Type["Тип события"]
    Type --> Date["Дата / диапазон"]
    Date --> Time["Время / длительность"]
    Time --> Guests["Количество гостей"]
    Guests --> Format["Формат: банкет / фуршет / смешанный"]
    Format --> Budget["Бюджет / депозит"]
    Budget --> Menu["Меню / напитки / ограничения"]
    Menu --> Tech["Техника / сцена / DJ / подрядчики"]
    Tech --> Contact["Контакт / комментарии"]
    Contact --> Summary["Сводка заявки"]
    Summary --> Confirm{"Гость подтверждает?"}
    Confirm -- "да" --> Manager["Передать менеджеру"]
    Confirm -- "нет / поправки" --> Type
    Manager --> Home["READY_FOR_DIALOG"]
```

Правила:

- не смешивать с обычной посадкой за стол;
- если запрос больше регулярной посадки, route в `EVENT_BOOKING`;
- итог - structured request для менеджера, не автоматическое подтверждение.

## MenuAssetsScenario

Целевой сценарий для меню, карты бара, банкетных документов и медиа.

```mermaid
flowchart TD
    Start["Intent: меню / бар / еда"] --> Which{"Что нужно?"}
    Which -- "основное меню" --> MainMenuDoc["Отправить актуальное меню"]
    Which -- "бар / коктейли" --> Bar["Отправить карту бара"]
    Which -- "банкет / фуршет" --> Banquet["Отправить банкетные материалы"]
    Which -- "непонятно" --> Clarify["Уточнить: еда, бар или банкет"]
    MainMenuDoc --> Home["READY_FOR_DIALOG"]
    Bar --> Home
    Banquet --> Home
    Clarify --> Which
```

Правила:

- не отправлять все файлы сразу;
- выбирать релевантный asset;
- после отправки предложить бронь стола или менеджера.

## QuietGuideScenario

Сценарий инфо-поддержки: афиши, расписание, правила, справки, доступные события. Это не маркетинговая рассылка, а ответ на запрос гостя.

```mermaid
flowchart TD
    Start["Intent: афиша / что сегодня / справка"] --> Type{"Что ищем?"}
    Type -- "афиша / события" --> Poster["Найти актуальные poster/content items"]
    Type -- "правила / справка" --> Help["Найти короткую справку"]
    Type -- "непонятно" --> Clarify["Уточнить: афиша, меню, бронь или менеджер"]
    Poster --> Reply["Короткий ответ + релевантные материалы"]
    Help --> Reply
    Reply --> Offer["Предложить следующий шаг без давления"]
    Offer --> Home["READY_FOR_DIALOG"]
    Clarify --> Home
```

Правила:

- не пушить афишу без запроса;
- максимум один релевантный набор материалов за раз;
- если гость хочет попасть на событие, route в `EventBookingScenario` или `TableBookingScenario`.

## SmartTipScenario

Сценарий благодарности из Legacy `tip/*`: гость хочет оставить чаевые или красиво поблагодарить команду.

```mermaid
flowchart TD
    Start["Intent: чаевые / благодарность"] --> Target{"Кому благодарность?"}
    Target -- "известно" --> Amount{"Есть сумма?"}
    Target -- "неизвестно" --> AskTarget["Уточнить: команда / официант / бар"]
    AskTarget --> Amount
    Amount -- "нет" --> AskAmount["Спросить сумму"]
    Amount -- "да" --> Summary["Сводка благодарности"]
    AskAmount --> Summary
    Summary --> Confirm{"Гость подтверждает?"}
    Confirm -- "да" --> Boundary["Создать tip intent<br/>future payment boundary"]
    Confirm -- "нет" --> Cancel["Отменить без давления"]
    Boundary --> Thanks["Поблагодарить гостя<br/>и записать timeline"]
    Thanks --> Home["READY_FOR_DIALOG"]
    Cancel --> Home
```

Правила:

- без payment integration сценарий создает только намерение/заявку, не проводит оплату;
- гость всегда видит сумму и адресата до подтверждения;
- чаевые не смешиваются с бронью, но могут быть предложены после завершенного события.

## HiddenHeartScenario

Сценарий социального вклада из Legacy `charity/*`: анонимный донат, поддержка проекта, благотворительность.

```mermaid
flowchart TD
    Start["Intent: донат / благотворительность"] --> Cause{"Понятна цель?"}
    Cause -- "нет" --> ShowCauses["Показать доступные инициативы"]
    Cause -- "да" --> Amount{"Есть сумма?"}
    ShowCauses --> Amount
    Amount -- "нет" --> AskAmount["Спросить сумму"]
    Amount -- "да" --> Privacy{"Анонимно?"}
    AskAmount --> Privacy
    Privacy -- "да / по умолчанию" --> Anonymous["Пометить anonymous"]
    Privacy -- "нет" --> Named["Пометить named"]
    Anonymous --> Summary["Сводка donation intent"]
    Named --> Summary
    Summary --> Confirm{"Гость подтверждает?"}
    Confirm -- "да" --> Create["Создать donation draft/order<br/>future payment boundary"]
    Confirm -- "нет" --> Cancel["Отменить без давления"]
    Create --> Impact["Записать событие для Impact Meter"]
    Impact --> Home["READY_FOR_DIALOG"]
    Cancel --> Home
```

Правила:

- по умолчанию донат анонимный;
- не использовать давление, рейтинги вины или публичное сравнение гостей;
- `Impact Meter` получает агрегированные факты, а не лишние персональные данные.

## ArtAuctionScenario

Legacy `auction/*` становится event-сценарием для благотворительной продажи картин / аукциона на мероприятии. Это стык `Hidden Heart`, `Safe Play`, `Impact Meter` и `EventBooking`.

```mermaid
flowchart TD
    Start["Intent: картина / аукцион / ставка"] --> Active{"Есть активный аукцион?"}
    Active -- "нет" --> Info["Показать ближайший/планируемый аукцион<br/>или предложить менеджера"]
    Active -- "да" --> Lot["Показать лот<br/>картина / описание / текущая ставка"]
    Lot --> Bid{"Гость делает ставку?"}
    Bid -- "нет" --> Watch["Остаться наблюдателем<br/>без давления"]
    Bid -- "да" --> Validate["Проверить шаг ставки<br/>>= current + minStep"]
    Validate -- "ошибка" --> Explain["Объяснить минимальную ставку"]
    Validate -- "ok" --> Confirm["Подтвердить ставку"]
    Confirm -- "да" --> Place["Принять bid<br/>обновить лидера"]
    Confirm -- "нет" --> Watch
    Place --> Notify["Уведомить event/hostess/manager"]
    Notify --> Impact["Записать impact event"]
    Impact --> Home["READY_FOR_DIALOG"]
    Info --> Home
    Watch --> Home
    Explain --> Lot
```

Правила:

- аукцион не запускается случайной фразой гостя, нужен активный event/lot;
- ставка требует явного подтверждения;
- гость может выйти командой safe exit на любом шаге;
- победитель/финал подтверждаются менеджером или event owner, не LLM.

## ImpactMeterScenario

Extension point для культурных KPI: сколько собрано, какие инициативы поддержаны, какие события дали вклад.

```mermaid
flowchart TD
    Start["Intent: impact / итоги / сколько собрали"] --> Scope{"Какой scope?"}
    Scope -- "событие" --> EventStats["Агрегаты события"]
    Scope -- "инициатива" --> CauseStats["Агрегаты инициативы"]
    Scope -- "общий" --> VenueStats["Агрегаты площадки"]
    EventStats --> Reply["Человекочитаемый отчет"]
    CauseStats --> Reply
    VenueStats --> Reply
    Reply --> Home["READY_FOR_DIALOG"]
```

Правила:

- показывать агрегаты, а не персональные платежные данные;
- использовать данные из donation/auction/tip/timeline events;
- в MVP можно оставить как boundary и тестовый отчет.

## ManagerHelpScenario

Целевой сценарий для ручного подключения команды.

```mermaid
flowchart TD
    Start["Intent: менеджер / человек / жалоба"] --> Reason["Коротко определить причину"]
    Reason --> Card["Собрать карточку для admin/manager"]
    Card --> Notify["Отправить в admin/manager chat"]
    Notify --> Guest["Гостю: команда увидит запрос"]
    Guest --> Home["READY_FOR_DIALOG"]
```

Правила:

- не скрывать ручную передачу;
- приложить chatId, имя, username, последнее сообщение, состояние FSM;
- не запускать этот сценарий из admin chat как гостевой.

## ChangeCancelScenario

Целевой сценарий для изменения или отмены существующей заявки.

```mermaid
flowchart TD
    Start["Intent: изменить / отменить"] --> Lookup{"Есть активная бронь?"}
    Lookup -- "нет" --> NoOrder["Сказать, что активной брони не видно"]
    Lookup -- "да" --> Action{"Что сделать?"}
    Action -- "отмена" --> Cancel["CANCELLED + release holds"]
    Action -- "перенос времени/стола/гостей" --> Change["Обновить draft"]
    Change --> Recheck["Проверить доступность заново"]
    Recheck --> Hostess["Новое подтверждение хостес"]
    Cancel --> Home["READY_FOR_DIALOG"]
    NoOrder --> Home
    Hostess --> Home
```

Правила:

- изменение после hold требует нового подтверждения;
- отмена освобождает holds;
- гостю всегда сообщается итог.

## RecoveryScenario

```mermaid
flowchart TD
    Start["Неизвестный intent / ошибка / LLM timeout"] --> Count{"Повторная неясность?"}
    Count -- "первый раз" --> Clarify["Показать варианты: стол / меню / менеджер / мероприятие"]
    Count -- "повтор / ошибка" --> Alert["Admin alert + безопасный ответ"]
    Clarify --> Home["READY_FOR_DIALOG"]
    Alert --> Home
```

Правила:

- сначала уточнять;
- затем эскалировать;
- не оставлять гостя в техническом тупике;
- логировать причину и correlation id.

## LLM Contract

LLM может:

- классифицировать intent;
- извлекать дату, время, гостей, стол, зону, тип события, сумму, адресата благодарности, цель доната, номер/название лота;
- переформулировать ответ в тоне Astor;
- предложить одно уточнение.

LLM не может:

- подтверждать бронь;
- создавать order/hold;
- обещать свободный стол;
- менять статус заявки;
- подтверждать от имени хостес;
- принимать ставки без явного подтверждения гостя;
- раскрывать приватные данные донатов и платежей;
- игнорировать FSM state.

Минимальный формат результата intent adapter в будущем:

```json
{
  "intent": "TABLE_BOOKING",
  "confidence": 0.91,
  "entities": {
    "date": "tomorrow",
    "time": "20:00",
    "partySize": 2,
    "tablePreference": null
  },
  "needsClarification": false,
  "clarificationQuestion": null
}
```

## Предкодовый Тест-План

Перед изменением кода проверяем сценарии на бумаге и потом покрываем тестами.

### First Touch

| Given | When | Then |
| --- | --- | --- |
| `UNKNOWN` | `/start` | `CONSENT_REQUIRED`, кнопка контакта |
| `CONSENT_REQUIRED` | текст без контакта | остается `CONSENT_REQUIRED`, короткий nudge |
| `CONSENT_REQUIRED` | contact | consent saved, `READY_FOR_DIALOG` |

### Main Menu

| Given | When | Then |
| --- | --- | --- |
| `READY_FOR_DIALOG` | "хочу столик завтра" | route `TABLE_BOOKING` |
| `READY_FOR_DIALOG` | "скинь меню" | route `MENU_ASSETS` |
| `READY_FOR_DIALOG` | "позови менеджера" | route `MANAGER_HELP` |
| `READY_FOR_DIALOG` | "день рождения на 30 человек" | route `EVENT_BOOKING` |
| `READY_FOR_DIALOG` | "что сегодня по афише" | route `QUIET_GUIDE` |
| `READY_FOR_DIALOG` | "хочу оставить чаевые" | route `SMART_TIP` |
| `READY_FOR_DIALOG` | "хочу поддержать благотворительный проект" | route `HIDDEN_HEART` |
| `READY_FOR_DIALOG` | "хочу поставить на картину" | route `ART_AUCTION`, если есть активный аукцион |
| `READY_FOR_DIALOG` | непонятный текст | показать варианты, не сразу admin alert |

### Table Booking

| Given | When | Then |
| --- | --- | --- |
| `READY_FOR_DIALOG` | "столик завтра в 20:00 на двоих" | send plan, wait table |
| `TABLE_BOOKING_WAIT_TABLE_SELECTION` | "17" | create order/hold, send hostess card |
| `TABLE_BOOKING_WAIT_TABLE_SELECTION` | повтор booking intent | resend plan, не создавать order |
| `TABLE_BOOKING_WAIT_HOSTESS_CONFIRMATION` | hostess `Да` | confirm order/hold, guest order, return ready |
| `TABLE_BOOKING_WAIT_HOSTESS_CONFIRMATION` | hostess `Нет` | reject/release, polite guest refusal |

### Diploma Capability Scenarios

| Given | When | Then |
| --- | --- | --- |
| `READY_FOR_DIALOG` | "хочу оставить чаевые 1000" | route `SMART_TIP`, show confirmation before payment boundary |
| `READY_FOR_DIALOG` | "донат 5000 анонимно" | route `HIDDEN_HEART`, anonymous donation draft |
| `READY_FOR_DIALOG` | "какие итоги благотворительности?" | route `IMPACT_METER`, aggregated report only |
| `READY_FOR_DIALOG` | "ставлю 20000 за картину" + active auction | route `ART_AUCTION`, validate min step, ask explicit confirmation |
| `AUCTION_WAIT_BID` | bid below min step | explain min step, keep auction state |
| any active scenario | "стоп", "назад", "отмена" | `SAFE_EXIT`, return `READY_FOR_DIALOG` |

### Observability

| Given | When | Then |
| --- | --- | --- |
| любое guest сообщение | accepted | outbox/Kafka event |
| admin chat message | incoming | stored + projection, guest FSM skipped |
| fallback | technical/unknown | admin alert with state/correlation |

## Кодовые Шаги После Утверждения Карты

1. Добавить `MainMenuScenario`.
2. Вынести intent routing из `MessageGatewayService` в явный scenario router.
3. Обновить `MessageGatewayService`: first touch -> active scenario -> main menu -> recovery.
4. Добавить тесты Main Menu routing.
5. Добавить будущие enum states только там, где нужен runtime state, не плодить состояния для простых одношаговых ответов.
6. Обновить LLM prompt contract под сценарный router.
7. Прогнать ручной сценарий в Telegram.
