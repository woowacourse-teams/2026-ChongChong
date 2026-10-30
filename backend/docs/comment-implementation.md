# 댓글 구현 명세

- 작성일: 2026-10-30
- 기준: [댓글 정책](comment-policy.md), [ADR 0057](adr/0057-reference-comment-resources-by-type-and-id.md)
- 상태: 1단계 계약·매핑·삭제·잠금 설계. 제품 코드와 DB 마이그레이션은 미구현이다.

## 적용 범위와 interface

공지, 과제, 제출물의 댓글을 하나의 `comment` 모듈로 구현한다.
외부 호출은 인증된 사용자 ID, 스터디 ID, 경로에서 결정한 원문 참조를 전달한다.
클라이언트가 본문의 작성자 ID, `resourceType`, `resourceId`, 알림 수신자를 지정하지 않는다.
원문 종류는 기존 `notification.entity.ResourceType`의 세 값을 재사용하며 enum 패키지 이동은 하지 않는다.

| 동작 | 입력 | 결과 |
| --- | --- | --- |
| create | 사용자, 스터디, 원문 참조, content | 201과 commentId |
| list | 사용자, 스터디, 원문 참조, cursor 또는 focusCommentId, size | 200과 comments·nextCursor·hasNext |
| update | 사용자, 스터디, 원문 참조, commentId, content | 본문 없는 204 |
| delete | 사용자, 스터디, 원문 참조, commentId | 본문 없는 204 |

원문 참조는 공지 ID, 과제 ID, 또는 과제 ID와 제출물 ID의 조합이다.
HTTP 경로는 각 원문의 기존 상세 경로 아래 `/comments`와 `/comments/{commentId}`를 사용한다.
OpenAPI의 댓글 operation 12개는 `x-backend.status: todo`, `owner: null`로 등록한다.
실제 구현·HTTP 인수 테스트가 준비되기 전에는 완료 상태로 바꾸지 않는다.

`CommentService`는 트랜잭션과 유스케이스 순서를 조율한다.
`CommentTargetResolver` 하나가 세 종류의 원문을 범위에 맞게 조회하고 필요한 기존 정책을 연결한다.
대상별 조회·수신자 선정에 실제 차이가 있는 부분만 분기하며 별도 전략 interface나 대상별 댓글 구현을 복제하지 않는다.
`CommentAccessPolicy`는 전달받은 작성자·요청자·현재 역할로 수정·삭제를 판정하며 Repository를 호출하지 않는다.
원문 삭제 경로는 댓글 정리 진입점을 호출하고, 정리 구현은 원문 조건을 사용하는 bulk query로 범위를 찾는다.

## 권한과 본문

| 대상 | 조회·작성 권한 | 작성 시 추가 조건 | 새 댓글 알림 후보 |
| --- | --- | --- | --- |
| 공지 | 현재 해당 스터디원 | 없음 | 현재 모든 리더 |
| 과제 | 현재 해당 스터디원 | 마감 제한 없음 | 현재 모든 리더 |
| 제출물 | 기존 AssignmentAccessPolicy.requireCanReadSubmission과 같은 권한 | submittedAt이 null이 아님, 마감 제한 없음 | 제출자만 |

공지 수신자나 과제 제출 대상인지 여부를 댓글 조회·작성 권한으로 사용하지 않는다.
스터디→공지, 스터디→과제→제출물, 원문 종류·ID→댓글의 소속을 모두 확인한다.
미제출 행의 content·link·첨부가 있어도 실제 제출로 간주하지 않는다.

수정은 `author_user_id`와 인증 사용자 ID가 일치할 때만 허용한다.
삭제는 작성자 본인이거나 해당 스터디의 현재 리더일 때 허용한다.
리더도 타인 댓글의 본문은 수정하지 못한다.
확정 정책이 원문 조회 권한을 요구한 범위는 조회·작성이다. 수정·삭제에 원문 조회 권한을 추가 조건으로 붙이지 않는다.
따라서 스터디 탈퇴나 제출물 비공개 전환 후에도 계정이 남아 있는 작성자는 알려진 댓글 ID로 본인 댓글을 수정·삭제할 수 있다.
이때도 원문·댓글의 경로 소속을 검증하고 수정·삭제 성공 응답으로 내용을 반환하지 않는다.
회원 탈퇴로 `author_user_id`가 null이 된 댓글은 어떤 사용자도 본인 댓글로 판정하지 않는다.

