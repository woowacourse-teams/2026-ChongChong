# 0064. 반복 과제 규칙을 저장하고 회차를 행 잠금 아래 하나씩 생성한다

- 날짜: 2026-11-22
- 상태: 초안
- 관련 이슈: [#718](https://github.com/woowacourse-teams/2026-ChongChong/issues/718)
- 관련 ADR: [0013](0013-unify-application-time-source.md)의 KST Clock,
  [0024](0024-use-database-cascades-for-hard-delete.md)의 DB CASCADE 삭제,
  [0028](0028-adopt-spec-first-api-contracts.md)의 명세 우선 API,
  [0033](0033-create-logical-reminder-notifications.md)의 논리 리마인드 알림,
  [0060](0060-preserve-database-compatibility-for-application-rollback.md)의 롤백 호환 마이그레이션,
  [0061](0061-clean-up-orphan-files-with-mark-and-sweep.md)의 `SKIP LOCKED` 배치

## 배경

스터디 과제 대부분은 "매주 화요일 23:59 마감"처럼 주 단위로 반복된다. 지금은 리더가 매주 같은 과제를 다시 만들어야 하고,
마감 시각과 리마인드 설정을 빠뜨리기 쉽다.

반복 생성을 설계할 때 현재 구조에서 고려해야 할 점은 다음과 같다.

| 현재 구조 | 반복 생성에 주는 영향 |
| --- | --- |
| `assignment_reminders.remind_at`은 절대 시각이다. | 첫 회차의 리마인드 시각을 다음 회차에 그대로 쓸 수 없다. |
| 제출 대상은 `assignment_submissions` 행으로 표현한다. | 회차를 만들 때 그 시점의 대상 멤버로 행을 만들어야 한다. |
| `assignment_attachments`에 `UNIQUE(file_id)`가 있다. | 같은 파일을 여러 회차에 연결할 수 없다. |
| `Assignment.create`는 과거 마감을 거부한다. | 서버가 멈춰 있던 동안의 회차는 만들 수 없다. |
| 생성 알림은 제출 대상에게만 가고, 만든 사용자는 제외한다. | 자동 생성 회차의 알림 수신자를 따로 정해야 한다. |
| 운영 인스턴스가 A/B 두 대다. | 생성 작업이 동시에 실행되어도 같은 회차가 두 번 생기면 안 된다. |

## 결정

### 반복 규칙을 별도 테이블에 저장한다

`assignment_recurrences`는 "리더가 매 회차 보냈을 생성 요청"을 대신 보관한다.

| 컬럼 | 설명 |
| --- | --- |
| `study_id` | `studies(id)` FK, `ON DELETE CASCADE` |
| `title`, `content`, `submission_method`, `submission_visibility` | 일반 과제와 같은 값과 검증 규칙 |
| `submitter_mode` | `RANGE`(범위 지정) 또는 `SELECTED`(멤버 직접 선택) |
| `submission_target` | `RANGE`일 때 사용하는 `MEMBERS_ONLY`/`MEMBERS_AND_LEADER` |
| `close_time` | 회차 마감 시각 (KST `LocalTime`) |
| `interval_weeks` | 반복 간격(주). 1~4만 허용한다. 1은 매주, 2는 격주 |
| `next_occurrence_date` | 다음에 만들 회차의 마감 날짜 (KST `LocalDate`). 마감 요일은 이 날짜의 요일이다. |
| `next_publish_at` | 다음 회차를 만들 시각. 직전 회차의 규칙상 마감 시각이다. |

- 종료일이나 횟수는 두지 않는다. 리더가 반복 과제 목록에서 규칙을 삭제하면 반복이 끝난다.
- 일시정지·재개는 지원하지 않으므로 상태 컬럼을 두지 않는다. 다시 시작하려면 새로 등록한다.
- 첨부파일은 받지 않는다. 필요하면 리더가 생성된 회차에 직접 첨부한다.
- `next_publish_at`에 인덱스를 둔다. 워커는 이 값이 지난 규칙만 조회한다.

### 리마인드는 마감 기준 상대 시간으로 저장한다

- 리더는 일반 과제처럼 첫 회차의 리마인드 절대 시각을 입력한다.
- 서버는 이를 "마감 N분 전"으로 바꿔 `assignment_recurrence_reminders(recurrence_id, minutes_before_close)`에 저장한다.
  `AssignmentRecurrence`의 `@ElementCollection`으로 매핑한다. 리마인드 오프셋은 상태가 없는 설정값이기 때문이다.
- 등록 시 오프셋은 0보다 크고 반복 간격(`interval_weeks`주)보다 작아야 한다. 간격보다 큰 오프셋은 회차가 생성되기 전 시각이 된다.
- 회차를 만들 때 `close_at - 오프셋`으로 계산한다. 계산한 시각이 이미 지났으면 그 리마인드만 만들지 않는다.

### 직접 선택한 제출 대상은 멤버 FK로 저장한다

- `SELECTED` 규칙의 멤버는 `assignment_recurrence_submitters(recurrence_id, member_id)`에 저장한다.
- `member_id`는 `study_members(id)` FK이며 `ON DELETE CASCADE`다. 스터디를 나간 멤버는 자동으로 빠지고,
  새로 들어온 멤버는 리더가 규칙을 수정해야 포함된다.
- 선택한 멤버가 모두 나가도 `RANGE`로 바뀌지 않도록 `submitter_mode`를 명시적으로 저장한다.
- `RANGE` 규칙은 회차를 만들 때마다 그 시점의 스터디 멤버로 대상을 정한다.
- 생성된 회차의 대상은 일반 과제처럼 고정되고, 회차별로 따로 수정할 수 있다.

### 회차를 규칙과 연결하고 유니크 제약으로 중복을 막는다

`assignments`에 다음 컬럼을 추가한다.

| 컬럼 | 설명 |
| --- | --- |
| `recurrence_id` | `assignment_recurrences(id)` FK, NULL 허용, `ON DELETE SET NULL` |
| `occurrence_date` | 회차가 처음 만들어질 때의 마감 날짜. 이후 리더가 마감을 수정해도 바꾸지 않는다. |

- `UNIQUE(recurrence_id, occurrence_date)`를 건다. PostgreSQL은 NULL끼리 중복으로 보지 않으므로 일반 과제에는 영향이 없다.
- `close_at` 대신 `occurrence_date`를 쓰는 이유는 리더가 한 회차의 마감을 수정해도 회차 식별자가 바뀌지 않게 하기 위해서다.
- 회차 번호와 제목 접미사는 저장하지 않는다. 회차는 마감일로 구분한다.

### 등록 시 첫 회차를 만들고, 이후에는 직전 회차 마감 때 다음 회차를 만든다

- 등록 API는 일반 과제 생성 요청에 `intervalWeeks`를 더해 받는다. `closeAt`은 첫 회차의 마감 시각이다.
- 등록 Transaction에서 규칙을 저장하고 첫 회차를 생성한다. 이때 `next_occurrence_date = 첫 마감 날짜 + interval_weeks주`,
  `next_publish_at = 첫 회차의 close_at`으로 둔다.
- 열린 회차는 규칙당 항상 하나다. 직전 회차가 마감되는 시각에 다음 회차가 생성된다.
- `next_publish_at`은 규칙 기준으로만 움직인다. 리더가 한 회차의 마감을 수정해도 다음 회차 생성 시각은 바뀌지 않는다.

### 워커는 유저 → 스터디 → 규칙 순서로 잠근 뒤 회차 하나를 만든다

`AssignmentRecurrenceWorker`는 각 인스턴스에서 `@Scheduled(fixedDelay)`로 실행된다. 기본 주기는 1분, 배치 크기는 100이며
설정값으로 바꿀 수 있다. 두 값은 부하 테스트 결과에 따라 조정한다.

```text
1. 후보 조회 (잠금 없음)
   SELECT id, study_id FROM assignment_recurrences
   WHERE next_publish_at <= :now ORDER BY next_publish_at LIMIT :batchSize

2. 후보마다 Transaction 하나
   스터디 멤버의 users  FOR SHARE (id 오름차순)
   → studies            FOR SHARE
   → 규칙               FOR UPDATE SKIP LOCKED, next_publish_at <= :now 재확인
      잠금을 얻지 못했거나 조건이 바뀌었으면 건너뛴다.
   → 회차 마감 계산: next_occurrence_date + close_time
      마감이 이미 지났으면 미래가 될 때까지 interval_weeks씩 넘기고, 건너뛴 회차 수를 로그로 남긴다.
   → 제출 대상 결정. 0명이면 회차를 만들지 않고 로그를 남긴다.
   → 과제·제출 대상 행·리마인드·생성 알림 행 INSERT
   → next_publish_at = 이번 회차 마감, next_occurrence_date += interval_weeks주
   Commit
```

- 잠금 순서는 기존 과제 생성(`AssignmentService.create`)과 스터디 삭제(`StudyService.deleteStudy`)가 따르는
  "부모 먼저" 순서와 같다. 규칙을 먼저 잠그면 스터디 삭제(스터디 `FOR UPDATE` → CASCADE로 규칙 삭제)와 교착 상태가 생길 수 있다.
- 스터디와 유저는 `FOR SHARE`이므로 두 인스턴스가 동시에 잡아도 서로 막지 않는다. 생성 주체는 규칙의 `SKIP LOCKED`가 정한다.
- Transaction 안에서는 DB 작업만 한다. 푸시 발송 같은 외부 호출을 넣지 않는다. 생성 알림은 논리 알림 행만 저장하고
  발송은 `NotificationDeliveryWorker`가 맡는다([0038](0038-separate-logical-notifications-and-web-push-delivery.md)).
- `UNIQUE(recurrence_id, occurrence_date)` 위반은 "이미 생성됨"으로 본다. 위반으로 Transaction이 중단되므로,
  새 Transaction에서 규칙을 다시 잠그고 `next_publish_at`과 `next_occurrence_date`를 그 회차 다음으로 옮긴 뒤 경고 로그를 남긴다.
- 리더 API와 워커는 권한 검사 이후의 회차 생성 로직을 공유한다. 리더 API는 기존처럼 권한 검사 후 이 로직을 호출한다.

### 자동 생성 회차의 생성 알림은 리더도 받는다

- 수신자는 그 회차의 제출 대상 전체와 스터디 리더다. 리더가 제출 대상이면 한 번만 보낸다.
- 리더가 등록 API로 첫 회차를 만들 때는 기존 일반 과제와 같이 리더 본인을 제외한다.

### 규칙 수정과 삭제는 이미 생성된 회차를 바꾸지 않는다

- 규칙 수정은 앞으로 생성될 회차에만 반영한다. 열린 회차는 리더가 일반 과제 수정 API로 직접 고친다.
- 마감 요일·시각·간격을 바꿀 때는 리더가 다음 회차의 마감 일시(`nextCloseAt`)를 입력한다. 등록 시 첫 회차 마감을 입력하는 것과 같은 방식이다.
  서버는 이 값으로 `next_occurrence_date`와 `close_time`을 다시 정한다. `nextCloseAt`은 열린 회차의 규칙상 마감보다 뒤여야 하며,
  `next_publish_at`은 바꾸지 않는다. 따라서 다음 회차는 지금처럼 열린 회차가 마감될 때 새 일정으로 생성된다.
- 규칙을 삭제하면 생성된 회차와 제출물은 남고 `recurrence_id`만 NULL이 된다.
- 규칙 수정·삭제 API는 규칙 행만 `FOR UPDATE`로 잠근다. 워커가 같은 규칙을 처리 중이면 Commit까지 기다린다.

### API

[0028](0028-adopt-spec-first-api-contracts.md)에 따라 OpenAPI 명세를 먼저 작성한다. 리더만 호출할 수 있다.

| 메서드 | 경로 | 설명 |
| --- | --- | --- |
| `POST` | `/studies/{studyId}/assignment-recurrences` | 규칙 등록과 첫 회차 생성 |
| `GET` | `/studies/{studyId}/assignment-recurrences` | 반복 과제 목록 |
| `PATCH` | `/studies/{studyId}/assignment-recurrences/{recurrenceId}` | 규칙 수정 (이후 회차부터) |
| `DELETE` | `/studies/{studyId}/assignment-recurrences/{recurrenceId}` | 규칙 삭제 (회차는 유지) |

## 선택 이유

- 회차를 하나씩 만들면 생성 알림이 한꺼번에 몰리지 않고, 규칙 수정이 이미 만든 회차와 어긋나지 않는다.
- 상대 리마인드를 규칙에 저장하면 한 회차만 고친 내용이 다음 회차로 번지지 않는다.
- 규칙 행 잠금 아래 회차 생성과 `next_publish_at` 이동을 한 Transaction으로 처리하면 정상 경로에서 중복이 생기지 않는다.
  유니크 제약은 잠금 로직에 결함이 있을 때를 막는 마지막 방어선이다.
- 외부 호출이 없으므로 [0061](0061-clean-up-orphan-files-with-mark-and-sweep.md)처럼 선점 컬럼을 둘 필요가 없다.
  작업 중 인스턴스가 죽으면 Rollback과 함께 잠금이 풀리고, 다음 주기에 다시 시도한다.
- 일반 과제와 같은 생성 로직을 쓰므로 제출, 제출 현황, 리마인드, 생성 알림, 캘린더가 회차에도 그대로 동작한다.

## 검토한 대안

### 등록 시 여러 회차를 미리 생성

미래 과제를 미리 보여 줄 수 있지만, 생성 알림이 한꺼번에 나가고 규칙을 수정하면 이미 만든 회차와 어긋난다.

### 직전 회차를 복사해 다음 회차 생성

규칙 테이블에 리마인드나 템플릿을 저장하지 않아도 된다. 하지만 리더가 한 회차만 바꾼 마감·내용이 이후 회차로 계속 복사되고,
직전 회차를 삭제하면 기준이 사라진다.

### 유니크 제약만으로 중복 방지

잠금 없이 단순하지만, 두 인스턴스가 매 주기 모두 회차를 만들고 진 쪽은 예외로 Rollback된다. 정상 동작에서 매번 예외가 발생한다.

### 선점 컬럼(`claimed_at`)을 둔 2단계 처리

잠금을 짧게 잡을 수 있지만, 이 작업에는 잠금 밖에서 기다릴 외부 호출이 없다. 선점한 채 멈춘 행을 만료시키는 처리만 늘어난다.

### 첨부파일 복사 또는 파일 공유 허용

회차마다 S3 객체를 복사하면 복사 실패와 고아 파일 정리가 얽힌다. `UNIQUE(file_id)`를 풀면
[0061](0061-clean-up-orphan-files-with-mark-and-sweep.md)의 참조 판단을 바꿔야 한다. 반복 과제는 회차마다 자료가 다른 경우가 많아 1차 범위에서 제외한다.

### 밀린 회차를 모두 생성

서버가 멈춰 있던 동안의 회차를 모두 만들면 이미 마감된 과제와 알림이 한꺼번에 생긴다. 지난 회차는 제출할 수도 없다.

## 영향

### 긍정적 영향

- 리더가 반복 규칙을 한 번 등록하면 회차가 자동으로 생성된다.
- 생성된 회차는 일반 과제와 같은 테이블과 로직을 쓰므로 기존 기능이 그대로 동작한다.
- 두 인스턴스가 동시에 실행되어도 같은 회차가 두 번 생기지 않는다.
- 규칙을 수정하거나 삭제해도 제출물이 있는 회차는 보존된다.

### 부정적 영향과 위험

- 워커가 규칙을 처리하는 동안 리더의 규칙 수정·삭제 요청은 잠깐 기다린다.
- 회차 생성 Transaction에 외부 호출이 추가되면 잠금 시간이 길어진다. 리뷰에서 이 규칙을 확인해야 한다.
- 워커 주기만큼 다음 회차 생성이 늦어질 수 있다. 기본 1분이면 직전 회차 마감 후 최대 약 1분 뒤에 생성된다.
- `SELECTED` 규칙은 새로 들어온 멤버를 자동으로 포함하지 않는다.

### 마이그레이션 호환성 ([0060](0060-preserve-database-compatibility-for-application-rollback.md))

- 새 테이블 3개와 `assignments`의 NULL 허용 컬럼 2개, 유니크 제약을 추가한다. 기존 컬럼과 저장 값은 바꾸지 않는다.
- 이전 앱은 새 컬럼을 모르므로 과제를 NULL로 INSERT하며, 유니크 제약에 걸리지 않는다.
- 이전 앱의 스터디 삭제와 멤버 탈퇴는 새 테이블의 CASCADE로 처리된다.
- 이전 앱으로 롤백하면 회차 자동 생성만 멈춘다. 다시 배포하면 지난 회차를 건너뛰고 이어서 생성한다.

## 미확정 사항

- 없음

## 후속 작업

- Flyway 마이그레이션 추가: 새 테이블 3개, `assignments` 컬럼과 유니크 제약, `next_publish_at` 인덱스
- OpenAPI 명세 작성 후 반복 규칙 등록·목록·수정·삭제 API 구현
- 회차 생성 로직을 `AssignmentService.create`에서 분리해 리더 API와 워커가 공유
- `AssignmentRecurrenceWorker` 구현
- 부하 테스트로 워커 주기와 배치 크기 검증 후 기본값 조정
- Testcontainers 테스트: 두 Transaction 동시 실행 시 회차 하나만 생성, 스터디 삭제와 워커 동시 실행 시 교착 상태 없음,
  밀린 회차 건너뛰기, 대상 0명 건너뛰기, KST 마감·리마인드 계산, 규칙 삭제 후 회차와 제출물 보존
