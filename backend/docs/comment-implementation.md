# 댓글 구현 명세

- 작성일: 2026-10-30
- 기준: [댓글 정책](comment-policy.md), [ADR 0057](adr/0057-reference-comment-resources-by-type-and-id.md)
- 상태: 공지·과제·제출물 댓글 CRUD를 공통 저장·잠금·삭제 흐름으로 구현하고 관련 로컬 검증을 통과했다.

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
OpenAPI의 댓글 operation 12개는 구현 후 `x-backend.status: review`, `owner: wontop02`로 표시한다.
병합 전에는 done으로 바꾸지 않는다.

`CommentService`는 트랜잭션과 유스케이스 순서를 조율한다.
각 Controller는 경로에서 CommentTarget을 생성하며, 클라이언트 본문에서 원문 종류를 받지 않는다.
CommentTargetResolver가 원문 종류별 범위 조회·잠금과 제출물 조회 권한·실제 제출 여부를 연결한다.
제출물 쓰기는 부모 과제와 제출물을 차례로 잠근다. 대상별 CRUD나 별도 전략 interface는 복제하지 않는다.
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

본문은 `String.trim()`으로 앞뒤 공백을 제거하며, 정리한 문자열이 `String.isBlank()`이면 거부한다.
NBSP 등 trim 대상이 아닌 문자는 별도로 제거하지 않는다.
정규화한 문자열은 공지·과제와 동일하게 `String.length()` 기준 1~1,000자여야 한다.
보조 평면의 이모지 등은 UTF-16 코드 단위 두 개로 계산하며 내부 공백·줄바꿈은 유지한다.
요청 DTO의 raw 문자열에 `maxLength: 1000`이나 같은 크기의 Size 검증을 적용하지 않는다.
앞뒤 공백을 포함한 긴 입력도 정규화 후 유효할 수 있기 때문이다.
생성·수정이 같은 도메인 정규화·검증을 사용하며, 요청 스키마는 이 의미 규칙 전체를 대신하지 않는다.
정규화 후 기존 본문과 같으면 수정 시각을 바꾸지 않고, 실제 변경이면 `edited_at`을 기록한다.

## 저장 매핑

`V28__create_comments.sql`이 아래 매핑을 관리한다. 실제 환경에는 해당 migration을 적용해야 한다.
스터디 공개 탐색·이미지 복구 PR #610의 V26·V27과 번호가 겹치지 않도록 V28을 사용한다.

| comments 컬럼 | 제약·책임 |
| --- | --- |
| id | 식별자 |
| resource_type | NOT NULL, NOTICE·ASSIGNMENT·ASSIGNMENT_SUBMISSION 중 하나 |
| resource_id | NOT NULL, 양수, 원문 FK 없음 |
| author_user_id | nullable users FK, ON DELETE SET NULL, 소유권 판별 |
| author_member_id | nullable study_members FK, ON DELETE SET NULL, 표시용 멤버십 |
| content | NOT NULL TEXT, 애플리케이션에서 정규화 후 String.length() 기준 1~1,000자 |
| created_at, updated_at | 기존 BaseEntity·Clock 사용, 작성 시각은 불변 |
| edited_at | nullable TIMESTAMP(6), 실제 본문 수정 시각 |

생성 팩터리는 현재 StudyMember에서 사용자와 멤버십 연결을 함께 얻는다.
서로 다른 사용자의 두 ID를 외부에서 받아 조합하지 않는다.
사용자·멤버 삭제의 SET NULL 순서에 따라 두 컬럼이 일시적으로 달라질 수 있으므로 둘의 동시 NULL 여부를 CHECK로 강제하지 않는다.
조회에서는 author_member_id가 남아 있으면 현재 멤버 이름·프로필 이미지를 사용한다.
없으면 `탈퇴한 스터디원`, `profileImageUrl: null`, `isDeparted: true`를 반환한다.
null 이미지는 기본 이미지 표시를 뜻하며 탈퇴한 작성자의 User 프로필로 대체하지 않는다.
재가입해도 과거 댓글을 새 멤버십에 연결하지 않는다.

기본 인덱스는 `(resource_type, resource_id, id)`다.
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

댓글 쓰기와 원문 삭제는 원문 Repository의 `PESSIMISTIC_WRITE` 조회로 직렬화한다.
공지 댓글은 공지 행을 잠그며 같은 스터디의 다른 공지 댓글은 동시에 등록할 수 있다.
잠금 획득과 트랜잭션 안의 권한·수신자 재검사는 각 유스케이스 Service가 수행한다.
별도의 `StudyWriteLockService`나 `LockedStudy` 전달 객체는 두지 않는다.
순수 댓글 조회와 외부 Push 전송은 이 잠금에 포함하지 않는다.

잠금 순서는 `User ID 오름차순 → 필요한 경우 부모 Study → Notice ID 오름차순 → Assignment ID 오름차순
→ AssignmentSubmission ID 오름차순 → 해당 리마인더·댓글·첨부`다.
단일 원문 작업은 그 원문과 필요한 부모만 잠근다. 원문 잠금 뒤 User 잠금을 새로 얻거나 공유 잠금을 쓰기 잠금으로 올리지 않는다.
제출물 변경은 부모 과제를 먼저 잠가 과제 삭제·제출 대상 변경과 같은 순서를 사용한다.