본문은 Unicode White_Space에 해당하는 앞뒤 공백만 제거한다.
정규화한 문자열의 Unicode 코드 포인트 수가 1~1,000이어야 하며 내부 공백·줄바꿈은 유지한다.
요청 DTO의 raw 문자열에 `maxLength: 1000`이나 같은 크기의 Size 검증을 적용하지 않는다.
앞뒤 공백을 포함한 긴 입력도 정규화 후 유효할 수 있기 때문이다.
생성·수정이 같은 도메인 정규화·검증을 사용하며, 요청 스키마는 이 의미 규칙 전체를 대신하지 않는다.
정규화 후 기존 본문과 같으면 수정 시각을 바꾸지 않고, 실제 변경이면 `edited_at`을 기록한다.

## 저장 매핑

새 Flyway migration은 구현 단계에서 추가한다. 이 문서는 DDL 적용 완료를 의미하지 않는다.

| comments 컬럼 | 제약·책임 |
| --- | --- |
| id | 식별자 |
| resource_type | NOT NULL, NOTICE·ASSIGNMENT·ASSIGNMENT_SUBMISSION 중 하나 |
| resource_id | NOT NULL, 양수, 원문 FK 없음 |
| author_user_id | nullable users FK, ON DELETE SET NULL, 소유권 판별 |
| author_member_id | nullable study_members FK, ON DELETE SET NULL, 표시용 멤버십 |
| content | NOT NULL TEXT, 정규화 후 1~1,000 코드 포인트 |
| created_at, updated_at | 기존 BaseEntity·Clock 사용, 작성 시각은 불변 |
| edited_at | nullable TIMESTAMP(6), 실제 본문 수정 시각 |

생성 팩터리는 현재 StudyMember에서 사용자와 멤버십 연결을 함께 얻는다.
서로 다른 사용자의 두 ID를 외부에서 받아 조합하지 않는다.
사용자·멤버 삭제의 SET NULL 순서에 따라 두 컬럼이 일시적으로 달라질 수 있으므로 둘의 동시 NULL 여부를 CHECK로 강제하지 않는다.
조회에서는 author_member_id가 남아 있으면 현재 멤버 이름·프로필 이미지를 사용한다.
없으면 `탈퇴한 스터디원`, `profileImageUrl: null`, `isDeparted: true`를 반환한다.
null 이미지는 기본 이미지 표시를 뜻하며 탈퇴한 작성자의 User 프로필로 대체하지 않는다.
재가입해도 과거 댓글을 새 멤버십에 연결하지 않는다.

기본 인덱스는 `(resource_type, resource_id, created_at, id)`다.
SET NULL 처리에 필요한 author_user_id·author_member_id 인덱스도 둔다.
원문 종류·ID의 유효값과 content 길이의 DB 제약은 도메인 검증을 보완한다.
작성자 연결 해제는 본문 수정으로 취급하지 않으며 `edited_at`에 영향을 주지 않는다.

| notifications 추가 컬럼 | 제약·책임 |
| --- | --- |
| comment_id | nullable comments FK, ON DELETE CASCADE |
| recipient_member_id | nullable study_members FK, ON DELETE CASCADE |

기존 알림은 두 컬럼을 모두 null로 유지한다.
댓글 알림은 두 컬럼 모두 필수이고 type은 기존 `NEW`다.
`(comment_id, recipient_id)` 유일 제약으로 한 댓글에 대한 사용자별 중복을 방지한다.
FK 삭제를 위한 comment_id·recipient_member_id 인덱스를 둔다.
댓글 알림 생성 팩터리는 댓글에서 원문 종류·ID를, 수신 멤버에서 recipient User와 멤버십을 함께 얻는다.
알림의 resource_type·resource_id는 댓글 ID가 아니라 원문을 계속 가리킨다.
기존 recipient_id의 User FK와 알림함·읽음·Delivery 구조는 유지한다.

## 알림 생성과 전달

