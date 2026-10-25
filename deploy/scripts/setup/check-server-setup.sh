#!/usr/bin/env bash
# 서버 역할별 로컬 준비 상태를 읽기 전용으로 검사한다. 원격 연결과 실제 메트릭 수신은 별도 확인한다.
set -Eeuo pipefail

role="${1:-}"
case "$role" in
    backend|edge) ;;
    *) echo "사용법: sudo bash $0 backend|edge" >&2; exit 2 ;;
esac
[[ "$EUID" == 0 ]] || { echo 'root로 실행하세요.' >&2; exit 1; }
root=/opt/chongchong
failed=0
check() {
    local label="$1"
    shift
    if "$@" >/dev/null 2>&1; then
        printf '[OK] %s\n' "$label"
    else
        printf '[누락/실패] %s\n' "$label"
        failed=1
    fi
}
private_file() {
    [[ -f "$1" && ! -L "$1" && "$(stat -c '%u:%a' "$1")" == 0:600 ]]
}
service_ready() {
    systemctl is-active --quiet "$1" && systemctl is-enabled --quiet "$1"
}
ssm_ready() {
    service_ready amazon-ssm-agent || service_ready snap.amazon-ssm-agent.amazon-ssm-agent
}

source /etc/os-release
check 'Ubuntu' test "$ID" = ubuntu
check 'ARM64' test "$(dpkg --print-architecture)" = arm64
for command in docker curl python3 tar flock timeout ssh sudo; do
    check "도구: $command" command -v "$command"
done
check 'Docker 실행 및 부팅 자동 시작' service_ready docker
check 'Docker daemon 접근' docker info
check 'Docker Compose 플러그인' docker compose version
check 'SSM 실행 및 부팅 자동 시작' ssm_ready
check 'CloudWatch Agent 실행 및 부팅 자동 시작' service_ready amazon-cloudwatch-agent
script_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
check '호스트 메모리 수집 설정' cmp -s "$script_dir/../../config/cloudwatch-agent-memory.json" \
    /opt/aws/amazon-cloudwatch-agent/etc/amazon-cloudwatch-agent.d/file_cloudwatch-agent-memory.json

if [[ "$role" == edge ]]; then
    check 'CodeDeploy 실행 및 부팅 자동 시작' service_ready codedeploy-agent
    check 'certbot' command -v certbot
    check '인증서 갱신 timer' service_ready chongchong-certbot-renew.timer
    check '인증서 갱신 실행 파일' test -x /usr/local/sbin/chongchong-renew-certificate
    check 'edge.env root:600' private_file "$root/config/edge.env"
    check 'backend.env root:600' private_file "$root/config/backend.env"
    check 'config 디렉터리 root:700' test "$(stat -c '%u:%a' "$root/config" 2>/dev/null || true)" = 0:700
    for file in edge.conf.template nginx.conf otel-collector.yaml; do
        check "edge-runtime/$file" test -s "$root/edge-runtime/$file"
    done
    check '활성 upstream 설정' test -s "$root/edge-routing/upstream.conf"
    if private_file "$root/config/edge.env"; then
        source "$root/config/edge.env"
        for key in BACKEND_A_HOST BACKEND_B_HOST SSH_USER SSH_KEY SSH_KNOWN_HOSTS SERVER_NAME CLOUD_WATCH_APP_ENV CLOUD_WATCH_AWS_REGION; do
            check "edge 설정: $key" test -n "${!key:-}"
        done
        check 'A/B 주소 구분' test "${BACKEND_A_HOST:-}" != "${BACKEND_B_HOST:-}"
        check '환경 명시(dev/prod)' bash -c '[[ "$1" == dev || "$1" == prod ]]' _ "${CLOUD_WATCH_APP_ENV:-}"
        check '배포 SSH 키 root:600' private_file "${SSH_KEY:-}"
        check 'SSH known_hosts' test -s "${SSH_KNOWN_HOSTS:-}"
        for host in "${BACKEND_A_HOST:-}" "${BACKEND_B_HOST:-}"; do
            check "호스트 키 등록: $host" ssh-keygen -F "$host" -f "${SSH_KNOWN_HOSTS:-}"
        done
        check 'HTTPS 인증서' test -s "/etc/letsencrypt/live/${SERVER_NAME:-}/fullchain.pem"
        check 'HTTPS 개인키' test -s "/etc/letsencrypt/live/${SERVER_NAME:-}/privkey.pem"
        check 'edge Compose 환경 설정' docker compose --env-file "$root/config/edge.env" \
            -f "$script_dir/../../config/docker-compose.edge.yml" --profile metrics config --quiet
    fi
fi
echo '로컬 준비 검사입니다. IAM·보안 그룹·SSH/sudo 연결·RDS·실제 메트릭 수신은 서버 준비 문서에 따라 별도 검증하세요.'
exit "$failed"
