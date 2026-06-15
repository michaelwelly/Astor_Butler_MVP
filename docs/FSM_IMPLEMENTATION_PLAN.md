# FSM Implementation Plan

Дата фиксации: 2026-06-12

Документ фиксирует семь опорных решений перед массовым написанием FSM-сценариев Astor Butler. Цель - убрать архитектурную неопределенность: дальше сценарии можно писать пачками, а спорные места выносить отдельно.

## 0. Базовое правило

FSM является single source of truth.

LLM, pgvector, Neo4j, Scylla, Redis и Telegram не принимают бизнес-решения сами:

- LLM помогает понять intent/entities/stage и сформулировать текст;
- pgvector достает похожий смысл и approved context;
- Neo4j помогает видеть граф сценариев и impact analysis;
- Scylla хранит durable timeline;
- Redis хранит hot runtime state/drafts/retries/idempotency;
- PostgreSQL хранит transactional facts;
- Telegram остается transport/UI.

## 1. Priority Order

Порядок входа в FSM фиксируется так:

1. `SERVICE_CHAT_CHECK` / service chat guard for admin, analytics, hostess and system chats.
2. Transport normalization: text, voice transcript, metadata, STT retry policy.
3. `/start` / `FirstTouchScenario`: safe restart, preview, consent/contact routing.
4. Active scenario continuation: если гость уже в runtime state, этот сценарий имеет приоритет.
5. `CompositeIntentPlan`: только из `READY_FOR_DIALOG` или `AI_FALLBACK`.
6. High-priority side-effect scenarios:
   - `TableBookingScenario`;
   - `ChangeCancelScenario`;
   - `ManagerHelpScenario`;
   - future `EventBookingScenario`.
7. Safe content scenarios:
   - `MenuAssetsScenario`;
   - `QuietGuideScenario`;
   - `ImpactMeterScenario`.
8. Money/confirmation scenarios:
   - `SmartTipScenario`;
   - `HiddenHeartScenario`;
   - `ArtAuctionScenario`.
9. `MainMenuScenario`.
10. `RecoveryScenario`.
11. `AI_FALLBACK` only after recovery rules, not as normal routing.

Правила:

- Active state важнее нового intent. Если гость в выборе стола и просит меню, текущее состояние не ломаем, а новый intent кладем в pending.
- `/start` всегда имеет право безопасно сбросить runtime state/drafts, но durable facts не удаляет.
- Service chats не запускают guest FSM. Текущий локальный `TELEGRAM_SYSTEM_CHAT_ID=-5403153261`.
- Telegram message cleanup через `DeleteMessage` временно отключен полностью: UX-чистка будет проектироваться отдельной session policy поверх Redis/durable timeline.

## 2. Scenario Contract

Каждый сценарий должен соответствовать единому контракту.

Целевой Java-контракт:

```java
public interface FsmScenario {
    String id();
    int priority();
    boolean supports(IncomingMessage incoming, BotState state, String text);
    OutgoingMessage handle(IncomingMessage incoming, BotState state, String text);
    boolean owns(BotState state);
    boolean sideEffecting();
    boolean canRunInParallel();
}
```

Минимальные требования к сценарию:

| Поле | Решение |
| --- | --- |
| `id` | Стабильный код сценария: `TABLE_BOOKING`, `MENU_ASSETS`. |
| `priority` | Используется router-ом, чтобы сценарии не перехватывали друг друга случайно. |
| `supports` | Только легкое распознавание входа. Не меняет состояние и не пишет draft. |
| `handle` | Единственное место сценария, где можно менять FSM state/draft. |
| `owns` | Определяет active runtime states сценария. |
| `sideEffecting` | `true`, если сценарий создает/меняет бронь, деньги, ставку, отмену, менеджерскую карточку. |
| `canRunInParallel` | `true` только для safe content сценариев. |

Статус реализации:

