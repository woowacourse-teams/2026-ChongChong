#!/usr/bin/env bash
# 서버 최초 준비용. 앱 배포마다 실행하지 않으며 기존 에이전트를 자동 업그레이드하지 않는다.
set -Eeuo pipefail
umask 077

[[ "$EUID" == 0 ]] || { echo 'root로 실행하세요.' >&2; exit 1; }
source /etc/os-release
[[ "$ID" == ubuntu ]] || { echo 'Ubuntu만 지원합니다.' >&2; exit 1; }
architecture="$(dpkg --print-architecture)"
[[ "$architecture" == arm64 ]] || { echo '현재 배포 대상은 ARM64입니다.' >&2; exit 1; }
script_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
source "$script_dir/host-monitoring.sh"
exec 9>/run/lock/chongchong-host-memory.lock
flock -n 9 || { echo '메모리 설정 작업이 이미 실행 중입니다.' >&2; exit 1; }

agent_root=/opt/aws/amazon-cloudwatch-agent
if [[ ! -x "$agent_root/bin/amazon-cloudwatch-agent-ctl" ]]; then
    command -v curl >/dev/null || { echo '먼저 curl과 ca-certificates를 설치하세요.' >&2; exit 1; }
    package_dir="$(mktemp -d)"
    trap 'rm -rf -- "$package_dir"' EXIT
    curl --fail --silent --show-error --location --connect-timeout 10 --max-time 180 \
        https://amazoncloudwatch-agent.s3.amazonaws.com/ubuntu/arm64/latest/amazon-cloudwatch-agent.deb \
        --output "$package_dir/amazon-cloudwatch-agent.deb"
    dpkg --install "$package_dir/amazon-cloudwatch-agent.deb"
fi

configure_host_memory "$script_dir/../cloudwatch-agent-memory.json" "$agent_root" \
    /opt/chongchong/observability-resources
echo '호스트 메모리 수집 설정을 적용했습니다. 실제 CloudWatch 수신은 별도로 확인하세요.'
