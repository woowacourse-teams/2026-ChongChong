# 공지

[← 현재 PRD](README.md) · [구현 범위](scope.md)

## 작성과 운영

리더는 제목·내용을 입력해 공지를 작성하고 수정·삭제한다.
제목은 최대 100자, 내용은 최대 10,000자까지 입력할 수 있다. 둘 다 비어 있거나 공백만으로 이루어져 있으면 저장할 수 없다.
리더는 요약 화면에서 공지를 읽은 멤버와 아직 읽지 않은 멤버, 각각의 인원을 확인한다. 상세 화면에서는 공지 본문을 읽을 수 있다.

## 리더 화면에 표시하는 기간

리더 홈의 미확인 공지 요약에는 최초 작성 후 7일이 지나지 않았고 아직 읽지 않은 확인 대상자가 있는 공지만 표시한다. 정확히 작성 시각 + 7일부터 요약과 내 스터디 목록의 미확인 공지 개수에서 제외한다.

기존 공지에도 같은 기준을 적용하며 다음 조회부터 반영한다. 내용을 수정하거나 재알림을 예약·발송해도 최초 작성 시각을 기준으로 하므로 노출 기간이 연장되지 않는다.

공지 원문과 읽음 기록은 삭제하지 않는다. 기간이 지난 공지도 전체 공지 목록과 상세에서 확인할 수 있으며, 일반 멤버의 미확인 공지 조회에는 이 기간 제한을 적용하지 않는다.

이 정책은 #654의 백엔드 구현 기준이다. 실제 웹 화면과 배포 환경에서의 동작은 별도로 확인해야 한다.

근거: [스터디 서비스](../../../backend/src/main/java/withoutc/chongchong/study/service/StudyService.java), [공지 조회](../../../backend/src/main/java/withoutc/chongchong/notice/repository/NoticeRepository.java), [노출 기간 HTTP 테스트](../../../backend/src/test/java/withoutc/chongchong/study/LeaderNoticeExpiryApiTest.java).

## 공지를 읽으면 어떻게 표시되는가

공지를 작성할 당시의 일반 멤버가 확인 대상이 된다. 리더는 확인 대상에서 제외한다.

확인 대상인 스터디원이 본문 끝까지 스크롤하면 웹이 자동으로 읽음 요청을 보낸다. 본문이 짧아 스크롤할 필요가 없을 때도 자동으로 요청한다.
서버가 읽음을 저장하면 화면에 읽음 상태와 읽은 시각, 완료 안내를 표시한다. 이미 읽은 공지는 읽음 완료 상태로 보여준다.

공지 작성 후 가입한 멤버도 본문을 읽을 수 있지만 확인 대상은 아니다. 서버는 이 상태를 `NOT_ASSIGNED`로 구분하며, 웹은 읽음 요청을 보내지 않는다.

## 다시 읽도록 알리는 예약 알림

서버에는 아직 공지를 읽지 않은 멤버에게 예약 알림을 보내는 기능이 있다. 앞으로의 시각만 예약할 수 있고, 같은 시각을 여러 번 보내면 하나로 저장한다.
예약 시각이 되면 아직 읽지 않은 확인 대상에게 알림을 만든다. 웹 작성·수정 화면에서 예약을 입력한다. 상세 응답의 `remindAts`에는 아직 처리되지 않은 미래 예약만 오름차순으로 반환하며, 없으면 빈 배열이다.

예약을 수정하는 API는 요청 값에 따라 다음과 같이 처리한다.

- 예약 목록을 보내지 않거나 `null`을 보내면 기존 예약을 유지한다.
- 빈 배열을 보내면 아직 발송하지 않은 예약을 지운다.
- 시각 목록을 보내면 아직 발송하지 않은 예약을 새 목록으로 바꾼다.

## 이미지 첨부

웹 작성·수정 화면에서 이미지를 선택하고 업로드 완료 후 `attachmentFileIds`로 연결한다. 서버 기본 정책은 JPEG·PNG·WebP, 최대 10장, 장당 5MiB이며 0바이트는 거부한다. 상세 응답의 `attachments`와 `imageUrl`로 이미지를 표시한다.
첨부 목록 생략·null은 유지, 빈 배열은 전체 제거이며 첨부 제거·공지 삭제는 파일 연결만 해제한다. 자세한 흐름은 [파일](file.md)을 따른다.
멤버가 공지를 읽었을 때 리더에게 별도 알림을 보내는 기능은 없다.

근거: [공지 서비스](../../../backend/src/main/java/withoutc/chongchong/notice/service/NoticeService.java), [읽음 서비스](../../../backend/src/main/java/withoutc/chongchong/notice/service/NoticeReadService.java), [공지 모델](../../../backend/src/main/java/withoutc/chongchong/notice/entity/Notice.java), [작성 폼](../../../frontend/src/features/notice/components/NoticeForm.tsx), [자동 읽음 화면](../../../frontend/src/features/notice/components/MemberNoticeDetailContent.tsx).
