#!/usr/bin/env bash
# edge에서 이전 백엔드로 라우팅만 복원한다. 이미지와 앱 설정은 다시 배포하지 않는다.
set -Eeuo pipefail
umask 077
source "$(dirname -- "${BASH_SOURCE[0]}")/edge-runtime.sh"
edge_initialize
previous="$STATE/previous-upstream.conf"
# 중단된 전환이 있으면 마지막 성공 기록보다 그 전환 직전 주소를 우선한다.
[[ ! -f "$STATE/pending-upstream.conf" ]] || previous="$STATE/pending-upstream.conf"
target="$(upstream_host "$previous")"
work="$(mktemp -d "$STATE/rollback.XXXXXX")"
ready "$target" "$work/readiness.json" || { echo 'Previous backend not ready; routing unchanged' >&2; exit 1; }
# 복구 도중에도 pending을 유지하며, 실제 HTTPS 검증 성공 후에만 해제한다.
cp "$previous" "$work/restore.conf"
cp "$work/restore.conf" "$STATE/pending-upstream.conf"
replace_upstream "$work/restore.conf"
for attempt in {1..10}; do
    if verify_edge "$target" "$work"; then
        rm -f "$STATE/pending-upstream.conf"
        prune_edge_deployments
        echo "Routing restored to $target; no application recreated"
        exit 0
    fi
    sleep 2
done
echo 'URGENT: rollback response could not be verified; inspect Nginx and backend' >&2
exit 1
