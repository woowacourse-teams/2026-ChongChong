# 현재 기능의 범위

[← v1 목차](README.md)

> [!IMPORTANT]
> **공지·과제 대상은 작성 시점에 정해진다**
>
> 공지와 과제는 작성 당시 참여 중인 일반 스터디원에게 할당된다.
> 이후 참여한 스터디원의 목록에는 이전 공지·과제가 표시되지 않는다.
> 다만 글 상세 API는 스터디 소속을 기준으로 조회를 허용하므로, 이전 글의 접근 자체를 모두 차단하는 구조는 아니다.

아래 항목은 현재 서버 구현이 있지만 웹에서 사용하는 기능으로 연결되어 있지 않다.

| 항목 | 현재 구현 범위 |
| --- | --- |
| 스터디 정보 수정 | 리더가 이름·설명을 수정하는 API가 있으며 웹 수정 화면은 없음 |
| 로그아웃 | 서버 API와 프론트엔드 호출 함수가 있으며 실행 화면은 없음 |
| 리마인드 예약 | 공지·과제의 예약 시각 저장 기능이 있으며 웹 예약 입력은 없음 |
| 푸시 알림 기반 | 토큰 등록과 알림·설치별 발송 기록 저장 모델이 있으며 실제 푸시 발송까지 연결되지 않음 |

## 리마인드·푸시 구현 근거

[공지 모델](../../../backend/src/main/java/withoutc/chongchong/notice/entity/Notice.java), [과제 모델](../../../backend/src/main/java/withoutc/chongchong/assignment/entity/Assignment.java), [푸시 토큰 서비스](../../../backend/src/main/java/withoutc/chongchong/notification/service/PushTokenService.java), [발송 기록 모델](../../../backend/src/main/java/withoutc/chongchong/notification/entity/NotificationDelivery.java)
