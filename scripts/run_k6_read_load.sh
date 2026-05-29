#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
export K6_BASE_URL="${K6_BASE_URL:-http://localhost:8080}"

cd "$ROOT_DIR"
exec k6 run k6/read-api-load.js
