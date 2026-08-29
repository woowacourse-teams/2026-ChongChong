#!/usr/bin/env bash

set -Eeuo pipefail

if [[ "${EUID}" -ne 0 ]]; then
    echo "Run this script as root" >&2
    exit 1
fi

readonly SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
readonly DEPLOY_ROOT="$(cd -- "${SCRIPT_DIR}/.." && pwd)"
readonly APP_ROOT="/opt/chongchong"
readonly APP_ENV="${APP_ROOT}/.env"

if [[ ! -f "${APP_ENV}" ]]; then
    echo "Copy deploy/.env.example to ${APP_ENV} and fill it before bootstrap" >&2
    exit 1
fi

set -a
source "${APP_ENV}"
set +a

for variable in AWS_REGION SERVER_NAME CERTBOT_EMAIL; do
    if [[ -z "${!variable:-}" ]]; then
        echo "Missing required variable: ${variable}" >&2
        exit 1
    fi
done

source /etc/os-release
if [[ "${ID:-}" != "ubuntu" ]]; then
    echo "This bootstrap script supports Ubuntu only" >&2
    exit 1
fi

apt-get update
apt-get install --yes ca-certificates curl gnupg certbot wget

install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg \
    | gpg --dearmor --yes --output /etc/apt/keyrings/docker.gpg
chmod a+r /etc/apt/keyrings/docker.gpg

architecture="$(dpkg --print-architecture)"
printf 'deb [arch=%s signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu %s stable\n' \
    "${architecture}" "${VERSION_CODENAME}" \
    > /etc/apt/sources.list.d/docker.list

apt-get update
apt-get install --yes docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
systemctl enable --now docker

install -d -m 0755 "${APP_ROOT}/deploy" /var/www/certbot
chown root:root "${APP_ENV}"
chmod 0600 "${APP_ENV}"

codedeploy_installer="/tmp/chongchong-codedeploy-install"
curl -fsSL \
    "https://aws-codedeploy-${AWS_REGION}.s3.${AWS_REGION}.amazonaws.com/latestv2/install" \
    --output "${codedeploy_installer}"
chmod 0700 "${codedeploy_installer}"
"${codedeploy_installer}" auto
systemctl enable --now codedeploy-agent

if [[ ! -r "/etc/letsencrypt/live/${SERVER_NAME}/fullchain.pem" ]]; then
    docker rm --force chongchong-certbot-bootstrap >/dev/null 2>&1 || true
    docker run \
        --detach \
        --name chongchong-certbot-bootstrap \
        --publish 80:80 \
        --volume /var/www/certbot:/usr/share/nginx/html:ro \
        nginx:1.28-alpine

    cleanup_certbot_container() {
        docker rm --force chongchong-certbot-bootstrap >/dev/null 2>&1 || true
    }
    trap cleanup_certbot_container EXIT

    certbot certonly \
        --non-interactive \
        --agree-tos \
        --email "${CERTBOT_EMAIL}" \
        --webroot \
        --webroot-path /var/www/certbot \
        --domain "${SERVER_NAME}"

    cleanup_certbot_container
    trap - EXIT
fi

install -m 0755 "${SCRIPT_DIR}/renew-certificate.sh" /usr/local/sbin/chongchong-renew-certificate
install -m 0644 "${DEPLOY_ROOT}/systemd/chongchong-certbot-renew.service" /etc/systemd/system/
install -m 0644 "${DEPLOY_ROOT}/systemd/chongchong-certbot-renew.timer" /etc/systemd/system/
systemctl daemon-reload
systemctl enable --now chongchong-certbot-renew.timer

echo "EC2 bootstrap completed for ${SERVER_NAME}"