| 경로 | 잠금과 재검사 |
| --- | --- |
| 댓글 생성 | 요청자와 사전 멤버 후보 User 공유 잠금, 공지 또는 과제·제출물 쓰기 잠금, 원문·현재 멤버십·조회 권한·실제 제출 여부·수신 후보 재검사 |
| 댓글 수정·삭제 | 요청자 User 공유 잠금, 공지 또는 과제·제출물 쓰기 잠금, 댓글 소속·작성자/현재 리더 권한 재검사 |
| 기존 공지·과제 생성 | 요청자와 사전 수신/제출 후보 User 공유 잠금, 부모 Study 공유 잠금, 최초 원문 save·flush와 알림 저장 |
| 기존 최초 제출 | 요청자와 사전 알림 후보 User 공유 잠금, 과제·해당 제출물 쓰기 잠금, 최초 제출 여부 재검사 후 알림 저장 |
| 리마인더 한 건 | 사전 멤버 후보 User 공유 잠금, 해당 원문 쓰기 잠금, 그 reminder ID만 SKIP LOCKED로 선점·상태/시각/수신자 재검사 |
| 공지·과제 삭제·수정 | 해당 공지 또는 과제 쓰기 잠금, 과제 삭제·제출 대상 축소 시 하위 제출물도 ID순 잠금, 댓글·알림 정리 후 원문 변경 |
| 스터디 탈퇴·방출 | 제거 대상 User 쓰기 잠금, 작성 댓글의 원문과 삭제될 제출물의 부모 과제를 모아 종류·ID순 잠금, 멤버 재조회 후 정리·삭제 |
| 스터디 삭제 | Study에 실제 FOR UPDATE, 하위 공지·과제·제출물을 종류·ID순 잠금, 댓글·알림 정리 후 Study 삭제 |
| 회원 탈퇴 | 기존 User 쓰기 잠금과 리더 차단 검사, 작성 댓글의 원문과 삭제될 제출물의 부모 과제를 종류·ID순 잠금, 댓글 정리 후 User 삭제 |

새 원문은 아직 자신의 행이 없으므로 생성 시에만 부모 Study를 공유 잠근다.
스터디 자체 삭제는 native `FOR UPDATE`를 사용한다. FK의 KEY SHARE와 호환되는 FOR NO KEY UPDATE에 의존하지 않는다.
따라서 삭제가 하위 원문 집합을 모은 뒤 새 원문이 추가되거나, 멤버를 먼저 붙잡은 생성자가 부모 삭제와 교착하는 일을 막는다.
기존 가입 인원 제한을 위한 Study 쓰기 잠금은 유지하며 댓글의 공통 잠금으로 사용하지 않는다.

User 공유 잠금은 PostgreSQL `FOR SHARE` 의미로 검증하며 ID순으로 획득한다.
사용자·멤버 삭제는 대상 User 쓰기 잠금을 먼저 획득하므로 원문 저장 중 작성자나 수신 멤버가 사라지지 않는다.
방출 대상 userId는 scalar로 탐색하고, 잠금 뒤 같은 memberId·studyId의 현재 멤버를 처음 로드해 재검사한다.
User 이름 변경도 기존 User 쓰기 잠금을 사용한다. 잠금 전 관리 엔티티를 권한 판정에 재사용하지 않는다.

탈퇴 시 작성자 연결의 SET NULL도 댓글 행을 변경한다. 제출물 삭제가 지우는 댓글뿐 아니라
탈퇴자의 작성 댓글이 속한 원문도 함께 잠가 두 사람의 댓글 DELETE와 SET NULL이 서로를 기다리는 교착을 막는다.
이 원문 집합은 User 쓰기 잠금 뒤 수집하므로 새 작성 댓글이나 멤버십으로 뒤늦게 확대되지 않는다.
회원 탈퇴는 이미 스터디를 떠난 뒤에도 authorUser 연결이 남은 댓글의 원문까지 포함한다.

기존 공지·과제 생성 및 제출 알림의 후보는 사전 탐색한 memberId와 userId 쌍으로 보존한다.
원문 잠금 후 현재 멤버십·권한을 처음 조회해 후보와 비교하고, 탈퇴 후 재가입한 같은 사용자의 새 멤버십으로 대체하지 않는다.
공지 수신자·과제 제출 대상 초기화에도 같은 후보를 사용하며 탐색 후 가입한 사용자를 뒤늦게 추가하지 않는다.
후보가 사라지면 제외하고, 요청자 멤버십·원문 유효성 실패는 기존 오류로 처리한다.
댓글의 현재 리더 후보가 사전 후보 밖이면 아무 댓글도 저장하지 않고 `409 COMMENT_STATE_CHANGED`로 끝낸다.

