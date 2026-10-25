#!/usr/bin/env bash
set -Eeuo pipefail
[[ "$1" == -a && "$2" == append-config && "$3" == -m && "$4" == ec2 && "$5" == -s && "$6" == -c ]]
printf 'apply\n' >> "$TEST_EVENTS"
[[ "${TEST_APPLY_FAIL:-0}" == 0 ]] || exit 1
cp "${7#file:}" "$TEST_AGENT_ROOT/etc/amazon-cloudwatch-agent.d/file_cloudwatch-agent-memory.json"
if [[ "${TEST_NO_START:-0}" == 0 ]]; then touch "$TEST_ACTIVE"; fi
