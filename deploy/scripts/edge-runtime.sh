#!/usr/bin/env bash
set -Eeuo pipefail

edge_initialize() {
    ROOT="${CHONGCHONG_APP_ROOT:-/opt/chongchong}"
    STATE="$ROOT/edge-state"
    UPSTREAM="$ROOT/edge-routing/upstream.conf"
    install -d -m 700 "$STATE"
    exec 9>"$STATE/deploy.lock"
    flock -n 9 || { echo 'Another edge operation is running' >&2; return 1; }
    source "$ROOT/config/edge.env"
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
    local address="$1" directory="$2"
    curl -fsS --connect-timeout 2 --max-time 5 -D "$directory/headers" \
        --resolve "$SERVER_NAME:443:127.0.0.1" \
        "https://$SERVER_NAME/actuator/health" > "$directory/health.json" &&
        grep -Fqi "X-Deploy-Backend: $address:8080" "$directory/headers" &&
        python3 -c 'import json,sys; sys.exit(json.load(sys.stdin).get("status") != "UP")' < "$directory/health.json"
}

replace_upstream() {
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

verify_edge_wait() {
    local attempt
    for attempt in {1..10}; do
        if verify_edge "$1" "$2"; then return 0; fi
        sleep 2
    done
    return 1
}
