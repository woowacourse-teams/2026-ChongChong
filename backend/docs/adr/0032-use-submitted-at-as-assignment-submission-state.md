# 0032. 제출 시각을 과제 제출 상태의 단일 기준으로 사용한다

- 날짜: 2026-09-15
- 관련 이슈: [#301](https://github.com/woowacourse-teams/2026-ChongChong/issues/301)
- 관련 ADR: [0013. 애플리케이션 기준 시각을 Clock으로 통일한다](0013-unify-application-time-source.md),
  [0025. 모든 생성 경로에서 성립할 불변식은 도메인 모델이 보호한다](0025-own-stable-invariants-in-domain-model.md)

## 배경

`AssignmentSubmission`은 제출 여부를 `submitted` boolean과 `submitted_at` 시각으로 함께 저장하고 있었다.
두 값이 서로 다른 상태를 가리킬 수 있고, 제출 여부와 제출 시각을 별도로 갱신해야 하는 중복 상태가 생긴다.

공지 수신자는 `read_at`의 null 여부로 읽음 상태를 판단한다. 과제 제출도 같은 방식으로 표현하면 상태와 시각의 기준이
하나로 통일된다.

## 결정

- `assignment_submissions.submitted` 컬럼을 제거한다.
- `submitted_at`을 과제 제출 상태의 유일한 기준으로 사용한다.
  - `submitted_at IS NULL`: 미제출
  - `submitted_at IS NOT NULL`: 제출 완료
- `AssignmentSubmission.isSubmitted()`는 `submittedAt != null`을 반환한다.
- 최초 제출 시에만 `submittedAt`을 기록하고, 제출 내용 수정은 제출 시각을 변경하지 않는다.
- 조회 Projection과 JPQL은 제출 여부가 필요할 때 `submittedAt`의 null 여부를 사용한다.
- API의 boolean `submitted` 응답은 호환성을 위해 유지하되 `submittedAt`에서 파생한다. 제출물 목록·상세의 `createdAt`은
  `submittedAt`을 사용한다.
- Flyway migration은 제출 시각을 추정하거나 보완하지 않고 `submitted` 컬럼만 삭제한다. 기존 `submitted_at`이 `NULL`인
  행은 migration 이후 미제출로 해석한다.

## 선택 이유

제출 여부와 제출 시각이 하나의 값에서 파생되므로 두 값의 불일치 가능성이 사라진다. 또한 `read_at`을 사용하는 공지
수신자와 동일한 null 의미를 사용해 도메인 모델과 조회 조건을 이해하기 쉬워진다.

## 검토한 대안

### boolean과 제출 시각을 함께 유지한다

기존 코드 변경은 작지만 두 값이 어긋날 수 있고, 모든 생성·수정 경로에서 동기화 규칙을 유지해야 한다. 단일 기준을
확립할 수 없어 선택하지 않았다.

### boolean만 유지한다

제출 상태는 표현할 수 있지만 최초 제출 시각을 안정적으로 제공할 수 없다. 제출 내역과 리마인드 관련 조회에 필요한
시각 정보를 잃으므로 선택하지 않았다.

### 내용 또는 링크의 null 여부로 제출 여부를 추론한다

내용과 링크는 둘 다 없어도 제출할 수 있으므로 제출 상태와 입력 값의 의미가 섞인다. 도메인 상태를 정확하게 표현하지
못하므로 선택하지 않았다.

## 영향

### 긍정적 영향

- 제출 상태와 제출 시각의 불일치가 사라진다.
- 공지 수신자의 `readAt`과 같은 방식으로 상태를 해석할 수 있다.
- 제출 완료 집계와 미제출 리마인드 대상 조회가 동일한 기준을 사용한다.
- 기존 API의 `submitted` 응답 형식은 유지된다.

### 부정적 영향과 위험

- 기존 `submitted` 값과 `submitted_at`이 불일치하는 데이터는 migration에서 보정하지 않는다.
- `submitted` 컬럼을 참조하는 테스트, SQL fixture, native query가 남아 있으면 실행에 실패한다.
- 기존 `submitted = TRUE`이면서 `submitted_at IS NULL`인 행은 제출 상태가 미제출로 바뀔 수 있다.

## 미확정 사항

없음

## 후속 작업

- `submitted`를 참조하는 테스트와 SQL fixture를 `submittedAt` 기준으로 수정한다.
- 제출 상태 Projection과 repository 조회 조건을 새 기준에 맞춘다.
- 운영 배포 전 `submitted = TRUE`이면서 `submitted_at IS NULL`인 기존 데이터가 있는지 확인한다.