댓글과 수신자별 Notification, 활성 구독별 NotificationDelivery를 하나의 쓰기 트랜잭션에 저장한다.
어느 저장 단계든 실패하면 전체를 롤백한다. 실제 Web Push는 커밋 뒤 기존 worker가 전달한다.
새 댓글마다 알림을 만들고 기존 알림에 합치지 않는다. 수정·삭제에는 새 알림을 만들지 않는다.

수신 후보에서 작성자 본인을 User ID로 제외하고, 현재 원문 조회 권한을 확인한 뒤 User ID로 중복을 제거한다.
제출물에서는 제출자가 아닌 리더를 추가하지 않으며 이전 댓글 참여 여부로 수신자를 늘리지 않는다.

- 제목: `[{스터디 이름}] 새 댓글`
- 내용: `{댓글 작성자 이름} 스터디원이 댓글을 달았어요`
- deepLink: 기존 원문 상대 경로에 `?focusCommentId={commentId}` 추가

예를 들어 `/studies/1/assignments/2/submissions/3?focusCommentId=42`로 이동한다.
앱은 원문을 열고 같은 focusCommentId로 댓글 목록을 조회한다.
스터디 이름과 작성자 이름은 알림 생성 시 snapshot이며 원문 제목이나 댓글 본문을 포함하지 않는다.

스터디 탈퇴·방출은 recipient_member_id CASCADE로 해당 수신자의 댓글 알림과 Delivery를 정리한다.
회원 탈퇴 시 본인의 수신 알림은 기존 User FK CASCADE로 정리한다.
다른 사람에게 보낸 알림은 작성자의 User·StudyMember 삭제만으로 지우지 않는다.
공개 제출물을 비공개로 바꾸어도 현재 댓글 알림 수신자인 제출자는 조회 권한을 유지하므로 삭제 대상이 없다.
권한 변경 테스트는 이 유지 조건과 멤버십 소멸 시 정리를 확인한다.
이미 전달됐거나 삭제와 동시에 외부 전달 중인 푸시의 회수는 보장하지 않는다.

## 삭제 범위와 순서

| 기존 진입점 | 원문 삭제 전에 정리할 댓글 범위 |
| --- | --- |
| NoticeService.delete | NOTICE + noticeId |
| AssignmentService.delete | ASSIGNMENT + assignmentId 및 그 과제의 모든 제출물 |
| StudyService.deleteStudy | 그 스터디의 모든 공지·과제·제출물 |
| StudyMemberService.leave / expel | 제거할 멤버 소유의 제출물 |
| UserService.withdraw | 삭제할 사용자 멤버십 소유의 제출물 |
| AssignmentService.update | 제출 대상 변경으로 orphanRemoval될 제출물 |
| CommentService.delete | 경로에 속한 개별 댓글 |

원문에 JOIN 또는 EXISTS하는 bulk query로 범위를 찾고, 제출물 ID를 한 건씩 애플리케이션에 로드하지 않는다.
댓글을 먼저 지우면 댓글 알림과 Delivery는 FK로 정리된다.
그다음 기존 원문 알림 정리와 원문 삭제를 같은 트랜잭션에서 수행한다.
과제 수정에서는 제거할 제출물 조건을 확보하고 관련 댓글·원문 알림을 정리한 뒤 컬렉션에서 제거한다.
첨부 변경이 먼저 flush를 유발하더라도 삭제할 제출물이 이미 사라지지 않도록 순서를 배치한다.
스터디 삭제의 EntityManager.clear는 DB 잠금을 해제하지 않는다. 기존 detach 목적은 유지한다.

탈퇴·방출·회원 탈퇴에서는 작성자 ID로 댓글을 일괄 삭제하지 않는다.
삭제되는 제출물 안의 타인 댓글은 지우지만, 다른 원문에 쓴 탈퇴자 댓글은 SET NULL로 보존한다.
실패하면 원문·댓글·알림·Delivery 삭제 전체가 롤백되어야 한다.

원문 FK가 없으므로 원문·스터디·멤버·사용자를 Repository나 관리 SQL에서 직접 삭제하면 이 정리를 우회할 수 있다.
제품의 삭제 진입점은 위 순서를 사용해야 하며, 운영 정리 SQL도 동일 트랜잭션의 댓글 정리를 포함해야 한다.
JPA 삭제 콜백만으로 DB CASCADE에 따른 제출물 삭제를 감지하려 하지 않는다.

