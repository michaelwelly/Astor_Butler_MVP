#!/usr/bin/env bash
set -euo pipefail

if [[ $# -lt 1 ]]; then
  echo "Usage: scripts/import_media_manifest_to_mongo.sh <manifest-jsonl> [database] [collection]" >&2
  exit 2
fi

MANIFEST="$(cd "$(dirname "$1")" && pwd)/$(basename "$1")"
DATABASE="${2:-${MONGO_DB:-astor_butler_documents_test}}"
COLLECTION="${3:-media_assets}"
CONTAINER="${MONGO_CONTAINER:-astor_mongo_test}"

docker compose up -d mongo
docker cp "${MANIFEST}" "${CONTAINER}:/tmp/astor_media_manifest.jsonl"
docker compose exec -T mongo mongoimport \
  --db "${DATABASE}" \
  --collection "${COLLECTION}" \
  --file /tmp/astor_media_manifest.jsonl \
  --mode upsert \
  --upsertFields _id

docker compose exec -T mongo mongosh "${DATABASE}" --quiet --eval "
db.${COLLECTION}.createIndex({sourcePath:1},{unique:true});
db.${COLLECTION}.createIndex({bucket:1,objectKey:1},{unique:true});
db.${COLLECTION}.createIndex({mediaType:1,tags:1});
db.${COLLECTION}.createIndex({sha256:1});
printjson({collection:'${COLLECTION}', count: db.${COLLECTION}.countDocuments()});
"
