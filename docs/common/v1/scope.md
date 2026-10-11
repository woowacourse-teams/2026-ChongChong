# 현재 기능의 범위

[← v1 목차](README.md)

> [!IMPORTANT]
> **공지·과제 대상은 작성 시점에 할당하되, 과제의 리더 참여 여부는 수정할 수 있다**
>
> 공지는 작성 당시 일반 스터디원에게, 과제는 제출 대상 설정에 따라 일반 스터디원 또는 리더까지 할당된다.
> 이후 가입한 멤버도 이전 공지·과제를 목록·상세에서 열람하지만 확인·제출 대상으로 자동 추가되지 않는다.
> 대상이 없으면 `NOT_ASSIGNED`를 반환하고 홈 미완료 수·현황·리마인드 대상에서 제외한다.
>
> 예외로, 과제 수정 시 리더 참여 여부를 바꾸면 리더의 제출 대상도 변경된다.
> 리더를 제외하면 기존 제출물도 삭제된다. 다시 포함하면 새 제출 행을 생성하며, 삭제된 제출 내용은 복원되지 않는다.
> 이 변경으로 이후 가입한 일반 멤버가 제출 대상에 추가되지는 않는다.

아래 항목은 현재 서버 구현이 있지만 웹에서 사용하는 기능으로 연결되어 있지 않다.

| 항목 | 현재 구현 범위 |
| --- | --- |
| 스터디 정보 수정 | 리더가 이름·설명을 수정하는 API가 있으며 웹 수정 화면은 없음 |
| 제출물 공개 | 서버의 공개 설정·완료 제출물 조회 권한 구현. 웹 공개 설정·목록 연결 필요 |
| 리마인드 예약 | 저장·예약 처리·알림 생성·발송 구현. 웹 예약 입력은 없음 |

로그아웃·회원탈퇴·알림 목록·브라우저 푸시 설정은 웹에 연결되어 있다. 실제 배포 환경의 푸시 수신은 이번 문서 작업에서 검증하지 않았다.

## 리마인드·푸시 구현 근거

[공지 모델](../../../backend/src/main/java/withoutc/chongchong/notice/entity/Notice.java), [과제 모델](../../../backend/src/main/java/withoutc/chongchong/assignment/entity/Assignment.java), [Web Push 구독 서비스](../../../backend/src/main/java/withoutc/chongchong/notification/service/WebPushSubscriptionService.java), [발송 기록 모델](../../../backend/src/main/java/withoutc/chongchong/notification/entity/NotificationDelivery.java)
