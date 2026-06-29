#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
AERIS_MENU_SOURCE_DIR="${AERIS_MENU_SOURCE_DIR:-/Users/michaelwelly/Desktop/AERISMENU}"

cd "$ROOT_DIR"

export ASTOR_BACKEND_UPSTREAM="${ASTOR_BACKEND_UPSTREAM:-c3flex-astor-butler-bot:8088}"
export APP_LLM_OLLAMA_BASE_URL="${APP_LLM_OLLAMA_BASE_URL:-http://llm-gateway:11434}"
export APP_TELEGRAM_REDIRECT_URI="${APP_TELEGRAM_REDIRECT_URI:-http://localhost:8080/auth/telegram/callback}"
export LLM_WARMUP_ENABLED="${LLM_WARMUP_ENABLED:-true}"
export LLM_OLLAMA_FRONTLINE_MODEL="${LLM_OLLAMA_FRONTLINE_MODEL:-qwen2.5:1.5b}"
export LLM_OLLAMA_QUALITY_MODEL="${LLM_OLLAMA_QUALITY_MODEL:-qwen2.5:3b}"
export LLM_OLLAMA_VISION_MODEL="${LLM_OLLAMA_VISION_MODEL:-qwen2.5vl:3b}"
export LLM_OLLAMA_MODEL="${LLM_OLLAMA_MODEL:-$LLM_OLLAMA_FRONTLINE_MODEL}"
export ASTOR_SEMANTIC_EMBEDDINGS_PROVIDER="${ASTOR_SEMANTIC_EMBEDDINGS_PROVIDER:-model-gateway}"
export ASTOR_SEMANTIC_EMBEDDING_MODEL="${ASTOR_SEMANTIC_EMBEDDING_MODEL:-nomic-embed-text}"
export ASTOR_INTENT_EXAMPLES_INGEST_ON_STARTUP="${ASTOR_INTENT_EXAMPLES_INGEST_ON_STARTUP:-true}"
export LLM_CONNECT_TIMEOUT_MS="${LLM_CONNECT_TIMEOUT_MS:-5000}"
export LLM_READ_TIMEOUT_MS="${LLM_READ_TIMEOUT_MS:-45000}"
export LLM_OLLAMA_KEEP_ALIVE="${LLM_OLLAMA_KEEP_ALIVE:-30m}"
export ASTOR_NLU_STACK_ENABLED="${ASTOR_NLU_STACK_ENABLED:-true}"
export ASTOR_NLU_DUCKLING_ENABLED="${ASTOR_NLU_DUCKLING_ENABLED:-false}"

if [[ "$ASTOR_NLU_STACK_ENABLED" == "true" ]]; then
  export ASTOR_NLU_NATASHA_ENABLED="${ASTOR_NLU_NATASHA_ENABLED:-true}"
  export ASTOR_NLU_NATASHA_URL="${ASTOR_NLU_NATASHA_URL:-http://natasha-nlu:8011/analyze}"
else
  export ASTOR_NLU_NATASHA_ENABLED="${ASTOR_NLU_NATASHA_ENABLED:-false}"
fi

docker rm -f astor_ollama_test >/dev/null 2>&1 || true

if [[ "$ASTOR_NLU_STACK_ENABLED" == "true" ]]; then
  docker compose --profile nlu up -d --build \
    natasha-nlu

  echo "Waiting for Natasha Russian NLU health..."
  for _ in {1..30}; do
    if [[ "$(docker inspect -f '{{.State.Health.Status}}' astor_natasha_nlu 2>/dev/null || true)" == "healthy" ]]; then
      break
    fi
    sleep 2
  done

  if [[ "$(docker inspect -f '{{.State.Health.Status}}' astor_natasha_nlu 2>/dev/null || true)" != "healthy" ]]; then
    echo "Natasha NLU did not become healthy in time. Check: docker logs astor_natasha_nlu" >&2
    exit 1
  fi
fi

docker compose --profile ai up -d \
  ollama-1 \
  ollama-2 \
  llm-gateway

echo "Waiting for Ollama pool health..."
for _ in {1..30}; do
  if [[ "$(docker inspect -f '{{.State.Health.Status}}' astor_ollama_1 2>/dev/null || true)" == "healthy" ]] \
    && [[ "$(docker inspect -f '{{.State.Health.Status}}' astor_ollama_2 2>/dev/null || true)" == "healthy" ]] \
    && [[ "$(docker inspect -f '{{.State.Health.Status}}' astor_llm_gateway 2>/dev/null || true)" == "healthy" ]]; then
    break
  fi
  sleep 2
done

for model_name in "$LLM_OLLAMA_FRONTLINE_MODEL" "$LLM_OLLAMA_QUALITY_MODEL" "$ASTOR_SEMANTIC_EMBEDDING_MODEL" "$LLM_OLLAMA_VISION_MODEL"; do
  if ! docker exec astor_ollama_1 ollama list | awk '{print $1}' | grep -qx "$model_name"; then
    echo "Pulling local LLM model: $model_name"
    docker exec astor_ollama_1 ollama pull "$model_name"
  fi
done

if [[ "$(docker inspect -f '{{.State.Health.Status}}' astor_llm_gateway 2>/dev/null || true)" != "healthy" ]]; then
  echo "LLM gateway did not become healthy in time. Check: docker logs astor_llm_gateway and docker logs astor_ollama_1" >&2
  exit 1
fi

docker compose --profile app --profile telegram --profile ai up -d --build \
  postgres \
  redis \
  mongo \
  kafka \
  redpanda-console \
  minio \
  minio-init \
  prometheus \
  grafana \
  c3flex-astor-butler-bot \
  aeris-astor-butler-bot \
  api-gateway

if [[ -d "$AERIS_MENU_SOURCE_DIR" ]]; then
  scripts/ingest_aeris_runtime_assets.sh "$AERIS_MENU_SOURCE_DIR" \
    || echo "AERIS runtime asset ingest failed; container stack remains running. Check MinIO/Postgres credentials before retrying." >&2
  scripts/ingest_aeris_menu_assets.sh "$AERIS_MENU_SOURCE_DIR" \
    || echo "AERIS inventory ingest failed; runtime assets may still be ready. Check Mongo credentials before retrying." >&2
else
  echo "AERIS menu folder not found, content ingest skipped: $AERIS_MENU_SOURCE_DIR" >&2
  echo "Set AERIS_MENU_SOURCE_DIR=/path/to/AERISMENU and rerun scripts/ingest_aeris_runtime_assets.sh if needed." >&2
fi

echo "Container stack is ready."
echo "- App health via gateway: http://localhost:8080/actuator/health"
echo "- Swagger via gateway: http://localhost:8080/swagger-ui/index.html"
echo "- Redpanda Console: http://localhost:8081"
echo "- MinIO Console: http://localhost:9001"
echo "- Ollama load balancer: http://localhost:11434"
if [[ "$ASTOR_NLU_STACK_ENABLED" == "true" ]]; then
  echo "- Natasha NLU: http://localhost:${NATASHA_NLU_PORT:-8011}/health"
fi
