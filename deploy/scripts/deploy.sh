#!/usr/bin/env bash
# legacy 단일 서버용 배포. backend와 Nginx를 재생성하므로 A/B 무중단 배포에서는 사용하지 않는다.

set -Eeuo pipefail

readonly SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"

source "${SCRIPT_DIR}/runtime.sh"

load_deployment_environment

readonly LETSENCRYPT_ROOT="${CHONGCHONG_LETSENCRYPT_ROOT:-/etc/letsencrypt}"

if [[ ! -r "${LETSENCRYPT_ROOT}/live/${SERVER_NAME}/fullchain.pem" ]] \
    || [[ ! -r "${LETSENCRYPT_ROOT}/live/${SERVER_NAME}/privkey.pem" ]]; then
    echo "TLS certificate for ${SERVER_NAME} is missing" >&2
    exit 1
fi

previous_image="$(docker inspect --format '{{.Config.Image}}' chongchong-backend 2>/dev/null || true)"

rollback() {
    if [[ -z "${previous_image}" ]] || [[ "${previous_image}" != *:* ]]; then
        echo "No previous backend image is available for rollback" >&2
        return 0
    fi

    export BACKEND_IMAGE="${previous_image%:*}"
    export BACKEND_IMAGE_TAG="${previous_image##*:}"
    echo "Rolling back to ${BACKEND_IMAGE}:${BACKEND_IMAGE_TAG}" >&2
    local backup=""
    if [[ -f "${APP_ROOT}/previous-deploy-dir" ]]; then
        backup="$(cat "${APP_ROOT}/previous-deploy-dir")"
    fi
    if [[ "$backup" != "${APP_ROOT}/deploy-backups/"* ]] || [[ ! -f "$backup/docker-compose.yml" ]]; then
        echo "No configuration backup available; restore the previous deployment artifact manually." >&2
        return 1
    fi
    local rollback_files=(--file "$backup/docker-compose.yml")
    if [[ "${CLOUD_WATCH_APP_ENV:-}" == "dev" ]] && [[ -f "$backup/docker-compose.dev.yml" ]]; then
        rollback_files+=(--file "$backup/docker-compose.dev.yml")
    fi
    docker compose --env-file "$APP_ENV" --env-file "$IMAGE_ENV" \
        "${rollback_files[@]}" up --detach --force-recreate --timeout "${BACKEND_STOP_TIMEOUT_SECONDS:-60}" backend nginx
    wait_for_application
}

printf '%s' "${DOCKERHUB_TOKEN}" \
    | docker login --username "${DOCKERHUB_USERNAME}" --password-stdin

compose config --quiet
compose pull backend nginx
compose run --rm --no-deps --entrypoint /bin/sh nginx -ec '
    printf "127.0.0.1 backend\n" >> /etc/hosts
    exec /docker-entrypoint.sh nginx -t
'

if ! compose up --detach --force-recreate --timeout "${BACKEND_STOP_TIMEOUT_SECONDS:-60}" backend nginx; then
    rollback
    exit 1
fi

if ! wait_for_application; then
    rollback
    exit 1
fi

if ! verify_nginx_limits; then
    rollback
    exit 1
fi

if [[ "${CLOUD_WATCH_APP_ENV:-}" == "dev" ]] || [[ ",${COMPOSE_PROFILES:-}," == *,metrics,* ]]; then
    compose --profile metrics pull metrics-collector
    compose --profile metrics up --detach --no-deps --force-recreate metrics-collector
    sleep 5
    if ! verify_metrics_collector; then
        echo "Metrics collector failed to start; application is running. Check collector logs before retrying." >&2
        exit 1
    fi
fi
