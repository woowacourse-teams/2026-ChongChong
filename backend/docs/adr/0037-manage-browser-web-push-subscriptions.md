# 0037. 브라우저 Web Push 구독을 endpoint 단위로 관리한다

- 날짜: 2026-09-25
- 관련 이슈: [#329](https://github.com/woowacourse-teams/2026-ChongChong/issues/329)
- 관련 ADR: [0030. 설치 단위 upsert와 활성 상태로 푸시 토큰을 관리한다](0030-manage-push-tokens-by-installation.md)의 Web Push 전환
- 후속 ADR: [0038. 논리 알림과 Web Push 전달을 NotificationDelivery 파이프라인으로 분리한다](0038-separate-logical-notifications-and-web-push-delivery.md)에서 당시 미확정으로 남긴 VAPID sender, Delivery worker, 재시도와 endpoint 만료 처리 전략을 결정한다.

## 배경

기존 앱 푸시 토큰은 `installationId`, `token`, `platform`을 저장했지만, Web Push는 브라우저가 발급한
`endpoint`, `p256dh`, `auth` 조합으로 구독을 식별한다. 이 값들은 브라우저가 생성하므로 백엔드가 직접 만들거나
하나의 토큰 문자열로 대체할 수 없다.

한 사용자는 PC Chrome, 모바일 Chrome, 모바일 Safari 등 여러 브라우저에서 각각 구독할 수 있다. 따라서 Web Push
구독은 사용자당 하나가 아니라 브라우저 구독마다 저장해야 한다. 반대로 로그인 세션 만료는 브라우저 구독을 자동으로
해제하지 않으므로, 구독의 활성 상태와 인증 세션의 생명주기를 같은 값으로 취급하면 안 된다.

iPhone·iPad에서는 일반 Safari 탭이 아니라 홈 화면에 추가한 웹 앱이 Web Push 대상이다. iOS/iPadOS 16.4 이상에서
사용자가 권한을 허용하고 Service Worker가 구독을 생성해야 한다. 자세한 플랫폼 조건은 [Apple WebKit의 Web Push
문서](https://webkit.org/blog/13878/web-push-for-web-apps-on-ios-and-ipados/)를 따른다.

## 결정

### Web Push 구독마다 `web_push_subscriptions` 행을 저장한다

- `user_id`는 구독을 등록한 사용자다.
- `endpoint`는 브라우저가 Push Service에서 발급받은 전송 주소다.
- `p256dh`와 `auth`는 해당 endpoint의 암호화·인증 키다.
- `is_active`는 해당 브라우저 구독을 발송 대상으로 사용할지 나타낸다.
- 한 사용자는 여러 endpoint를 가질 수 있다.
- `endpoint`는 데이터베이스에서 전역적으로 유일하게 관리한다.
- 기존 앱 푸시 토큰과 Web Push 구독은 별도 모델로 취급한다.

### 프론트엔드는 브라우저 구독 정보를 그대로 등록한다

프론트엔드는 Service Worker와 `PushManager.subscribe()`로 `PushSubscription`을 생성하고, `toJSON()`으로 얻은
값을 다음 API에 전달한다.

```http
POST /api/web-push-subscriptions
Authorization: Bearer {accessToken}
Content-Type: application/json
```

```json
{
  "endpoint": "https://push-service.example/...",
  "keys": {
    "p256dh": "...",
    "auth": "..."
  }
}
```

`endpoint`, `p256dh`, `auth`는 프론트엔드가 직접 생성하거나 수정하지 않는다. VAPID private key는 프론트엔드에
전달하지 않고 백엔드에만 보관한다. `expirationTime`, 앱 푸시용 `installationId`, `token`, `platform`은 이 API에
전달하지 않는다.

등록 성공 시 백엔드는 다음과 같이 구독 식별자를 반환한다.

```json
{
  "id": 1
}
```

프론트엔드는 이 `id`를 현재 브라우저의 구독 식별자로 보관한다.

### 등록은 endpoint 기준으로 원자적 upsert한다

- 같은 사용자와 같은 endpoint를 다시 등록하면 예외 없이 `p256dh`, `auth`를 최신 값으로 갱신하고 활성화한다.
- 다른 사용자가 이미 등록한 endpoint를 등록하면 `409 WEB_PUSH_SUBSCRIPTION_ALREADY_REGISTERED`를 반환한다.
- 다른 사용자에게 endpoint를 바로 재할당하지 않는다. 기존 `NotificationDelivery`가 새 사용자 브라우저로 전달될 수
  있기 때문이다.
- 등록 전 조회 후 저장하지 않고, 데이터베이스의 원자적 upsert 결과로 중복 등록을 처리한다.

### 비활성화는 구독별로 처리한다

```http
DELETE /api/web-push-subscriptions/{subscriptionId}
Authorization: Bearer {accessToken}
```

- 인증된 사용자의 구독 ID가 일치하는 행만 `is_active = false`로 변경한다.
- 행은 삭제하지 않는다. 같은 사용자가 다시 등록하면 기존 행을 재활성화한다.
- 세션 만료만으로 구독을 비활성화하지 않는다.
- 현재는 모든 브라우저 구독을 한 번에 재활성화하는 로그인 정책을 사용하지 않는다.

## 선택 이유

Web Push의 endpoint와 암호화 키는 브라우저 구독의 실제 전달 계약이다. 이를 기존 앱 푸시 토큰 테이블에 억지로
저장하면 endpoint 길이, 여러 브라우저 구독, 암호화 키, 브라우저별 수명주기를 표현할 수 없다.

endpoint를 전역 유일하게 두면 같은 브라우저 구독이 다른 사용자에게 재할당되는 상황을 데이터베이스에서 막을 수 있다.
같은 사용자에 대한 재등록은 원자적 upsert로 처리하므로 프론트가 로그인·페이지 진입 때 현재 구독을 반복해서 동기화해도
중복 예외가 발생하지 않는다.

비활성화 시 행을 보존하면 Push Service가 같은 구독을 계속 제공하는 동안 등록 식별자를 유지할 수 있다. 단, 로그아웃은
현재 브라우저의 구독만 비활성화하는 계약으로 연결해야 하며, 세션 만료나 다른 브라우저의 상태를 자동으로 변경하지 않는다.

## 검토한 대안

### 기존 `push_tokens` 테이블을 재사용한다

Web Push의 endpoint와 키 조합을 앱 토큰 컬럼에 저장하면 플랫폼·provider 의미가 섞이고 기존 길이 제한과 모델 검증이
맞지 않는다. 한 사용자의 여러 브라우저 구독과 Web Push 전송 실패 상태도 명확히 표현할 수 없어 선택하지 않았다.

### `(user_id, endpoint)`를 유일 키로 두고 다른 사용자도 같은 endpoint를 등록한다

계정별 등록 이력은 보존할 수 있지만, 이전 사용자 구독이 활성 상태로 남으면 같은 브라우저로 잘못된 사용자 알림이
전달될 수 있다. 구독 전환 시 기존 delivery를 취소하는 별도 정책이 확정되기 전까지 선택하지 않는다.

### 다른 사용자가 등록하면 기존 행의 `user_id`를 갱신한다

구독 행 하나만 유지할 수 있지만, 기존 사용자에게 생성된 미전송 delivery가 새 사용자의 endpoint로 전송될 위험이 있다.
delivery 상태와 소유권 전환을 함께 처리해야 하므로 현재 등록 API에서는 허용하지 않는다.

### 로그아웃 시 사용자의 모든 구독을 비활성화하고 로그인 시 모두 재활성화한다

구현은 단순하지만 PC에서 로그아웃할 때 모바일 구독까지 중단되고, 오래된 브라우저나 다른 사람이 사용하는 기기의
구독을 다시 활성화할 수 있다. 계정 전체 알림 설정이 필요해지면 사용자 단위 설정과 전체 기기 로그아웃을 별도
결정한다.

## 영향

### 긍정적 영향

- 사용자의 PC·모바일·브라우저별 구독을 모두 저장할 수 있다.
- 같은 사용자의 동일 endpoint 재등록이 멱등적으로 처리된다.
- endpoint, 암호화 키, 활성 상태를 Web Push 의미에 맞게 관리한다.
- 구독 비활성화와 인증 세션 만료를 분리해 수명주기를 명확히 한다.
- 실제 전달 Worker와 발송 provider를 후속 작업으로 독립적으로 구현할 수 있다.

### 부정적 영향과 위험

- 기존 앱 푸시 API와 요청·응답 계약이 달라져 프론트엔드 전환이 필요하다.
- 기존 `push_tokens` 데이터는 Web Push endpoint·키로 변환할 수 없어 V11에서 마이그레이션하지 않는다.
- 기존 `notification_deliveries` 데이터가 있으면 전달 대상 보존을 위해 V11 마이그레이션이 실패한다.
- Web Push 발송 전까지는 구독을 저장해도 실제 알림을 보낼 수 없다.
- 비활성 endpoint 정리, 404·410 응답 처리, 재시도 정책은 발송 Worker에서 추가해야 한다.

## 미확정 사항

- 로그아웃 API에서 현재 구독 비활성화를 호출하는 프론트엔드 연결 방식
- 계정 전체 알림 끄기 또는 모든 기기 로그아웃 기능의 필요 여부
- VAPID public key 제공 API와 백엔드 private key 관리 방식
- Web Push sender, delivery worker, 재시도·claim·실패 endpoint 비활성화 정책
- 공유 DB에 남아 있는 기존 `notification_deliveries` 처리 정책

## 후속 작업

- 프론트엔드가 Service Worker, 권한 요청, `PushSubscription` 생성, 등록·비활성화 API를 연결한다.
- Web Push sender와 VAPID 서명·암호화 기능을 백엔드에 추가한다.
- `NotificationDelivery` worker가 활성 구독별로 발송하고 실패 endpoint를 비활성화하도록 구현한다.
- 공유 dev DB에서 `notification_deliveries` 상태를 확인한 뒤 V11을 적용한다.
