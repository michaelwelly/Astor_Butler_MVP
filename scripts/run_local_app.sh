#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ENV_FILE="${1:-$ROOT_DIR/.env}"

if [[ ! -f "$ENV_FILE" ]]; then
  echo "Missing $ENV_FILE. Create local .env with database and service credentials before starting the app."
  exit 1
fi

set -a
# shellcheck disable=SC1090
source "$ENV_FILE"
set +a

: "${POSTGRES_URL:?POSTGRES_URL is required in .env}"
: "${POSTGRES_USER:?POSTGRES_USER is required in .env}"
: "${POSTGRES_PASSWORD:?POSTGRES_PASSWORD is required in .env}"
: "${REDIS_PORT:?REDIS_PORT is required in .env}"
: "${MONGODB_URI:?MONGODB_URI is required in .env}"
: "${S3_ENDPOINT:?S3_ENDPOINT is required in .env}"
: "${S3_PUBLIC_ENDPOINT:?S3_PUBLIC_ENDPOINT is required in .env}"
: "${S3_ACCESS_KEY:?S3_ACCESS_KEY is required in .env}"
: "${S3_SECRET_KEY:?S3_SECRET_KEY is required in .env}"
: "${S3_BUCKET_MEDIA:?S3_BUCKET_MEDIA is required in .env}"
: "${S3_BUCKET_DOCUMENTS:?S3_BUCKET_DOCUMENTS is required in .env}"

unset SPRING_PROFILES_ACTIVE
export SERVER_PORT="${SERVER_PORT:-8088}"

export REDIS_HOST="localhost"
export REDIS_PASSWORD="${REDIS_PASSWORD:-}"

export KAFKA_PORT="${KAFKA_PORT:-9092}"
export KAFKA_BOOTSTRAP_SERVERS="localhost:${KAFKA_PORT}"

export OLLAMA_PORT="${OLLAMA_PORT:-11434}"
export LLM_OLLAMA_BASE_URL="${LLM_OLLAMA_BASE_URL:-http://localhost:${OLLAMA_PORT}}"

export JAVA_HOME="${ASTOR_JAVA_HOME:-/Users/michaelwelly/Library/Java/JavaVirtualMachines/jbrsdk_jcef-21.0.10/Contents/Home}"
export PATH="${JAVA_HOME}/bin:${PATH}"

cd "$ROOT_DIR"
mvn spring-boot:run
