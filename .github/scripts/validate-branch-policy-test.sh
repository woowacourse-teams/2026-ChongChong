#!/usr/bin/env bash

set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
validator="$script_dir/validate-branch-policy.sh"

expect_pass() {
  "$validator" "$1" "$2" >/dev/null
}

expect_fail() {
  if "$validator" "$1" "$2" >/dev/null 2>&1; then
    echo "예상과 달리 정책을 통과했습니다: $1 -> $2" >&2
    exit 1
  fi
}

expect_pass "fe/feat/12-login" "dev"
expect_pass "fe/fix/login" "dev"
expect_pass "fe/docs/contributing" "dev"
expect_pass "fe/style/format" "dev"
expect_pass "be/refactor/23-auth" "dev"
expect_pass "be/perf/query" "dev"
expect_pass "be/test/auth" "dev"
expect_pass "common/chore/coderabbit" "dev"
expect_pass "dev" "prod"

expect_fail "fe/feat/12-login" "prod"
expect_fail "be/fix/login" "prod"
expect_fail "common/chore/coderabbit" "prod"
expect_fail "dev" "main"
expect_fail "fe/dev" "dev"
expect_fail "be/dev" "prod"
expect_fail "prod" "main"
expect_fail "feature/feat/login" "dev"
expect_fail "fe/feature/login" "dev"
expect_fail "be/bugfix/login" "dev"
expect_fail "common/hotfix/policy" "dev"
expect_fail "fe/feat/0-login" "dev"

echo "브랜치 정책 테스트를 통과했습니다."
