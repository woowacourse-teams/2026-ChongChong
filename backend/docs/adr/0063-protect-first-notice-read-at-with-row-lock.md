# 0063. 공지 최초 읽음 시각을 수신자 행 잠금으로 보존한다

- 날짜: 2026-11-17
- 관련 이슈: [#88](https://github.com/woowacourse-teams/2026-ChongChong/issues/88)
- 대체하는 ADR: [0019](0019-defer-notice-read-concurrency-strategy.md)의 동시성 보장 유보 결정
- 관련 ADR: [0029](0029-protect-study-membership-limits-with-row-locks.md)의 행 잠금 방식,
  [0031](0031-run-postgresql-specific-tests-with-testcontainers.md)의 PostgreSQL 전용 테스트

## 배경

[0019](0019-defer-notice-read-concurrency-strategy.md)는 같은 수신자의 읽음 요청이 동시에 실행되면 최초 `readAt`이
덮어써질 수 있음을 기록하고, 해결 방식은 후속 결정으로 미뤘다.

실제 PostgreSQL에서 두 Transaction을 겹쳐 실행해 문제를 재현했다. 첫 번째 요청이 `read_at IS NULL` 상태를 읽고
Commit하기 전에 두 번째 요청이 같은 행을 읽으면, 두 요청 모두 아직 읽지 않았다고 판단해 서로 다른 시각을 기록한다.
그 결과 두 요청이 다른 `readAt`을 응답했고, 나중에 Commit된 값이 최종 저장값이 되었다.

읽음 요청은 같은 사용자가 같은 공지를 거의 동시에 여는 경우에만 경합한다. 따라서 성능보다 코드 단순성, 기존 관례와의
일관성, 테스트 환경 호환성이 더 중요한 판단 기준이다.

## 결정

- `NoticeReadService.markAsRead`는 공지 수신자 행을 `PESSIMISTIC_WRITE`(`SELECT ... FOR UPDATE`)로 조회한 뒤
  기존 Entity 메서드 `NoticeRecipient.markAsRead`로 최초 읽음 시각을 기록한다.
- 잠금을 기다린 요청은 앞선 Transaction이 Commit한 `readAt`을 읽으므로 값을 바꾸지 않고, 저장된 최초 읽음 시각을
  그대로 응답한다.
- 잠금 대상은 해당 수신자 행 하나다. 이 Transaction은 다른 행을 잠그지 않으므로 잠금 순서로 인한 교착 상태 경로가
  새로 생기지 않는다.
- `NoticeReadConcurrencyTest`는 Testcontainers PostgreSQL에서 첫 번째 Transaction이 읽음 처리 후 Commit하지 않은
  상태로 두 번째 요청을 실행하고, 두 응답과 저장된 `readAt`이 모두 같은지 검증한다.

## 선택 이유

세 가지 방식을 같은 재현 테스트로 검증했다.

| 방식 | 재현 테스트 | 관찰 결과 |
| --- | --- | --- |
| 변경 없음 | 실패 | 두 응답의 `readAt`이 다르다. |
| 비관적 쓰기 잠금 | 통과 | 두 번째 요청이 첫 번째 Commit까지 기다린 뒤 같은 값을 응답한다. |
| `COALESCE` 조건부 Update + `RETURNING` | 통과 | PostgreSQL에서는 통과하지만 H2 기반 API 테스트 2건이 500으로 실패한다. |
| 낙관적 잠금(`@Version`) | 실패 | 덮어쓰기는 막지만 늦은 요청이 `ObjectOptimisticLockingFailureException`으로 실패한다. |

비관적 잠금은 Repository 조회 메서드 하나와 호출부 한 줄만 바꾸고, Entity의 상태 변경 흐름과 일반 테스트 환경을 그대로
유지한다. 과제 제출 시각과 스터디 멤버십 상한도 같은 행 잠금 방식을 사용하므로 팀이 이미 아는 패턴이다. 경합이 드문
경로라 잠금 대기 비용은 실질적으로 발생하지 않는다.

## 검토한 대안

### `COALESCE(read_at, :now)` 조건부 Update와 `RETURNING`

쿼리 한 번으로 최초 시각을 보존하고 저장된 값을 반환하며, 잠금을 Update 순간에만 잡는다. 하지만 PostgreSQL 전용
Native Query가 필요해 H2로 실행하는 일반 테스트가 깨지고, 벌크 Update라서 `updated_at` Auditing과 영속성 Context
동기화를 직접 처리해야 한다. 경합이 드문 경로에서 이 비용을 감수할 이유가 없다.

`RETURNING` 없이 `read_at IS NULL` 조건부 Update 후 재조회하는 방식도 같은 원자성을 얻지만, 0건 갱신 분기와
재조회, 영속성 Context 초기화가 추가되어 잠금 방식보다 코드가 복잡하다.

### 낙관적 잠금(`@Version`)

충돌이 없을 때 추가 비용이 거의 없지만, `version` 컬럼 마이그레이션이 필요하고 늦게 Commit한 요청이 예외로 실패한다.
"모든 요청이 저장된 최초 시각을 응답한다"는 계약을 지키려면 Transaction 밖에서 예외를 잡고 재조회하거나 재시도하는
계층을 추가해야 한다. 또한 이 Entity의 다른 모든 갱신에 버전 검사가 적용된다.

### 격리 수준 상향과 애플리케이션 수준 잠금

`REPEATABLE READ` 이상은 직렬화 실패 시 재시도가 필요하다. JVM 내부 잠금은 여러 애플리케이션 인스턴스에서 동작하지
않고, Redis 같은 분산 잠금은 이 문제에 비해 인프라 비용이 크다.

## 영향

### 긍정적 영향

- 같은 수신자의 동시 읽음 요청에서도 최초 읽음 시각이 보존되고, 모든 요청이 같은 `readAt`을 응답한다.
- 기존 Entity 메서드와 H2 기반 테스트를 그대로 유지한다.

### 부정적 영향과 위험

- 이미 읽은 공지를 다시 읽는 요청도 `FOR UPDATE` 조회를 수행한다. 행 하나의 짧은 잠금이므로 영향은 작다.
- 같은 수신자의 요청이 겹치면 뒤 요청은 앞 Transaction이 끝날 때까지 기다린다.

## 미확정 사항

- 없음

## 후속 작업

- 없음