- 2026-06-15: введен первый runtime-контракт `FsmScenario`.
- 2026-06-15: `FirstTouchScenario`, `TableBookingScenario`, `MenuAssetsScenario`, `QuietGuideScenario` и `MainMenuScenario` подключены к контракту напрямую как Spring components.
- 2026-06-15: введен `ScenarioRouter`, который забрал из `MessageGatewayService` выбор сценария и текущую composite-intent логику.
- 2026-06-15: добавлен первый новый explicit-сценарий по контракту - `ManagerHelpScenario`.
- 2026-06-15: добавлен `RecoveryScenario` как последний router-сценарий перед LLM/fallback: первый непонятный текст уточняет intent, повторный отправляет admin alert.
- 2026-06-15: добавлен `ChangeCancelScenario`: безопасный сбор reference для изменения/отмены брони и admin handoff без автоматического изменения holds/orders.
- 2026-06-15: добавлен первый слой `EventBookingScenario`: сбор структурированной заявки на мероприятие и admin handoff без автоподтверждения.
- 2026-06-15: добавлен `SmartTipScenario`: явный FSM-контур чаевых до будущей СБП-интеграции.
- 2026-06-15: добавлен `HiddenHeartScenario`: явный FSM-контур анонимного donation draft до будущей СБП-интеграции и Impact Meter.
- 2026-06-15: добавлен `ArtAuctionScenario`: явный FSM-контур ставки, где активный лот/минимальный шаг/финальное принятие требуют проверки, а LLM не принимает ставку сам.
- 2026-06-15: добавлен `ImpactMeterScenario`: read-only FSM-контур агрегированных итогов без приватных платежных данных.
- 2026-06-15: добавлен `FeedbackScenario`: explicit-сценарий отзыва, который собирает текст и отправляет admin handoff без случайного fallback.
- 2026-06-15: добавлен `MerchScenario`: explicit-сценарий merch request/order draft с manual confirmation boundary.
- 2026-06-15: добавлен `SafePlayScenario`: безопасный сабражный ритуал с team confirmation и запретом dangerous how-to.
- 2026-06-15: `MainMenuScenario` очищен от product branches. Теперь он отвечает только за явный показ главного меню и safe exit из активного сценария; чаевые, донаты, аукцион, мероприятия, мерч, отзывы, Quiet Guide и ручная помощь живут в отдельных `FsmScenario`.
- 2026-06-15: sequential composite-intent начал сохранять deferred safe-intents в Redis через `FSMStorage.setPendingIntents`, а не только в metadata ответа.
- 2026-06-15: pending intent хранит код и точную prompt-подсказку (`MENU_ASSETS::покажи винную карту`), чтобы при resume не отправлять гостю лишние материалы.
- 2026-06-15: `ScenarioRouter` умеет исполнять сохраненный safe-content pending intent после успешного completion-action и очищать Redis pending key.
- 2026-06-15: подтверждение брони через кнопку хостес теперь тоже доставляет сохраненные safe-content pending intents гостю (`MENU_ASSETS`, `QUIET_GUIDE`) и очищает Redis pending key после успешной отправки.
- 2026-06-15: `AERIS Channel Ingest` получил MVP runtime: public `t.me/s/aeris_gastrobar` parser, rules classifier, MinIO media mirroring, PostgreSQL `venue_content_posts/assets`, manual endpoint `/api/content/ingest/aeris-channel`, scheduled toggle и read path в `QuietGuideScenario`.
- 2026-06-15: начат DB-first vertical slice для `FirstTouch + MainMenu`: добавлен Telegram runtime read/reset API поверх PostgreSQL identity/consent/message facts, Redis FSM state/pending intents и Redis table-booking draft.
- Следующий шаг: добавлять новые сценарии как отдельные `FsmScenario`-компоненты, а не расширять `MessageGatewayService`.

## 3. IntentPlan Contract

Система должна постепенно перейти от `first supports wins` к плану намерений.

Минимальная структура:

```json
{
  "planType": "SINGLE | PARALLEL_CONTENT | SEQUENTIAL | ASK_CONFIRMATION | RECOVERY",
  "currentState": "READY_FOR_DIALOG",
  "intents": [
    {
      "intent": "TABLE_BOOKING",
      "confidence": 0.92,
      "entities": {
        "date": "2026-06-13",
        "time": "20:00",
        "partySize": 2
      },
      "sideEffecting": true,
      "canRunInParallel": false
    }
  ],
  "primaryIntent": "TABLE_BOOKING",
  "pendingIntents": ["MENU_ASSETS"],
  "reason": "booking has side effects; menu can be sent after booking step"
}
```

Правила:

- `SINGLE`: один intent, обычный сценарий.
- `PARALLEL_CONTENT`: несколько safe content intents можно выполнить вместе.
- `SEQUENTIAL`: primary intent выполняется первым, остальные фиксируются как pending.
- `ASK_CONFIRMATION`: деньги, отмена, ставка, подтверждение брони.
- `RECOVERY`: intent слабый или конфликтует со state/draft.

