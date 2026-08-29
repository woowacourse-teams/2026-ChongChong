# 커밋 컨벤션

## 기본 형식

[AngularJS Git Commit Guidelines](https://github.com/angular/angular.js/blob/master/DEVELOPERS.md#commits)를 사용합니다.

```text
<type>(<scope>): <subject>

<body>

<footer>
```

- Header는 필수이며 scope는 선택 사항입니다.
- 모든 줄은 100자를 넘지 않습니다.
- Subject는 명령형 현재 시제로 작성하고, 첫 글자를 대문자로 쓰거나 끝에 마침표를 붙이지 않습니다.
- 한국어 Subject는 `추가`, `수정`, `분리`처럼 현재의 변경 동작을 간결하게 표현합니다.
- 하나의 커밋에는 함께 되돌려야 하는 하나의 논리적 변경만 포함합니다.

예시:

```text
feat(frontend): 모임 생성 화면 추가
fix(backend): 로그인 만료 처리 오류 수정
docs: 브랜치 전략 문서화
chore(common): CodeRabbit 리뷰 설정 추가
```

## 타입

| 타입       | 사용 시점                                               |
| ---------- | ------------------------------------------------------- |
| `feat`     | 새로운 기능을 추가할 때                                 |
| `fix`      | 버그를 수정할 때                                        |
| `docs`     | 문서만 추가하거나 수정할 때                             |
| `style`    | 포매팅 등 코드 동작에 영향을 주지 않는 표현을 수정할 때 |
| `refactor` | 동작 변경 없이 코드 구조를 개선할 때                    |
| `perf`     | 성능을 개선할 때                                        |
| `test`     | 테스트를 추가하거나 수정할 때                           |
| `chore`    | 빌드 과정, 보조 도구, 설정 등 유지보수 작업을 할 때     |

## Scope

Scope는 변경이 적용되는 영역을 나타내며 생략할 수 있습니다.

- `frontend`: 프론트엔드 변경
- `backend`: 백엔드 변경
- `common`: 저장소 공통 설정과 도구 변경
- `*`: 둘 이상의 영역에 걸친 변경

## 본문과 이슈 연결

Body도 명령형 현재 시제로 작성하며 변경 동기와 이전 동작과의 차이를 설명합니다. Footer에는 이슈 참조와 호환성을 깨는 변경을 기록합니다.

```text
refactor(backend): 인증 예외 처리 책임 분리

컨트롤러마다 중복되던 인증 예외 변환을 전역 예외 처리기로 이동한다.

Refs #12
```

관련 이슈를 참조만 할 때는 `Refs #<이슈번호>`, 병합하면서 이슈를 닫을 때는 `Closes #<이슈번호>`를 사용합니다.

호환성을 깨는 변경은 `BREAKING CHANGE:`로 시작합니다. 이전 커밋을 되돌릴 때는 `revert: <되돌릴 커밋의 header>` 형식을 사용하고 Body에 `This reverts commit <SHA>.`를 포함합니다.

## 작업 커밋, PR과 Squash 커밋 제목

작업 브랜치의 커밋, PR과 `dev`의 Squash 커밋 제목은 서로 다른 형식을 사용합니다.

| 구분              | 형식                              | 예시                                       |
| ----------------- | --------------------------------- | ------------------------------------------ |
| 작업 브랜치 커밋  | `<type>(<scope>): <subject>`      | `chore(common): CodeRabbit 리뷰 설정 추가` |
| PR 제목           | `[<type>] <summary>`              | `[chore] CodeRabbit 리뷰 설정 추가`        |
| `dev` Squash 커밋 | `[<type>] <summary> (#<PR 번호>)` | `[chore] CodeRabbit 리뷰 설정 추가 (#11)`  |

PR 제목의 타입과 내용은 실제 변경 범위를 대표해야 합니다. 저장소의 Squash merge 기본 제목은 PR 제목으로 설정되어
있으므로 `dev`에 병합하면 GitHub가 PR 제목 뒤에 PR 번호를 붙여 커밋 제목을 자동 생성합니다. Squash merge 화면에서
제목을 수동으로 수정하지 않습니다.
