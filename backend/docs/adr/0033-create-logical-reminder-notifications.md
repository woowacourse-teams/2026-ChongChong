# 0033. 리마인드 시각에 논리 알림을 생성하고 발송·동시성 전략은 후속으로 결정한다

- 날짜: 2026-09-15
- 관련 이슈: [#301](https://github.com/woowacourse-teams/2026-ChongChong/issues/301)
- 관련 ADR: [0013. 애플리케이션 기준 시각을 Clock으로 통일한다](0013-unify-application-time-source.md),
  [0025. 모든 생성 경로에서 성립할 불변식은 도메인 모델이 보호한다](0025-own-stable-invariants-in-domain-model.md),
  [0030. 설치 단위 upsert와 활성 상태로 푸시 토큰을 관리한다](0030-manage-push-tokens-by-installation.md),
  [0031. Testcontainers PostgreSQL로 데이터베이스 특화 테스트를 실행하고 CI에서 Docker를 확인한다](0031-run-postgresql-specific-tests-with-testcontainers.md)
- 후속 ADR: [0038. 논리 알림과 Web Push 전달을 NotificationDelivery 파이프라인으로 분리한다](0038-separate-logical-notifications-and-web-push-delivery.md)에서 당시 미확정으로 남긴
  Delivery 생성, 선점, 재시도와 Web Push 전달 전략을 결정한다.

## 배경

`NoticeReminder`와 `AssignmentReminder`는 리마인드 시각과 처리 상태를 저장하지만, 시각이 도래했을 때 실제
`Notification`을 생성하는 공통 흐름과 연결되어 있지 않았다. 공지와 과제 리마인드를 같은 애플리케이션 서비스로
처리하면서, 리마인드 대상 선정 규칙과 논리 알림의 생성 기준을 먼저 검증할 필요가 있다.

초기 논의에는 설치별 `NotificationDelivery` 저장과 Push provider 호출도 포함되어 있었다. 그러나 논리 알림 생성과
실제 전달은 서로 다른 실패·재시도·동시성 문제를 가지므로, 첫 vertical slice에서는 논리 알림 생성까지만 다룬다.

## 결정

- 공지와 과제 리마인드의 논리 알림 생성 진입점은 `NotificationService.createNotification()`으로 통일한다.
- 현재 시각은 주입된 `Clock`으로 구하고, `PENDING` 상태이면서 `remindAt <= now`인 리마인드만 처리한다.
- 공지 리마인드는 `NoticeRecipient.readAt IS NULL`인 스터디원을 대상으로 한다.
- 과제 리마인드는 `AssignmentSubmission.submittedAt IS NULL`인 스터디원을 대상으로 한다. `submittedAt`은 제출 상태의
  단일 기준이다.
- 리마인드 1건과 수신자 1명의 조합마다 `Notification` 1건을 생성한다.
  - 공지 리마인드: `NotificationResourceType.NOTICE`와 공지 ID를 사용한다.
  - 과제 리마인드: `NotificationResourceType.ASSIGNMENT`와 과제 ID를 사용한다.
  - 알림 유형은 `NotificationType.REMIND`로 저장한다.
- 대상 알림을 생성한 뒤 리마인드를 `SENT`로 변경하는 작업은 하나의 트랜잭션에서 수행한다. 이 범위에서 `SENT`는
  Push provider 전송 성공이 아니라 논리 알림 생성 처리가 끝났다는 의미다.
- 이번 이슈에는 `NotificationDelivery` 생성, `PushToken`별 전달 대상 생성, Push provider 호출, 발송 worker,
  재시도, 실패 토큰 비활성화, 앱 내 알림함, 임의 수신자·메시지 발송 API를 포함하지 않는다.
- 스케줄러의 실행 주기와 실제 서비스 호출 진입점도 이번 논리 알림 생성 구현과 분리해 후속 작업에서 결정한다.

## 선택 이유

리마인드 대상 선정과 `Notification` 생성은 외부 Push provider의 성공 여부와 독립적인 도메인 동작이다. 먼저 이
경계를 검증하면 공지와 과제의 수신자 규칙, 알림 리소스 연결, 기준 시각을 작은 범위에서 확인할 수 있다. 또한
전달 실패와 재시도 정책이 논리 알림 생성 트랜잭션을 오염시키지 않도록 책임을 분리할 수 있다.

`Clock`을 사용하면 테스트에서 기준 시각을 고정할 수 있고, `remindAt <= now` 조건으로 경계 시각에 도달한
리마인드도 누락하지 않는다. 공지의 `readAt`과 과제의 `submittedAt`을 각각 null 여부로 판단해 도메인 상태 표현도
일관되게 유지한다.

## 검토한 대안

### 논리 알림 생성과 Push 전달을 한 트랜잭션에서 처리한다

provider 네트워크 지연·실패·재시도와 데이터베이스 트랜잭션을 결합하게 된다. provider 결과에 따라 리마인드 상태를
결정해야 하므로 처리 시간이 길어지고, 부분 실패와 재실행 기준도 복잡해진다. 첫 vertical slice의 검증 범위를
불필요하게 넓히므로 선택하지 않았다.

### 리마인드마다 수신자에게 직접 Push를 보낸다

논리 알림의 저장 기록과 설치별 전달 기록이 분리되지 않아 여러 기기, 토큰 회전, 실패 토큰 비활성화, 재시도를
안전하게 처리하기 어렵다. 후속 전달 파이프라인에서 `Notification`과 설치 단위 delivery를 연결하는 방식으로
검토한다.

### 현재 단계에서 리마인드를 동시 실행에 안전하게 처리한다

처리 주체가 하나라는 가정 없이 바로 동시성 제어를 넣으려면 claim 상태, 행 잠금 또는 원자적 상태 변경과 논리 알림
중복 기준을 함께 확정해야 한다. 첫 구현에서 이 결정을 임시로 고정하면 이후 스케줄러·worker 구조와 충돌할 수 있어
후속 ADR로 유보한다.

## 영향

### 긍정적 영향

- 공지와 과제 리마인드가 하나의 공통 알림 생성 서비스로 연결된다.
- 리마인드 시각, 수신자 선정, 알림 리소스 연결을 외부 Push provider 없이 검증할 수 있다.
- 과제 제출 상태는 `submittedAt` 하나로 판단하고 공지 읽음 상태와 같은 null 의미를 사용한다.
- 논리 알림 생성 실패와 실제 전달 실패의 책임을 분리할 수 있다.

### 부정적 영향과 위험

- 현재 `NotificationService`를 여러 실행 주체가 동시에 호출하면 같은 리마인드를 중복 처리할 수 있다.
- 논리 알림 자체에 리마인드·수신자 기준의 유일 제약이 없어, `SENT` 변경만으로 중복 생성을 방지하지 못한다.
- 현재 구현만으로는 리마인드 시각에 서비스가 자동 호출되지 않는다.
- `SENT`라는 상태명이 실제 Push 전송 완료로 오해될 수 있으므로 전달 파이프라인 도입 시 상태 의미를 재검토해야 한다.

## 미확정 사항

- 스케줄러 실행 주기, 배치 크기, 실패한 배치의 재실행 단위
- 여러 scheduler·worker가 같은 리마인드를 처리할 때의 claim 방식
  (`PROCESSING` 상태, 원자적 UPDATE, 행 잠금·`SKIP LOCKED` 등)
- 논리 알림의 멱등성 키와 데이터베이스 유일 제약
  (예: 리마인드 ID와 수신자 회원 ID 조합)
- 리마인드 처리 중 수신자가 공지를 읽거나 과제를 제출하는 경우의 기준 시점
- 논리 알림 생성 이후 `NotificationDelivery`와 Push provider를 연결하는 상태·재시도·실패 토큰 정책

## 후속 작업

- 스케줄러 또는 명시적 worker가 도래한 리마인드를 주기적으로 호출하도록 연결한다.
- 리마인드 claim과 멱등성 키를 결정하고 PostgreSQL 동시성 통합 테스트를 추가한다.
- 동시 실행, 재실행, 대상 상태 변경 경쟁 상황에서 중복·누락이 없는지 검증한다.
- 별도 이슈에서 `NotificationDelivery` 생성, Push provider 전송, 재시도, 실패 토큰 비활성화를 구현한다.
