#!/usr/bin/env bash

set -Eeuo pipefail

readonly APP_ROOT="/opt/chongchong"

install -d -m 0755 "${APP_ROOT}" "${APP_ROOT}/deploy" /var/www/certbot

if [[ ! -f "${APP_ROOT}/.env" ]]; then
    echo "Create ${APP_ROOT}/.env before the first deployment" >&2
    exit 1
fi

chown root:root "${APP_ROOT}/.env"
chmod 0600 "${APP_ROOT}/.env"

docker --version
docker compose version
systemctl is-active --quiet codedeploy-agent
