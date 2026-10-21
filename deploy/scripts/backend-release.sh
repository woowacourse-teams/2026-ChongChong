#!/usr/bin/env bash
set -Eeuo pipefail
umask 077

slot="${1:?slot required}"
address="${2:?address required}"
[[ "$slot" == a || "$slot" == b ]]
[[ "$address" =~ ^[0-9]+\.[0-9]+\.[0-9]+\.[0-9]+$ ]]
root="${CHONGCHONG_APP_ROOT:-/opt/chongchong}"
install -d -m 700 "$root/releases"
exec 8>"$root/releases/deploy.lock"
flock -n 8 || { echo 'Backend deployment already running' >&2; exit 1; }
release="$(mktemp -d "$root/releases/release.XXXXXX")"
cleanup_release() {
    local status=$?
    trap - EXIT
    if [[ "$status" != 0 && ! "$root/current-backend-release" -ef "$release" ]]; then
        rm -rf -- "$release"
    fi
    exit "$status"
}
trap cleanup_release EXIT
trap 'exit 130' INT
trap 'exit 143' TERM
tar -xf - -C "$release" --no-same-owner --no-same-permissions
chmod 600 "$release/backend.env" "$release/image.env"
set -a
source "$release/backend.env"
source "$release/image.env"
set +a
export BACKEND_SLOT="$slot" BACKEND_BIND_IP="$address"
unset COMPOSE_PROFILES
for key in BACKEND_IMAGE BACKEND_IMAGE_TAG DOCKERHUB_USERNAME DOCKERHUB_TOKEN DB_URL DB_USERNAME DB_PASSWORD; do
    [[ -n "${!key:-}" ]] || { echo "Missing $key" >&2; exit 1; }
done
compose=(docker compose --project-name chongchong-backend
    --env-file "$release/backend.env" --env-file "$release/image.env"
    -f "$release/docker-compose.backend.yml")
"${compose[@]}" config --quiet
printf '%s' "$DOCKERHUB_TOKEN" | docker login --username "$DOCKERHUB_USERNAME" --password-stdin
"${compose[@]}" pull backend
if docker container inspect chongchong-backend >/dev/null 2>&1; then
    docker stop --time "${BACKEND_STOP_TIMEOUT_SECONDS:-60}" chongchong-backend >/dev/null
    docker rm chongchong-backend >/dev/null
fi
"${compose[@]}" up -d --no-deps backend
ln -sfn "$release" "$root/current-backend-release"
