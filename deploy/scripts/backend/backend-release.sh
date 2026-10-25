#!/usr/bin/env bash
# edge가 SSH 표준 입력으로 보낸 배포 묶음을 대상 백엔드에서 실행한다. 트래픽 전환은 edge가 맡는다.
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
# 실패 후보만 정리한다. 실행에 사용한 release의 설정과 진단 자료는 보존한다.
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
# 앱 설정은 edge에서 받은 release 기준이다. 호스트의 legacy .env는 사용하지 않는다.
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
# pull 실패로 대기 앱까지 잃지 않도록 다운로드가 끝난 뒤 기존 컨테이너를 멈춘다.
if docker container inspect chongchong-backend >/dev/null 2>&1; then
    docker stop --time "${BACKEND_STOP_TIMEOUT_SECONDS:-60}" chongchong-backend >/dev/null
    docker rm chongchong-backend >/dev/null
fi
"${compose[@]}" up -d --no-deps backend
# 시작에 사용한 release를 기록한다. readiness와 최종 전환 검증은 edge가 이어서 수행한다.
ln -sfn "$release" "$root/current-backend-release"
