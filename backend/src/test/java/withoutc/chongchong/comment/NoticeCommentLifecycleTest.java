package withoutc.chongchong.comment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import withoutc.chongchong.assignment.entity.Assignment;
import withoutc.chongchong.assignment.entity.AssignmentSubmission;
import withoutc.chongchong.assignment.entity.SubmissionTarget;
import withoutc.chongchong.assignment.entity.SubmissionVisibility;
import withoutc.chongchong.assignment.repository.AssignmentRepository;
import withoutc.chongchong.assignment.repository.AssignmentSubmissionRepository;
import withoutc.chongchong.comment.controller.dto.CommentResponse;
import withoutc.chongchong.comment.controller.dto.CommentWriteRequest;
import withoutc.chongchong.comment.entity.Comment;
import withoutc.chongchong.comment.repository.CommentRepository;
import withoutc.chongchong.comment.service.CommentService;
import withoutc.chongchong.notice.entity.Notice;
import withoutc.chongchong.notice.repository.NoticeRepository;
import withoutc.chongchong.notice.service.NoticeService;
import withoutc.chongchong.notification.entity.Notification;
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
import withoutc.chongchong.study.service.StudyService;
import withoutc.chongchong.support.PostgresContainerTest;
import withoutc.chongchong.support.TestDatabaseCleaner;
import withoutc.chongchong.user.entity.User;
import withoutc.chongchong.user.repository.UserRepository;
import withoutc.chongchong.user.service.UserService;

class NoticeCommentLifecycleTest extends PostgresContainerTest {

    private static final int MAX_CONTENT_LENGTH = 1000;
    private static final int MAX_TWO_UNIT_EMOJI_COUNT = 500;

    @Autowired
    private UserRepository users;

    @Autowired
    private StudyRepository studies;

    @Autowired
    private StudyMemberRepository members;

    @Autowired
    private NoticeRepository notices;

    @Autowired
    private CommentRepository comments;

    @Autowired
    private CommentService commentService;

    @Autowired
    private NotificationRepository notifications;

    @Autowired
    private NotificationDeliveryRepository deliveries;

    @Autowired
    private WebPushSubscriptionRepository subscriptions;

    @Autowired
    private NoticeService noticeService;

    @Autowired
    private StudyService studyService;

    @Autowired
    private StudyMemberService memberService;

    @Autowired
    private UserService userService;

    @Autowired
    private AssignmentRepository assignments;

    @Autowired
    private AssignmentSubmissionRepository submissions;

    @Autowired
    private TestDatabaseCleaner cleaner;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void setUp() {
        cleaner.clean();
    }

