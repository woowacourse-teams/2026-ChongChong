#!/usr/bin/env bash
# legacy 단일 서버용 환경 로딩·Compose·검증 함수. edge 경로는 edge-runtime.sh를 사용한다.

set -Eeuo pipefail

readonly APP_ROOT="${CHONGCHONG_APP_ROOT:-/opt/chongchong}"
readonly APP_ENV="${APP_ROOT}/.env"
readonly DEPLOY_ROOT="${APP_ROOT}/deploy"
readonly IMAGE_ENV="${DEPLOY_ROOT}/image.env"
readonly COMPOSE_FILE="${DEPLOY_ROOT}/docker-compose.yml"

load_deployment_environment() {
    if [[ ! -f "${APP_ENV}" ]]; then
        echo "Missing ${APP_ENV}" >&2
        return 1
    fi

    if [[ ! -f "${IMAGE_ENV}" ]]; then
        echo "Missing ${IMAGE_ENV}" >&2
        return 1
    fi

    set -a
    source "${APP_ENV}"
    source "${IMAGE_ENV}"
    set +a

    local required_variables=(
        BACKEND_IMAGE
        BACKEND_IMAGE_TAG
        SERVER_NAME
        DOCKERHUB_USERNAME
        DOCKERHUB_TOKEN
        DB_URL
        DB_USERNAME
        DB_PASSWORD
        STUDY_INVITE_JWT_SECRET
        FRONTEND_BASE_URL
        AUTH_JWT_ISSUER
        AUTH_JWT_AUDIENCE
        AUTH_JWT_SECRET_BASE64
        AUTH_KAKAO_REST_API_KEY
        AUTH_KAKAO_CLIENT_SECRET
        AUTH_KAKAO_REDIRECT_URI
        WEB_PUSH_NOTIFICATION_CLICK_TOKEN_SECRET
    )

    local variable
    for variable in "${required_variables[@]}"; do
        if [[ -z "${!variable:-}" ]]; then
            echo "Missing required variable: ${variable}" >&2
            return 1
        fi
    done

    if [[ "${CLOUD_WATCH_APP_ENV:-}" == "dev" ]] || [[ ",${COMPOSE_PROFILES:-}," == *,metrics,* ]]; then
        local metrics_variables=(CLOUD_WATCH_AWS_REGION CLOUD_WATCH_APP_ENV)
        for variable in "${metrics_variables[@]}"; do
            if [[ -z "${!variable:-}" ]]; then
                echo "Missing required metrics variable: ${variable}" >&2
                return 1
            fi
        done
        if [[ "${CLOUD_WATCH_APP_ENV}" != "dev" && "${CLOUD_WATCH_APP_ENV}" != "prod" ]]; then
            echo "CLOUD_WATCH_APP_ENV must be dev or prod when metrics are enabled" >&2
            return 1
        fi
        if [[ ! "${CLOUD_WATCH_AWS_REGION}" =~ ^[a-z]{2}-[a-z]+-[0-9]+$ ]]; then
            echo "CLOUD_WATCH_AWS_REGION must be an AWS region when metrics are enabled" >&2
            return 1
        fi
    fi
}

compose() {
    local compose_args=(--env-file "${APP_ENV}" --env-file "${IMAGE_ENV}" --file "${COMPOSE_FILE}")
    if [[ "${CLOUD_WATCH_APP_ENV:-}" == "dev" ]]; then
        compose_args+=(--file "${DEPLOY_ROOT}/docker-compose.dev.yml")
    fi
    docker compose "${compose_args[@]}" "$@"
}

verify_nginx_limits() {
    local container="${1:-chongchong-nginx}"
    docker exec "$container" nginx -t || return 1
    if [[ "${CLOUD_WATCH_APP_ENV:-}" != "dev" ]]; then
        return 0
    fi
    docker exec "$container" sh -ec '
        nginx -T 2>&1 | grep -Eq "worker_connections[[:space:]]+4096;"
        workers=0
        for p in /proc/[0-9]*; do
            [ "$(cat "$p/comm" 2>/dev/null)" = nginx ] || continue
            [ "${p##*/}" = 1 ] && continue
            workers=$((workers + 1))
            awk '\''$1 == "Max" && $2 == "open" && $3 == "files" { found=1; if ($4 != 8192 || $5 != 8192) exit 1 } END { if (!found) exit 1 }'\'' "$p/limits" || exit 1
        done
        [ "$workers" -gt 0 ]
    '
}

verify_metrics_collector() {
    if [[ "${CLOUD_WATCH_APP_ENV:-}" != "dev" ]] && [[ ",${COMPOSE_PROFILES:-}," != *,metrics,* ]]; then
        return 0
    fi
    local state
    state="$(docker inspect --format '{{.State.Running}} {{.RestartCount}} {{.State.OOMKilled}}' chongchong-metrics-collector)" || return 1
    if [[ "$state" != "true 0 false" ]]; then
        echo "Metrics collector is not stable: $state" >&2
        return 1
    fi
    local actual
    actual="$(docker inspect --format '{{range .Config.Env}}{{println .}}{{end}}' chongchong-metrics-collector)" || return 1
    grep -Fxq "METRICS_SCRAPE_INTERVAL=${METRICS_SCRAPE_INTERVAL:-60s}" <<< "$actual" || return 1
    grep -Fxq "METRICS_SCRAPE_TIMEOUT=${METRICS_SCRAPE_TIMEOUT:-10s}" <<< "$actual" || return 1
}

wait_for_application() {
    local attempt
    for attempt in {1..30}; do
        if [[ "$(docker inspect --format '{{.State.Running}}' chongchong-backend 2>/dev/null || true)" == "true" ]] \
            && [[ "$(docker inspect --format '{{.State.Running}}' chongchong-nginx 2>/dev/null || true)" == "true" ]] \
            && docker exec chongchong-nginx nc -z backend 8080; then
            return 0
        fi
        sleep 2
    done

    compose logs --tail 100 backend nginx >&2 || true
    return 1
}
