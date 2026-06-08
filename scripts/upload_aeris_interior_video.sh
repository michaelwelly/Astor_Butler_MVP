#!/usr/bin/env bash
set -euo pipefail

SOURCE="${1:-/Users/michaelwelly/Desktop/AERISMENU/INTERIOR.mp4}"
BUCKET="${S3_BUCKET_MEDIA:-astor-media}"
OBJECT_KEY="${AERIS_INTERIOR_VIDEO_OBJECT_KEY:-content/aeris/interior/INTERIOR.mp4}"
MINIO_CONTAINER="${MINIO_CONTAINER:-astor_minio_test}"
MC_IMAGE="${MC_IMAGE:-minio/mc:latest}"
ALIAS_NAME="astor"
ENDPOINT="${S3_ENDPOINT_FOR_MC:-http://minio:9000}"
ACCESS_KEY="${S3_ACCESS_KEY:-}"
SECRET_KEY="${S3_SECRET_KEY:-}"

if [[ ! -f "$SOURCE" ]]; then
  echo "Interior video not found: $SOURCE" >&2
  exit 1
fi

if docker ps --format '{{.Names}}' | grep -qx "$MINIO_CONTAINER"; then
  ACCESS_KEY="${ACCESS_KEY:-$(docker inspect "$MINIO_CONTAINER" --format '{{range .Config.Env}}{{println .}}{{end}}' | sed -n 's/^MINIO_ROOT_USER=//p' | head -n1)}"
  SECRET_KEY="${SECRET_KEY:-$(docker inspect "$MINIO_CONTAINER" --format '{{range .Config.Env}}{{println .}}{{end}}' | sed -n 's/^MINIO_ROOT_PASSWORD=//p' | head -n1)}"
  docker run --rm \
    --network container:"$MINIO_CONTAINER" \
    -v "$SOURCE:/tmp/INTERIOR.mp4:ro" \
    --entrypoint /bin/sh \
    "$MC_IMAGE" \
    -c "mc alias set '$ALIAS_NAME' http://127.0.0.1:9000 '$ACCESS_KEY' '$SECRET_KEY' >/dev/null && mc mb --ignore-existing '$ALIAS_NAME/$BUCKET' >/dev/null && mc cp /tmp/INTERIOR.mp4 '$ALIAS_NAME/$BUCKET/$OBJECT_KEY'"
else
  ACCESS_KEY="${ACCESS_KEY:-astor}"
  SECRET_KEY="${SECRET_KEY:-astor_minio_password}"
  docker run --rm \
    --network astor_butler_mvp_default \
    -v "$SOURCE:/tmp/INTERIOR.mp4:ro" \
    --entrypoint /bin/sh \
    "$MC_IMAGE" \
    -c "mc alias set '$ALIAS_NAME' '$ENDPOINT' '$ACCESS_KEY' '$SECRET_KEY' >/dev/null && mc mb --ignore-existing '$ALIAS_NAME/$BUCKET' >/dev/null && mc cp /tmp/INTERIOR.mp4 '$ALIAS_NAME/$BUCKET/$OBJECT_KEY'"
fi

echo "Uploaded interior video: s3://$BUCKET/$OBJECT_KEY"
