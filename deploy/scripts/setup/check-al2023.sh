#!/usr/bin/env bash
set -Eeuo pipefail

role="${1:-}"
[[ "$#" == 1 && ( "$role" == edge || "$role" == backend ) ]] || {
    echo 'Usage: check-al2023.sh edge|backend' >&2; exit 1;
}
source /etc/os-release
[[ "${ID:-}" == amzn && "${VERSION_ID:-}" == 2023 && "$(uname -m)" == aarch64 ]] || {
    echo 'Amazon Linux 2023 ARM64 is required' >&2; exit 1;
}
for dependency in docker python3 curl flock timeout; do
    command -v "$dependency" >/dev/null || { echo "Missing dependency: $dependency" >&2; exit 1; }
done
python3 -c 'import sys; sys.exit(sys.version_info < (3, 9))'
docker info >/dev/null
docker compose version >/dev/null
systemctl is-active --quiet docker
systemctl is-enabled --quiet docker

if [[ "$role" == edge ]]; then
    for dependency in ssh certbot; do
        command -v "$dependency" >/dev/null || { echo "Missing edge dependency: $dependency" >&2; exit 1; }
    done
    systemctl is-active --quiet codedeploy-agent
    systemctl is-enabled --quiet codedeploy-agent
    systemctl is-active --quiet chongchong-certbot-renew.timer
    systemctl is-enabled --quiet chongchong-certbot-renew.timer
fi
echo "AL2023 $role runtime prerequisites verified; no packages or services changed"
