#!/usr/bin/env bash
set -Eeuo pipefail
umask 077
ROOT="${CHONGCHONG_APP_ROOT:-/opt/chongchong}"
SOURCE="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../.." && pwd)"
[[ "$ROOT" == /opt/chongchong ]] || { echo 'systemd installation requires /opt/chongchong' >&2; exit 1; }
install -d -m 700 "$ROOT/edge-runtime" "$ROOT/edge-state"
exec 9>"$ROOT/edge-state/deploy.lock"
flock -n 9 || { echo 'Another deployment is running' >&2; exit 1; }
exec 8>"$ROOT/edge-state/routing.lock"
flock -n 8 || { echo 'A routing operation is running' >&2; exit 1; }
release="$(mktemp -d "$ROOT/edge-runtime/runtime.XXXXXX")"
for name in edge-control.sh edge_cli.py edge_control.py edge_release.py edge_state.py edge_io.py backend_retention.py; do
    install -m 700 "$SOURCE/scripts/edge/$name" "$release/$name"
done
install -m 700 "$SOURCE/scripts/backend/backend-release.sh" "$release/backend-release.sh"
python3 -m py_compile "$release"/*.py
ln -s "$release" "$release/next-link"
mv -Tf "$release/next-link" "$ROOT/edge-runtime/current"
for name in chongchong-edge-{monitor,finalize}.{service,timer}; do
    install -m 644 "$SOURCE/config/systemd/$name" /etc/systemd/system/
done
systemctl daemon-reload
echo 'Runtime installed. Bootstrap and inspect status before enabling the timers.'
