# 0040. 브라우저 Web Push 구독에 설치 식별자를 사용한다

- 날짜: 2026-09-28
- 관련 이슈: [#396](https://github.com/woowacourse-teams/2026-ChongChong/issues/396)
- 대체 범위: [0037. 브라우저 Web Push 구독을 endpoint 단위로 관리한다](0037-manage-browser-web-push-subscriptions.md)의 등록 식별자, 재등록, 비활성화 정책

## 배경

브라우저에서 Web Push 권한을 다시 활성화하거나 `PushManager.subscribe()`를 반복 호출하면 endpoint가 바뀌거나
여러 구독이 남을 수 있다. endpoint만 저장하면 같은 브라우저 프로필에서 생성된 현재 구독을 기존 행과 연결할 수 없다.

## 문제 상황과 원인

### 증상

- 사용자가 알림을 껐다가 다시 켜거나 페이지 진입 때마다 구독 등록을 반복하면 `web_push_subscriptions` row가 계속 늘어난다.
- 이전 endpoint를 가진 row가 활성 상태로 남아 하나의 브라우저에 여러 delivery가 생성될 수 있다.
- 만료된 이전 구독으로 발송을 시도하면서 Push Service의 `404` 또는 `410` 응답이 반복될 수 있다.

### 원인

- 기존 등록 요청에는 endpoint, `p256dh`, `auth`만 있었고, 서버는 endpoint를 전역 유일 키로 사용했다.
- endpoint는 브라우저 프로필의 고정 식별자가 아니라 Push Service가 발급하고 갱신할 수 있는 전송 주소다.
- 따라서 같은 브라우저 프로필에서 endpoint가 바뀌면 기존 row와 충돌하지 않고 새 row가 생성된다. 서버가 두 endpoint가 같은 브라우저에서 나온 것인지 판단할 안정적인 식별자도 없었다.

즉, 문제의 핵심은 Web Push 구독을 새로 만드는 것 자체가 아니라, 구독이 바뀌었을 때 기존 row를 갱신할 기준이 없었던 것이다.

## 결정

### 클라이언트가 생성한 `installationId`를 등록 식별자로 사용한다

- 프론트엔드는 브라우저 프로필에 installationId를 한 번 생성해 보관하고, 구독 등록 요청마다 같은 값을 보낸다.
- `POST /web-push-subscriptions`는 installationId를 필수로 받으며 공백이 아닌 255자 이하의 값을 허용한다.
- `user_id`와 `installation_id` 조합을 `web_push_subscriptions`의 유일한 등록 식별자로 사용한다.
- 같은 사용자와 같은 `installationId`를 다시 등록하면 endpoint와 암호화 키를 갱신하고 활성화한다.
- endpoint는 식별자나 전역 유일 키로 사용하지 않는다. 같은 브라우저 프로필을 여러 사용자가 사용할 수 있고, endpoint는
  브라우저가 갱신할 수 있는 값이기 때문이다.
- 다른 사용자가 같은 `installationId` 또는 endpoint를 등록하면 기존 구독의 `user_id`는 변경하지 않고 비활성화하며, 별도의 사용자 구독 row를 만든다.
- 등록 트랜잭션은 `installationId`와 endpoint를 정해진 순서로 advisory lock한 뒤 충돌 구독 비활성화와 upsert를 수행한다.
  따라서 같은 `installationId` 또는 endpoint의 동시 등록도 하나의 활성 구독으로 직렬화한다.

### 기존 Web Push 데이터는 V15에서 정리한다

- V15에서 기존 `web_push_subscriptions` 행을 삭제한다.
- `notification_deliveries.web_push_subscription_id`는 `ON DELETE CASCADE`이므로 기존 구독에 연결된 delivery도 함께 삭제된다.
- 마이그레이션 이후 프론트엔드가 새 `installationId`로 등록하면 새로운 활성 row가 생성된다.

### 비활성화는 `userId + subscriptionId`로 처리한다

- `DELETE /web-push-subscriptions/{subscriptionId}`는 인증된 사용자와 subscriptionId가 일치하는 row만 비활성화한다.
- endpoint가 변경되어도 같은 `(user_id, installationId)` row를 갱신하므로 subscriptionId는 유지된다.
- 등록 row가 없거나 다른 사용자의 row이면 변경 없이 `204`를 반환한다.

## 영향

- 같은 사용자와 브라우저 프로필의 재등록은 하나의 row로 수렴한다.
- 다른 사용자 등록이 기존 row를 덮어쓰지 않으면서 기존 활성 row를 비활성화하므로, 이후 생성되는 delivery는 현재 사용자의 활성 구독을 대상으로 한다.
- 동시 등록에서도 같은 `installationId` 또는 endpoint를 가진 활성 구독이 여러 개 남지 않는다.
- 이미 생성된 `PENDING` 또는 `RETRY_WAIT` delivery의 취소는 별도 후속 작업으로 남긴다.
- 기존 Web Push 구독과 연결된 delivery는 V15 마이그레이션에서 함께 정리된다.
- installationId를 추가하지 않은 기존 프론트 요청은 400을 반환하므로 프론트 배포가 API 변경보다 먼저 또는 함께 이뤄져야 한다.
- 브라우저 저장소를 초기화해 installationId를 잃으면 새 installationId로 별도 row가 생길 수 있으므로, 프론트엔드는
  브라우저 프로필 저장소를 유지해야 한다.

## 검증 방법

- 같은 사용자와 같은 `installationId`로 endpoint를 바꿔 두 번 등록했을 때 row 수가 1이고 subscriptionId가 유지되는지 확인한다.
- 같은 endpoint라도 다른 `installationId` 또는 다른 사용자가 등록했을 때 별도 row가 생성되는지 확인한다.
- 재발 시 프론트 요청의 `installationId`가 브라우저 프로필 변경 없이 동일한지, 서버에서 같은 `(user_id, installation_id)` row가 갱신되는지 확인한다.

## 후속 작업

- 프론트엔드는 로그인한 계정의 구독 등록 요청에 `installationId`를 포함한다.
- 프론트엔드는 등록 응답의 `subscriptionId`를 저장하고, 로그아웃 등 알림을 끌 때 비활성화 요청에 포함한다.
