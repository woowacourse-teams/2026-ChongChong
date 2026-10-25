#!/usr/bin/env bash
# legacy AppSpec의 BeforeInstall. 덮어쓰기 전에 이전 배포 파일을 복구용으로 보관한다.

set -Eeuo pipefail

readonly APP_ROOT="/opt/chongchong"

install -d -m 0755 "${APP_ROOT}" "${APP_ROOT}/deploy" /var/www/certbot

if [[ ! -f "${APP_ROOT}/.env" ]]; then
    echo "Create ${APP_ROOT}/.env before the first deployment" >&2
    exit 1
fi

chown root:root "${APP_ROOT}/.env"
chmod 0600 "${APP_ROOT}/.env"

if [[ -f "${APP_ROOT}/deploy/docker-compose.yml" ]]; then
    install -d -m 0700 "${APP_ROOT}/deploy-backups"
    backup="$(mktemp -d "${APP_ROOT}/deploy-backups/before-install.XXXXXX")"
    cp -a "${APP_ROOT}/deploy" "${backup}/deploy"
    printf '%s\n' "${backup}/deploy" > "${APP_ROOT}/previous-deploy-dir"
else
    : > "${APP_ROOT}/previous-deploy-dir"
fi

docker --version
docker compose version
systemctl is-active --quiet codedeploy-agent
