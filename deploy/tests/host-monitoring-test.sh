#!/usr/bin/env bash
set -Eeuo pipefail

deploy="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
source "$deploy/scripts/setup/host-monitoring.sh"
sandbox="$(mktemp -d)"
trap 'rm -rf -- "$sandbox"' EXIT
export TEST_AGENT_ROOT="$sandbox/agent" TEST_EVENTS="$sandbox/events" TEST_ACTIVE="$sandbox/active"
mkdir -p "$TEST_AGENT_ROOT/bin" "$TEST_AGENT_ROOT/etc/amazon-cloudwatch-agent.d"
cp "$deploy/tests/fixtures/cloudwatch-agent-ctl.sh" "$TEST_AGENT_ROOT/bin/amazon-cloudwatch-agent-ctl"
chmod +x "$TEST_AGENT_ROOT/bin/amazon-cloudwatch-agent-ctl"
config="$deploy/config/cloudwatch-agent-memory.json"
state="$sandbox/state"
systemctl() {
    printf '%s\n' "$*" >> "$TEST_EVENTS"
    case "$1" in
        enable) return 0 ;;
        is-active) [[ -f "$TEST_ACTIVE" ]] ;;
        start) touch "$TEST_ACTIVE" ;;
        *) return 1 ;;
    esac
}
count_applies() { grep -c '^apply$' "$TEST_EVENTS" || true; }

configure_host_memory "$config" "$TEST_AGENT_ROOT" "$state"
[[ "$(count_applies)" == 1 && -f "$TEST_ACTIVE" && -f "$state/memory-config.verified" ]]
echo 'PASS: 최초 설정 적용 및 실행 확인'

configure_host_memory "$config" "$TEST_AGENT_ROOT" "$state"
[[ "$(count_applies)" == 1 ]]
echo 'PASS: 동일 설정 반복 시 재적용 없음'

rm "$TEST_ACTIVE"
configure_host_memory "$config" "$TEST_AGENT_ROOT" "$state"
[[ "$(count_applies)" == 1 && -f "$TEST_ACTIVE" ]]
echo 'PASS: 설정이 같고 중지된 서비스만 시작'

printf '\n' >> "$state/memory-config.verified"
printf '{"logs":{}}\n' > "$TEST_AGENT_ROOT/etc/amazon-cloudwatch-agent.d/file_logs.json"
configure_host_memory "$config" "$TEST_AGENT_ROOT" "$state"
[[ "$(count_applies)" == 2 ]]
[[ "$(cat "$TEST_AGENT_ROOT/etc/amazon-cloudwatch-agent.d/file_logs.json")" == '{"logs":{}}' ]]
echo 'PASS: 설정 변경 재적용 및 다른 로그 설정 보존'

sed 's/"metrics_collection_interval": 10/"metrics_collection_interval": 20/g' "$config" > "$sandbox/changed.json"
configure_host_memory "$sandbox/changed.json" "$TEST_AGENT_ROOT" "$state"
cmp -s "$sandbox/changed.json" "$TEST_AGENT_ROOT/etc/amazon-cloudwatch-agent.d/file_cloudwatch-agent-memory.json"
configure_host_memory "$config" "$TEST_AGENT_ROOT" "$state"
[[ "$(count_applies)" == 4 ]]
echo 'PASS: 수집 주기 변경을 실제 적용 파일에 반영'

printf '{"metrics":{"namespace":"Other"}}\n' > "$TEST_AGENT_ROOT/etc/amazon-cloudwatch-agent.d/file_other.json"
if configure_host_memory "$config" "$TEST_AGENT_ROOT" "$state"; then exit 1; fi
[[ "$(count_applies)" == 4 ]]
rm "$TEST_AGENT_ROOT/etc/amazon-cloudwatch-agent.d/file_other.json"
echo 'PASS: 다른 메트릭 설정과 충돌 시 적용 전 중단'

printf '\n' >> "$state/memory-config.verified"
cp "$state/memory-config.verified" "$sandbox/previous-verified"
export TEST_APPLY_FAIL=1
if configure_host_memory "$config" "$TEST_AGENT_ROOT" "$state"; then exit 1; fi
cmp -s "$sandbox/previous-verified" "$state/memory-config.verified"
unset TEST_APPLY_FAIL
configure_host_memory "$config" "$TEST_AGENT_ROOT" "$state"
[[ "$(count_applies)" == 6 ]]
echo 'PASS: 적용 실패를 성공으로 기록하지 않고 다음 실행에서 재시도'

printf '\n' >> "$state/memory-config.verified"
cp "$state/memory-config.verified" "$sandbox/previous-verified"
export TEST_NO_START=1
rm "$TEST_ACTIVE"
if configure_host_memory "$config" "$TEST_AGENT_ROOT" "$state"; then exit 1; fi
cmp -s "$sandbox/previous-verified" "$state/memory-config.verified"
echo 'PASS: 프로세스 실행 확인 실패를 성공으로 기록하지 않음'
