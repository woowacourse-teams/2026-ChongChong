#!/usr/bin/env bash

set -euo pipefail

head_branch="${1:?검사할 브랜치 이름을 입력해야 합니다.}"
base_branch="${2:-}"

branch_pattern='^(fe|be|common)/[a-z][a-z0-9]*(-[a-z0-9]+)*/([1-9][0-9]*-)?[a-z][a-z0-9]*(-[a-z0-9]+)*$'

case "$head_branch" in
  dev)
    expected_base="prod"
    ;;
  *)
    if [[ ! "$head_branch" =~ $branch_pattern ]]; then
      echo "유효하지 않은 브랜치 이름입니다: $head_branch" >&2
      echo "형식: <fe|be|common>/<작업성격>/[<이슈번호>-]<작업명>" >&2
      echo "작업성격과 작업명: 소문자로 시작하는 kebab-case" >&2
      exit 1
    fi

    expected_base="dev"
    ;;
esac

if [[ -n "$base_branch" && "$base_branch" != "$expected_base" ]]; then
  echo "허용되지 않은 PR 타깃입니다: $head_branch -> $base_branch" >&2
  echo "올바른 PR 타깃: $expected_base" >&2
  exit 1
fi

echo "브랜치 정책을 통과했습니다: $head_branch${base_branch:+ -> $base_branch}"
