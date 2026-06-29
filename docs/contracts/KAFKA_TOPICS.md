# Kafka Topics

Astor Butler MVP keeps Kafka intentionally small. Telegram/FSM writes events once into PostgreSQL `outbox_events`; Debezium routes them to Kafka; one admin consumer renders human-readable Telegram notifications.

## Application Topics

### `astor.user.events`

Purpose: canonical user/FSM event stream.

Producer path:

1. Spring Boot writes JSON payload to `outbox_events`.
2. Debezium Outbox Event Router reads `outbox_events`.
3. Debezium publishes JSON records to `astor.user.events`.

Key:

- `telegram:user:<telegramUserId>` when Telegram user id is known;
- `chat:<chatId>` as fallback.

Value:

- JSON envelope with `eventId`, `eventType`, `eventVersion`, `occurredAt`, `source`, `channel`, `idempotencyKey`, `actor`, `payload`.

Current event types:

- `USER_MESSAGE_RECEIVED`
- `LLM_RESPONSE_GENERATED`

Consumers:

- `astor-admin-events` - reads JSON records and sends human-readable cards to `TELEGRAM_ANALYTICS_CHAT_ID`.

Keep/delete decision:

| Topic | Keep? | Why |
| --- | --- | --- |
| `astor.user.events` | Yes | The only business topic in the MVP. Guest messages, FSM transitions, LLM responses, and later booking events flow here. |
| `astor.connect.configs` | Yes while Debezium Connect is used | Kafka Connect stores connector configuration here. |
| `astor.connect.offsets` | Yes while Debezium Connect is used | Kafka Connect stores read offsets here, so Debezium does not resend old outbox rows after restart. |
| `astor.connect.statuses` | Yes while Debezium Connect is used | Kafka Connect stores connector/task state here. |
| `_schemas` | Ignore locally | Redpanda Schema Registry internal topic. It may remain from older Avro experiments or be recreated by Redpanda. The MVP JSON pipeline does not use it. |

## Kafka Connect Internal Topics

These are infrastructure topics. They are not business events.

- `astor.connect.configs`
- `astor.connect.offsets`
- `astor.connect.statuses`

## Redpanda Internal Topics

- `_schemas` - Redpanda Schema Registry internal topic. It may exist locally, but the MVP admin pipeline does not depend on Schema Registry now.

## Obsolete Local Consumer Groups

These names came from earlier Avro experiments and should not be used for new code:

- `astor-analytics-admin`
- `astor-analytics-admin-avro`
- `astor-analytics-admin-avro-v2`

Canonical current group:

- `astor-admin-events`

Local cleanup command if old groups create noise:

```bash
docker exec astor_kafka_test rpk group delete astor-analytics-admin astor-analytics-admin-avro astor-analytics-admin-avro-v2
```

## Local Checks

List topics:

```bash
docker exec astor_kafka_test rpk topic list
```

Check admin lag:

```bash
docker exec astor_kafka_test rpk group describe astor-admin-events
```

Check Debezium:

```bash
curl -s http://localhost:8083/connectors/astor-outbox-connector/status
```