## 잠금 대상과 획득 순서

초기에는 같은 스터디의 댓글 쓰기와 원문 수명·권한 변경을 Study 행의 `PESSIMISTIC_WRITE`로 직렬화한다.
외래 키가 없는 원문 종류마다 별도 잠금 규칙을 만들지 않고 스터디 행을 공통 잠금 대상으로 사용한다.
같은 스터디의 댓글 쓰기 처리량을 제한하는 비용을 감수한다. 순수 댓글 조회와 외부 Push 전송은 이 잠금에 포함하지 않는다.
기존 코드에 Transaction 선언이 있다는 것만으로 이 규칙이 적용됐다고 보지 않는다.

명시적 잠금은 `User ID 오름차순 → Study ID 오름차순 → 기존 원문·리마인더·댓글 잠금` 순서다.
User 잠금이 필요한 경로는 Study 잠금 뒤에 User 잠금을 새로 얻거나 공유 잠금을 쓰기 잠금으로 올리지 않는다.
같은 트랜잭션에서 스터디 잠금 뒤 다른 스터디 잠금을 얻어야 한다면 전체 대상을 먼저 모아 오름차순으로 잠근다.

| 경로 | 잠금과 재검사 |
| --- | --- |
| 댓글 생성 | 요청자와 알림 후보 User에 PESSIMISTIC_READ를 ID순으로 획득, Study에 PESSIMISTIC_WRITE, 원문·멤버십·권한·제출 여부·실제 수신 후보 재조회 |
| 댓글 수정·삭제 | 요청자 User에 PESSIMISTIC_READ, Study에 PESSIMISTIC_WRITE, 경로·댓글·작성자/리더 권한 재검사 |
| 기존 공지·과제 생성 | 요청자와 기존 정책의 수신/제출 대상 User 공유 잠금, Study 쓰기 잠금, 대상 멤버십 재검사 후 최초 원문 save·flush와 알림 저장 |
| 기존 최초 제출 | 요청자와 기존 제출 알림 후보 리더 User 공유 잠금, Study 쓰기 잠금, 제출물 쓰기 잠금·최초 제출 여부 재검사 후 기존 알림 저장 |
| 리마인더 한 건 | 사전 탐색한 수신 후보 User 공유 잠금, Study 쓰기 잠금, 그 reminder ID만 SKIP LOCKED로 선점·상태/시각/수신자 재검사, 알림 저장과 SENT 전환 |
| 공지·과제 삭제, 멤버 탈퇴·방출 | Study에 PESSIMISTIC_WRITE, 현재 역할·원문/대상 멤버 재조회, 기존 원문·리마인더 잠금, 댓글·알림·원문 삭제 |
| 과제 제출 대상·공개 범위 수정 | Study에 PESSIMISTIC_WRITE를 기존 과제 조회·수정·첨부 flush보다 먼저 획득, 변경 권한과 삭제 범위 재검사 |
| 스터디 삭제 | Study에 PESSIMISTIC_WRITE, 리더 재검사, 하위 댓글·알림 정리 후 Study 삭제 |
| 회원 탈퇴 | 기존 User PESSIMISTIC_WRITE와 리더 차단 검사, 멤버십의 Study ID를 Projection으로 수집, 해당 Study를 ID순으로 PESSIMISTIC_WRITE, 제출물 댓글 정리 후 User 삭제 |

User 공유 잠금은 기존 FK 삽입의 KEY SHARE 잠금과 호환되는 PostgreSQL `FOR SHARE` 의미로 검증한다.
댓글 트랜잭션은 User를 수정하지 않는다. 불필요한 User 쓰기 잠금으로 기존 원문 생성의 FK 삽입을 막지 않는다.
알림 수신자의 User FK 삽입이 회원 탈퇴의 User 잠금과 역순으로 대기하지 않도록 수신 후보도 먼저 공유 잠근다.

