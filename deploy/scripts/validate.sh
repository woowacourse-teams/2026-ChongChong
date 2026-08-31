#!/usr/bin/env bash

set -Eeuo pipefail

readonly SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"

source "${SCRIPT_DIR}/runtime.sh"

load_deployment_environment
wait_for_application

HEALTH_STATUS_CODE="$(curl \
    --silent \
    --show-error \
    --output /dev/null \
    --write-out "%{http_code}" \
    --resolve "${SERVER_NAME}:443:127.0.0.1" \
    "https://${SERVER_NAME}/actuator/health")"
readonly HEALTH_STATUS_CODE

if [[ ! "${HEALTH_STATUS_CODE}" =~ ^2[0-9]{2}$ ]]; then
    echo "Health check returned HTTP ${HEALTH_STATUS_CODE}" >&2
    exit 1
fi

docker image prune --all --force
