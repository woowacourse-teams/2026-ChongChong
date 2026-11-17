#!/usr/bin/env bash
set -Eeuo pipefail
umask 077

image="${1:?repository:tag required}"
output="${2:?output path required}"
repository="${image%:*}"
repository="${repository#docker.io/}"
repository="${repository#index.docker.io/}"
digests="$(docker image inspect --format '{{range .RepoDigests}}{{println .}}{{end}}' "$image")"
pin=''
while IFS= read -r candidate; do
    candidate="${candidate#docker.io/}"
    candidate="${candidate#index.docker.io/}"
    if [[ "$candidate" == "$repository"@sha256:* &&
          "${candidate#*@sha256:}" =~ ^[a-f0-9]{64}$ ]]; then
        pin="$candidate"
        break
    fi
done <<< "$digests"
[[ -n "$pin" ]] || { echo 'Pushed image digest was not found for this repository' >&2; exit 1; }
printf '%s\n' "$pin" > "$output"
