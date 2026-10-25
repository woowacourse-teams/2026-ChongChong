#!/usr/bin/env bash
# edge의 CodeDeploy 진입점. 대기 백엔드만 교체하고 실제 응답을 검증한 뒤 전환을 확정한다.
set -Eeuo pipefail
umask 077
DEPLOY="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
source "$DEPLOY/scripts/edge-runtime.sh"
edge_initialize
# 미완료 전환은 새 배포로 덮지 않고 이전 라우팅부터 복구한다.
[[ ! -e "$STATE/pending-upstream.conf" ]] || {
    echo 'Interrupted switch detected; run edge-rollback.sh before deploying again' >&2; exit 1;
}
current="$(upstream_host "$UPSTREAM")"
if [[ "$current" == "$BACKEND_A_HOST" ]]; then target="$BACKEND_B_HOST"; slot=b
else target="$BACKEND_A_HOST"; slot=a; fi
docker exec chongchong-nginx nginx -t
workers="$(docker top chongchong-nginx -eo pid,args)"
# 이전 worker가 대기 슬롯으로 연결 중일 수 있으므로 그 슬롯의 앱 교체를 미룬다.
if grep -Fq 'worker process is shutting down' <<< "$workers"; then
    echo 'Previous Nginx workers are still draining; retry later' >&2; exit 1
fi
work="$(mktemp -d "$STATE/deployment.XXXXXX")"
switched=false
completed=false
finish() {
    local status=$?
    trap - EXIT
    # 전환 검증을 끝내지 못했다면 이전 경로를 복구하고 실제 응답까지 확인한다.
    if [[ "$switched" == true && "$completed" != true ]]; then
        if replace_upstream "$work/previous-upstream.conf" && verify_edge_wait "$current" "$work"; then
            rm -f "$STATE/pending-upstream.conf"
            echo 'Restored previous backend routing' >&2
        else
            echo 'URGENT: rollback verification failed; pending state retained for edge-rollback.sh' >&2
        fi
    fi
    if ! prune_edge_deployments; then
        echo 'Deployment snapshot cleanup failed' >&2
        status=1
    fi
    exit "$status"
}
trap finish EXIT
trap 'exit 130' INT
trap 'exit 143' TERM
verify_edge "$current" "$work" || {
    echo 'Live routing does not match upstream.conf or is unhealthy; neither backend changed' >&2; exit 1;
}
cp "$UPSTREAM" "$work/previous-upstream.conf"
# 같은 배포에서는 원본 설정이 아니라 이 사본을 끝까지 사용한다.
install -m 600 "$ROOT/config/backend.env" "$work/backend.env"
install -m 600 "$DEPLOY/image.env" "$work/image.env"
cp "$DEPLOY/docker-compose.yml" "$DEPLOY/docker-compose.backend.yml" "$work/"
ssh_args=(-o BatchMode=yes -o StrictHostKeyChecking=yes -o ConnectTimeout=10
    -o ServerAliveInterval=15 -o ServerAliveCountMax=3
    -o "UserKnownHostsFile=$SSH_KNOWN_HOSTS" -i "$SSH_KEY")
remote="$SSH_USER@$target"
ssh "${ssh_args[@]}" "$remote" 'sudo -n install -d -m 700 /opt/chongchong/deploy-control'
ssh "${ssh_args[@]}" "$remote" 'sudo -n tee /opt/chongchong/deploy-control/backend-release.sh >/dev/null' < "$DEPLOY/scripts/backend-release.sh"
# 대기 슬롯을 덮어쓰기 전에 그 슬롯을 직전 버전으로 간주한 롤백 기록을 무효화한다.
rm -f "$STATE/previous-upstream.conf"
tar -C "$work" -cf - backend.env image.env docker-compose.yml docker-compose.backend.yml |
    timeout 240 ssh "${ssh_args[@]}" "$remote" "sudo -n bash /opt/chongchong/deploy-control/backend-release.sh $slot $target"
is_ready=false
for attempt in {1..30}; do
    if ready "$target" "$work/readiness.json"; then is_ready=true; break; fi
    sleep 2
done
[[ "$is_ready" == true ]] || { echo 'Candidate not ready; routing unchanged' >&2; exit 1; }
# 전환 중 프로세스가 종료되어도 복구할 이전 주소를 먼저 기록한다.
cp "$work/previous-upstream.conf" "$STATE/pending-upstream.conf"
switched=true
printf 'upstream chongchong_backend { server %s:8080; }\n' "$target" > "$work/candidate-upstream.conf"
replace_upstream "$work/candidate-upstream.conf"
for attempt in {1..10}; do
    if verify_edge "$target" "$work"; then
        cp "$work/previous-upstream.conf" "$STATE/previous-upstream.next"
        mv "$STATE/previous-upstream.next" "$STATE/previous-upstream.conf"
        printf '%s\n' "$work" > "$STATE/last-successful-deployment"
        rm "$STATE/pending-upstream.conf"
        completed=true
        # 이전 앱은 기존 요청 처리와 다음 배포 전까지의 즉시 롤백을 위해 유지한다.
        echo "Deployment verified on backend $slot; previous backend retained"
        exit 0
    fi
    sleep 2
done
echo 'Edge verification failed' >&2
exit 1
