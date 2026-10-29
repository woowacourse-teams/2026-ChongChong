#!/usr/bin/env bash
set -Eeuo pipefail

# 실행: bash deploy/scripts/measure-availability.sh HTTPS_URL OUTPUT_DIR [요청수]
# curl, jq, Perl(Time::HiRes)이 필요하다. 재시도와 리다이렉트 추적은 하지 않는다.
url="${1:?HTTPS URL 필요}"
output="${2:?새 출력 디렉터리 필요}"
count="${3:-1500}"
[[ "$url" == https://* && "$count" =~ ^[1-9][0-9]*$ ]] || exit 2
for dependency in curl jq perl; do command -v "$dependency" >/dev/null; done
mkdir "$output"
output="$(cd "$output" && pwd)"
temporary="$(mktemp -d)"
trap 'rm -rf -- "$temporary"' EXIT
now() { perl -MTime::HiRes=time -e 'printf "%.6f", time'; }

probe() {
    local id="$1" started finished status curl_exit=0 success=false backend
    started="$(now)"
    status="$(curl --silent --show-error --connect-timeout 2 --max-time 2 \
        --output "$temporary/$id.body" --dump-header "$temporary/$id.headers" \
        --write-out '%{http_code}' "$url" 2>"$temporary/$id.error")" || curl_exit=$?
    finished="$(now)"
    if [[ "$curl_exit" == 0 && "$status" == 200 ]] &&
        jq -e '.status == "UP"' "$temporary/$id.body" >/dev/null 2>&1; then
        success=true
    fi
    backend="$(awk 'tolower($1)=="x-deploy-backend:" {gsub("\r", "", $2); print $2}' "$temporary/$id.headers")"
    jq -cn --argjson id "$id" --argjson started "$started" --argjson finished "$finished" \
        --arg status "$status" --argjson curl_exit "$curl_exit" --argjson success "$success" \
        --arg backend "$backend" --rawfile error "$temporary/$id.error" \
        '{id:$id,started:$started,finished:$finished,http_status:$status,curl_exit:$curl_exit,success:$success,backend:$backend,error:$error}' \
        >> "$output/requests.jsonl"
}

printf 'URL=%s\n요청수=%s\n발사 간격=0.2초 sleep + 로컬 실행 오버헤드\n타임아웃=2초\n성공=HTTP 200 및 JSON status UP\n' "$url" "$count" > "$output/settings.txt"
for ((id=1; id<=count; id++)); do
    [[ ! -e "$output/stop" ]] || break
    probe "$id" &
    sleep 0.2
done
wait
launched=$((id - 1))
jq -se --argjson launched "$launched" 'length == $launched and ([.[].id] | unique | length) == $launched' \
    "$output/requests.jsonl" >/dev/null || { echo '요청 기록 누락: 결과를 유효한 측정으로 사용하지 마세요.' >&2; exit 1; }
jq -s 'sort_by(.started) | {total:length,failed:map(select(.success == false))|length,first:.[0].started,last:.[-1].finished,backends:(group_by(.backend)|map({backend:.[0].backend,requests:length}))}' \
    "$output/requests.jsonl" > "$output/summary.json"
cat "$output/summary.json"
