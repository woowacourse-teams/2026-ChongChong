#!/usr/bin/env bash

set -euo pipefail

head_branch="${1:?검사할 브랜치 이름을 입력해야 합니다.}"
base_branch="${2:-}"

branch_pattern='^(fe|be|common)/[a-z]+/([1-9][0-9]*-)?[a-z][a-z0-9]*(-[a-z0-9]+)*$'

case "$head_branch" in
  fe/dev)
    expected_base="fe/prod"
    ;;
  be/dev)
    expected_base="be/prod"
    ;;
  *)
    if [[ ! "$head_branch" =~ $branch_pattern ]]; then
      echo "유효하지 않은 브랜치 이름입니다: $head_branch" >&2
      echo "형식: <fe|be|common>/<타입>/[<이슈번호>-]<작업명>" >&2
      exit 1
    fi

    area="${head_branch%%/*}"
    if [[ "$area" == "common" ]]; then
      expected_base="main"
    else
      expected_base="$area/dev"
    fi
    ;;
esac

if [[ -n "$base_branch" && "$base_branch" != "$expected_base" ]]; then
  echo "허용되지 않은 PR 타깃입니다: $head_branch -> $base_branch" >&2
  echo "올바른 PR 타깃: $expected_base" >&2
  exit 1
fi

echo "브랜치 정책을 통과했습니다: $head_branch${base_branch:+ -> $base_branch}"
