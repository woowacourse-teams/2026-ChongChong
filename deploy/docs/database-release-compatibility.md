# A/B 배포의 DB 호환성 확인

A/B는 같은 DB를 사용한다. 앱의 트래픽을 이전 서버로 돌리는 것과 DB를 이전 상태로
복원하는 것은 다른 작업이다. 장애 자동 전환에는 DB 복원, Flyway undo·repair,
이미 적용한 migration 수정이 포함되지 않는다.

이번 자동 전환 기능은 DB 스키마를 변경하지 않는다. 아래 절차는 향후 앱 배포에서
특정 구·신 릴리스의 호환성을 확인하기 위한 기준이며, 현재 어떤 릴리스 쌍이
호환된다고 인증한 결과가 아니다.

## 배포 전에 확인할 것과 자동 복귀 전에 확인할 것

`backend/src/main/resources/application.yaml`은 Flyway를 활성화한다. 후보 B가 시작할 때
공유 DB에 migration이 적용될 수 있다. 그 순간에도 기존 A는 HTTP 요청을 처리하며,
`NotificationDeliveryWorker`와 `NotificationReminderWorker`는 두 서버에서 실행될 수 있다.

1. **공존 가능성:** 새 migration을 적용하는 동안과 적용한 뒤에도 기존 A의 API·워커가
   동작해야 한다. 구버전 자동 복귀를 금지해도 이 조건은 배포 전에 필요하다.
2. **복귀 가능성:** 새 B가 작성한 데이터도 기존 A가 읽고 변경할 수 있어야 한다.
   이 조건까지 확인한 구·신 릴리스 쌍에만 구버전 자동 전환을 허용한다.

readiness 성공이나 Flyway validate 성공만으로 업무 호환성을 확인했다고 판단하지 않는다.
Flyway validate는 migration 이력·checksum 검증이며 두 버전 API의 동작을 대신 검증하지 않는다.

## DB 변경은 추가 → 전환 → 제거로 나눈다

예를 들어 컬럼 이름을 변경하려면, 먼저 새 컬럼을 추가하고 구·신 앱이 함께 동작할 수
있게 읽기·쓰기와 기존 데이터 이관을 준비한다. 새 앱으로 전환한 후 구버전으로 복귀할
기간이 끝나고 모든 실행 주체가 새 구조를 사용하는 것을 확인한 다음 별도 배포로 옛 컬럼을
제거한다. 컬럼 추가도 기본값·NOT NULL·잠금 시간·구버전 쓰기 동작을 확인해야 한다.

저장소의 과거 migration은 왜 이 확인이 필요한지 보여 준다.

| migration | 해당 변경을 가로지르는 버전 쌍에서 확인할 점 |
| --- | --- |
| `V5__use_submitted_at_as_assignment_submission_state.sql` | `submitted` 컬럼 삭제. 이 컬럼을 사용하는 구버전과 공존할 수 없다. |
| `V10__replace_notification_types.sql` | `CREATED`·`SUBMITTED`를 `NEW`로 변경. 구버전 enum 처리와 쓰기 제약을 확인해야 한다. |
| `V11__replace_push_tokens_with_web_push_subscriptions.sql` | 컬럼 이름 변경과 `push_tokens` 삭제. 구버전 API·워커의 참조를 확인해야 한다. |
| `V21__allow_multiple_auth_sessions_per_user.sql` | 사용자당 세션 유일 제약 제거. 새 다중 세션 데이터를 구버전도 처리하는지 확인해야 한다. |

이는 과거 변경의 예시이며 현재 배포의 결함이라는 뜻은 아니다. 이미 적용한 migration은
수정하지 않고, 필요한 호환 변경은 후속 migration과 앱 변경으로 준비한다.

## 릴리스 쌍별 검증 절차

1. 이전·신규 이미지 digest, 설정 식별자, migration 목록과 checksum, 목표 PostgreSQL
   버전을 기록한다. 단순 태그 이름이나 공통 `compatible=true`를 다음 배포에 재사용하지 않는다.
2. 운영 DB와 분리된 PostgreSQL에 이전 스키마와 대표 테스트 데이터를 준비한다.
   실제 사용자 데이터·실제 알림 발송 대상은 사용하지 않는다.
3. 이전 앱의 HTTP 요청과 워커를 실행한 채 새 migration을 적용한다. 조회·작성·로그인 갱신·
   제출·첨부·알림 claim의 성공 여부와 DDL 잠금 대기 시간을 확인한다.
4. 새 앱으로 데이터를 생성한 뒤 이전 앱에서 읽고 변경한다. enum 값, 다중 세션,
   제출 상태, 첨부 참조와 알림 상태를 포함한다.
5. 이전 앱을 재시작하여 Flyway/JPA 검사와 readiness, 실제 HTTP 요청을 확인한다.
   계속 실행 중인 프로세스만 정상인 경우를 구분한다.
6. 구·신 워커를 함께 실행하여 claim 경합과 중단된 작업 회수를 확인한다.
   외부 발송 직후 프로세스가 죽는 경우까지 exactly-once로 보장한다고 표현하지 않는다.
7. 확인한 릴리스 쌍, 환경, 명령, 통과·실패·미검증 항목, 확인자를 기록한다.
   공존 근거와 구버전 자동 복귀 근거를 구분해 배포 호환성 기록에 연결한다.

실제 DB 복구가 필요한 장애는 별도 복구 절차로 다룬다. 백업·PITR 복원은 그 이후에
작성된 데이터에 영향을 줄 수 있어 자동 HTTP 전환의 부수 동작으로 실행하지 않는다.

## 기존 테스트와 한계

- `AuthSessionPostgresMigrationTest`: PostgreSQL V20 → V21 이행과 기존 데이터 검증.
- `ReminderClaimPostgresRepositoryTest`: PostgreSQL 동시 claim 검증.
- `PostgresContainerTest`: PostgreSQL 16 Testcontainers와 테스트 알림 발송 기반.
- `AssignmentVisibilityMigrationTest`: V16 → V17 검증이지만 H2 사용.

이 테스트들은 현재 앱이나 migration 자체를 검증한다. 구·신 이미지 두 개를 함께
실행하는 호환성 확인을 대신하지 않는다. Docker가 없어 테스트가 skip되면 Gradle 종료
코드가 0이어도 PostgreSQL 검증을 수행했다고 기록하지 않는다.

참고: [Flyway validate 공식 문서](https://github.com/flyway/flyway/blob/main/documentation/Reference/Commands/Validate.md),
[Flyway undo 공식 문서](https://github.com/flyway/flyway/blob/main/documentation/Reference/Commands/Undo.md).
