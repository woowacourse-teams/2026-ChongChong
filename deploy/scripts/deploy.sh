#!/usr/bin/env bash

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
    compose up --detach --remove-orphans
    wait_for_application
}

printf '%s' "${DOCKERHUB_TOKEN}" \
    | docker login --username "${DOCKERHUB_USERNAME}" --password-stdin

compose pull backend nginx

if ! compose up --detach --remove-orphans; then
    rollback
    exit 1
fi

if ! wait_for_application; then
    rollback
    exit 1
fi
