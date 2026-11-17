#!/usr/bin/env bash
set -Eeuo pipefail
ROOT="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
TEMP="$(mktemp -d)"
NAME="chongchong-edge-check-$$"
cleanup() {
    docker rm -f "$NAME" "$NAME-a" "$NAME-b" >/dev/null 2>&1 || true
    docker network rm "$NAME" >/dev/null 2>&1 || true
}
trap cleanup EXIT
mkdir -p "$TEMP/certs/live/localhost" "$TEMP/edge-routing"
openssl req -x509 -newkey rsa:2048 -nodes -days 1 -subj /CN=localhost \
    -keyout "$TEMP/certs/live/localhost/privkey.pem" \
    -out "$TEMP/certs/live/localhost/fullchain.pem" 2>/dev/null
docker network create "$NAME" >/dev/null
for slot in a b; do
    mkdir -p "$TEMP/$slot"
    docker run -d --name "$NAME-$slot" --network "$NAME" -e SLOT="$slot" \
        -v "$TEMP/$slot:/test" python:3.13-alpine \
        python -u -c 'from http.server import BaseHTTPRequestHandler,ThreadingHTTPServer
import os,time,json,pathlib
class Handler(BaseHTTPRequestHandler):
 def do_GET(self):
  self.send_response(200); self.send_header("X-Test-Slot", os.environ["SLOT"]); self.send_header("X-Accel-Buffering", "no"); self.end_headers(); self.wfile.flush()
  if self.path == "/api/slow": time.sleep(4)
  self.wfile.write(json.dumps({"status":"UP", "slot":os.environ["SLOT"]}).encode())
 def do_POST(self):
  self.rfile.read(int(self.headers.get("Content-Length", "0")))
  with open("/test/post-count", "a") as count: count.write("1\n")
  deadline = time.monotonic() + 15
  while not pathlib.Path("/test/release-post").exists() and time.monotonic() < deadline: time.sleep(0.05)
  self.close_connection = True
ThreadingHTTPServer(("0.0.0.0",8080),Handler).serve_forever()' >/dev/null
done
address_a="$(docker inspect -f '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' "$NAME-a")"
address_b="$(docker inspect -f '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' "$NAME-b")"
printf 'upstream chongchong_backend { server %s:8080; }\n' "$address_a" > "$TEMP/edge-routing/upstream.conf"
docker run -d --name "$NAME" --network "$NAME" -p 127.0.0.1::443 \
    --ulimit nofile=8192:8192 \
    -e SERVER_NAME=localhost -e NGINX_API_LOG_FORMAT=loadtest \
    -e 'NGINX_ENVSUBST_FILTER=^(SERVER_NAME|NGINX_API_LOG_FORMAT)$' \
    -v "$ROOT/config/nginx/nginx.edge.conf:/etc/nginx/nginx.conf:ro" \
    -v "$ROOT/config/nginx/edge.conf.template:/etc/nginx/templates/default.conf.template:ro" \
    -v "$TEMP/edge-routing:/etc/nginx/upstreams:ro" \
    -v "$TEMP/certs:/etc/letsencrypt:ro" nginx:1.28-alpine >/dev/null
port="$(docker port "$NAME" 443/tcp | cut -d: -f2)"
verify() {
    for attempt in {1..30}; do
        if curl -kfsS --max-time 2 -D "$TEMP/headers" "https://127.0.0.1:$port/actuator/health" > "$TEMP/body" &&
            grep -Fqi "X-Deploy-Backend: $1:8080" "$TEMP/headers"; then return 0; fi
        sleep 0.2
    done
    return 1
}
verify "$address_a"
replace_test_upstream() (
    source "$ROOT/scripts/edge/edge-runtime.sh"
    ROOT="$TEMP"
    UPSTREAM="$ROOT/edge-routing/upstream.conf"
    docker() { command docker exec "$NAME" "${@:3}"; }
    replace_upstream "$1"
)
mkdir -p "$TEMP/bin"
cat > "$TEMP/bin/docker" <<'SH'
#!/usr/bin/env bash
set -Eeuo pipefail
if [[ "${1:-}" == exec && "${2:-}" == chongchong-nginx ]]; then
    shift 2
    exec "$REAL_DOCKER" exec "$TEST_NGINX_NAME" "$@"
