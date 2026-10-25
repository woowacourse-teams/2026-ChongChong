#!/usr/bin/env bash
# edge 배포·롤백에서 공유하는 잠금, 라우팅 검증, Nginx 교체와 사본 정리 함수.
set -Eeuo pipefail

edge_initialize() {
    ROOT="${CHONGCHONG_APP_ROOT:-/opt/chongchong}"
    STATE="$ROOT/edge-state"
    UPSTREAM="$ROOT/edge-routing/upstream.conf"
    install -d -m 700 "$STATE"
    exec 9>"$STATE/deploy.lock"
    # 배포와 수동 롤백이 같은 잠금을 쓴다. CodeDeploy 파일 복사는 이 잠금의 범위 밖이다.
    flock -n 9 || { echo 'Another edge operation is running' >&2; return 1; }
    source "$ROOT/config/edge.env"
    EDGE_DEPLOYMENT_RETENTION_COUNT="${EDGE_DEPLOYMENT_RETENTION_COUNT:-3}"
    [[ "$EDGE_DEPLOYMENT_RETENTION_COUNT" =~ ^[1-9][0-9]*$ ]] || {
        echo 'EDGE_DEPLOYMENT_RETENTION_COUNT must be a positive integer' >&2; return 1;
    }
    for key in BACKEND_A_HOST BACKEND_B_HOST SSH_USER SSH_KEY SSH_KNOWN_HOSTS SERVER_NAME; do
        [[ -n "${!key:-}" ]] || { echo "Missing $key" >&2; return 1; }
    done
    for host in "$BACKEND_A_HOST" "$BACKEND_B_HOST"; do
        [[ "$host" =~ ^[0-9]+\.[0-9]+\.[0-9]+\.[0-9]+$ ]] || return 1
    done
    [[ "$BACKEND_A_HOST" != "$BACKEND_B_HOST" ]]
    [[ "$SSH_USER" =~ ^[a-z_][a-z0-9_-]*$ ]]
    [[ "$SERVER_NAME" =~ ^[A-Za-z0-9.-]+$ ]]
    [[ -r "$SSH_KEY" && -r "$SSH_KNOWN_HOSTS" ]]
}

upstream_host() {
    local content
    content="$(cat "$1")"
    case "$content" in
        "upstream chongchong_backend { server $BACKEND_A_HOST:8080; }") printf '%s\n' "$BACKEND_A_HOST" ;;
        "upstream chongchong_backend { server $BACKEND_B_HOST:8080; }") printf '%s\n' "$BACKEND_B_HOST" ;;
        *) echo 'Unknown upstream configuration; refusing to deploy' >&2; return 1 ;;
    esac
}

ready() {
    local address="$1" output="$2"
    curl -fsS --connect-timeout 2 --max-time 3 "http://$address:8080/actuator/health/readiness" > "$output" &&
        python3 -c 'import json,sys; sys.exit(json.load(sys.stdin).get("status") != "UP")' < "$output"
}

verify_edge() {
    # upstream 파일뿐 아니라 Nginx가 실제로 연결한 주소와 앱 상태를 함께 검증한다.
    local address="$1" directory="$2"
    curl -fsS --connect-timeout 2 --max-time 5 -D "$directory/headers" \
        --resolve "$SERVER_NAME:443:127.0.0.1" \
        "https://$SERVER_NAME/actuator/health" > "$directory/health.json" &&
        grep -Fqi "X-Deploy-Backend: $address:8080" "$directory/headers" &&
        python3 -c 'import json,sys; sys.exit(json.load(sys.stdin).get("status") != "UP")' < "$directory/health.json"
}

replace_upstream() {
    # rename을 반영하려면 Nginx에 파일 하나가 아닌 edge-routing 디렉터리를 마운트해야 한다.
    local backup next="$ROOT/edge-routing/upstream.next"
    backup="$(mktemp "$ROOT/edge-routing/upstream.backup.XXXXXX")" || return 1
    if ! cp -p "$UPSTREAM" "$backup" || ! cp "$1" "$next" || ! mv "$next" "$UPSTREAM"; then
        rm -f -- "$backup" "$next"
        return 1
    fi
    if docker exec chongchong-nginx nginx -t && docker exec chongchong-nginx nginx -s reload; then
        rm -f -- "$backup"
        return 0
    fi
    if ! mv "$backup" "$UPSTREAM"; then
        echo "URGENT: upstream restoration failed; original configuration remains at $backup" >&2
    fi
    return 1
}

prune_edge_deployments() {
    # pending 중에는 모든 복구 자료를 남긴다. 마지막 성공 사본은 보관 개수에 포함해 보호한다.
    python3 - "$STATE" "$EDGE_DEPLOYMENT_RETENTION_COUNT" <<'PY'
import pathlib
import shutil
import sys

state = pathlib.Path(sys.argv[1]).resolve()
limit = int(sys.argv[2])
if (state / 'pending-upstream.conf').exists():
    sys.exit(0)
marker = state / 'last-successful-deployment'
current = pathlib.Path(marker.read_text().strip()).resolve() if marker.exists() else None
snapshots = sorted(
    (path for path in state.glob('deployment.*') if not path.is_symlink() and path.is_dir()),
    key=lambda path: (path.stat().st_mtime_ns, path.name), reverse=True)
retained = {current} if current in snapshots else set()
for snapshot in snapshots:
    if snapshot in retained:
        continue
    if len(retained) < limit:
        retained.add(snapshot)
    else:
        shutil.rmtree(snapshot)
PY
}

verify_edge_wait() {
    local attempt
    for attempt in {1..10}; do
        if verify_edge "$1" "$2"; then return 0; fi
        sleep 2
    done
    return 1
}
