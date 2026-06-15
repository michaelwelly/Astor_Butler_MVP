# Telegram Stars Payments Journal

Status: foundation layer added, runtime invoice sending is next.

Source of truth:

- Official Telegram docs: https://core.telegram.org/bots/payments-stars

Current contract:

- Telegram Stars are used for digital goods and services inside Telegram.
- Currency is always `XTR`.
- `provider_token` is empty for digital goods/services.
- The bot must answer `pre_checkout_query` within 10 seconds.
- Goods/services are delivered only after `successful_payment`.
- Store `telegram_payment_charge_id` for future refunds.

Implemented in this repo:

- `telegram_star_payments` table.
- `TelegramStarPaymentService`.
- REST API:
  - `POST /api/payments/telegram-stars`
  - `GET /api/payments/telegram-stars/{id}`
  - `GET /api/payments/telegram-stars/telegram/{chatId}`

Next runtime layer:

- Add Telegram adapter sender for `sendInvoice`.
- Store `invoice_message_id` after sending.
- Handle `pre_checkout_query` and approve/reject from stored payload.
- Handle `successful_payment`, mark payment `PAID`, and publish admin/Kafka event.
- Add `/paysupport` response before live mode.

Product boundary:

- SBP remains direct real-money handoff for restaurant/staff/initiatives.
- Stars are useful for Telegram-native digital goods, access, badges, digital auction passes, and test flows.
