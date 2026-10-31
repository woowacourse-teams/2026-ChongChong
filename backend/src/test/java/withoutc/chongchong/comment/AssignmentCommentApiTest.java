package withoutc.chongchong.comment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import withoutc.chongchong.assignment.entity.Assignment;
import withoutc.chongchong.assignment.entity.AssignmentSubmission;
import withoutc.chongchong.assignment.entity.SubmissionTarget;
import withoutc.chongchong.assignment.entity.SubmissionVisibility;
import withoutc.chongchong.assignment.repository.AssignmentRepository;
import withoutc.chongchong.assignment.repository.AssignmentSubmissionRepository;
import withoutc.chongchong.auth.support.TestAuthRequest;
import withoutc.chongchong.comment.entity.Comment;
import withoutc.chongchong.comment.repository.CommentRepository;
import withoutc.chongchong.comment.service.CommentService;
import withoutc.chongchong.comment.service.CommentTarget;
import withoutc.chongchong.comment.controller.dto.CommentWriteRequest;
import withoutc.chongchong.notification.entity.NotificationDelivery;
import withoutc.chongchong.notification.entity.ResourceType;
import withoutc.chongchong.notification.entity.WebPushSubscription;
import withoutc.chongchong.notification.repository.NotificationDeliveryRepository;
import withoutc.chongchong.notification.repository.NotificationRepository;
import withoutc.chongchong.notification.repository.WebPushSubscriptionRepository;
import withoutc.chongchong.study.entity.Study;
import withoutc.chongchong.study.entity.StudyMember;
import withoutc.chongchong.study.entity.StudyMemberRole;
import withoutc.chongchong.study.repository.StudyMemberRepository;
import withoutc.chongchong.study.repository.StudyRepository;
import withoutc.chongchong.study.service.StudyMemberService;
import withoutc.chongchong.support.TestDatabaseCleaner;
import withoutc.chongchong.user.entity.User;
import withoutc.chongchong.user.repository.UserRepository;
import withoutc.chongchong.user.service.UserService;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.jpa.hibernate.ddl-auto=validate", "spring.flyway.enabled=true"
})
class AssignmentCommentApiTest {

    private static final String ASSIGNMENT_PATH = "/studies/%d/assignments/%d/comments";
    private static final String SUBMISSION_PATH = "/studies/%d/assignments/%d/submissions/%d/comments";
    private static final String VISIBILITY_SQL = "UPDATE assignments SET submission_visibility = ? WHERE id = ?";
    private static final String CLOSE_AT_SQL = "UPDATE assignments SET close_at = ? WHERE id = ?";
    private static final String UNSUBMIT_SQL = "UPDATE assignment_submissions SET submitted_at = NULL WHERE id = ?";

    @LocalServerPort
    private int port;
    @Autowired
    private TestAuthRequest auth;
    @Autowired
    private TestDatabaseCleaner cleaner;
    @Autowired
    private UserRepository users;
    @Autowired
    private StudyRepository studies;
    @Autowired
    private StudyMemberRepository members;
    @Autowired
    private AssignmentRepository assignments;
    @Autowired
    private AssignmentSubmissionRepository submissions;
    @Autowired
    private CommentRepository comments;
    @Autowired
    private NotificationRepository notifications;
    @MockitoSpyBean
    private NotificationDeliveryRepository deliveries;
    @Autowired
    private WebPushSubscriptionRepository subscriptions;
    @Autowired
    private StudyMemberService memberService;
    @Autowired
    private UserService userService;
    @Autowired
    private JdbcTemplate jdbc;
    @MockitoSpyBean
    private CommentService commentService;

    private Study study;
    private User leader;
    private User secondLeader;
    private User author;
    private User owner;
    private User reader;
    private StudyMember ownerMember;
    private Assignment assignment;
    private AssignmentSubmission submission;

