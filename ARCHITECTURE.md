┌───────────────────────────────────────────────────────────────┐
│                          Clients                              │
│                                                               │
│   Guest / Staff / Manager                                     │
│   Telegram UI                                                  │
│   (чат, кнопки, zero-push)                                    │
└───────────────────────────────┬───────────────────────────────┘
│
│ updates / messages
▼
┌───────────────────────────────────────────────────────────────┐
│                    Telegram Bot API                            │
│                  (Transport only, no logic)                   │
└───────────────────────────────┬───────────────────────────────┘
│
▼
┌───────────────────────────────────────────────────────────────┐
│                Load Balancer / Ingress                         │
│         (Yandex Cloud LB / k8s ingress / NLB)                  │
│         stateless routing, HTTPS termination                   │
└───────────────────────────────┬───────────────────────────────┘
│
▼
┌─────────────────────────────────────────────────────────────────────────┐
│                    Astor Butler MVP                                      │
│           Modular Monolith · Java 21 · Spring Boot                        │
│           (multiple pods, single codebase)                                │
│                                                                           │
│  ┌─────────────────────────────────────────────────────────────────────┐ │
│  │                    Infrastructure / Platform                          │ │
│  │                                                                         │ │
│  │  - Spring Boot config                                                   │ │
│  │  - Feature flags                                                        │ │
│  │  - Logging                                                              │ │
│  │  - Observability hooks                                                  │ │
│  │                                                                         │ │
│  │  ┌──────────────┐        ┌────────────────┐                           │ │
│  │  │ Telegram     │        │  AI Adapter    │                           │ │
│  │  │ Adapter      │        │  (LLM / Alisa) │                           │ │
│  │  │ (IO only)    │        │                │                           │ │
│  │  └──────┬───────┘        └───────┬────────┘                           │ │
│  │         │ events                  │ intents / parsing                 │ │
│  │         └──────────────┬──────────┘                                   │ │
│  │                        ▼                                              │ │
│  │                ┌──────────────────────────────┐                       │ │
│  │                │           FSM CORE            │                       │ │
│  │                │   (Orchestration Layer)       │                       │ │
│  │                │                                │                       │ │
│  │                │ - scenario router              │                       │ │
│  │                │ - state transitions            │                       │ │
│  │                │ - guards / validation          │                       │ │
│  │                │ - timeout / fallback logic     │                       │ │
│  │                │                                │                       │ │
│  │                │ FSM = single source of truth   │                       │ │
│  │                └───────┬─────────┬─────────┬───┘                       │ │
│  │                        │         │         │                           │ │
│  │                        │         │         │                           │ │
│  │                        ▼         ▼         ▼                           │ │
│  │  ┌────────────────┐  ┌────────────────┐  ┌────────────────────────┐ │ │
│  │  │ User Domain     │  │ Booking Domain  │  │ Tips Domain             │ │ │
│  │  │ (Memory Engine) │  │ (Slot Keeper)  │  │ (Smart Tip)             │ │ │
│  │  │                │  │                │  │                          │ │ │
│  │  │ - identity     │  │ - date          │  │ - intent                │ │ │
│  │  │ - phone        │  │ - slot          │  │ - amount                │ │ │
│  │  │ - preferences  │  │ - guests        │  │ - status                │ │ │
│  │  │                │  │                │  │                          │ │ │
│  │  │ no Telegram    │  │ no Telegram    │  │ finance-isolated         │ │ │
│  │  │ no FSM logic   │  │ no FSM logic   │  │ future service candidate │ │ │
│  │  └────────────────┘  └────────────────┘  └────────────────────────┘ │ │
│  │                        │                                               │ │
│  │                        ▼                                               │ │
│  │               ┌──────────────────────────┐                            │ │
│  │               │ Info Domain               │                            │ │
│  │               │ (Quiet Guide)             │                            │ │
│  │               │                            │                            │ │
│  │               │ - events                  │                            │ │
│  │               │ - posters                 │                            │ │
│  │               │ - descriptions            │                            │ │
│  │               │                            │                            │ │
│  │               │ no AI / no FSM logic      │                            │ │
│  │               └──────────────────────────┘                            │ │
│  │                                                                         │ │
│  │  ┌────────────────┐        ┌────────────────┐                          │ │
│  │  │ PostgreSQL      │        │ Redis           │                          │ │
│  │  │                │        │                │                          │ │
│  │  │ - users         │        │ - FSM cache    │                          │ │
│  │  │ - FSM state     │        │ - slots        │                          │ │
│  │  │ - audit log     │        │ - temp ctx     │                          │ │
│  │  └────────────────┘        └────────────────┘                          │ │
│  │                                                                         │ │
│  │  ┌─────────────────────────────────────────────────────────────────┐  │ │
│  │  │ Kafka (optional / MVP flag)                                      │  │ │
│  │  │                                                                 │  │ │
│  │  │ - domain events (UserCreated, BookingRequested, TipIntented)   │  │ │
│  │  │ - async observers / future integrations                         │  │ │
│  │  │ - NOT required for core MVP demo                                │  │ │
│  │  └─────────────────────────────────────────────────────────────────┘  │ │
│  └─────────────────────────────────────────────────────────────────────┘ │
│                                                                           │
│  Observability                                                            │
│  - Logs                                                                   │
│  - Metrics (Prometheus)                                                   │
│  - Dashboards (Grafana)                                                   │
│                                                                           │
└───────────────────────────────────────────────────────────────────────────┘
│
▼
┌───────────────────────────────────────────────────────────────┐
│                       Yandex Cloud                              │
│                                                               │
│  - Load Balancer                                               │
│  - Compute / k8s pods                                          │
│  - Managed PostgreSQL                                         │
│  - Managed Redis                                              │
│  - (Kafka optional)                                           │
└───────────────────────────────────────────────────────────────┘