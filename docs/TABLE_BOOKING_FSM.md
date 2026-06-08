# Table Booking FSM

## Purpose

This document is the canonical FSM map for regular AERIS table reservations.
It is separate from `EVENT_BOOKING`, which covers banquets, private events,
corporate events and other larger manager-led requests.

The LLM is only an adapter. It may classify intent and extract slots, but it
must not own state, create holds, confirm reservations or invent availability.

## Actors

| Actor | Role |
| --- | --- |
| Guest | Asks for a table, chooses date/time/table, receives final order |
| Astor Butler | Collects slots, calls domain services, sends plan/order messages |
| Hostess chat | Confirms or rejects the pending reservation with buttons |
| PostgreSQL | Durable order and hold state |
| SBIS | Future external status authority |

## State Map

| State | Meaning | Required input | Action | Next states |
| --- | --- | --- | --- | --- |
| `READY_FOR_DIALOG` | Guest can start any scenario | free text | classify intent | `TABLE_BOOKING_INTENT`, `AI_FALLBACK` |
| `TABLE_BOOKING_INTENT` | Table booking detected | date/time/party if present | create or resume draft | collect first missing slot |
| `TABLE_BOOKING_COLLECT_DATE` | Missing date | date | store date | collect time |
| `TABLE_BOOKING_COLLECT_TIME` | Missing time | time | store time | collect party size |
| `TABLE_BOOKING_COLLECT_PARTY_SIZE` | Missing guest count | integer/phrase | store party size | show plan |
| `TABLE_BOOKING_SHOW_PLAN` | Ready to show AERIS plan | none | send AERIS plan PDF/media | wait table choice |
| `TABLE_BOOKING_WAIT_TABLE_SELECTION` | Waiting table/zone/preference | table code, zone, "choose for me" | check availability and create hold | wait hostess confirmation or ask another option |
| `TABLE_BOOKING_WAIT_HOSTESS_CONFIRMATION` | Hold created, hostess card sent | hostess button | no guest data collection | confirmed/rejected |
| `TABLE_BOOKING_CONFIRMED` | Hostess approved | none | send guest order | ready dialog |
| `TABLE_BOOKING_REJECTED` | Hostess rejected | guest choice | release hold, offer alternative | change requested / cancelled |
| `TABLE_BOOKING_CHANGE_REQUESTED` | Guest wants to change request | date/time/party/table | update draft, re-check | wait hostess confirmation |
| `TABLE_BOOKING_CANCELLED` | Guest/staff cancelled | none | release holds | ready dialog |

## Happy Path

```text
Guest: хочу столик сегодня на 20:00 на двоих
Astor: asks table/zone preference and sends AERIS plan
Guest: стол 17
Astor: creates table_reservation_order + HELD hold
Astor -> hostess chat: approval card with Да/Нет
Hostess presses Да
Astor: order CONFIRMED, hold CONFIRMED
Astor -> guest: beautiful confirmed order
```

## Auto-Selection Path

If the guest says "любой", "выберите сами", "где удобно", Astor may call the
availability service without `tableCode`. The service selects the smallest
available fitting table. Astor still must ask hostess for confirmation.

## Hall Plan

Before asking the guest to choose a table, Astor must send the AERIS hall plan.
Current resource asset:

```text
MinIO/S3 object:

```text
content/aeris/floor-plan/AERIS_PLAN.pdf
```
```

Runtime configuration:

```text
TELEGRAM_BOOKING_PLAN_PDF_ASSET_CODE=AERIS_FLOOR_PLAN
```

The next implementation step for `TableBookingScenario` must attach this PDF in
`TABLE_BOOKING_SHOW_PLAN`, then move the guest to
`TABLE_BOOKING_WAIT_TABLE_SELECTION`.

## Rejection Path

```text
Hostess presses Нет
Astor: order REJECTED, hold RELEASED
Astor -> guest: polite refusal and offer another table/time
Guest: тогда на 21:00
Astor: checks availability again and sends a new approval card
```

## Cancellation Path

Guest cancellation signals:

- "отмени бронь"
- "не придем"
- "планы поменялись"
- "cancel"

Action:

- set order `CANCELLED`;
- release active holds;
- send short confirmation to guest;
- optionally notify hostess if the order was already sent.

## Change Path

Guest may change:

- date;
- time;
- party size;
- table/zone;
- phone/comment.

Changing date/time/table/party after a hold exists must release or supersede the
old hold before creating the new one. The new variant must go to hostess
confirmation again.

## Ambiguity Rules

Ask only one question at a time:

- no date -> ask date;
- date but no time -> ask time;
- date/time but no party size -> ask guest count;
- date/time/party but no table preference -> send plan and ask table/zone or
  permission to choose automatically;
- conflicting data -> summarize what you understood and ask for correction.

## Event Booking Boundary

Route to `EVENT_BOOKING` or manager review, not table booking, when the guest
mentions:

- banquet, corporate party, birthday event, wedding, buyout;
- more than regular seating capacity;
- menu packages, deposit, contract, technical equipment;
- decorators, contractors, stage, DJ, host, presentation.

## LLM Contract

Runtime prompt fragment lives at:

```text
src/main/resources/fsm/table-booking-llm-contract.md
```

The prompt is intentionally compact. This full document remains the human
source of truth.