    @BeforeEach
    void setUp() {
        cleaner.clean();
        study = studies.saveAndFlush(Study.create("과제 스터디", "설명", false));
        leader = member("리더", StudyMemberRole.LEADER).getUser();
        secondLeader = member("다른 리더", StudyMemberRole.LEADER).getUser();
        author = member("작성자", StudyMemberRole.MEMBER).getUser();
        ownerMember = member("제출자", StudyMemberRole.MEMBER);
        owner = ownerMember.getUser();
        reader = member("독자", StudyMemberRole.MEMBER).getUser();
        assignment = assignment(study);
        submission = AssignmentSubmission.create(ownerMember, assignment);
        submission.submit("제출 내용", null, LocalDateTime.now());
        submission = submissions.saveAndFlush(submission);
        subscribe(leader, "leader");
        subscribe(secondLeader, "second-leader");
        subscribe(owner, "owner");
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @DisplayName("과제·제출물 댓글 CRUD와 대상별 알림·위치 조회 및 작성자·리더 권한을 적용한다")
    void crudAndNotificationRecipients(boolean onSubmission) {
        String path = path(onSubmission);
        long id = create(author, path, "  댓글 내용  ");
        request(reader).get(path).then().statusCode(200)
                .body("comments[0].content", equalTo("댓글 내용"))
                .body("comments[0].canEdit", equalTo(false)).body("comments[0].canDelete", equalTo(false));
        request(author).queryParam("focusCommentId", id).get(path).then().statusCode(200)
                .body("comments[0].id", equalTo((int) id)).body("comments[0].canEdit", equalTo(true));
        List<Long> recipientIds = List.of(leader.getId(), secondLeader.getId());
        ResourceType resourceType = ResourceType.ASSIGNMENT;
        Long resourceId = assignment.getId();
        if (onSubmission) {
            recipientIds = List.of(owner.getId());
            resourceType = ResourceType.ASSIGNMENT_SUBMISSION;
            resourceId = submission.getId();
        }
        assertThat(notifications.findAll()).extracting(n -> n.getRecipient().getId())
                .containsExactlyInAnyOrderElementsOf(recipientIds);
        assertThat(deliveries.count()).isEqualTo(recipientIds.size());
        for (var notification : notifications.findAll()) {
            assertThat(notification.getResourceType()).isEqualTo(resourceType);
            assertThat(notification.getResourceId()).isEqualTo(resourceId);
            assertThat(notification.getDeepLink()).isEqualTo(path.replace("/comments", "") + "?focusCommentId=" + id);
            assertThat(notification.getTitle()).isEqualTo("[과제 스터디] 새 댓글");
            assertThat(notification.getBody()).isEqualTo("작성자 스터디원이 댓글을 달았어요");
        }
        request(leader).body(Map.of("content", "타인 수정")).patch(path + "/" + id)
                .then().statusCode(403);
        request(author).body(Map.of("content", " 수정한 내용 ")).patch(path + "/" + id)
                .then().statusCode(204);
        request(author).get(path).then().statusCode(200)
                .body("comments[0].content", equalTo("수정한 내용")).body("comments[0].isEdited", equalTo(true));
        assertThat(notifications.count()).isEqualTo(recipientIds.size());
        request(reader).delete(path + "/" + id).then().statusCode(403);
        request(leader).delete(path + "/" + id).then().statusCode(204);
        assertThat(comments.existsById(id)).isFalse();
        assertThat(notifications.count()).isZero();
        assertThat(deliveries.count()).isZero();
        request(leader).delete(path + "/" + id).then().statusCode(404);
    }

    @Test
    @DisplayName("과제 댓글의 리더 작성자는 자기 알림을 받지 않고 제출자 본인 댓글은 알림을 만들지 않는다")
    void excludeAuthorFromRecipients() {
        create(leader, path(false), "리더 댓글");
        assertThat(notifications.findAll()).extracting(n -> n.getRecipient().getId())
                .containsExactly(secondLeader.getId());
        create(owner, path(true), "내 제출물 댓글");
        assertThat(notifications.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("비공개 제출물은 제출자와 리더만 조회·작성하며 공개 전 작성자는 이후에도 본인 댓글을 수정·삭제한다")
    void privateSubmissionAndFormerReadPermission() {
        String path = path(true);
        long id = create(author, path, "공개 중 작성");
        jdbc.update(VISIBILITY_SQL, SubmissionVisibility.LEADER_ONLY.name(), assignment.getId());
        request(reader).get(path).then().statusCode(403);
        request(reader).body(Map.of("content", "거부")).post(path).then().statusCode(403);
        request(owner).get(path).then().statusCode(200);
        request(leader).get(path).then().statusCode(200);
        request(author).body(Map.of("content", "비공개 전환 후 수정")).patch(path + "/" + id)
                .then().statusCode(204);
        request(author).delete(path + "/" + id).then().statusCode(204);
        create(leader, path, "비공개 피드백");
        assertThat(notifications.findAll()).extracting(n -> n.getRecipient().getId()).containsExactly(owner.getId());
    }

    @Test
    @DisplayName("실제 제출 전 행은 본문이 있어도 작성할 수 없으며 리더·제출자만 빈 댓글 목록을 볼 수 있다")
    void rejectUnsubmittedTarget() {
        jdbc.update(UNSUBMIT_SQL, submission.getId());
        String path = path(true);
        request(owner).get(path).then().statusCode(200).body("comments", hasSize(0));
        request(leader).get(path).then().statusCode(200);
        request(reader).get(path).then().statusCode(403);
        for (User user : List.of(owner, leader)) {
            request(user).body(Map.of("content", "미제출 댓글")).post(path).then().statusCode(409)
                    .body("code", equalTo("COMMENT_TARGET_NOT_READY"));
        }
        assertThat(comments.count()).isZero();
        assertThat(notifications.count()).isZero();
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @DisplayName("마감 이후에도 댓글을 작성할 수 있으며 잘못된 본문은 저장하지 않는다")
    void commentAfterDeadlineAndValidateContent(boolean onSubmission) {
        jdbc.update(CLOSE_AT_SQL, LocalDateTime.now().minusDays(1), assignment.getId());
        String path = path(onSubmission);
        for (String content : List.of(" ", "가".repeat(1001))) {
            request(author).body(Map.of("content", content)).post(path).then().statusCode(400)
                    .body("code", equalTo("COMMENT_INVALID_CONTENT"));
        }
        create(author, path, "마감 뒤 댓글");
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @DisplayName("ID 커서·삭제 후 이어 조회·focus 범위와 혼합 경로 거부를 과제·제출물에 공통 적용한다")
    void paginationAndScope(boolean onSubmission) {
        String path = path(onSubmission);
        long first = create(author, path, "첫 댓글");
        long second = create(author, path, "두 번째");
        long third = create(author, path, "세 번째");
        request(reader).queryParam("size", 1).get(path).then().statusCode(200)
                .body("nextCursor", equalTo((int) first)).body("hasNext", equalTo(true));
        request(author).delete(path + "/" + first).then().statusCode(204);
        request(reader).queryParam("cursor", first).get(path).then().statusCode(200)
                .body("comments", hasSize(2)).body("comments[0].id", equalTo((int) second))
                .body("comments[1].id", equalTo((int) third)).body("nextCursor", nullValue());
        request(reader).queryParam("focusCommentId", second).queryParam("size", 1).get(path)
                .then().statusCode(200).body("comments[0].id", equalTo((int) second));
        request(reader).queryParam("cursor", second).queryParam("focusCommentId", third).get(path)
                .then().statusCode(400).body("code", equalTo("COMMENT_INVALID_PAGE_REQUEST"));
        request(reader).queryParam("focusCommentId", first).get(path).then().statusCode(404);
        Assignment other = assignment(study);
        String wrongPath = ASSIGNMENT_PATH.formatted(study.getId(), other.getId());
        if (onSubmission) {
            wrongPath = SUBMISSION_PATH.formatted(study.getId(), other.getId(), submission.getId());
            request(reader).get(wrongPath).then().statusCode(404)
                    .body("code", equalTo("ASSIGNMENT_SUBMISSION_NOT_FOUND"));
        }
        request(author).body(Map.of("content", "잘못된 원문")).patch(wrongPath + "/" + second)
                .then().statusCode(404);
        Study otherStudy = studies.saveAndFlush(Study.create("다른 스터디", null, false));
        members.saveAndFlush(StudyMember.create(otherStudy, author, "작성자", null, StudyMemberRole.MEMBER));
        request(author).get(path.replace("/studies/" + study.getId(), "/studies/" + otherStudy.getId()))
                .then().statusCode(404).body("code", equalTo("ASSIGNMENT_NOT_FOUND"));
        User outsider = users.saveAndFlush(User.create("외부인", null));
        request(outsider).get(path).then().statusCode(403);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @DisplayName("스터디 탈퇴 후 댓글을 보존하며 계정이 남은 작성자는 알려진 본인 댓글을 수정·삭제할 수 있다")
    void preserveAfterLeaving(boolean onSubmission) {
        String path = path(onSubmission);
        long id = create(author, path, "탈퇴 후 보존");
        memberService.leave(author.getId(), study.getId());
        request(leader).get(path).then().statusCode(200)
                .body("comments[0].author.isDeparted", equalTo(true));
        request(author).get(path).then().statusCode(403);
        request(author).body(Map.of("content", "탈퇴 후 수정")).patch(path + "/" + id).then().statusCode(204);
        request(author).delete(path + "/" + id).then().statusCode(204);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @DisplayName("계정 탈퇴 후 원문이 남은 댓글과 다른 수신자의 알림을 보존한다")
    void preserveAfterWithdrawal(boolean onSubmission) {
        long id = create(author, path(onSubmission), "보존");
        long notificationCount = notifications.count();
        userService.withdraw(author.getId());
        Comment comment = comments.findById(id).orElseThrow();
        assertThat(comment.getAuthorUser()).isNull();
        assertThat(comment.getAuthorMember()).isNull();
        assertThat(notifications.count()).isEqualTo(notificationCount);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @DisplayName("Delivery 저장 실패는 과제·제출물 댓글과 알림을 함께 롤백한다")
    void rollbackOnDeliveryFailure(boolean onSubmission) {
        doThrow(new DataIntegrityViolationException("delivery failure")).when(deliveries)
                .save(any(NotificationDelivery.class));
        CommentTarget target = CommentTarget.assignment(assignment.getId());
        if (onSubmission) {
            target = CommentTarget.submission(assignment.getId(), submission.getId());
        }
        CommentTarget commentTarget = target;
        assertThatThrownBy(() -> commentService.create(author.getId(), study.getId(), commentTarget,
                new CommentWriteRequest("롤백"))).isInstanceOf(DataIntegrityViolationException.class);
        assertThat(comments.count()).isZero();
        assertThat(notifications.count()).isZero();
        assertThat(deliveries.count()).isZero();
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @DisplayName("과제·제출물 댓글도 알려진 잠금 실패를 409로 변환한다")
    void translateLockFailure(boolean onSubmission) {
        doThrow(new CannotAcquireLockException("lock timeout")).when(commentService)
                .create(any(Long.class), any(Long.class), any(CommentTarget.class), any(CommentWriteRequest.class));
        request(author).body(Map.of("content", "잠금 경합")).post(path(onSubmission)).then().statusCode(409)
                .body("code", equalTo("COMMENT_STATE_CHANGED"));
        assertThat(comments.count()).isZero();
    }

    private String path(boolean onSubmission) {
        if (onSubmission) {
            return SUBMISSION_PATH.formatted(study.getId(), assignment.getId(), submission.getId());
        }
        return ASSIGNMENT_PATH.formatted(study.getId(), assignment.getId());
    }

    private StudyMember member(String name, StudyMemberRole role) {
        User user = users.saveAndFlush(User.create(name, null));
        return members.saveAndFlush(StudyMember.create(study, user, name, null, role));
    }

    private Assignment assignment(Study targetStudy) {
        LocalDateTime now = LocalDateTime.now();
        return assignments.saveAndFlush(Assignment.create(targetStudy, "과제", "내용", null,
                SubmissionTarget.MEMBERS_ONLY, SubmissionVisibility.ALL_STUDY_MEMBERS, now.plusDays(1), now));
    }

    private void subscribe(User user, String endpoint) {
        subscriptions.saveAndFlush(WebPushSubscription.create(user, endpoint, "https://push.example.com/" + endpoint,
                "p256dh", "auth"));
    }

    private RequestSpecification request(User user) {
        return auth.givenAuthenticatedUser(user.getId()).port(port).contentType(ContentType.JSON);
    }

    private long create(User user, String path, String content) {
        return request(user).body(Map.of("content", content)).post(path).then().statusCode(201)
                .extract().jsonPath().getLong("commentId");
    }
}
