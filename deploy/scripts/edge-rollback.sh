#!/usr/bin/env bash
set -Eeuo pipefail
umask 077
source "$(dirname -- "${BASH_SOURCE[0]}")/edge-runtime.sh"
edge_initialize
previous="$STATE/previous-upstream.conf"
[[ ! -f "$STATE/pending-upstream.conf" ]] || previous="$STATE/pending-upstream.conf"
target="$(upstream_host "$previous")"
work="$(mktemp -d "$STATE/rollback.XXXXXX")"
ready "$target" "$work/readiness.json" || { echo 'Previous backend not ready; routing unchanged' >&2; exit 1; }
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
