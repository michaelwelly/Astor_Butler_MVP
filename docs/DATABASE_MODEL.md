# Модель данных Astor Butler MVP

## Главный принцип

`users` — это внутренняя личность Astor Butler.  
`telegram_profiles` — это внешний Telegram-аккаунт, через который человек пришел в систему.

Telegram не должен быть главным пользователем. Он является первым каналом входа. Позже рядом могут появиться web chat, WhatsApp, WeChat, email или PMS-интеграции.

## Целевая нормализация MVP

```text
users
  id
  display_name
  role
  status
  created_at
  updated_at

telegram_profiles
  telegram_user_id
  user_id -> users.id
  chat_id
  username
  first_name
  last_name
  language_code
  is_bot
  phone_number
  last_seen_at

user_contacts
  id
  user_id -> users.id
  contact_type
  contact_value
  source
  is_primary
  verified_at

user_consents
  id
  user_id -> users.id
  telegram_user_id
  consent_type
  policy_version
  status
  evidence

telegram_messages
  id
  user_id -> users.id
  telegram_user_id
  chat_id
  message_kind
  text
  raw_payload
  received_at

event_bookings
  id
  user_id -> users.id
  manager_user_id -> users.id
  status
  event_type
  event_date
  guest_count
  budget
  created_at
```

## Отношения и constraints

```text
users 1 -- 0..1 telegram_profiles
users 1 -- 0..N user_contacts
users 1 -- 0..N user_consents
users 1 -- 0..N telegram_messages
users 1 -- 0..N event_bookings as guest/client
users 1 -- 0..N event_bookings as manager
users 1 -- 0..N table_reservation_orders as guest/client
users 1 -- 0..N table_reservation_orders as manager
venue_tables 1 -- 0..N table_reservation_orders
venue_tables 1 -- 0..N table_reservation_holds
table_reservation_orders 1 -- 0..N table_reservation_holds
```

Целевые ключи:

- `telegram_profiles.user_id -> users.id`, `ON DELETE SET NULL`;
- `telegram_messages.user_id -> users.id`, `ON DELETE SET NULL`;
- `user_consents.user_id -> users.id`, `ON DELETE SET NULL`;
- `user_contacts.user_id -> users.id`, `ON DELETE CASCADE`;
- `event_bookings.user_id -> users.id`, `ON DELETE SET NULL`;
- `event_bookings.manager_user_id -> users.id`, `ON DELETE SET NULL`.
- `table_reservation_orders.user_id -> users.id`, `ON DELETE SET NULL`;
- `table_reservation_orders.manager_user_id -> users.id`, `ON DELETE SET NULL`;
- `table_reservation_orders.table_id -> venue_tables.id`, `ON DELETE SET NULL`;
- `table_reservation_holds.table_id -> venue_tables.id`, `ON DELETE CASCADE`;
- `table_reservation_holds.order_id -> table_reservation_orders.id`, `ON DELETE CASCADE`.

Целевые уникальности и индексы:

- `telegram_profiles.telegram_user_id` — primary key внешнего Telegram-аккаунта;
- `telegram_profiles.user_id` — unique partial index для текущего MVP-правила `1 user = 1 Telegram profile`;
- `telegram_messages(user_id, received_at)` — история сообщений пользователя;
- `user_consents(user_id, consent_type, policy_version)` — быстрый поиск актуального согласия;
- `user_contacts(contact_type, contact_value)` — поиск пользователя по телефону/e-mail;
- `event_bookings(user_id, status, created_at)` — карточка гостя и история заявок;
- `event_bookings(manager_user_id, status, created_at)` — менеджерский workload.
- `venue_tables(venue_code, zone, active, sort_order)` — справочник посадочных мест;
- `table_reservation_orders(status, requested_start_at)` — менеджерская очередь брони столов;
- `table_reservation_holds(table_id, start_at, end_at, status)` — проверка занятости стола.

## CQRS-подход для MVP

CQRS вводим сначала внутри монолита, без отдельной PostgreSQL read replica:

```text
Command side:
  identity command service
  booking command service
  consent command service

Query side:
  user profile query service
  booking read model
  manager dashboard read model
```

Правило: write-сервисы меняют состояние и публикуют события, query-сервисы собирают read model для Swagger, manager UI и будущего frontend.

Read replica планируется после появления стабильных API и первого k6-нагрузочного сценария. В MVP local/dev весь read/write traffic идет в один PostgreSQL primary, чтобы не усложнять отладку Telegram/FSM.

## Совместимость текущего кода

В таблице `users` временно остаются legacy-поля `telegram_id`, `first_name`, `last_name`, `username`, `phone`. Они нужны для текущего JPA-слоя и старых API. Новая логика должна работать через `user_id`, `telegram_profiles` и `user_contacts`.

Следующий шаг после стабилизации сценариев:

- перевести `UserRepository` и `UserProfileService` на внутренний `user_id`;
- убрать бизнес-смысл из `users.telegram_id`;
- оставить Telegram ID только в `telegram_profiles`;
- обновить Swagger User API под внутреннюю модель.

## Правило для новых доменов

Booking, Timeline, Notifications, Consent Vault и будущие capability-модули должны ссылаться на `users.id`, а не на `telegram_user_id`.

Если домену нужен Telegram-контекст, он получает его через identity boundary, а не напрямую из транспортного слоя.
