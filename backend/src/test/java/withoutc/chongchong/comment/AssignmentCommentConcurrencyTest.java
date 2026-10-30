package withoutc.chongchong.comment;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import withoutc.chongchong.assignment.controller.dto.AssignmentUpdateRequest;
import withoutc.chongchong.assignment.entity.Assignment;
import withoutc.chongchong.assignment.entity.AssignmentSubmission;
import withoutc.chongchong.assignment.entity.SubmissionTarget;
import withoutc.chongchong.assignment.entity.SubmissionVisibility;
import withoutc.chongchong.assignment.repository.AssignmentRepository;
import withoutc.chongchong.assignment.repository.AssignmentSubmissionRepository;
import withoutc.chongchong.assignment.service.AssignmentService;
import withoutc.chongchong.comment.controller.dto.CommentWriteRequest;
import withoutc.chongchong.comment.repository.CommentRepository;
import withoutc.chongchong.comment.service.CommentService;
import withoutc.chongchong.comment.service.CommentTarget;
import withoutc.chongchong.global.exception.BusinessException;
import withoutc.chongchong.notification.repository.NotificationDeliveryRepository;
import withoutc.chongchong.notification.repository.NotificationRepository;
import withoutc.chongchong.study.entity.Study;
import withoutc.chongchong.study.entity.StudyMember;
import withoutc.chongchong.study.entity.StudyMemberRole;
import withoutc.chongchong.study.repository.StudyMemberRepository;
import withoutc.chongchong.study.repository.StudyRepository;
import withoutc.chongchong.study.service.StudyMemberService;
import withoutc.chongchong.support.PostgresContainerTest;
import withoutc.chongchong.support.TestDatabaseCleaner;
import withoutc.chongchong.user.entity.User;
import withoutc.chongchong.user.repository.UserRepository;

class AssignmentCommentConcurrencyTest extends PostgresContainerTest {