리마인더는 비잠금 Projection으로 후보를 탐색하고 한 건씩 독립된 쓰기 트랜잭션에서 처리한다.
원문을 먼저 잠근 뒤 사전 탐색한 reminder ID와 현재 PENDING·시각 조건에 한정해 FOR UPDATE SKIP LOCKED로 선점한다.
원문이나 리마인더가 삭제·처리됐거나 다른 worker가 선점했다면 건너뛴다.
사전 후보에 없는 새 수신자가 필요한 건도 PROCESSING으로 바꾸지 않고 다음 탐색으로 넘긴다.
알림·Delivery 저장과 SENT 전환은 같은 트랜잭션이며 자기 호출로 Transaction 프록시를 우회하지 않는다.

현재 상태를 재검사해 이미 삭제된 원문·댓글은 해당 404, 없어진 조회·작성 권한은 기존 403으로 반환한다.
알려진 잠금 시간 초과·교착으로 댓글 쓰기가 취소되면 롤백 후 `409 COMMENT_STATE_CHANGED`로 변환한다.
그 밖의 DB 제약·프로그래밍 오류를 이 코드로 일괄 변환하지 않는다. 서버가 등록 요청을 자동 재실행하지 않는다.
동일 공지 직렬화, 다른 공지 병렬 등록, 생성·삭제와 동시 탈퇴의 결과를 실제 PostgreSQL로 검증한다.

## 목록과 특정 댓글 위치

정렬은 `id ASC`다. size는 기본 10, 1~100이며 size+1개로 hasNext를 계산한다.
페이지 크기 검증, size+1 조회 개수, 반환 목록 분할과 hasNext·nextCursor 계산은
기존 Long 기반 CursorPageRequest와 CursorPageResponse를 공유한다. 댓글 전용 pagination 타입은 두지 않는다.
cursor는 생략하거나 양수 Long으로 전달하며 nextCursor는 마지막으로 반환한 댓글 ID다.
같은 원문에서 `id > cursor`인 댓글을 조회하므로 기준 댓글이 삭제되어도 이어서 조회할 수 있다.
cursor는 숫자로 된 조회 경계이며 기준 댓글의 존재나 원문 소속을 별도로 조회하지 않는다.
모든 조회는 요청한 원문 종류·ID로 제한하고 원문 조회 권한은 요청마다 검사한다.
동시 등록에서는 ID 발급 순서와 작성 시각 순서가 다를 수 있으며, 정확한 작성 시각순 정렬은 후속 검토로 남긴다.

focusCommentId는 같은 원문에 속한 댓글인지 확인한 뒤 그 댓글을 포함하여 이후 size개를 조회한다.
cursor와 동시에 지정하면 `400 COMMENT_INVALID_PAGE_REQUEST`다.
포커스 댓글이 삭제됐거나 다른 원문에 속하면 `404 COMMENT_NOT_FOUND`이며 삭제 표시를 만들지 않는다.
응답은 comments, nullable nextCursor, hasNext다. hasNext가 false면 nextCursor는 null이다.
댓글 응답에는 작성자 표시 정보, createdAt, content, isEdited와 현재 사용자의 canEdit·canDelete를 담는다.
작성자 User ID·멤버 ID를 응답으로 노출하지 않으며 권한 플래그는 실제 쓰기 요청에서 다시 검증한다.

## 오류와 검증 기준

기존 인증·파라미터·원문 없음·권한 거부 형식을 재사용한다.
새 오류는 COMMENT_INVALID_CONTENT(400), COMMENT_INVALID_PAGE_REQUEST(400), COMMENT_NOT_FOUND(404),
COMMENT_TARGET_NOT_READY(409), COMMENT_STATE_CHANGED(409)다.
수정·삭제는 실패하더라도 새로운 알림을 만들지 않는다.

문서·계약은 OpenAPI lint·bundle과 메타데이터·문서 링크로 검증한다.
실행 검증은 ADR 0003과 ADR 0031에 따라 다음 범위를 포함한다.

- 도메인·Policy: 본문 경계와 trim·isBlank 기준 공백·줄바꿈, 수정 시각, 작성자/리더 권한.
- HTTP: 세 대상 CRUD, 원문 소속 혼합 거부, 제출물 공개 범위·미제출 제한, 페이지와 focus 조회.
- PostgreSQL: 두 작성자 SET NULL, 알림 CASCADE·유일 제약, 탈퇴 후 보존·재가입, 모든 삭제 경로와 롤백.
- 별도 트랜잭션 경합: 작성↔원문/스터디 삭제, 작성↔멤버 탈퇴·방출/회원 탈퇴,
  제출물 댓글 작성↔제출 대상/공개 범위 변경, 복수 스터디 회원 탈퇴, User 프로필 수정·기존 원문 생성과의 잠금 상호작용.
- 기존 NotificationResourceDeletionConcurrencyTest를 확장해 리마인더 생성·원문 삭제·회원 탈퇴의 3자 경합과
  기존 공지·과제 생성·회원 탈퇴의 FK 잠금 경합에서 공통 순서가 지켜지는지 확인한다.

권한·트랜잭션·데이터 무결성 구현은 ADR 0003·0031의 테스트 전략에 따라 검증한 뒤 독립 검토한다.
