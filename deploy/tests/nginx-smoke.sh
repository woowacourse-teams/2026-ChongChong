#!/usr/bin/env bash
set -Eeuo pipefail
ROOT="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
TEMP="$(mktemp -d)"
NAME="chongchong-nginx-check-$$"
cleanup() {
    docker rm -f "$NAME" "${NAME}-backend" >/dev/null 2>&1 || true
    docker network rm "$NAME" >/dev/null 2>&1 || true
}
trap cleanup EXIT
mkdir -p "$TEMP/certs/live/localhost"
openssl req -x509 -newkey rsa:2048 -nodes -days 1 -subj /CN=localhost \
    -keyout "$TEMP/certs/live/localhost/privkey.pem" \
    -out "$TEMP/certs/live/localhost/fullchain.pem" 2>/dev/null
docker network create "$NAME" >/dev/null
docker run --detach --name "${NAME}-backend" --network "$NAME" --network-alias backend \
    python:3.13-alpine python -m http.server 8080 >/dev/null

source "$ROOT/scripts/runtime.sh"
export CLOUD_WATCH_APP_ENV=dev
for attempt in 1 2; do
    docker run --detach --name "$NAME" --network "$NAME" \
        --ulimit nofile=8192:8192 -p 127.0.0.1::443 \
        -e SERVER_NAME=localhost -e NGINX_API_LOG_FORMAT=loadtest \
        -e 'NGINX_ENVSUBST_FILTER=^(SERVER_NAME|NGINX_API_LOG_FORMAT)$' \
        -v "$ROOT/nginx/nginx.dev.conf:/etc/nginx/nginx.conf:ro" \
        -v "$ROOT/nginx/default.conf.template:/etc/nginx/templates/default.conf.template:ro" \
        -v "$TEMP/certs:/etc/letsencrypt:ro" nginx:1.28-alpine >/dev/null
    port="$(docker port "$NAME" 443/tcp | cut -d: -f2)"
    ready=false
    for wait in {1..20}; do
        if curl -ksS --output /dev/null "https://127.0.0.1:$port/"; then
            ready=true
            break
        fi
        sleep 0.2
    done
    [[ "$ready" == true ]]
    verify_nginx_limits "$NAME"
    curl -ksS --output /dev/null -H "X-Request-Id: smoke-$attempt" \
        -H 'Authorization: Bearer must-not-log' \
        "https://127.0.0.1:$port/api/check?secret=must-not-log"
    docker logs "$NAME" > "$TEMP/log-$attempt.txt" 2>&1
    python3 - "$TEMP/log-$attempt.txt" "smoke-$attempt" <<'PY'
import json
import sys
rows = []
with open(sys.argv[1]) as source:
    for line in source:
        try:
            row = json.loads(line)
        except json.JSONDecodeError:
            continue
        if row.get('request_id') == sys.argv[2]:
            rows.append(row)
assert len(rows) == 1, rows
row = rows[0]
assert row['uri'] == '/api/check', row
assert row['status'] == row['upstream_status'] == '404', row
assert row['upstream_addr'] and float(row['upstream_response_time']) >= 0, row
assert 'must-not-log' not in json.dumps(row), row
PY
    docker rm -f "$NAME" >/dev/null
done
docker run --detach --name "$NAME" --network "$NAME" \
    -p 127.0.0.1::443 -e SERVER_NAME=localhost -e NGINX_API_LOG_FORMAT=main \
    -e 'NGINX_ENVSUBST_FILTER=^(SERVER_NAME|NGINX_API_LOG_FORMAT)$' \
    -v "$ROOT/nginx/default.conf.template:/etc/nginx/templates/default.conf.template:ro" \
    -v "$TEMP/certs:/etc/letsencrypt:ro" nginx:1.28-alpine >/dev/null
port="$(docker port "$NAME" 443/tcp | cut -d: -f2)"
ready=false
for wait in {1..20}; do
    if curl -ksS --output /dev/null "https://127.0.0.1:$port/api/prod-check"; then
        ready=true
        break
    fi
    sleep 0.2
done
[[ "$ready" == true ]]
docker exec "$NAME" nginx -t
docker logs "$NAME" > "$TEMP/log-prod.txt" 2>&1
grep -Eq '"GET /api/prod-check HTTP/[0-9.]+" 404 ' "$TEMP/log-prod.txt"
echo "PASS: syntax, worker limits, JSON proxy log, runtime variables, recreation ($TEMP)"
