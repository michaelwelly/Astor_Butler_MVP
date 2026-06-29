# Table Booking

## Source Plan

Current resource asset:

```text
MinIO/S3 object:

```text
content/aeris/floor-plan/AERIS_PLAN.pdf
```
```

The PDF has three pages. Page 1 is a cover, pages 2 and 3 contain the venue
plan in dark and light variants. The booking flow should send this resource to
the guest as a Telegram document before asking for table choice.

Runtime path:

```text
TELEGRAM_BOOKING_PLAN_PDF_ASSET_CODE=AERIS_FLOOR_PLAN
```

## AERIS Seating Model

The first database seed is based on the plan:

| Zone | Tables | Capacity rule |
| --- | --- | --- |
| Main hall | 1, 3, 5, 6, 9, 10, 11, 12, 16 | up to 4 guests |
| Main hall | 2, 4 | up to 5 guests |
| Main hall | 17, 18, 19 | up to 2 guests |
| Wine room | 7, 8 | each modeled as 6, combinable up to 12 |
| VIP zone | 13, 14, 15 | each modeled as 4, combinable up to 12 |
| Bar | BAR | bar seating up to 18 guests |

## Tables

`venue_tables` is the canonical local reference for bookable physical places.
It contains the table code visible on the plan, zone, capacity, active/bookable
flags, and combinable groups such as `WINE_ROOM_7_8` and `VIP_13_14_15`.

`table_reservation_orders` is the guest-facing request/order. It keeps the
Telegram chat, optional normalized `user_id`, selected table, requested time
window, party size, comments, manager routing, hostess routing, and future SBIS
external id.

`table_reservation_holds` is the operational occupancy layer. A hold blocks a
table for a time window while a manager confirms or rejects the order. Confirmed
holds become the local source of truth until SBIS integration is connected.

## Statuses

Initial order statuses:

- `DRAFT` - created but not enough details yet.
- `AWAITING_GUEST_SELECTION` - plan sent, waiting for table/time/party size.
- `AWAITING_MANAGER_CONFIRMATION` - guest choice is captured and routed to manager.
- `CONFIRMED` - manager confirmed and hostess should see the order.
- `REJECTED` - manager rejected the requested table/time.
- `CANCELLED` - guest or staff cancelled.
- `EXPIRED` - temporary hold expired without confirmation.

Initial hold statuses:

- `HELD` - temporary local block.
- `CONFIRMED` - final local block.
- `RELEASED` - no longer blocks availability.
- `CANCELLED` - cancelled with the order.

## MVP Flow

```text
Guest asks for a table
  -> FSM creates or resumes table_reservation_order
  -> bot sends AERIS PLAN PDF
  -> guest chooses table, time, party size
  -> service checks local holds/orders for conflicts
  -> service creates HELD hold
  -> hostess chat receives confirmation request
  -> hostess confirms or rejects with inline buttons
  -> order becomes CONFIRMED
  -> guest receives a human-readable confirmed order
  -> if rejected: hold is released and guest receives a polite refusal with an offer to choose another option
```

The system may also initiate this flow when a guest enters the scenario without
a table, because a guest should not remain in the restaurant flow without a
place assignment.

## REST API

Current second-stage endpoints:

```http
GET /api/bookings/tables?venueCode=AERIS
```

Returns the seeded seating plan from `venue_tables`.

```http
GET /api/bookings/tables/availability?venueCode=AERIS&from=2026-06-06T17:00:00Z&to=2026-06-06T19:00:00Z&partySize=3
```

Returns bookable active tables that fit the party size and do not overlap with
active `HELD` or `CONFIRMED` holds.

```http
POST /api/bookings/table-reservations
Content-Type: application/json

{
  "chatId": 1773317437,
  "telegramUserId": 1773317437,
  "venueCode": "AERIS",
  "tableCode": "5",
  "requestedStartAt": "2026-06-06T17:00:00Z",
  "requestedEndAt": "2026-06-06T19:00:00Z",
  "partySize": 3,
  "guestName": "Наталья",
  "guestPhone": "+79990000000",
  "guestComment": "Хочу спокойный стол"
}
```

Creates a `table_reservation_orders` row with status
`AWAITING_MANAGER_CONFIRMATION` and a matching `HELD` row in
`table_reservation_holds`. If `tableCode` is omitted, the service chooses the
smallest available table that fits the party size.

When the order is created, Astor sends a human-readable Telegram card to the
hostess chat. The current test hostess chat is:

```text
TELEGRAM_HOSTESS_CHAT_ID=-1004291419562
```

The hostess chat receives the card with inline buttons:

```text
Да
Нет
```

`Да` confirms the reservation. `Нет` rejects the reservation, releases the hold,
and sends the guest a polite refusal with an offer to choose another table or
time.

Plain chat text no longer confirms reservations. Messages in the hostess chat
are consumed by the table approval boundary so they do not accidentally enter
the guest FSM.

```http
POST /api/bookings/table-reservations/{id}/confirm
```

Changes the order to `CONFIRMED`, changes its active holds from `HELD` to
`CONFIRMED`, sends the confirmed order to the hostess chat, and sends a
human-readable order confirmation to the guest. This endpoint remains as a REST
fallback for Swagger/manual checks.

```http
POST /api/bookings/table-reservations/{id}/reject
```

Changes the order to `REJECTED` and releases active `HELD` holds.

Telegram sending is controlled by:

```text
TELEGRAM_BOOKING_NOTIFICATIONS_ENABLED=true
TELEGRAM_HOSTESS_CHAT_ID=
```

The current MVP confirmation path is hostess-chat buttons first. REST endpoints
remain as Swagger/manual fallback.

## SBIS Boundary

SBIS must be isolated behind a port:

```text
TableAvailabilityPort
  LocalTableAvailabilityAdapter now
  SbisTableAvailabilityAdapter later

TableReservationPort
  LocalTableReservationAdapter now
  SbisTableReservationAdapter later
```

Until SBIS is connected, PostgreSQL plus manager confirmation is the operating
source of truth. After SBIS is connected, the local tables remain useful as the
FSM/order journal and fallback cache, while SBIS becomes the external status
authority.
