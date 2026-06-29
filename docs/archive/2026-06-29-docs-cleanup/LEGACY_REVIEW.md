# Astor Butler Legacy Review

Source repository: https://github.com/michaelwelly/Astor_Butler_Legacy

Review date: 2026-05-29

## Decision

Legacy is useful as a product and scenario source, but it must not be merged wholesale into MVP.

MVP keeps the new architecture as the source of truth:

- Java 21 + Spring Boot modular monolith.
- FSM as the single source of truth.
- REST/Kafka public boundary.
- Service/gRPC internal boundary.
- PostgreSQL through the target JDBC/Liquibase direction.
- Redis for FSM hot context, idempotency and cache.
- Capability extension layer for product modules.

Legacy code is imported only when it fits these boundaries or can be adapted safely.

## What Legacy Contains

Useful product areas:

- Telegram registration and phone/contact flow.
- Role and status model.
- Table reservation and slot model.
- Menu PDF and event poster model.
- Tip scenario.
- Charity scenario.
- Feedback scenario.
- Reward policy and transaction log concepts.
- Central/FSM router idea.
- Liquibase changelog examples.
- Privacy policy draft.

Repository hygiene:

- No `target/`, `build/`, `.idea/`, `.gradle` or `node_modules` artifacts were found in the cloned Legacy repository.
- No hardcoded bot token or database password was found. Configuration uses environment placeholders such as `TELEGRAM_BOT_TOKEN` and `DB_PASSWORD`.

## What We Should Not Copy As-Is

- JPA entities and Spring Data repositories: MVP target is explicit JDBC, not JPA/Hibernate.
- Old MVC controllers: API shape must follow the new REST API list and Swagger contracts.
- Feature packages such as `table`, `merch`, `charity`, `tip` as top-level architecture: in MVP they become domain/capability modules.
- Old README and ROADMAP as project truth: they describe the previous architecture and contain inconsistent build stack notes.
- In-memory FSM storage as production logic: useful only as reference for tests or sandbox flows.
- Telegram handlers that contain business decisions directly: in MVP Telegram remains transport/UI.
- Payment provider stubs such as Tinkoff integration: keep as future integration ideas, not production code.

## First Code Imported

`TelegramAuthService` from Legacy was reviewed and replaced with a safer MVP implementation:

- New class: `domain.auth.TelegramLoginVerifier`.
- It verifies the complete Telegram login payload, not only `id`.
- It excludes `hash`, sorts all received fields and validates HMAC-SHA256.
- It checks `auth_date` freshness.
- It uses constant-time hash comparison.
- Unit tests cover valid payload, tampered payload and expired payload.

## Legacy-to-MVP Mapping

| Legacy area | MVP target | Status |
| --- | --- | --- |
| `user/TelegramAuthService` | `domain.auth.TelegramLoginVerifier` | Imported as corrected implementation |
| `telegram/handler/StartHandler` contact flow | `fsm` + `telegram.adapter` + `domain.user` | Use as reference, do not copy directly |
| `tip/*` | `capability.smarttip` | Keep scenario model, rewrite persistence |
| `charity/*` | `capability.hiddenheart` | Keep concept, rewrite persistence/API |
| `slot/*` | `capability.slotkeeper` + `domain.booking` | Reuse schema ideas, rewrite services |
| `reference/MenuPdf*` | `capability.quietguide` + `domain.content/media` | Reuse fields, move files to S3 metadata model |
| `poster/*` | `domain.content` | Reuse event poster concept |
| `feedback/*` | `domain.timeline` + future feedback capability | Reuse flow idea |
| `finance/reward/*` | `capability.preference` / loyalty extension | Defer |
| `central/FSMRouter` | current `fsm.core` + service layer | Reference only |
| Liquibase changelogs | future JDBC schema migrations | Rework naming, UUID strategy and constraints |

## Next Imports

1. Convert Legacy role/status/reference schema ideas into new Liquibase migrations.
2. Create JDBC repositories for `user`, `booking`, `content`, `media`, `timeline`.
3. Adapt Telegram contact flow into FSM transitions without business logic in Telegram handlers.
4. Model `Smart Tip`, `Hidden Heart`, `Quiet Guide` and `Slot Keeper` as capability contracts.
5. Add Swagger controllers for API skeletons before implementing full domain persistence.
