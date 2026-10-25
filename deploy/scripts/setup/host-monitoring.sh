#!/usr/bin/env bash
# 호스트 메모리 설정 적용 함수. 다른 수집 설정을 보호하고 성공한 설정만 기록한다.

configure_host_memory() {
    local source_config="$1" agent_root="$2" state_dir="$3"
    local control="$agent_root/bin/amazon-cloudwatch-agent-ctl"
    local applied="$agent_root/etc/amazon-cloudwatch-agent.d/file_cloudwatch-agent-memory.json"
    local config="$state_dir/cloudwatch-agent-memory.json"
    local verified="$state_dir/memory-config.verified"
    local fragment

    if [[ -e "$applied" || -L "$applied" ]]; then
        if [[ ! -f "$verified" || -L "$verified" || ! -f "$applied" || -L "$applied" ]] \
            || ! cmp -s "$applied" "$verified"; then
            echo "기존 메모리 설정의 관리 이력이 없거나 내용이 변경되었습니다. 병합 검토가 필요합니다: $applied" >&2
            return 1
        fi
    fi

    # namespace는 메트릭 조각 사이에서 공유되므로 다른 지표의 저장 위치를 묵시적으로 바꾸지 않는다.
    for fragment in "$agent_root"/etc/amazon-cloudwatch-agent.d/*; do
        [[ -f "$fragment" && "$fragment" != "$applied" ]] || continue
        if grep -Eq '"metrics"[[:space:]]*:' "$fragment"; then
            echo "다른 메트릭 설정과 병합 검토가 필요합니다: $fragment" >&2
            return 1
        fi
    done
    install -d -m 700 "$state_dir" || return 1
    if cmp -s "$source_config" "$verified" && cmp -s "$source_config" "$applied"; then
        systemctl enable amazon-cloudwatch-agent || return 1
        if ! systemctl is-active --quiet amazon-cloudwatch-agent; then
            systemctl start amazon-cloudwatch-agent || return 1
        fi
    else
        install -m 600 "$source_config" "$config" || return 1
        "$control" -a append-config -m ec2 -s -c "file:$config" || return 1
        systemctl enable amazon-cloudwatch-agent || return 1
    fi
    systemctl is-active --quiet amazon-cloudwatch-agent || return 1
    cmp -s "$source_config" "$applied" || {
        echo '적용된 메모리 설정이 원본과 다릅니다.' >&2
        return 1
    }
    # 설정과 프로세스 실행만 검증한 기록이다. 실제 CloudWatch 수신은 별도로 확인한다.
    install -m 600 "$source_config" "$verified"
}