이 규약은 댓글 생성자에만 적용하지 않는다.
현재 공지·과제 생성은 원문 INSERT에서 Study FK KEY SHARE를 얻은 뒤 수신자 User FK를 삽입한다.
리마인더는 reminder 행을 먼저 잠근 뒤 User FK를 삽입한다.
이를 그대로 두면 User 쓰기 잠금을 가진 회원 탈퇴와 새 Study 잠금을 추가한 삭제 경로 사이에 교착이 생길 수 있다.
따라서 기존 알림 생산자의 User 공유 잠금은 NotificationService 호출 직전이 아니라
최초 원문 save·flush 또는 제출물 잠금보다 먼저 획득한다.
회원 탈퇴에 Study 잠금을 추가하는 구현과 이 기존 생산자 변경은 같은 검증 범위에 포함한다.

기존 공지·과제 생성 및 제출 알림의 수신 후보는 기존 정책상 선정 시점의 ID snapshot을 사용한다.
snapshot은 memberId와 userId 쌍으로 보존하며, 탈퇴 후 재가입한 같은 사용자의 새 멤버십으로 대체하지 않는다.
공지 수신자·과제 제출 대상 초기화에도 같은 snapshot을 사용한다.
Study 잠금 후에는 그 후보의 현재 멤버십·권한을 재검사하여 더 이상 유효하지 않은 대상을 제외한다.
탐색 후 가입한 사용자를 snapshot에 뒤늦게 추가하거나 User 잠금을 Study 뒤에서 획득하지 않는다.
이는 기존 후보 선정 후 저장하는 흐름을 유지하기 위한 규칙이며 댓글 전용 409를 기존 HTTP 응답에 추가하지 않는다.
후보 User가 이미 삭제됐다면 그 후보를 제외하고, 요청자의 현재 멤버십·원문 유효성 실패는 기존 오류로 처리한다.

리마인더는 공지 batch와 과제 batch를 하나의 쓰기 트랜잭션에서 차례로 처리하지 않는다.
기존 batch 크기 범위에서 두 종류의 reminder ID·스터디 ID·수신 후보를 비잠금 Projection으로 탐색하고,
한 건씩 독립된 쓰기 트랜잭션으로 위 순서를 적용한다. 자기 호출로 Transaction 프록시를 우회하지 않는다.
선점 쿼리는 사전 탐색한 그 reminder ID와 현재 PENDING·시각 조건에 한정하고 FOR UPDATE SKIP LOCKED를 유지한다.
이미 삭제·처리됐거나 다른 worker가 선점한 건은 건너뛴다.
잠그지 않은 새 수신자가 필요한 건도 PROCESSING으로 바꾸지 않고 건너뛰어 다음 탐색에서 다시 평가한다.
각 reminder의 알림·Delivery 저장과 SENT 전환은 여전히 하나의 트랜잭션이다.
따라서 공지 reminder에서 Study를 잠근 뒤 과제 reminder의 User를 새로 잠그는 순서는 발생하지 않는다.

댓글 생성의 최초 원문·수신자 탐색은 ID Projection으로만 수행한다.
잠금 전 조회한 관리 엔티티를 잠금 후 권한 판정에 재사용하지 않는다.
Study 잠금 후 후보가 줄었다면 현재 허용 후보만 사용한다.
새 후보가 생겨 아직 공유 잠그지 않은 User가 필요하면 새 잠금을 뒤늦게 얻지 않고
아무 댓글도 저장하지 않은 채 `409 COMMENT_STATE_CHANGED`로 전체 트랜잭션을 끝낸다.
사용자가 재요청하면 처음부터 현재 후보로 검사한다. 서버가 커밋 성공 여부가 불명확한 요청을 자동 재실행하지 않는다.

회원 탈퇴 중 수집한 스터디가 잠금 전에 이미 삭제됐다면 그 스터디는 건너뛴다.
User 쓰기 잠금은 유지되므로 해당 사용자의 새 멤버십 생성은 기존 가입 규칙에 따라 대기한다.
멤버십 엔티티 전체를 로드해 삭제된 User를 다시 flush하는 방식은 사용하지 않는다.

현재 상태를 재검사해 이미 삭제된 원문·댓글은 해당 404, 없어진 조회·작성 권한은 기존 403으로 반환한다.
알려진 잠금 시간 초과·교착으로 댓글 쓰기가 취소되면 롤백 후 `409 COMMENT_STATE_CHANGED`로 변환한다.
그 밖의 DB 제약·프로그래밍 오류를 이 코드로 일괄 변환하지 않는다.
원문 삭제와 생성 중 어느 쪽이 먼저 잠금을 얻어도 고아 댓글·알림이 남지 않는지 실제 PostgreSQL로 증명한다.

