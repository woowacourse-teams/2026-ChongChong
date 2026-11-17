# 0061. 리더 공지·과제 목록의 진행 상황을 집계 쿼리로 조회한다

- 날짜: 2026-11-17
- 관련 이슈: [#135](https://github.com/woowacourse-teams/2026-ChongChong/issues/135)
- 관련 결정: [ADR-0015](0015-use-records-for-read-projections.md)

## 배경

리더가 공지 목록과 과제 목록을 조회하면 항목마다 진행 상황을 함께 반환한다.

| 목록 | 반환 값 | 기존 계산 위치 | 순회하는 컬렉션 |
| --- | --- | --- | --- |
| 공지 | `recipientCount`, `readRecipientCount`, `remindAt` | `Notice.getRecipientCount()`, `getReadRecipientCount()`, `getNextRemindAt()` | `recipients`, `reminders` |
| 과제 | `memberCount`, `completeCount`, `remindAt` | `Assignment.getSubmissionCount()`, `getSubmittedCount(now)`, `getNextRemindAt()` | `submissions`, `reminders` |

두 목록 모두 응답 DTO의 `forLeader`에서 이 메서드를 호출했다. 컬렉션은 지연 로딩되므로 항목마다 컬렉션 조회
쿼리가 2개씩 추가로 실행된다(N+1).

Hibernate Statistics로 실행된 SQL 수를 세어 확인했다.

- 리더 공지 목록: `2 + 2 × 페이지 크기`. 기본 페이지 크기 20에서 42개
- 리더 과제 목록: 정렬 방식(최신순, 마감순, 미제출순)과 관계없이 `3 + 2 × 페이지 크기`. 페이지 크기 20에서 43개
- 스터디원 목록: 진행 상황을 사용하지 않으므로 페이지 크기와 관계없이 3개

쿼리 수와 별개로, 화면에는 숫자 두 개와 시각 하나만 필요한데 이를 위해 수신자·제출물 엔티티를 전부 메모리에
올린다. 스터디원이 늘면 로딩되는 엔티티 수도 비례해서 늘어난다.

## 결정

- 리더 목록은 페이지의 항목 ID 목록으로 집계 쿼리를 한 번 호출해 진행 상황을 DB에서 계산한다.
  - 공지 `NoticeRepository.findProgressByIdIn`
    - `recipientCount`: 수신자 수
    - `readRecipientCount`: `read_at IS NOT NULL`인 수신자 수
    - `nextRemindAt`: 상태가 `PENDING`인 리마인더의 최소 `remind_at`
  - 과제 `AssignmentRepository.findProgressByIdIn`
    - `memberCount`: `assigned = true`인 제출물 수
    - `completeCount`: `assigned = true`이고 `submitted_at IS NOT NULL`인 제출물 수
    - `nextRemindAt`: 상태가 `PENDING`인 리마인더의 최소 `remind_at`
- 결과는 `NoticeProgressProjection`, `AssignmentProgressProjection` record로 받는다
  ([ADR-0015](0015-use-records-for-read-projections.md)).
- `NoticeSummaryResponse.forLeader`와 `AssignmentSummaryResponse.forLeader`는 엔티티 컬렉션 대신 집계 값을
  인자로 받는다.
- 엔티티의 진행 상황 메서드는 단건 상세·변경 흐름에서 계속 사용하므로 유지한다. 두 계산이 같은 결과를
  내는지 PostgreSQL 테스트로 고정한다.
- 이후 리더 목록에 항목별 집계 값이 필요하면 같은 방식으로 조회한다.
- `default_batch_fetch_size` 전역 설정은 이번 결정에 포함하지 않는다.

## 선택 이유

목록마다 세 방식을 같은 조건에서 측정했다.

- 측정 환경: PostgreSQL 16(Testcontainers, 로컬), 리더, 페이지 크기 20, 항목당 리마인더 3개,
  스터디원의 절반이 읽음 또는 제출 완료
- 측정 방법: 서비스 메서드 `getList`를 300회 워밍업한 뒤 500회 호출한 시간
- 측정 코드: `NoticeListQueryCountPostgresTest`, `AssignmentListQueryCountPostgresTest`
- 한 목록의 세 방식은 연달아 측정했다. 공지와 과제는 다른 시점에 측정해 장비 부하가 달랐으므로,
  두 표 사이의 절대 시간은 비교하지 않는다.

### 리더 공지 목록

| 방식 | 스터디원 | 쿼리 수 | 로딩 엔티티 | p50 | p95 |
| --- | --- | --- | --- | --- | --- |
| 기존(지연 로딩) | 10 | 42 | 282 | 13.03ms | 17.42ms |
|  | 50 | 42 | 1,082 | 15.70ms | 20.33ms |
|  | 100 | 42 | 2,082 | 19.87ms | 26.28ms |
| `default_batch_fetch_size=100` | 10 | 4 | 295 | 4.14ms | 5.43ms |
|  | 50 | 4 | 1,135 | 5.28ms | 6.96ms |
|  | 100 | 4 | 2,185 | 8.22ms | 11.81ms |
| 집계 쿼리(채택) | 10 | 3 | 22 | 2.79ms | 4.92ms |
|  | 50 | 3 | 22 | 3.25ms | 4.92ms |
|  | 100 | 3 | 22 | 3.10ms | 4.61ms |

### 리더 과제 목록

| 방식 | 스터디원 | 쿼리 수 | 로딩 엔티티 | p50 | p95 |
| --- | --- | --- | --- | --- | --- |
| 기존(지연 로딩) | 10 | 43 | 282 | 5.53ms | 6.25ms |
|  | 50 | 43 | 1,082 | 6.61ms | 7.07ms |
|  | 100 | 43 | 2,082 | 7.94ms | 8.78ms |
| `default_batch_fetch_size=100` | 10 | 5 | 295 | 1.78ms | 2.38ms |
|  | 50 | 5 | 1,135 | 2.79ms | 4.00ms |
|  | 100 | 5 | 2,185 | 4.23ms | 6.17ms |
| 집계 쿼리(채택) | 10 | 4 | 22 | 1.43ms | 1.98ms |
|  | 50 | 4 | 22 | 1.23ms | 1.79ms |
|  | 100 | 4 | 22 | 1.47ms | 1.58ms |

- 집계 쿼리는 두 목록 모두 쿼리 수와 로딩 엔티티 수가 스터디원 수와 페이지 크기에 관계없이 일정하다.
  스터디원 100명에서 기존 대비 p50이 공지는 19.87ms에서 3.10ms, 과제는 7.94ms에서 1.47ms로 줄었다.
- batch fetch도 쿼리 수 문제는 해결한다. 하지만 수신자·제출물 엔티티를 모두 올리는 구조는 그대로라서
  스터디원 수에 비례해 로딩 엔티티와 응답 시간이 늘어났다. 스터디원 100명에서 공지 p50은 8.22ms로
  집계 쿼리의 약 2.7배였다.
- 측정 DB가 같은 호스트에 있어 쿼리당 네트워크 왕복 시간이 거의 0이다. 운영 DB에서는 왕복 시간이 쿼리
  수만큼 더해지므로 기존 방식과의 차이는 이 표보다 커질 것으로 예상한다. 이 부분은 측정하지 않았다.

## 검토한 대안

### `default_batch_fetch_size` 전역 설정

설정 한 줄로 이 화면을 포함한 모든 지연 로딩 컬렉션의 N+1을 IN 쿼리로 묶을 수 있다. 코드 변경이 없다.
그러나 집계를 위해 엔티티를 모두 올리는 비용이 남아, 측정에서 스터디원 수에 따라 응답 시간이 늘어났다.
또한 전역 설정은 다른 화면의 쿼리 형태도 함께 바꾸므로, 영향 범위를 확인하지 않은 채 이 문제의 해결책으로
도입하지 않는다. 전역 도입 여부는 다른 N+1 사례를 확인한 뒤 별도로 결정한다.

### fetch join으로 항목과 컬렉션을 함께 조회

두 컬렉션은 모두 `List`라서 함께 fetch join하면 Hibernate가 `MultipleBagFetchException`으로 거부한다.
타입을 `Set`으로 바꿔도 결과 행이 두 컬렉션 크기의 곱으로 늘어난다. 또한 컬렉션 fetch join과 페이지 크기
제한(`LIMIT`)을 함께 쓰면 Hibernate가 DB가 아닌 메모리에서 페이지를 나누므로 커서 페이지네이션과 맞지 않는다.
엔티티를 모두 올리는 문제도 그대로 남는다.

### 공지·과제 테이블에 집계 컬럼을 두고 갱신

조회 시 집계가 필요 없어 가장 빠르다. 그러나 읽음 처리, 제출, 제출 대상 변경, 리마인더 상태 변경마다 컬럼을
함께 갱신해야 하고, 갱신을 빠뜨리면 값이 원본과 어긋난다. 읽음 처리는 동시 요청이 겹칠 수 있어
([ADR-0019](0019-defer-notice-read-concurrency-strategy.md)) 카운터 갱신에도 동시성 처리가 필요하다.
측정한 규모에서 집계 쿼리가 수 ms 안팎이므로 정합성 위험을 감수할 이유가 없다.

## 영향

### 긍정적 영향

- 리더 공지·과제 목록의 쿼리 수와 응답 시간이 페이지 크기와 스터디원 수에 영향을 받지 않는다.
- 응답 조립에 필요한 값만 조회하므로 영속성 컨텍스트에 올라가는 엔티티가 목록 항목 수준으로 줄어든다.
- 쿼리 수 회귀를 `NoticeListQueryCountPostgresTest`, `AssignmentListQueryCountPostgresTest`가 감지한다.

### 부정적 영향과 위험

- 같은 규칙(수신자·제출 대상 수, 읽음·제출 완료 수, 다음 리마인드 시각)이 엔티티 메서드와 JPQL 두 곳에
  존재한다. 한쪽 규칙만 바뀌면 목록과 상세의 값이 어긋날 수 있다. `NoticeProgressPostgresTest`,
  `AssignmentProgressPostgresTest`가 두 계산의 결과를 비교해 이를 감지한다.
- 집계 쿼리는 항목마다 상관 서브쿼리 3개를 실행한다. `notice_recipients`와 `assignment_submissions`는
  `(notice_id, member_id)`, `(assignment_id, member_id)` 유니크 인덱스를 사용할 수 있지만,
  `notice_reminders.notice_id`와 `assignment_reminders.assignment_id`에는 인덱스가 없다. 리마인더 행이 많아지면
  실행 계획을 확인해야 한다.

## 미확정 사항

- 운영 DB(네트워크 왕복 포함)에서의 개선 폭은 측정하지 않았다.
- `default_batch_fetch_size`를 전역 기본값으로 둘지.

## 후속 작업

1. `NoticeProgressProjection`, `NoticeRepository.findProgressByIdIn` 추가
2. `AssignmentProgressProjection`, `AssignmentRepository.findProgressByIdIn` 추가
3. 두 서비스와 `forLeader`가 집계 값을 사용하도록 변경
4. 테스트
   - `NoticeListQueryCountPostgresTest`, `AssignmentListQueryCountPostgresTest`: 리더 쿼리 수가 페이지 크기와
     무관한지 검증하고, 스터디원 수별 응답 시간을 측정
   - `NoticeProgressPostgresTest`, `AssignmentProgressPostgresTest`: 읽음·제출 대상 제외·발송된 리마인더가 있을 때
     집계 결과와 엔티티 계산 비교
5. 리마인더 테이블의 `EXPLAIN ANALYZE`를 확인하고 부모 ID 인덱스 필요 여부 판단
