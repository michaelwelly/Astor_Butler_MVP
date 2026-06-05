#!/usr/bin/env bash
set -euo pipefail

if [ -f ".env" ]; then
  set -a
  # shellcheck disable=SC1091
  source ".env"
  set +a
fi

CONNECT_URL="${DEBEZIUM_CONNECT_URL:-http://localhost:${DEBEZIUM_CONNECT_PORT:-8083}}"
CONNECTOR_NAME="${DEBEZIUM_OUTBOX_CONNECTOR_NAME:-astor-outbox-connector}"
POSTGRES_HOST="${DEBEZIUM_POSTGRES_HOST:-postgres}"
POSTGRES_PORT_INTERNAL="${DEBEZIUM_POSTGRES_PORT:-5432}"
POSTGRES_DB_NAME="${POSTGRES_DB:-astor_butler_test}"
POSTGRES_USER_NAME="${POSTGRES_USER:-astor}"
POSTGRES_PASSWORD_VALUE="${POSTGRES_PASSWORD:-astor}"

echo "Registering Debezium outbox connector '${CONNECTOR_NAME}' at ${CONNECT_URL}"

curl -fsS -X PUT "${CONNECT_URL}/connectors/${CONNECTOR_NAME}/config" \
  -H "Content-Type: application/json" \
  --data-binary @- <<JSON
{
  "connector.class": "io.debezium.connector.postgresql.PostgresConnector",
  "key.converter": "org.apache.kafka.connect.storage.StringConverter",
  "value.converter": "org.apache.kafka.connect.json.JsonConverter",
  "value.converter.schemas.enable": "false",
  "plugin.name": "pgoutput",
  "database.hostname": "${POSTGRES_HOST}",
  "database.port": "${POSTGRES_PORT_INTERNAL}",
  "database.user": "${POSTGRES_USER_NAME}",
  "database.password": "${POSTGRES_PASSWORD_VALUE}",
  "database.dbname": "${POSTGRES_DB_NAME}",
  "topic.prefix": "astor.postgres",
  "slot.name": "astor_outbox_slot",
  "publication.name": "astor_outbox_publication",
  "table.include.list": "public.outbox_events",
  "tombstones.on.delete": "false",
  "snapshot.mode": "initial",
  "transforms": "outbox",
  "transforms.outbox.type": "io.debezium.transforms.outbox.EventRouter",
  "transforms.outbox.route.by.field": "aggregatetype",
  "transforms.outbox.route.topic.replacement": "\${routedByValue}",
  "transforms.outbox.table.field.event.id": "id",
  "transforms.outbox.table.field.event.key": "aggregateid",
  "transforms.outbox.table.field.event.type": "type",
  "transforms.outbox.table.field.event.payload": "payload",
  "transforms.outbox.table.expand.json.payload": "true"
}
JSON

echo
echo "Connector status:"
curl -fsS "${CONNECT_URL}/connectors/${CONNECTOR_NAME}/status"
echo
