#!/usr/bin/env bash

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
    )

    local variable
    for variable in "${required_variables[@]}"; do
        if [[ -z "${!variable:-}" ]]; then
            echo "Missing required variable: ${variable}" >&2
            return 1
        fi
    done
}

compose() {
    docker compose \
        --env-file "${APP_ENV}" \
        --env-file "${IMAGE_ENV}" \
        --file "${COMPOSE_FILE}" \
        "$@"
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
