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
```

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
