# 알림

[← 현재 PRD](README.md) · [구현 범위](scope.md)

## 알림함과 이동

알림함에서는 내 알림을 최신순으로 볼 수 있다. 아직 읽지 않은 알림은 따로 표시한다.
알림을 누르면 서버에 읽음 처리를 요청하고 관련 화면으로 이동한다. 알림을 읽었다고 해서 공지를 끝까지 읽었거나 과제를 제출한 것으로 처리되지는 않는다.

## 언제, 누구에게 알리는가

| 계기 | 대상 |
| --- | --- |
| 새 공지 | 생성 시 공지 확인 대상인 일반 멤버 |
| 새 과제 | 생성 시 제출 대상 중 작성자 제외 |
| 공지 예약 시각이 됨 | 확인 대상 중 아직 공지를 읽지 않은 멤버 |
| 과제 예약 시각이 됨 | 제출 대상 중 아직 제출하지 않은 멤버. 참여 중인 리더도 포함 |
| 최초 과제 제출 | 제출자를 제외한 스터디 리더 |

다시 제출하거나 제출물을 수정할 때, 공지를 읽을 때는 새 알림을 만들지 않는다.
공지·과제·스터디가 삭제되거나 멤버가 탈퇴·방출되면 관련 알림도 정리한다.

## 브라우저 푸시 알림

브라우저에서 알림 권한을 허용하고 푸시 구독을 등록하면 알림을 받을 수 있다. 웹은 서비스 워커를 통해 푸시 알림을 수신한다.
서버에는 브라우저 설치 ID, 알림을 보낼 주소(endpoint), 암호화 키를 저장한다.
서버의 발송 작업은 활성 구독으로 알림을 보낸다. 일시적인 오류로 보내지 못하면 다시 시도하고, 구독이 만료되면 더 이상 보내지 않도록 비활성화한다.

마이페이지의 푸시 스위치는 현재 브라우저에만 적용된다. 계정 전체나 네이티브 앱의 알림 설정을 바꾸지는 않는다.
서버에는 예약 알림 기능이 있지만, 웹의 공지·과제 작성 화면에는 예약 시각을 입력하는 기능이 없다.
실제 운영 환경에서 푸시가 도착하는지는 확인하지 않았다. 알림 보관 기간도 이 문서에서 확정하지 않는다.

근거: [알림 서비스](../../../backend/src/main/java/withoutc/chongchong/notification/service/NotificationService.java), [발송 워커](../../../backend/src/main/java/withoutc/chongchong/notification/worker/NotificationDeliveryWorker.java), [구독 서비스](../../../backend/src/main/java/withoutc/chongchong/notification/service/WebPushSubscriptionService.java), [웹 푸시](../../../frontend/src/features/notification/push.ts), [알림함](../../../frontend/src/features/notification/pages/NotificationListPage.tsx).
