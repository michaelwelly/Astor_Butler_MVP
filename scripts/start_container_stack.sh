#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
AERIS_MENU_SOURCE_DIR="${AERIS_MENU_SOURCE_DIR:-/Users/michaelwelly/Desktop/AERISMENU}"

cd "$ROOT_DIR"

export ASTOR_BACKEND_UPSTREAM="${ASTOR_BACKEND_UPSTREAM:-app:8088}"
export APP_LLM_OLLAMA_BASE_URL="${APP_LLM_OLLAMA_BASE_URL:-http://llm-gateway:11434}"
export APP_TELEGRAM_REDIRECT_URI="${APP_TELEGRAM_REDIRECT_URI:-http://localhost:8080/auth/telegram/callback}"
export LLM_WARMUP_ENABLED="${LLM_WARMUP_ENABLED:-true}"
export LLM_CONNECT_TIMEOUT_MS="${LLM_CONNECT_TIMEOUT_MS:-5000}"
export LLM_READ_TIMEOUT_MS="${LLM_READ_TIMEOUT_MS:-45000}"
export LLM_OLLAMA_KEEP_ALIVE="${LLM_OLLAMA_KEEP_ALIVE:-30m}"

docker rm -f astor_ollama_test >/dev/null 2>&1 || true

docker compose --profile ai up -d \
  ollama-1 \
  ollama-2 \
  ollama-3 \
  llm-gateway

echo "Waiting for Ollama pool health..."
for _ in {1..30}; do
  if [[ "$(docker inspect -f '{{.State.Health.Status}}' astor_ollama_1 2>/dev/null || true)" == "healthy" ]] \
    && [[ "$(docker inspect -f '{{.State.Health.Status}}' astor_ollama_2 2>/dev/null || true)" == "healthy" ]] \
    && [[ "$(docker inspect -f '{{.State.Health.Status}}' astor_ollama_3 2>/dev/null || true)" == "healthy" ]] \
    && [[ "$(docker inspect -f '{{.State.Health.Status}}' astor_llm_gateway 2>/dev/null || true)" == "healthy" ]]; then
    break
  fi
  sleep 2
done

if [[ "$(docker inspect -f '{{.State.Health.Status}}' astor_llm_gateway 2>/dev/null || true)" != "healthy" ]]; then
  echo "LLM gateway did not become healthy in time. Check: docker logs astor_llm_gateway and docker logs astor_ollama_1" >&2
  exit 1
fi

docker compose --profile app --profile ai up -d --build \
  postgres \
  redis \
  mongo \
  kafka \
  redpanda-console \
  minio \
  minio-init \
  prometheus \
  grafana \
  app \
  api-gateway

if [[ -d "$AERIS_MENU_SOURCE_DIR" ]]; then
  scripts/ingest_aeris_menu_assets.sh "$AERIS_MENU_SOURCE_DIR" \
    || echo "AERIS menu ingest failed; container stack remains running. Check MinIO credentials before retrying." >&2
else
  echo "AERIS menu folder not found, content ingest skipped: $AERIS_MENU_SOURCE_DIR" >&2
  echo "Set AERIS_MENU_SOURCE_DIR=/path/to/AERISMENU and rerun scripts/ingest_aeris_menu_assets.sh if needed." >&2
fi

echo "Container stack is ready."
echo "- App health via gateway: http://localhost:8080/actuator/health"
echo "- Swagger via gateway: http://localhost:8080/swagger-ui/index.html"
echo "- Redpanda Console: http://localhost:8081"
echo "- MinIO Console: http://localhost:9001"
echo "- Ollama load balancer: http://localhost:11434"
