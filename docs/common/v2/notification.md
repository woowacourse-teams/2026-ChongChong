# 알림

[← v2 개요](README.md) · [v1 대비 변경](changes.md)

## 디자인에 있는 기능

- 알림 목록에서 메시지·관련 내용·발생 시각과 상태 점을 표시한다.
- 예시에는 ‘공지를 확인해주세요’, ‘새 공지가 올라왔어요’가 있다.
- 시작 단계의 권한 안내와 마이페이지의 푸시 스위치를 제공한다.
- 공지·과제 작성 화면에서 리마인드 날짜·시간을 등록·제거한다.

## 디자인 메모에 있는 발송 범위

- 공지·과제를 올렸을 때 스터디원에게 알림을 보낸다.
- 설정한 리마인드 시각에 알림을 보낸다.
- 과제에 참여하는 리더도 과제 리마인드 대상에 포함한다.
- 당시 디자인 메모는 읽음·제출 알림을 후속 검토로 남겼다. 현재 서버는 최초 과제 제출 시 본인을 제외한 리더에게 알림을 생성한다. 공지 읽음 알림은 구현되지 않았다.

## 현재 구현과 남은 작업 (2026-09-30)

- 기존 앱 푸시 토큰 API는 Web Push 구독 API로 교체되었다. 브라우저 설치 ID·endpoint·암호화 키를 등록하고 구독 ID로 비활성화한다.
- 새 공지·과제, 최초 제출, 도래한 리마인드의 알림 생성과 활성 구독별 발송 워커가 구현되어 있다. 일시 장애 재시도와 만료 구독 비활성화도 처리한다.
- 알림 목록·읽음 API와 웹 알림함이 연결돼 있다. 상태 점은 `isRead=false`를 의미한다. 알림함에서 선택하면 읽음 요청을 보내고 관련 화면으로 이동한다.
- 마이페이지의 푸시 스위치, 브라우저 권한 요청, 서비스 워커 수신·클릭 이동이 연결되어 있다. 네이티브 앱 푸시와 계정 전체 수신 설정은 별도다.
- 리마인드 예약 입력 UI는 남아 있다. 실제 운영 환경의 푸시 수신은 이번 문서 수정에서 검증하지 않았다.

근거: [알림 서비스](../../../backend/src/main/java/withoutc/chongchong/notification/service/NotificationService.java), [발송 워커](../../../backend/src/main/java/withoutc/chongchong/notification/worker/NotificationDeliveryWorker.java), [웹 푸시](../../../frontend/src/features/notification/push.ts), [알림함](../../../frontend/src/features/notification/pages/NotificationListPage.tsx).

> [!IMPORTANT]
> 공지 읽음 알림 확대와 알림 보관 기간은 추가 정책 확인이 필요하다. 최초 제출 알림은 이미 구현된 동작으로 구분한다.

## Figma 근거

- [06-01-01 알림 목록](https://www.figma.com/design/sT7K2tOQl8JtyHzuwT0nnl?node-id=1702-18702)
- [발송 범위 메모](https://www.figma.com/design/sT7K2tOQl8JtyHzuwT0nnl?node-id=1892-764)
- [00-03-01 권한 안내](https://www.figma.com/design/sT7K2tOQl8JtyHzuwT0nnl?node-id=1702-19902)
- [01-02-01 푸시 스위치](https://www.figma.com/design/sT7K2tOQl8JtyHzuwT0nnl?node-id=1702-19740)
