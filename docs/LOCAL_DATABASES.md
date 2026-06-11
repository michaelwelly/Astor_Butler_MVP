# Local Databases

This file is the quick local map for Astor Butler databases.

## Status Check

```bash
docker compose ps postgres mongo scylla neo4j redis kafka
docker stats --no-stream
```

## PostgreSQL + pgvector

Purpose:

- canonical relational data;
- bookings, users, orders, scenario records;
- semantic/RAG tables through `pgvector`.

Local endpoint:

```text
host: localhost
port: 5434
database: aether
user/password: from .env POSTGRES_USER / POSTGRES_PASSWORD
```

Quick check:

```bash
docker exec astor_postgres_test psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c "SELECT extversion FROM pg_extension WHERE extname='vector';"
```

In IntelliJ IDEA Database:

- add PostgreSQL data source;
- host `localhost`;
- port `5434`;
- database/user/password from `.env`.

## MongoDB

Purpose:

- document metadata and media/catalog structures;
- flexible assets before they become strict relational entities.

Local endpoint:

```text
host: localhost
port: 27017
auth database: admin
database: aether
user/password: from .env MONGO_USER / MONGO_PASSWORD
```

The local container is resource-limited:

- memory: `MONGO_MEMORY_LIMIT`, default `1g`;
- CPU: `MONGO_CPUS`, default `1.0`;
- WiredTiger cache: `MONGO_WIREDTIGER_CACHE_GB`, default `0.5`.

Quick check:

```bash
docker exec astor_mongo_test mongosh --quiet -u "$MONGO_USER" -p "$MONGO_PASSWORD" --authenticationDatabase admin --eval 'db.adminCommand({ ping: 1 })'
```

Expected local log noise:

- `WTCHKPT` once in a while is WiredTiger checkpointing and is normal;
- healthcheck does not open MongoDB client connections, it only checks that `mongod` is running;
- repeated `mongosh` auth/connect bursts usually mean a manual shell, IDE probe, or an old container config.

In IntelliJ IDEA Database:

- add MongoDB data source;
- host `localhost`;
- port `27017`;
- auth database `admin`;
- database/user/password from `.env`.

## ScyllaDB

Purpose:

- Cassandra-compatible append-only timeline;
- FSM/user state history by guest;
- event trail for analytics and future prediction.

Local endpoint:

```text
host: localhost
port: 9042
datacenter: datacenter1
keyspace: astor_timeline
```

Quick check:

```bash
docker exec astor_scylla_timeline sh -lc 'cqlsh $(hostname -i | cut -d" " -f1) 9042 -e "DESCRIBE KEYSPACES"'
```

In IntelliJ IDEA Database:

- use Cassandra/ScyllaDB driver if available;
- host `localhost`;
- port `9042`;
- datacenter `datacenter1`;
- auth is not enabled for local dev.

Useful CQL:

```sql
DESCRIBE KEYSPACES;
USE astor_timeline;
DESCRIBE TABLES;
SELECT * FROM fsm_timeline_by_user LIMIT 10;
```

Resource guard:

- container memory: `SCYLLA_MEMORY_LIMIT`, default `1g`;
- container CPU: `SCYLLA_CPUS`, default `1.0`;
- Scylla engine memory: `SCYLLA_ENGINE_MEMORY`, default `384M`.

## Neo4j

Purpose:

- graph of FSM states, scenarios, capabilities and transitions;
- visual/system reasoning layer;
- later: guest journey graph and recommendation paths.

Ports:

```text
7474 - Neo4j Browser web UI
7687 - Bolt driver for apps/tools
```

Open:

```text
http://localhost:7474
```

Login:

```text
user: neo4j
password: from .env NEO4J_PASSWORD, default astor_graph_password
```

Quick query:

```cypher
MATCH (n) RETURN labels(n) AS labels, count(*) AS count ORDER BY count DESC;
```

Useful Cypher:

```cypher
MATCH p=(:Scenario)-[*1..3]-(:FsmState) RETURN p LIMIT 25;
MATCH (s:FsmState)-[r]->(n) RETURN s,r,n LIMIT 50;
```

In IntelliJ IDEA Database:

- add Neo4j data source/plugin if available;
- HTTP UI is easier for visual work: `http://localhost:7474`;
- Bolt connection: `bolt://localhost:7687`;
- user/password from `.env`.

Resource guard:

- memory: `NEO4J_MEMORY_LIMIT`, default `1g`;
- CPU: `NEO4J_CPUS`, default `1.0`;
- heap max: `NEO4J_HEAP_MAX`, default `512m`;
- page cache: `NEO4J_PAGECACHE`, default `256m`.

## Redis

Purpose:

- hot state/cache;
- idempotency and short-lived runtime locks;
- fast runtime pointers, not permanent truth.

Local endpoint:

```text
host: localhost
port: 6379
```

## Kafka / Redpanda

Purpose:

- event bus;
- observability stream;
- future integration stream for analytics and external systems.

Local endpoint:

```text
host: localhost
port: 9092
```

Redpanda Console:

```text
http://localhost:8081
```

## Rule Of Thumb

- PostgreSQL answers: "what is true now?"
- pgvector answers: "what known context is semantically close?"
- Scylla answers: "what happened to this guest over time?"
- Neo4j answers: "what paths and relationships exist?"
- Redis answers: "what must be fast right now?"
- Kafka answers: "what happened and who should react?"