## 목록과 특정 댓글 위치

정렬은 `created_at ASC, id ASC`다. size는 기본 10, 1~100이며 size+1개로 hasNext를 계산한다.
기존 Long 기반 CursorPageRequest·Response는 변경하지 않고 댓글 전용 cursor 문자열을 사용한다.
cursor는 Base64URL(패딩 없음)로 인코딩한 UTF-8 JSON이며 다음 값을 담는다.

`{"v":1,"resourceType":"NOTICE","resourceId":1,"createdAt":"2026-10-30T10:00:00.000000","id":11}`

DB에 저장된 마이크로초 정밀도를 사용하고, cursor의 버전·시각·양수 ID·원문 종류·ID를 검증한다.
길이는 1~512자, 문자는 Base64URL 범위이며 잘못되거나 다른 원문의 cursor는 `400 COMMENT_INVALID_CURSOR`다.
다음 페이지는 `(created_at, id)`가 cursor보다 큰 댓글을 조회한다.
cursor의 댓글이 삭제되어도 다음 조회에 그 행을 다시 찾을 필요가 없다.
cursor는 조회 위치일 뿐 권한 증명이 아니며 모든 요청에서 원문 조회 권한을 검사한다.

focusCommentId는 같은 원문에 속한 댓글인지 확인한 뒤 그 댓글을 포함하여 이후 size개를 조회한다.
cursor와 동시에 지정하면 `400 COMMENT_INVALID_PAGE_REQUEST`다.
포커스 댓글이 삭제됐거나 다른 원문에 속하면 `404 COMMENT_NOT_FOUND`이며 삭제 표시를 만들지 않는다.
응답은 comments, nullable nextCursor, hasNext다. hasNext가 false면 nextCursor는 null이다.
댓글 응답에는 작성자 표시 정보, createdAt, content, isEdited와 현재 사용자의 canEdit·canDelete를 담는다.
작성자 User ID·멤버 ID를 응답으로 노출하지 않으며 권한 플래그는 실제 쓰기 요청에서 다시 검증한다.

## 오류와 검증 기준

기존 인증·파라미터·원문 없음·권한 거부 형식을 재사용한다.
새 오류는 COMMENT_INVALID_CONTENT(400), COMMENT_INVALID_CURSOR(400),
COMMENT_INVALID_PAGE_REQUEST(400), COMMENT_NOT_FOUND(404),
COMMENT_TARGET_NOT_READY(409), COMMENT_STATE_CHANGED(409)다.
수정·삭제는 실패하더라도 새로운 알림을 만들지 않는다.

1단계에서는 OpenAPI lint·bundle·build, 예제 스키마·미구현 메타데이터와 문서 링크를 검증한다.
아래 실행 검증은 후속 구현 단계의 완료 조건이며, 이번 문서 작성으로 통과한 것으로 간주하지 않는다.

- 도메인·Policy: 본문 경계와 Unicode 공백·줄바꿈, 수정 시각, 작성자/리더 권한.
- HTTP: 세 대상 CRUD, 원문 소속 혼합 거부, 제출물 공개 범위·미제출 제한, 페이지와 focus 조회.
- PostgreSQL: 두 작성자 SET NULL, 알림 CASCADE·유일 제약, 탈퇴 후 보존·재가입, 모든 삭제 경로와 롤백.
- 별도 트랜잭션 경합: 작성↔원문/스터디 삭제, 작성↔멤버 탈퇴·방출/회원 탈퇴,
  제출물 댓글 작성↔제출 대상/공개 범위 변경, 복수 스터디 회원 탈퇴, User 프로필 수정·기존 원문 생성과의 잠금 상호작용.
- 기존 NotificationResourceDeletionConcurrencyTest를 확장해 리마인더 생성·원문 삭제·회원 탈퇴의 3자 경합과
  기존 공지·과제 생성·회원 탈퇴의 FK 잠금 경합에서 공통 순서가 지켜지는지 확인한다.

권한·트랜잭션·데이터 무결성 구현은 ADR 0003·0031의 테스트 전략에 따라 검증한 뒤 독립 검토한다.
