# Kafka Outbox and CDC

Astor Butler uses Kafka as a future event backbone. The local MVP uses a durable outbox path by default:

- Spring Boot writes domain events into PostgreSQL `outbox_events`;
- Debezium Outbox Event Router publishes them into Kafka;
- Kafka Connect publishes JSON values, not Avro;
- analytics/admin consumers read JSON records and render human-readable Telegram admin messages.

There is no direct Spring Kafka producer in the MVP path. PostgreSQL outbox is the single handoff point.

## Local endpoints

- Kafka broker: `localhost:9092`
- Redpanda Console: `http://localhost:8081`
- Debezium Connect: `http://localhost:8083`

## Topics

- `astor.user.events` - runtime user/FSM events, JSON value, stable user/chat key.
- `_schemas` - Redpanda Schema Registry internal topic.
- `astor.connect.*` - Kafka Connect internal topics.

Local `astor.user.events` uses 3 partitions and replication factor 1 because the local Redpanda compose stack currently has one broker.

Production target:

- at least 3 brokers;
- replication factor 3;
- `min.insync.replicas=2`;
- producer `acks=all`;
- producer idempotence enabled.

## Outbox table

`outbox_events` follows the Debezium Outbox Event Router shape:

- `id` - event id for Debezium routing;
- `aggregatetype` - target Kafka topic, for example `astor.user.events`;
- `aggregateid` - Kafka record key, for example `telegram:user:1773317437`;
- `type` - event type;
- `payload` - event payload as JSONB;
- `timestamp` - event timestamp.

The application stores payloads in the same logical envelope that admin/analytics consumers understand:

- `eventId`
- `eventType`
- `eventVersion`
- `occurredAt`
- `source`
- `channel`
- `idempotencyKey`
- `actor`
- `payload`

## Register connector

Start local infrastructure:

```bash
docker compose up -d postgres kafka redpanda-console debezium-connect
```

Register or update the connector:

```bash
scripts/register_debezium_outbox_connector.sh
```

Check status:

```bash
curl -s http://localhost:8083/connectors/astor-outbox-connector/status
```

Check admin consumer group:

```bash
docker exec astor_kafka_test rpk group describe astor-admin-events
```

## Local smoke result

Verified on 2026-06-04:

- connector status is `RUNNING`;
- an outbox insert with key `telegram:user:1773317437` was routed to `astor.user.events`;
- local topic `astor.user.events` has 3 partitions and replication factor 1.

Updated on 2026-06-05:

- Debezium local converter was simplified to JSON because Avro subject compatibility made admin delivery fragile;
- canonical admin consumer group is `astor-admin-events`;
- old local groups such as `astor-analytics-admin-avro` and `astor-analytics-admin-avro-v2` are obsolete and can be ignored or deleted in local Kafka.
