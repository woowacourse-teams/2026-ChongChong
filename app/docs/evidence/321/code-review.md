# #321 코드·시각 QA Pass A 리뷰

## 범위와 근거

- 목표: UI 전용 역할별 스터디 홈, 알림 목록, 공지/과제의 목록 및 읽기 상세 이동을 제공한다. 공지/과제 전체 워크플로는 다음 이슈 범위다.
- 검사한 변경: `app/src/features/{home,activity}`, `EntryProvider`, `app/study`, `notifications`, `activity/[id]`, 루트 레이아웃, `studies` 및 증거 캡처.
- 증거: `app/docs/evidence/321/member.png`, `app/docs/evidence/321/member-after-read.png`, `app/docs/evidence/321/figma-member.png`.
- 재검증: `pnpm type-check`, `pnpm lint`, `git diff --check` 모두 통과.
- 시각 검토: 두 실제 화면 캡처를 열어 확인했다. 다른 Figma 캔버스 배율 때문에 픽셀 점수나 diff 수치는 산정하지 않았다.

## 스킬 관점 확인

- `programming` TypeScript 기준을 확인했다. 새 코드에서 `any`, 타입 단언, non-null assertion, 억제 주석은 발견하지 못했고, 현재 import/포맷 검사도 통과한다.
- `remove-ai-slops` 기준을 확인했다. 불필요한 파싱·정규화·추출, 프롬프트 테스트, 삭제 전용 테스트, 구현 상수를 그대로 미러링한 테스트, 화면을 이미지로 대체한 구현은 발견하지 못했다.

## Findings

### CRITICAL

없음.

### HIGH

없음. 이전 로그아웃 초기화 누락은 수정됐다. `exit`가 라우팅 전 `resetActivity()`를 호출하고([account.tsx](../../../src/app/account.tsx:26), [account.tsx](../../../src/app/account.tsx:27)), 이 함수가 두 읽음 상태를 모두 비운다([ActivityProvider.tsx](../../../src/features/activity/ActivityProvider.tsx:64)).

### MEDIUM

없음.

### LOW

초기 캡처 확장자 문제는 최종 정리에서 해결했다. 실제 JPEG 파일을 `.jpg`로 보관하고 최신 기본 화면을 다시 촬영했다. [시각 재검토](visual-review.md)에서 최종 형식을 확인했다.

## Good, keep it

- 역할 화면은 선택된 스터디의 role을 직접 사용하며([index.tsx](../../../src/app/study/index.tsx:12), [index.tsx](../../../src/app/study/index.tsx:14)), 스터디 카드와 알림 진입에서 선택값을 갱신한다([studies.tsx](../../../src/app/studies.tsx:60), [notifications.tsx](../../../src/app/notifications.tsx:30)).
- 공지 상세 진입은 읽음 상태를 업데이트하고([id].tsx](../../../src/app/activity/[id].tsx:13)), 읽기 전후 캡처에서 '읽지 않은 공지'가 사라지는 실제 상태 변화를 확인했다.
- 홈/목록/알림은 실제 `Pressable`, 목록, Context 상태로 구성되어 있으며 화면 전체를 이미지로 대체하지 않았다. 색상·글꼴·주요 반경은 기존 토큰을 사용한다.
- 잘못된 상세 ID는 제목과 본문에서 '내용을 찾을 수 없어요'로 안전하게 표시되어 직접 진입이 예외로 종료되지는 않는다([id].tsx](../../../src/app/activity/[id].tsx:25)).

## Verdict

- `codeQualityStatus`: CLEAR
- `recommendation`: APPROVE
- `blockers`: 없음.

이 보고서의 승인은 코드 경로와 정적 검증 결과에 대한 것이다. 이후 주 작업자가 로그아웃 → 목 로그인 → 알림 초기화 UI 회귀를 완료했고 `notifications-logout-reset.jpg`에 기록했다.