Источники распознавания:

1. rules/keywords;
2. active state;
3. pgvector top-K examples/context;
4. LLM intent adapter;
5. final FSM validation.

## 4. Target FSM States

Перед реализацией каждого сценария state должен быть описан как строка в таблице.

Обязательные поля:

| Поле | Смысл |
| --- | --- |
| `state` | `BotState` enum value. |
| `owner` | Сценарий-владелец. |
| `waitsFor` | Что ожидаем от гостя. |
| `allowedTransitions` | Куда можно перейти. |
| `draftStorage` | Где хранится промежуточный draft. |
| `sideEffects` | Какие внешние изменения разрешены в этом state. |
| `recovery` | Что делаем при неясном/ошибочном ответе. |

Решение по основным группам:

| Сценарий | State policy |
| --- | --- |
| `FirstTouchScenario` | Минимальные states: `UNKNOWN`, `CONSENT_REQUIRED`, `READY_FOR_DIALOG`. |
| `MainMenuScenario` | Не владеет product subflows. Только явное главное меню и safe exit из активного сценария. |
| `TableBookingScenario` | Явные states для date/time/party/table/hostess confirmation. |
| `MenuAssetsScenario` | `MENU_ASSETS_CLARIFY`, `MENU_ASSETS_DELIVERED`; без long draft. |
| `QuietGuideScenario` | `QUIET_GUIDE_CLARIFY`, `QUIET_GUIDE_DELIVERED`; content-only. |
| `EventBookingScenario` | Первый слой: `EVENT_BOOKING_COLLECT_DETAILS`, `EVENT_BOOKING_SENT`; structured request для менеджера. Следующий слой: отдельные states на type/date/guests/budget/format/menu/tech/contact/confirm. |
| `ChangeCancelScenario` | Первый слой: сбор даты/времени/номера заявки и admin handoff. Следующий слой: поиск active order, explicit confirmation, release/update holds. |
| `ManagerHelpScenario` | `MANAGER_HELP_COLLECT_REASON`, `MANAGER_HELP_SENT`; причина обращения, admin handoff, возврат в main menu. |
| `FeedbackScenario` | `FEEDBACK_COLLECT_TEXT`, `FEEDBACK_SENT`; отзыв/обратная связь, admin handoff, возврат в main menu. |
| `MerchScenario` | `MERCH_COLLECT_REQUEST`, `MERCH_SENT`; запрос по мерчу/сабражной цепи, admin handoff, manual confirmation. |
| `SafePlayScenario` | `SAFE_PLAY_COLLECT_DETAILS`, `SAFE_PLAY_WAIT_TEAM_CONFIRMATION`; сабражный ритуал, safety guard, team confirmation, optional merch link. |
| `SmartTipScenario` | Первый слой: amount -> confirmation -> draft confirmed/cancelled. Следующий слой: staff target, SBP link, receipt/audit. |
| `HiddenHeartScenario` | Amount, anonymity/cause, confirmation, future payment boundary. |
| `ArtAuctionScenario` | Active lot, bid amount, explicit confirmation, manager/event owner validation. |
| `RecoveryScenario` | Redis retry counter, first unclear input -> options, repeated unclear input -> admin handoff, return to `READY_FOR_DIALOG`. |

## 5. Draft Storage Policy

Хранилища распределяются по роли.

| Layer | Что хранит |
| --- | --- |
| Redis | current FSM state, active draft, pending intents, STT retry counters, idempotency locks, short cache. Pending intents хранятся рядом с state: `astor:fsm:telegram:{chatId}:pending-intents`. |
| PostgreSQL | users, consents, media catalog, booking orders, table holds, confirmed business facts. |
| Scylla | append-only FSM timeline: message, previous/next state, intent/actions, metadata, occurredAt. |
| MinIO/S3 | PDF, video, large binary assets. |
| MongoDB | flexible metadata, parsed documents, inventory/manifests where schema еще меняется. |
| pgvector/PostgreSQL | semantic sources/chunks/embeddings for RAG and intent examples. |
| Neo4j | scenario/capability/state graph projection, not source of truth. |

First live API slice:

