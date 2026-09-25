# 0038. 논리 알림과 Web Push 전달을 NotificationDelivery 파이프라인으로 분리한다

- 날짜: 2026-09-26
- 관련 이슈: [#329](https://github.com/woowacourse-teams/2026-ChongChong/issues/329)
- 관련 ADR: [0033. 리마인드 시각에 논리 알림을 생성하고 발송·동시성 전략은 후속으로 결정한다](0033-create-logical-reminder-notifications.md),
  [0035. 수동 알림 검증 방식을 검토 및 선택한다](0035-detect-notification-events-in-backend-and-separate-channel-delivery.md),
  [0037. 브라우저 Web Push 구독을 endpoint 단위로 관리한다](0037-manage-browser-web-push-subscriptions.md)

## 배경

기존 알림 구조는 `Notification` 저장, 앱 푸시 토큰, Discord 수동 알림이 각각 준비된 상태였지만 실제 브라우저 Web
Push 전달까지 연결되어 있지 않았다. 앱 푸시 토큰은 설치 단위의 문자열만 표현하므로 PC·모바일·브라우저별 Web Push
구독의 `endpoint`, `p256dh`, `auth` 조합을 저장할 수 없다.

논리 알림 생성과 외부 Push provider 호출은 서로 다른 실패 경계를 가진다. 도메인 저장 트랜잭션 안에서 provider를
호출하면 네트워크 지연이나 provider 오류가 공지·과제·제출 저장을 실패시키고, 서버가 전송 중 종료될 때 재처리할
영속적인 기준도 없다. 반대로 메모리 이벤트만 사용하면 프로세스 종료 시 전송 대상이 유실될 수 있다.

ADR 0033은 리마인드의 논리 알림 생성까지만 결정했고, ADR 0037은 Web Push 구독 저장까지만 결정했다. 이번 ADR은
두 결정을 연결해 논리 알림과 구독별 전달 기록을 데이터베이스에 저장하고, 별도 worker가 실제 전송하도록 하는 현재
구조를 확정한다.

## 결정

### `Notification`은 사용자에게 보여줄 논리 알림의 원본으로 저장한다

`NotificationService`는 공지·과제·과제 최초 제출 이벤트와 공지·과제 리마인드에 대해 수신자별 `Notification`을
생성한다.

- 수신자는 `StudyMember`가 아니라 `StudyMember.getUser()`로 저장한다.
- 새 공지·과제·제출물은 `NotificationType.NEW`로 저장한다.
- 리마인드는 `NotificationType.REMIND`로 저장한다.
- `resourceType`, `resourceId`로 원본 리소스를 식별한다.
- `title`, `body`, `deepLink`는 생성 시점의 snapshot으로 저장한다.
- 알림 목록 조회와 읽음 처리는 이 논리 알림을 기준으로 동작한다.

논리 알림과 활성 Web Push 구독별 `NotificationDelivery` 생성은 같은 데이터베이스 트랜잭션에서 수행한다. 따라서
알림은 저장됐지만 전달 대상이 누락되거나, 전달 대상만 남는 상태를 만들지 않는다.

### Web Push 구독은 endpoint 단위로 별도 관리한다

`web_push_subscriptions`는 사용자와 브라우저 구독의 관계를 저장한다.

- `user_id`, `endpoint`, `p256dh`, `auth`, `is_active`를 저장한다.
- `endpoint`는 데이터베이스에서 전역 유일하다.
- 같은 사용자가 같은 endpoint를 다시 등록하면 `p256dh`, `auth`를 갱신하고 활성화한다.
- 다른 사용자가 이미 등록한 endpoint는 `409`로 거부한다.
- 비활성화 시 행을 삭제하지 않고 `is_active = false`로 보존한다.
- 세션 만료만으로 구독을 비활성화하지 않는다.
- 로그인·로그아웃 시 사용자의 모든 구독을 일괄 활성화·비활성화하는 정책은 아직 결정하지 않는다.

기존 `push_tokens`는 Web Push 구독으로 변환하지 않고 제거한다. Web Push provider의 성공·실패 시각과 최근 오류를
구독 행에 누적하는 `last_success_at`, `last_failure_at`, `last_error`도 현재 모델에는 저장하지 않는다.

### 전달 대상은 `Notification`과 Web Push 구독의 조합마다 만든다

활성 구독이 있는 사용자에게 논리 알림을 만들 때 구독마다 `NotificationDelivery`를 만든다. 한 알림과 한 구독의
조합은 다음 유일 제약으로 중복 전달 대상을 막는다.

```text
UNIQUE (notification_id, web_push_subscription_id)
```

Delivery는 다음 정보를 관리한다.

- `status`: `PENDING`, `PROCESSING`, `SENT`, `RETRY_WAIT`, `FAILED`
- `claimed_at`: worker가 처리 대상으로 선점한 시각
- `sent_at`: provider 성공 응답을 반영한 시각
- `attempt_count`: 전송 시도 횟수
- `next_retry_at`: 다음 재시도 가능 시각
- `last_error`: 마지막 실패 사유

논리 알림 자체의 중복 방지 키와 유일 제약은 이번 결정에 포함하지 않는다. 따라서 동일 도메인 이벤트가 애플리케이션
수준에서 다시 실행될 때 `Notification`이 중복 생성될 가능성은 남아 있으며, 별도 후속 결정으로 다룬다.

### Delivery worker가 선점·전송·상태 갱신을 담당한다

전체 흐름은 다음과 같다.

```text
도메인 데이터 저장
  → Notification + 활성 구독별 NotificationDelivery 저장
  → 트랜잭션 커밋
  → DeliveryRecoveryService가 오래된 PROCESSING 복구
  → DeliveryClaimService가 PENDING/재시도 대기 Delivery 선점
  → WebPushNotificationSender가 외부 provider 호출
  → DeliveryResultService가 결과를 별도 트랜잭션으로 반영
```

Delivery claim query는 PostgreSQL의 `FOR UPDATE SKIP LOCKED`를 사용한다. 여러 애플리케이션 인스턴스의 worker가 동시에
실행되어도 같은 Delivery를 함께 선점하지 않도록 하기 위해서다.

현재 기본 실행 정책은 다음과 같다.

- Delivery worker 고정 지연: 1초 (`web-push.delivery.fixed-delay-ms`)
- Delivery batch size: 100
- `PROCESSING` 복구 기준: 5분 (`web-push.delivery.processing-timeout-ms=300000`)
- 재시도 지연: 1분, 2분, 4분, 8분
- 최대 시도 횟수: 5회

상태 전이는 다음 규칙을 따른다.

```text
PENDING/RETRY_WAIT → PROCESSING
PROCESSING → SENT
PROCESSING → RETRY_WAIT
PROCESSING → FAILED
```

서버가 처리 중 종료되어 `PROCESSING`이 기준 시간보다 오래 유지되면 `DeliveryRecoveryService`가
`RETRY_WAIT`로 되돌린다. 최대 시도 횟수에 도달한 경우에는 `FAILED`로 종료한다.

### Web Push provider 호출은 도메인 트랜잭션 밖에서 수행한다

`WebPushNotificationSender`는 저장된 Notification snapshot을 JSON payload로 만들고 `WebPushClient`에 전달한다.
`WebPushClient`는 VAPID public key, private key, subject로 초기화된 provider client를 사용한다.

- VAPID private key와 subject는 백엔드 설정으로만 관리한다.
- 브라우저가 구독을 생성하는 데 필요한 public key만 설정 API로 제공한다.
- 2xx 응답은 `SENT`로 처리한다.
- 404·410 응답은 만료된 구독으로 보고 Delivery를 실패 처리한 뒤 해당 구독을 비활성화한다.
- 429·5xx 응답과 네트워크 통신 오류는 `RETRY_WAIT` 대상으로 처리한다.
- 그 외 provider 오류, 암호화 오류, payload 직렬화 오류는 `FAILED`로 처리한다.
- 인터럽트가 발생하면 현재 스레드의 인터럽트 상태를 보존하고 현재 batch 처리를 중단한다.

선점과 결과 반영은 각각 데이터베이스 트랜잭션으로 수행하고, 외부 HTTP 호출 자체는 트랜잭션에 포함하지 않는다.
따라서 Web Push 실패가 이미 커밋된 공지·과제·제출 저장을 롤백시키지 않는다.

### 리마인드는 별도 scheduler가 논리 알림 생성을 시작한다

`NotificationReminderWorker`는 기본 60초 주기로 `NotificationService`를 호출한다. 서비스는 주입된 `Clock`을 기준으로
`remind_at <= now`인 `PENDING` 리마인드를 최대 100건씩 `FOR UPDATE SKIP LOCKED`로 선점한다.

- 공지 리마인드는 읽지 않은 대상자만 선택한다.
- 과제 리마인드는 제출하지 않은 대상자만 선택한다.
- 리마인드를 `PROCESSING`으로 바꾸고 Notification과 Delivery를 생성한 뒤 같은 트랜잭션에서 `SENT`로 변경한다.
- 실제 Web Push provider 전송은 리마인드 worker가 아니라 Delivery worker가 담당한다.

리마인드의 `SENT`는 provider 전송 성공이 아니라 논리 알림과 Delivery 생성 완료를 의미한다. 실제 provider 결과는
각 `NotificationDelivery`의 상태로 확인한다.

## 선택 이유

논리 알림을 먼저 영속화하고 구독별 Delivery를 별도 상태로 관리하면 다음 요구를 동시에 만족할 수 있다.

- 한 사용자의 PC·모바일·브라우저 구독에 독립적으로 전송한다.
- 일부 구독의 실패가 다른 구독의 성공과 논리 알림 저장에 영향을 주지 않는다.
- 서버 재시작이나 provider 네트워크 오류 뒤에도 재시도할 수 있다.
- 여러 worker가 실행되어도 데이터베이스 선점으로 중복 전송을 줄인다.
- 원본 공지·과제의 현재 값이 바뀌거나 삭제되어도 Notification snapshot으로 당시 알림 내용을 유지한다.

`Notification`만 저장하고 전송 시점에 구독을 조회하는 방식은 여러 브라우저 구독별 상태와 부분 실패를 기록할 수 없다.
반대로 도메인 저장 트랜잭션에서 provider를 직접 호출하면 외부 시스템의 불확실성이 핵심 도메인 트랜잭션으로 전파된다.
데이터베이스 Delivery를 영속적인 작업 대상으로 선택하면 별도 메시지 브로커 없이 현재 공유 PostgreSQL 환경에서
선점·재시도·복구를 구현할 수 있다.

## 검토한 대안

### 도메인 트랜잭션 안에서 Web Push를 직접 발송한다

provider 응답을 즉시 확인할 수 있지만 네트워크 지연이 도메인 요청에 포함되고, provider 오류가 공지·과제·제출 저장을
롤백시킬 수 있다. 서버가 전송 중 종료됐을 때 재시도 기준도 불명확하므로 선택하지 않았다.

### ApplicationEventPublisher의 AFTER_COMMIT 이벤트만 사용한다

도메인 저장과 provider 호출을 분리할 수 있지만, 이벤트 처리 전에 프로세스가 종료되면 전송 대상이 유실될 수 있다.
Delivery를 먼저 데이터베이스에 저장해 재시작 후에도 worker가 다시 찾을 수 있도록 하는 현재 방식을 선택했다.

### Notification 하나당 사용자에게 직접 한 번만 발송한다

한 사용자의 여러 브라우저 구독과 구독별 성공·실패·재시도를 표현할 수 없다. 구독 조합마다 Delivery를 만드는 방식을
선택했다.

### 기존 `push_tokens`를 Web Push 구독 저장소로 재사용한다

앱 토큰과 Web Push 구독은 데이터 구조와 수명주기가 다르고, endpoint와 암호화 키 및 여러 브라우저 구독을 표현하기
어렵다. ADR 0037의 결정에 따라 별도 테이블을 사용한다.

### `PROCESSING` 없이 `PENDING → SENT`로 바로 처리한다

worker가 어떤 Delivery를 맡았는지 기록할 수 없고, 처리 중 서버가 종료된 작업을 찾아 재처리하기 어렵다. 선점 시각과
고착 복구가 필요한 현재 요구에 맞지 않아 선택하지 않았다.

## 영향

### 긍정적 영향

- 논리 알림, 구독, 외부 전달의 책임과 실패 경계가 분리된다.
- PC와 모바일을 포함한 사용자별 다중 브라우저 구독을 지원한다.
- provider 오류를 재시도 가능·만료 구독·영구 실패로 구분할 수 있다.
- worker 재시작과 다중 인스턴스 실행을 고려한 선점·복구 구조를 갖는다.
- Notification 목록·읽음 처리와 Web Push 전달을 독립적으로 운영할 수 있다.

### 부정적 영향과 위험

- Notification 생성과 Delivery 생성이 같은 트랜잭션에서 수행되므로 활성 구독 수만큼 저장 작업이 증가한다.
- worker가 모든 Delivery를 처리할 때까지 실제 알림 전송 지연이 발생할 수 있다.
- provider별 응답·암호화 라이브러리 동작에 대한 운영 모니터링이 필요하다.
- 논리 알림 중복 방지 키가 아직 없어 동일 이벤트 재실행 시 Notification이 중복될 수 있다.
- 구독별 성공·실패 통계는 저장하지 않으므로 장애 원인 분석을 위해 별도 로그·메트릭이 필요하다.

## 미확정 사항

- `Notification`의 논리 중복 방지 키와 `NEW`·`REMIND` 유형별 유일 제약
- 로그인·로그아웃 시 현재 사용자 구독을 어떻게 활성화·비활성화할지
- Delivery batch size, polling 주기, retry 지연과 최대 시도 횟수의 운영 최적값
- Web Push provider 응답과 Delivery 상태를 위한 메트릭·알람 정책
- 현재 단일 PostgreSQL Delivery queue를 메시지 브로커 또는 별도 outbox로 확장할 시점

## 후속 작업

- 논리 알림 중복 방지 정책과 데이터베이스 제약을 별도 ADR로 결정한다.
- 로그인·로그아웃 및 세션 만료와 Web Push 구독의 관계를 별도 정책으로 결정한다.
- dev·prod 환경에 서로 다른 VAPID 키를 안전하게 주입하고 키 교체 절차를 정한다.
- frontend Service Worker가 public key 조회, 구독 등록, 비활성화 API를 연결하도록 계약을 공유한다.
- provider 응답, retry, expired subscription, worker lag를 관찰할 로그·메트릭을 추가한다.
