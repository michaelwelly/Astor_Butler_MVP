┌──────────────────────────────────────────────────────────────────────┐
│                               USERS                                  │
│                                                                      │
│   Guest / Staff / Manager                                            │
│   (любой клиент через Telegram UI)                                   │
└───────────────────────────────┬──────────────────────────────────────┘
│
│ messages / callbacks / contacts
▼
┌──────────────────────────────────────────────────────────────────────┐
│                          TELEGRAM PLATFORM                            │
│                                                                        │
│   Telegram Bot API                                                     │
│   - retries                                                           │
│   - delayed updates                                                   │
│   - duplicate delivery                                                │
│                                                                        │
│   (внешняя среда, неконтролируемая)                                   │
└───────────────────────────────┬──────────────────────────────────────┘
│
▼
┌──────────────────────────────────────────────────────────────────────┐
│                      TELEGRAM ADAPTER (IO)                            │
│                                                                        │
│   - принимает update                                                   │
│   - маппит в InboundEvent                                              │
│   - НЕ содержит бизнес-логики                                         │
│   - НЕ принимает решений                                              │
│                                                                        │
└───────────────────────────────┬──────────────────────────────────────┘
│
▼
┌──────────────────────────────────────────────────────────────────────┐
│            EVENT NORMALIZATION & IDEMPOTENCY LAYER                    │
│                                                                        │
│   Idempotency Guard                                                    │
│   - проверка duplicate eventId                                        │
│   - Redis (processed events)                                          │
│                                                                        │
│   Context Restore                                                      │
│   - Redis (FSM context, hot path)                                     │
│   - PostgreSQL (fallback)                                             │
│                                                                        │
└───────────────────────────────┬──────────────────────────────────────┘
│
▼
┌──────────────────────────────────────────────────────────────────────┐
│                         FSM CORE (ORCHESTRATION)                     │
│                                                                        │
│   FSM Router                                                           │
│   - текущее состояние пользователя                                   │
│   - роль (guest / staff / manager)                                   │
│   - тип события                                                       │
│                                                                        │
│   FSM Transition Engine                                                │
│   - допустимые переходы                                               │
│   - fallback / safe state                                             │
│   - offline / late messages                                           │
│                                                                        │
│   FSM = single source of truth                                        │
│                                                                        │
└───────────────┬───────────────┬───────────────┬─────────────────────┘
│               │               │
│               │               │
│               │               │
▼               ▼               ▼
┌──────────────────────┐ ┌──────────────────────┐ ┌───────────────────┐
│   AI / ALISA ADAPTER │ │     USER DOMAIN       │ │   BOOKING DOMAIN   │
│   (optional plugin)  │ │  (Memory Engine)     │ │   (Slot Keeper)    │
│                      │ │                      │ │                   │
│ - intent parsing     │ │ - identity           │ │ - date             │
│ - entity extraction │ │ - phone               │ │ - slot             │
│ - text → meaning     │ │ - preferences        │ │ - guests           │
│                      │ │                      │ │                   │
│ - cache (Redis)      │ │ - repository         │ │ - repository       │
│ - timeout            │ │ - no FSM logic       │ │ - no FSM logic     │
│ - fallback to rules  │ │ - no Telegram        │ │ - no Telegram      │
└──────────────┬───────┘ └──────────────┬───────┘ └─────────┬─────────┘
│                          │                   │
│                          │                   │
▼                          ▼                   ▼
┌──────────────────────┐  ┌──────────────────────┐  ┌───────────────────┐
│     INFO DOMAIN       │  │     TIPS DOMAIN       │  │  FUTURE DOMAINS   │
│   (Quiet Guide)      │  │    (Smart Tip)        │  │                   │
│                      │  │                      │  │ - Safe Play       │
│ - events              │  │ - tip intent         │  │ - Hidden Heart    │
│ - posters             │  │ - amount             │  │ - Preference Map │
│ - descriptions        │  │ - status             │  │                   │
│                      │  │                      │  │                   │
│ no FSM logic          │  │ finance-isolated     │  │ FSM-controlled    │
│ no AI                 │  │ future service cut  │  │                   │
└──────────────┬───────┘  └──────────────┬───────┘  └───────────────────┘
│                          │
│                          │
▼                          ▼
┌──────────────────────────────────────────────────────────────────────┐
│                     CONTEXT UPDATE & SIDE EFFECTS                    │
│                                                                        │
│   - обновление FSM Context                                            │
│   - сохранение доменных сущностей                                    │
│   - commit как точка успеха                                          │
│                                                                        │
└───────────────┬───────────────────────────────┬─────────────────────┘
│                               │
│                               │
▼                               ▼
┌──────────────────────────────┐      ┌──────────────────────────────┐
│        KAFKA EVENT BUS        │      │      RESPONSE BUILDER         │
│   (async / optional MVP)     │      │                                │
│                              │      │ - message templates           │
│ - audit events               │      │ - localization                │
│ - metrics                    │      │ - Redis cache                 │
│ - observers                  │      │                                │
│                              │      └──────────────┬───────────────┘
└───────────────┬──────────────┘                     │
│                                     │
▼                                     ▼
┌──────────────────────────────┐      ┌──────────────────────────────┐
│        AUDIT / METRICS        │      │     TELEGRAM ADAPTER (OUT)    │
│        WORKERS                │      │                                │
│                              │      │ - serialize response          │
│ - consume Kafka               │      │ - retry send                 │
│ - enrich metrics              │      │ - no FSM logic               │
│ - write to Audit DB           │      │                                │
│                              │      └──────────────┬───────────────┘
└──────────────────────────────┘                     │
▼
┌──────────────────────────┐
│      TELEGRAM API         │
│   (send message)         │
└──────────────────────────┘