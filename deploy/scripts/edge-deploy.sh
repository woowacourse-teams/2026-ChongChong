#!/usr/bin/env bash
set -Eeuo pipefail
umask 077
DEPLOY="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
source "$DEPLOY/scripts/edge-runtime.sh"
edge_initialize
[[ ! -e "$STATE/pending-upstream.conf" ]] || {
    echo 'Interrupted switch detected; run edge-rollback.sh before deploying again' >&2; exit 1;
}
current="$(upstream_host "$UPSTREAM")"
if [[ "$current" == "$BACKEND_A_HOST" ]]; then target="$BACKEND_B_HOST"; slot=b
else target="$BACKEND_A_HOST"; slot=a; fi
docker exec chongchong-nginx nginx -t
workers="$(docker top chongchong-nginx -eo args)"
if grep -Fq 'worker process is shutting down' <<< "$workers"; then
    echo 'Previous Nginx workers are still draining; retry later' >&2; exit 1
fi
work="$(mktemp -d "$STATE/deployment.XXXXXX")"
verify_edge "$current" "$work" || {
    echo 'Live routing does not match upstream.conf or is unhealthy; neither backend changed' >&2; exit 1;
}
cp "$UPSTREAM" "$work/previous-upstream.conf"
install -m 600 "$ROOT/config/backend.env" "$work/backend.env"
install -m 600 "$DEPLOY/image.env" "$work/image.env"
cp "$DEPLOY/docker-compose.yml" "$DEPLOY/docker-compose.backend.yml" "$work/"
switched=false
completed=false
finish() {
    local status=$?
    trap - EXIT
    if [[ "$switched" == true && "$completed" != true ]]; then
        if replace_upstream "$work/previous-upstream.conf" && verify_edge_wait "$current" "$work"; then
            rm -f "$STATE/pending-upstream.conf"
            echo 'Restored previous backend routing' >&2
        else
            echo 'URGENT: rollback verification failed; pending state retained for edge-rollback.sh' >&2
        fi
    fi
    exit "$status"
}
trap finish EXIT
trap 'exit 130' INT
trap 'exit 143' TERM
ssh_args=(-o BatchMode=yes -o StrictHostKeyChecking=yes -o ConnectTimeout=10
    -o ServerAliveInterval=15 -o ServerAliveCountMax=3
    -o "UserKnownHostsFile=$SSH_KNOWN_HOSTS" -i "$SSH_KEY")
remote="$SSH_USER@$target"
ssh "${ssh_args[@]}" "$remote" 'sudo -n install -d -m 700 /opt/chongchong/deploy-control'
ssh "${ssh_args[@]}" "$remote" 'sudo -n tee /opt/chongchong/deploy-control/backend-release.sh >/dev/null' < "$DEPLOY/scripts/backend-release.sh"
rm -f "$STATE/previous-upstream.conf"
tar -C "$work" -cf - backend.env image.env docker-compose.yml docker-compose.backend.yml |
    timeout 240 ssh "${ssh_args[@]}" "$remote" "sudo -n bash /opt/chongchong/deploy-control/backend-release.sh $slot $target"
is_ready=false
for attempt in {1..30}; do
    if ready "$target" "$work/readiness.json"; then is_ready=true; break; fi
    sleep 2
done
[[ "$is_ready" == true ]] || { echo 'Candidate not ready; routing unchanged' >&2; exit 1; }
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
        echo "Deployment verified on backend $slot; previous backend retained"
        exit 0
    fi
    sleep 2
done
echo 'Edge verification failed' >&2
exit 1