    private static final long TIMEOUT_SECONDS = 15;
    private static final long WAIT_TIMEOUT_SECONDS = 5;
    private static final long POLL_INTERVAL_MILLIS = 20;

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
    private AssignmentService assignmentService;
    @Autowired
    private StudyMemberService memberService;
    @Autowired
    private CommentService commentService;
    @Autowired
    private CommentRepository comments;
    @Autowired
    private NotificationRepository notifications;
    @Autowired
    private NotificationDeliveryRepository deliveries;
    @Autowired
    private TestDatabaseCleaner cleaner;
    @Autowired
    private PlatformTransactionManager transactionManager;
    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void clean() {
        cleaner.clean();
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @DisplayName("과제·제출물 댓글 생성이 먼저 잠그면 과제 삭제는 커밋을 기다린 뒤 댓글·알림을 정리한다")
    void createBeforeAssignmentDeletion(boolean onSubmission) throws Exception {
        Fixture f = fixture(false);
        race(() -> create(f, onSubmission),
                () -> assignmentService.delete(f.leaderId(), f.studyId(), f.assignmentId()), null);
        assertThat(assignments.existsById(f.assignmentId())).isFalse();
        assertEmpty();
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @DisplayName("과제 삭제가 먼저 잠그면 대기한 과제·제출물 댓글은 원문 없음으로 실패한다")
    void deleteBeforeCommentCreation(boolean onSubmission) throws Exception {
        Fixture f = fixture(false);
        race(() -> assignmentService.delete(f.leaderId(), f.studyId(), f.assignmentId()),
                () -> create(f, onSubmission), "ASSIGNMENT_NOT_FOUND");
        assertEmpty();
    }

    @Test
    @DisplayName("제출물 댓글 생성 뒤 제출자가 탈퇴하면 새 댓글과 알림도 제출물과 함께 삭제한다")
    void createBeforeSubmitterLeaves() throws Exception {
        Fixture f = fixture(false);
        race(() -> create(f, true), () -> memberService.leave(f.ownerId(), f.studyId()), null);
        assertThat(submissions.existsById(f.submissionId())).isFalse();
        assertEmpty();
    }

    @Test
    @DisplayName("제출자 탈퇴가 먼저 진행되면 대기한 제출물 댓글은 사라진 제출물에 저장되지 않는다")
    void submitterLeavesBeforeCreation() throws Exception {
        Fixture f = fixture(false);
        race(() -> memberService.leave(f.ownerId(), f.studyId()), () -> create(f, true),
                "ASSIGNMENT_SUBMISSION_NOT_FOUND");
        assertEmpty();
    }

    @Test
    @DisplayName("공개 제출물 댓글 생성 뒤 비공개 전환은 기다리며 기존 제출자 알림은 유지한다")
    void createBeforeVisibilityChange() throws Exception {
        Fixture f = fixture(false);
        race(() -> create(f, true), () -> makePrivate(f), null);
        assertThat(comments.count()).isEqualTo(1);
        assertThat(notifications.findAll()).extracting(n -> n.getRecipient().getId())
                .containsExactly(f.ownerId());
    }

    @Test
    @DisplayName("비공개 전환이 먼저 잠그면 대기한 일반 스터디원의 댓글은 변경 후 권한으로 거부한다")
    void visibilityChangeBeforeCreation() throws Exception {
        Fixture f = fixture(false);
        race(() -> makePrivate(f), () -> create(f, true), "ACCESS_DENIED");
        assertEmpty();
    }

    @Test
    @DisplayName("제출 대상 축소로 리더 제출물이 삭제되면 대기한 댓글도 없는 원문으로 실패한다")
    void targetReductionBeforeCreation() throws Exception {
        Fixture f = fixture(true);
        race(() -> assignmentService.update(f.leaderId(), f.studyId(), f.assignmentId(),
                        new AssignmentUpdateRequest(null, null, null, SubmissionTarget.MEMBERS_ONLY,
                                null, null, null)),
                () -> create(f, true), "ASSIGNMENT_SUBMISSION_NOT_FOUND");
        assertThat(submissions.existsById(f.submissionId())).isFalse();
        assertEmpty();
    }

    private void makePrivate(Fixture f) {
        assignmentService.update(f.leaderId(), f.studyId(), f.assignmentId(),
                new AssignmentUpdateRequest(null, null, null, null, SubmissionVisibility.LEADER_ONLY,
                        null, null));
    }

    private long create(Fixture f, boolean onSubmission) {
        CommentTarget target = CommentTarget.assignment(f.assignmentId());
        if (onSubmission) {
            target = CommentTarget.submission(f.assignmentId(), f.submissionId());
        }
        return commentService.create(f.authorId(), f.studyId(), target, new CommentWriteRequest("경합 댓글"))
                .commentId();
    }

    private void race(Runnable firstAction, Runnable secondAction, String expectedError) throws Exception {
        CountDownLatch release = new CountDownLatch(1);
        CompletableFuture<Void> held = new CompletableFuture<>();
        CompletableFuture<Integer> secondPid = new CompletableFuture<>();
        var executor = Executors.newFixedThreadPool(2);
        try {
            Future<?> first = executor.submit(() -> {
                try {
                    new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                        firstAction.run();
                        held.complete(null);
                        awaitRelease(release);
                    });
                } catch (RuntimeException | Error exception) {
                    held.completeExceptionally(exception);
                    throw exception;
                }
            });
            held.get(TIMEOUT_SECONDS, SECONDS);
            Future<?> second = executor.submit(() -> {
                try {
                    new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                        jdbc.execute("SET LOCAL lock_timeout = '10s'");
                        secondPid.complete(jdbc.queryForObject("SELECT pg_backend_pid()", Integer.class));
                        secondAction.run();
                    });
                    assertThat(expectedError).isNull();
                } catch (BusinessException exception) {
                    assertThat(exception.getErrorCode().getCode()).isEqualTo(expectedError);
                }
            });
            awaitDatabaseWait(secondPid.get(TIMEOUT_SECONDS, SECONDS));
            release.countDown();
            first.get(TIMEOUT_SECONDS, SECONDS);
            second.get(TIMEOUT_SECONDS, SECONDS);
        } finally {
            release.countDown();
            executor.shutdownNow();
            assertThat(executor.awaitTermination(TIMEOUT_SECONDS, SECONDS)).isTrue();
        }
    }

    private void awaitRelease(CountDownLatch release) {
        try {
            assertThat(release.await(TIMEOUT_SECONDS, SECONDS)).isTrue();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(exception);
        }
    }

    private void awaitDatabaseWait(int pid) throws InterruptedException {
        long deadline = System.nanoTime() + Duration.ofSeconds(WAIT_TIMEOUT_SECONDS).toNanos();
        while (System.nanoTime() < deadline) {
            if (Boolean.TRUE.equals(jdbc.queryForObject("""
                    SELECT EXISTS (SELECT 1 FROM pg_stat_activity
                        WHERE pid = ? AND wait_event_type = 'Lock')
                    """, Boolean.class, pid))) {
                return;
            }
            Thread.sleep(POLL_INTERVAL_MILLIS);
        }
        throw new AssertionError("두 번째 트랜잭션이 DB 잠금을 기다리지 않았다");
    }

    private Fixture fixture(boolean leaderOwnsSubmission) {
        Study study = studies.saveAndFlush(Study.create("경합 스터디", null));
        StudyMember leader = member(study, "리더", StudyMemberRole.LEADER);
        StudyMember author = member(study, "작성자", StudyMemberRole.MEMBER);
        StudyMember owner = leader;
        if (!leaderOwnsSubmission) {
            owner = member(study, "제출자", StudyMemberRole.MEMBER);
        }
        LocalDateTime now = LocalDateTime.now();
        Assignment assignment = assignments.saveAndFlush(Assignment.create(study, "과제", "내용", null,
                SubmissionTarget.MEMBERS_AND_LEADER, SubmissionVisibility.ALL_STUDY_MEMBERS, now.plusDays(1), now));
        AssignmentSubmission submission = AssignmentSubmission.create(owner, assignment);
        submission.submit("제출", null, now);
        submissions.saveAndFlush(submission);
        return new Fixture(study.getId(), leader.getUser().getId(), author.getUser().getId(), owner.getUser().getId(),
                assignment.getId(), submission.getId());
    }

    private StudyMember member(Study study, String name, StudyMemberRole role) {
        User user = users.saveAndFlush(User.create(name, null));
        return members.saveAndFlush(StudyMember.create(study, user, name, null, role));
    }

    private void assertEmpty() {
        assertThat(comments.count()).isZero();
        assertThat(notifications.count()).isZero();
        assertThat(deliveries.count()).isZero();
    }

    private record Fixture(Long studyId, Long leaderId, Long authorId, Long ownerId,
                           Long assignmentId, Long submissionId) {
    }
}
