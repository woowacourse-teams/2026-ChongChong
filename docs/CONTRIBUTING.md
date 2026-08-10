# 기여 가이드

커밋 메시지와 PR 제목 형식은 [커밋 컨벤션](COMMIT_CONVENTION.md)을 따릅니다.

## 브랜치 규칙

작업 브랜치는 아래 형식을 사용합니다.

```text
<영역>/<타입>/[<이슈번호>-]<작업명>
```

- 영역: `fe`, `be`, `common`
  - `common`: CI, GitHub 설정, 공통 문서 등 저장소 전체 작업
- 타입: 영문 소문자(예: `feat`, `fix`, `chore`)
- 이슈 번호(선택): 0으로 시작하지 않는 양의 정수
- 작업명: 영문 소문자로 시작하고 소문자와 숫자를 사용하며, 여러 단어는 하이픈(`-`)으로 구분

예시:

```text
be/feat/77-ci-cd
fe/fix/81-login-error
be/chore/branch-policy
fe/docs/contributing-guide
common/chore/branch-policy
```

## PR 타깃 규칙

| PR 브랜치                             | 타깃 브랜치 |
| ------------------------------------- | ----------- |
| `fe/<타입>/[<이슈번호>-]<작업명>`     | `dev`       |
| `be/<타입>/[<이슈번호>-]<작업명>`     | `dev`       |
| `common/<타입>/[<이슈번호>-]<작업명>` | `dev`       |
| `dev`                                 | `prod`      |

다른 조합으로 PR을 생성하면 `Branch policy / branch-policy` 검사가 실패합니다.

## 머지 방식

| 타깃 브랜치 | 머지 방식    |
| ----------- | ------------ |
| `dev`       | Squash merge |
| `prod`      | Merge commit |

작업 브랜치의 커밋은 하나로 정리하고, 개발 브랜치에서 운영 브랜치로 승격한 시점은 머지 커밋으로 남깁니다.

## 브랜치 보호 규칙

`dev`, `prod`에는 다음 보호 규칙을 적용합니다.

- Pull Request를 통해서만 병합
- 병합 전 `branch-policy` 상태 검사 통과 필수
- 강제 push 차단
- 브랜치 삭제 제한
- 브랜치 보호 규칙 우회 금지

GitHub 저장소의 **Settings > Rules > Rulesets**에서 위 규칙이 활성화되어 있어야 합니다.
