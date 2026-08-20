#!/usr/bin/env bash

set -Eeuo pipefail

readonly SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"

source "${SCRIPT_DIR}/runtime.sh"

load_deployment_environment
wait_for_application

curl \
    --silent \
    --show-error \
    --output /dev/null \
    --resolve "${SERVER_NAME}:443:127.0.0.1" \
    "https://${SERVER_NAME}/"