    @Test
    @DisplayName("스터디 탈퇴와 재가입 후에도 옛 작성자 표시를 복구하지 않으며 본인 수정은 허용한다")
    void preserveAfterLeavingAndRejoining() {
        Fixture f = fixture();
        long id = create(f, f.author(), "보존할 댓글");

        memberService.leave(f.author().getId(), f.study().getId());
        assertDeparted(f, id);
        assertThat(comments.findById(id).orElseThrow().authorUserId()).isEqualTo(f.author().getId());
        assertThat(notifications.count()).isEqualTo(1);
        assertThat(deliveries.count()).isEqualTo(1);

        commentService.update(f.author().getId(), f.study().getId(), f.notice().getId(), id,
                new CommentWriteRequest("탈퇴 후 본인 수정"));
        StudyMember rejoined = members.saveAndFlush(StudyMember.create(f.study(), f.author(),
                "재가입 이름", "https://example.com/new.png", StudyMemberRole.MEMBER));
        assertThat(rejoined.getId()).isNotEqualTo(f.authorMember().getId());
        assertDeparted(f, id);
        assertThat(view(f, id).content()).isEqualTo("탈퇴 후 본인 수정");

        userService.withdraw(f.author().getId());
        assertThat(comments.findById(id).orElseThrow().getAuthorUser()).isNull();
        assertDeparted(f, id);
        assertThat(notifications.count()).isEqualTo(1);
        assertThat(deliveries.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("회원 탈퇴는 남은 원문의 모든 댓글을 보존하고 탈퇴한 사람의 수신 알림만 지운다")
    void preserveAuthoredCommentsAndOtherRecipientsNotifications() {
        Fixture f = fixture();
        long authored = create(f, f.author(), "본인 작성");
        Comment other = comments.saveAndFlush(Comment.create(ResourceType.NOTICE, f.notice().getId(),
                f.otherMember(), "다른 작성자"));
        notifications.saveAndFlush(Notification.createForComment(other, f.authorMember(),
                "수신한 알림", "내용", "/studies/1"));

        userService.withdraw(f.author().getId());

        assertThat(comments.count()).isEqualTo(2);
        assertThat(comments.findById(authored).orElseThrow().getAuthorUser()).isNull();
        assertThat(comments.findById(authored).orElseThrow().getAuthorMember()).isNull();
        assertThat(notifications.findAllByRecipientIdOrderByCreatedAtDesc(f.author().getId())).isEmpty();
        assertThat(notifications.findAllByRecipientIdOrderByCreatedAtDesc(f.leader().getId())).hasSize(1);
        assertThat(notices.existsById(f.notice().getId())).isTrue();
        assertDeparted(f, authored);
    }

    @Test
    @DisplayName("방출 시 공지 댓글은 보존하고 삭제되는 제출물 안의 타인 댓글과 알림은 정리한다")
    void removeSubmissionCommentsWhenMemberIsExpelled() {
        Fixture f = fixture();
        long preserved = create(f, f.author(), "공지에 남을 댓글");
        Comment submissionComment = submissionComment(f);

        memberService.expel(f.leader().getId(), f.study().getId(), f.authorMember().getId());

        assertThat(comments.existsById(submissionComment.getId())).isFalse();
        assertThat(notifications.count()).isEqualTo(1);
        assertDeparted(f, preserved);
    }

    @Test
    @DisplayName("회원 탈퇴로 제출물이 삭제되면 타인의 제출물 댓글도 함께 삭제한다")
    void removeSubmissionCommentsWhenAccountIsDeleted() {
        Fixture f = fixture();
        long preserved = create(f, f.author(), "공지 댓글");
        Comment submissionComment = submissionComment(f);

        userService.withdraw(f.author().getId());

        assertThat(comments.existsById(submissionComment.getId())).isFalse();
        assertDeparted(f, preserved);
        assertThat(notifications.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("공지 삭제는 작성자와 관계없이 그 원문의 댓글·알림·Delivery만 정리한다")
    void deleteNoticeAndKeepOtherNoticeComments() {
        Fixture f = fixture();
        create(f, f.author(), "첫 댓글");
        create(f, f.otherMember().getUser(), "다른 작성자");
        Notice otherNotice = notices.saveAndFlush(Notice.create(f.study(), "다른 공지", "내용"));
        long remaining = commentService.create(f.author().getId(), f.study().getId(), otherNotice.getId(),
                new CommentWriteRequest("다른 원문 댓글")).commentId();

        noticeService.delete(f.leader().getId(), f.study().getId(), f.notice().getId());

        assertThat(comments.findAll()).extracting(Comment::getId).containsExactly(remaining);
        assertThat(notifications.findAll()).allSatisfy(n -> assertThat(n.getResourceId()).isEqualTo(otherNotice.getId()));
        assertThat(notifications.count()).isEqualTo(1);
        assertThat(deliveries.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("스터디 삭제 시 하위 댓글과 알림을 지우고 다른 스터디는 보존한다")
    void deleteStudy() {
        Fixture deleted = fixture();
        create(deleted, deleted.author(), "삭제 대상");
        Fixture kept = fixture();
        long remaining = create(kept, kept.author(), "보존 대상");

        studyService.deleteStudy(deleted.leader().getId(), deleted.study().getId());

        assertThat(comments.findAll()).extracting(Comment::getId).containsExactly(remaining);
        assertThat(notifications.count()).isEqualTo(1);
        assertThat(deliveries.count()).isEqualTo(1);
        assertThat(users.existsById(deleted.author().getId())).isTrue();
    }

    @Test
    @DisplayName("생성 또는 원문 삭제가 실패하면 댓글·알림·Delivery를 함께 롤백한다")
    void rollbackCreationAndDeletion() {
        Fixture f = fixture();
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        assertThatThrownBy(() -> tx.executeWithoutResult(status -> {
            create(f, f.author(), "롤백 생성");
            throw new IllegalStateException("생성 실패");
        })).isInstanceOf(IllegalStateException.class);
        assertThat(comments.count()).isZero();
        assertThat(notifications.count()).isZero();
        assertThat(deliveries.count()).isZero();

        long id = create(f, f.author(), "보존");
        assertThatThrownBy(() -> tx.executeWithoutResult(status -> {
            noticeService.delete(f.leader().getId(), f.study().getId(), f.notice().getId());
            entityManager.flush();
            assertThat(comments.count()).isZero();
            assertThat(notifications.count()).isZero();
            assertThat(deliveries.count()).isZero();
            throw new IllegalStateException("삭제 실패");
        })).isInstanceOf(IllegalStateException.class);
        assertThat(notices.existsById(f.notice().getId())).isTrue();
        assertThat(comments.existsById(id)).isTrue();
        assertThat(notifications.count()).isEqualTo(1);
        assertThat(deliveries.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("PostgreSQL에서 String.length 기준 1000자 이모지를 저장하고 수신자 중복을 제약한다")
    void enforceRealDatabaseConstraints() {
        Fixture f = fixture();
        long id = create(f, f.author(), "😀".repeat(MAX_TWO_UNIT_EMOJI_COUNT));
        Comment comment = comments.findById(id).orElseThrow();
        StudyMember recipient = members.findByStudyIdAndUserId(f.study().getId(), f.leader().getId()).orElseThrow();

        assertThat(comment.getContent()).hasSize(MAX_CONTENT_LENGTH);
        assertThatThrownBy(() -> notifications.saveAndFlush(Notification.createForComment(comment, recipient,
                "중복", "내용", "/studies/1")))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(notifications.count()).isEqualTo(1);
    }

    private Comment submissionComment(Fixture f) {
        LocalDateTime now = LocalDateTime.now();
        Assignment assignment = Assignment.create(f.study(), "과제", "내용", "제출",
                SubmissionTarget.MEMBERS_ONLY, SubmissionVisibility.ALL_STUDY_MEMBERS, now.plusDays(1), now);
        assignment.initializeSubmissions(List.of(f.authorMember()));
        assignments.saveAndFlush(assignment);
        AssignmentSubmission submission = submissions.findByAssignmentIdAndMemberId(assignment.getId(),
                f.authorMember().getId()).orElseThrow();
        submission.submit("제출 완료", null, now);
        submissions.saveAndFlush(submission);
        Comment comment = comments.saveAndFlush(Comment.create(ResourceType.ASSIGNMENT_SUBMISSION,
                submission.getId(), f.otherMember(), "타인의 댓글"));
        StudyMember leader = members.findByStudyIdAndUserId(f.study().getId(), f.leader().getId()).orElseThrow();
        notifications.saveAndFlush(Notification.createForComment(comment, leader, "제출물 댓글", "내용", "/studies/1"));
        return comment;
    }

    private Fixture fixture() {
        User leader = users.save(User.create("리더", null));
        User author = users.save(User.create("바니", "https://example.com/author.png"));
        User other = users.save(User.create("다른 작성자", null));
        Study study = studies.save(Study.create("수명 스터디", "설명"));
        members.saveAndFlush(StudyMember.create(study, leader, "리더", null, StudyMemberRole.LEADER));
        StudyMember authorMember = members.saveAndFlush(StudyMember.create(study, author,
                "바니", author.getProfileImageUrl(), StudyMemberRole.MEMBER));
        StudyMember otherMember = members.saveAndFlush(StudyMember.create(study, other,
                "다른 작성자", null, StudyMemberRole.MEMBER));
        Notice notice = notices.saveAndFlush(Notice.create(study, "공지", "내용"));
        subscriptions.saveAndFlush(WebPushSubscription.create(leader, "browser",
                "https://push.example.com/" + leader.getId(), "key", "auth"));
        return new Fixture(leader, author, study, authorMember, otherMember, notice);
    }

    private long create(Fixture f, User author, String content) {
        return commentService.create(author.getId(), f.study().getId(), f.notice().getId(),
                new CommentWriteRequest(content)).commentId();
    }

    private CommentResponse view(Fixture f, long id) {
        return commentService.getList(f.leader().getId(), f.study().getId(), f.notice().getId(),
                null, id, 10).comments().getFirst();
    }

    private void assertDeparted(Fixture f, long id) {
        CommentResponse response = view(f, id);
        assertThat(response.author().name()).isEqualTo("탈퇴한 스터디원");
        assertThat(response.author().profileImageUrl()).isNull();
        assertThat(response.author().isDeparted()).isTrue();
        assertThat(response.canEdit()).isFalse();
        assertThat(response.canDelete()).isTrue();
    }

    private record Fixture(User leader, User author, Study study, StudyMember authorMember,
                           StudyMember otherMember, Notice notice) {
    }
}