fi
exec "$REAL_DOCKER" "$@"
SH
chmod +x "$TEMP/bin/docker"
replace_control_upstream() {
    REAL_DOCKER="$(command -v docker)" TEST_NGINX_NAME="$NAME" \
        PATH="$TEMP/bin:$PATH" PYTHONPATH="$ROOT/scripts/edge" \
        python3 - "$TEMP" "$address_a" "$address_b" "$1" <<'PY'
from pathlib import Path
import sys
from edge_io import Config, Operations
config = Config({"a": sys.argv[2], "b": sys.argv[3]}, "localhost", "test", "unused", "unused")
Operations(Path(sys.argv[1]), config).replace(sys.argv[4])
PY
}
cp "$TEMP/edge-routing/upstream.conf" "$TEMP/original-upstream.conf"
printf 'invalid upstream;\n' > "$TEMP/invalid-upstream.conf"
if replace_test_upstream "$TEMP/invalid-upstream.conf"; then
    echo 'Invalid upstream unexpectedly accepted' >&2
    exit 1
fi
cmp "$TEMP/original-upstream.conf" "$TEMP/edge-routing/upstream.conf"
docker restart "$NAME" >/dev/null
port="$(docker port "$NAME" 443/tcp | cut -d: -f2)"
verify "$address_a"
: > "$TEMP/slow-headers"
curl -kfsS --max-time 15 -D "$TEMP/slow-headers" "https://127.0.0.1:$port/api/slow" > "$TEMP/slow-body" &
slow_pid=$!
for attempt in {1..30}; do
    if grep -qi 'X-Test-Slot: a' "$TEMP/slow-headers"; then break; fi
    sleep 0.1
done
grep -qi 'X-Test-Slot: a' "$TEMP/slow-headers"
python3 - "$port" > "$TEMP/requests.txt" <<'PY' &
import json, ssl, sys, time, urllib.request
context = ssl._create_unverified_context()
slots = set()
for _ in range(150):
    with urllib.request.urlopen('https://127.0.0.1:' + sys.argv[1] + '/api/check', context=context, timeout=5) as response:
        assert response.status == 200
        data = json.load(response)
        assert data['status'] == 'UP'
        slots.add(data['slot'])
    time.sleep(0.03)
assert slots == {'a', 'b'}, slots
print('PASS: 150 requests, 0 failures, both slots observed')
PY
requests_pid=$!
sleep 0.3
kill -0 "$slow_pid"
printf 'upstream chongchong_backend { server %s:8080; }\n' "$address_b" > "$TEMP/candidate-upstream.conf"
replace_test_upstream "$TEMP/candidate-upstream.conf"
verify "$address_b"
wait "$slow_pid"
python3 -c 'import json,sys; assert json.load(open(sys.argv[1]))["slot"] == "a"' "$TEMP/slow-body"
wait "$requests_pid"
printf 'upstream chongchong_backend { server %s:8080; }\n' "$address_a" > "$TEMP/candidate-upstream.conf"
replace_test_upstream "$TEMP/candidate-upstream.conf"
verify "$address_a"
cat "$TEMP/requests.txt"
echo "PASS: invalid upstream restored before restart; A -> B -> A, in-flight A request completed ($TEMP)"

# Given: A accepted a state-changing request but has not returned its response.
curl -kfsS --max-time 20 -X POST --data 'submission=once' \
    "https://127.0.0.1:$port/api/submit" > "$TEMP/post-body" 2> "$TEMP/post-error" &
post_pid=$!
for attempt in {1..100}; do
    if [[ -s "$TEMP/a/post-count" ]]; then break; fi
    sleep 0.05
done
[[ -s "$TEMP/a/post-count" ]]

# When: routing changes to B and A loses its response after applying the request.
replace_control_upstream b
verify "$address_b"
touch "$TEMP/a/release-post"

# Then: the failed POST is not replayed on B; subsequent reads reach B.
if wait "$post_pid"; then
    echo 'A response-loss POST unexpectedly succeeded' >&2
    exit 1
fi
[[ "$(wc -l < "$TEMP/a/post-count" | tr -d ' ')" == 1 ]]
[[ ! -e "$TEMP/b/post-count" ]]
verify "$address_b"
echo 'PASS: accepted POST was not replayed during routing switch and response loss'
