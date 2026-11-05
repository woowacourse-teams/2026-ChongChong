# 댓글

[← 현재 PRD](README.md) · [구현 범위](scope.md)

## 조회와 작성

서버는 공지·과제·제출물의 댓글 조회·작성·수정·삭제 API를 제공한다. 공지·과제는 현재 스터디원이 가입 전 글에도 댓글을 조회·작성할 수 있다. 제출물은 기존 제출물 조회 권한을 따르며, 아직 제출하지 않은 행에는 댓글을 작성할 수 없다.
본문은 앞뒤 공백을 제거한 뒤 1~1,000자여야 한다. 내부 공백과 줄바꿈은 유지하며 답글·첨부는 지원하지 않는다.
목록은 댓글 ID 오름차순으로 기본 10개·최대 100개를 반환한다. `cursor`로 이어 읽거나 `focusCommentId`로 특정 댓글 위치부터 조회할 수 있으며 두 값의 동시 지정은 거부한다.

## 수정·삭제와 작성자 표시

작성자 본인은 댓글을 수정·삭제할 수 있고 현재 리더는 다른 사람의 댓글을 삭제할 수 있다. 리더라도 타인의 본문은 수정할 수 없다.
탈퇴·방출 뒤에도 계정과 작성자 연결이 남아 있으면 본인 댓글을 수정·삭제할 수 있다. 조회·작성에 필요한 현재 멤버십과 수정·삭제 권한은 구분한다.
정규화한 내용이 실제로 바뀌면 수정 표시를 남기며 최초 작성 시각과 목록 순서는 유지한다. 탈퇴·방출 또는 회원 탈퇴한 작성자는 목록에서 ‘탈퇴한 스터디원’과 기본 이미지로 표시한다.

## 알림

공지·과제 댓글은 해당 스터디 리더들, 제출물 댓글은 제출자를 알림 후보로 삼는다. 작성자 본인과 원문 조회 권한이 없는 사람은 제외한다. 수정·삭제는 새 알림을 만들지 않는다.
댓글·알림·발송 내역은 함께 저장하고 푸시는 이후 발송한다. 알려진 상태 변경·잠금 경합으로 저장에 실패하면 `409 COMMENT_STATE_CHANGED`를 반환하고 댓글·알림을 남기지 않는다.

이 문서는 서버 계약을 설명하며 웹 댓글 UI와 실제 푸시 수신 완료를 의미하지 않는다.
근거: [댓글 모델](../../../backend/src/main/java/withoutc/chongchong/comment/entity/Comment.java), [권한 정책](../../../backend/src/main/java/withoutc/chongchong/comment/policy/CommentAccessPolicy.java), [원문 권한](../../../backend/src/main/java/withoutc/chongchong/comment/service/CommentTargetResolver.java), [댓글 서비스](../../../backend/src/main/java/withoutc/chongchong/comment/service/CommentService.java), [공지 댓글 계약](../../../backend/docs/openapi/paths/comments/notice-collection.yaml), [제출물 댓글 계약](../../../backend/docs/openapi/paths/comments/submission-collection.yaml).