| Endpoint | Storage touched | Purpose |
| --- | --- | --- |
| `GET /api/fsm/telegram/{chatId}/state` | PostgreSQL + Redis | Manual/runtime visibility for Natalia and service checks. |
| `POST /api/fsm/telegram/{chatId}/reset` | Redis + PostgreSQL read | Safe `/start`-like reset without deleting durable identity facts. |
| `PUT /api/fsm/telegram/{chatId}/state` | Redis | Internal recovery while scenarios are being filled. |
| `DELETE /api/fsm/telegram/{chatId}/state` | Redis | Clear hot state/drafts without touching PostgreSQL history. |

Правила:

- Redis draft может быть потерян без разрушения durable facts.
- Любой side effect должен иметь durable record в PostgreSQL или явный outbox/timeline event.
- Pending intents живут в Redis, а факт их появления пишется в Scylla timeline/Kafka.
- Large media никогда не кладем в jar/git.

## 6. Recovery Policy

Recovery нужен, чтобы фоллбэков становилось меньше, а не чтобы скрывать плохой routing.

Порядок:

1. First unclear input: one useful clarification.
2. Second unclear input in same state: offer concrete buttons/options and keep state.
3. Third unclear input or technical failure: `RecoveryScenario`.
4. If recovery cannot continue: admin alert + polite guest message.
5. After admin handoff: state returns to `READY_FOR_DIALOG` or remains in active state only if draft is still valid.

Гость не должен видеть:

- stacktrace;
- class names типа `ResourceAccessException`;
- STT engine warnings;
- Kafka offsets;
- internal state dump.

Admin должен видеть:

- guest identity;
- original text/transcript;
- previous/next state;
- intent/confidence if known;
- correlation id;
- failure reason class;
- suggested next action.

## 7. Manual Test Scenarios

Каждый новый FSM block считается готовым только после ручного сценария и focused tests.

### Natalia baseline

Тестовый guest:

```text
telegram_id/chat_id = 1773317437
username = Poedinenko
```

Перед тестом можно сбросить Наталью через:

```bash
scripts/reset_natalia_test_user.sh
```

### Smoke checklist

| Flow | Проверка |
| --- | --- |
| `/start` known guest | persistent preview pinned/refreshed, state `READY_FOR_DIALOG`, no duplicate plain message. |
| First touch unknown guest | contact/consent flow, profile persisted, admin event visible. |
| Table booking | date/time/party/table, hall plan, hostess card, approve/reject path. |
| Menu assets | kitchen/bar/elements/wine/all, documents sent from MinIO/catalog. |
| Quiet guide | interior video as document, concept copy, poster/help branch. |
| Composite content | "винная карта и видео-тур" sends both. |
| Composite side effect | "стол и винная карта" starts booking and keeps menu as pending. |
| Voice input | successful transcript routes same as text; two failed STT attempts ask for text. |
| Recovery | unclear text clarifies before admin fallback. |
| Admin observability | every guest step creates human-readable admin projection while feature flag enabled. |
| System chat guard | message from `TELEGRAM_SYSTEM_CHAT_ID=-5403153261` is stored/audited, but guest FSM scenarios and LLM are skipped. |
| Telegram cleanup disabled | bot does not delete guest or previous bot messages; preview stays pinned, chat history remains visible for manual testing. |

## 8. Implementation Order

Recommended sequence:

1. Introduce `FsmScenario` contract and `ScenarioRouter`.
2. Move current ordered routing from `MessageGatewayService` into router.
3. Promote current composite logic into `IntentPlanner`.
4. Add `PendingIntentStorage` over Redis.
5. Implement `RecoveryScenario`.
6. Deepen `MenuAssetsScenario` and `QuietGuideScenario` with pgvector context.
7. Implement `ManagerHelpScenario` and `ChangeCancelScenario`.
8. Implement `EventBookingScenario` only after enough Яна/material inputs, but scaffolding states can be added earlier.
9. Add scenario graph projection updates to Neo4j seeder.
10. Expand manual and unit tests per scenario.

## 9. Open Decisions

Нет блокирующих решений перед началом инфраструктурного FSM-кода.

Открытые, но неблокирующие:

| Вопрос | Решение на сейчас |
| --- | --- |
| Реальные event/banquet документы | Нужны для глубокого `EventBookingScenario`; не блокируют router/contracts/recovery/menu/quiet-guide. |
| Полный LLM intent adapter | Начать с rules + pgvector contract; LLM подключать после стабильного router. |
| Payment boundary | Чаевые/донаты/ставки пока draft/confirmation only, без реального платежа. |
| Production auth/admin UI | Не блокирует Telegram/FSM MVP. |
