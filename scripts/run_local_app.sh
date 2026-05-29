#!/usr/bin/env bash
set -euo pipefail

if [[ -f .env ]]; then
  set -a
  # shellcheck disable=SC1091
  source .env
  set +a
fi

export SPRING_PROFILES_ACTIVE="${SPRING_PROFILES_ACTIVE:-test}"
export SERVER_PORT="${SERVER_PORT:-8080}"

export POSTGRES_PORT="${POSTGRES_PORT:-5434}"
export POSTGRES_DB="${POSTGRES_DB:-astor_butler_test}"
export POSTGRES_USER="${POSTGRES_USER:-astor}"
export POSTGRES_PASSWORD="${POSTGRES_PASSWORD:-astor}"
export POSTGRES_URL="jdbc:postgresql://localhost:${POSTGRES_PORT}/${POSTGRES_DB}"

export REDIS_HOST="localhost"
export REDIS_PORT="${REDIS_PORT:-6379}"
export REDIS_PASSWORD="${REDIS_PASSWORD:-}"

export MONGO_PORT="${MONGO_PORT:-27017}"
export MONGO_DB="${MONGO_DB:-astor_butler_documents_test}"
export MONGODB_URI="mongodb://localhost:${MONGO_PORT}/${MONGO_DB}"

export KAFKA_PORT="${KAFKA_PORT:-9092}"
export KAFKA_BOOTSTRAP_SERVERS="localhost:${KAFKA_PORT}"

export OLLAMA_PORT="${OLLAMA_PORT:-11434}"
export OLLAMA_BASE_URL="http://localhost:${OLLAMA_PORT}"

export S3_PORT="${S3_PORT:-9000}"
export S3_ENDPOINT="http://localhost:${S3_PORT}"
export S3_PUBLIC_ENDPOINT="http://localhost:${S3_PORT}"
export S3_ACCESS_KEY="${S3_ACCESS_KEY:-astor}"
export S3_SECRET_KEY="${S3_SECRET_KEY:-astor_minio_password}"
export S3_BUCKET_MEDIA="${S3_BUCKET_MEDIA:-astor-media}"
export S3_BUCKET_DOCUMENTS="${S3_BUCKET_DOCUMENTS:-astor-documents}"

export JAVA_HOME="${ASTOR_JAVA_HOME:-/Users/michaelwelly/Library/Java/JavaVirtualMachines/jbrsdk_jcef-21.0.10/Contents/Home}"
export PATH="${JAVA_HOME}/bin:${PATH}"

mvn spring-boot:run
