package withoutc.chongchong.comment;

import static org.assertj.core.api.Assertions.assertThat;
import static java.util.concurrent.TimeUnit.SECONDS;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import withoutc.chongchong.assignment.entity.Assignment;
import withoutc.chongchong.assignment.entity.AssignmentSubmission;
import withoutc.chongchong.assignment.entity.SubmissionTarget;
import withoutc.chongchong.assignment.entity.SubmissionVisibility;
import withoutc.chongchong.assignment.repository.AssignmentRepository;
import withoutc.chongchong.assignment.repository.AssignmentSubmissionRepository;
import withoutc.chongchong.comment.controller.dto.CommentWriteRequest;
import withoutc.chongchong.comment.entity.Comment;
import withoutc.chongchong.comment.repository.CommentRepository;
import withoutc.chongchong.comment.service.CommentService;
import withoutc.chongchong.global.exception.BusinessException;
import withoutc.chongchong.notice.controller.dto.NoticeCreateRequest;
import withoutc.chongchong.notice.entity.Notice;
import withoutc.chongchong.notice.repository.NoticeRepository;
import withoutc.chongchong.notice.service.NoticeService;
import withoutc.chongchong.notification.repository.NotificationDeliveryRepository;
import withoutc.chongchong.notification.entity.ResourceType;
import withoutc.chongchong.notification.repository.NotificationRepository;
import withoutc.chongchong.notification.service.NotificationReminderService;
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

class NoticeCommentConcurrencyTest extends PostgresContainerTest {

    private static final long TIMEOUT = 15;
    private static final long PARALLEL_COMPLETION_TIMEOUT = 5;
    @Autowired
    private AssignmentRepository assignments;
    @Autowired
    private AssignmentSubmissionRepository submissions;
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
    private NoticeService noticeService;

    @Autowired
    private StudyService studyService;

    @Autowired
    private StudyMemberService memberService;

    @Autowired
    private UserService userService;

    @Autowired
    private NotificationRepository notifications;

    @Autowired
    private NotificationDeliveryRepository deliveries;

    @Autowired
    private NotificationReminderService reminderService;

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

    @Test
    @DisplayName("댓글 생성이 먼저 잠그면 공지 삭제가 기다린 뒤 새 댓글과 알림까지 삭제한다")
    void createBeforeNoticeDeletion() throws Exception {
        Fixture f = fixture(false);
        race(() -> create(f), () -> noticeService.delete(f.leaderId(), f.studyId(), f.noticeId()));
        assertThat(notices.existsById(f.noticeId())).isFalse();
        assertEmpty();
    }

    @Test
    @DisplayName("공지 삭제가 먼저 잠그면 대기한 댓글 생성은 원문 없음으로 실패한다")
    void deleteBeforeCommentCreation() throws Exception {
        Fixture f = fixture(false);
        race(() -> noticeService.delete(f.leaderId(), f.studyId(), f.noticeId()), () -> create(f),
                "NOTICE_NOT_FOUND");
        assertEmpty();
    }

    @Test
    @DisplayName("댓글 생성 뒤 회원 탈퇴가 진행되면 원문과 댓글은 남고 작성자 연결만 해제한다")
    void createBeforeAccountWithdrawal() throws Exception {
        Fixture f = fixture(false);
        AtomicLong commentId = new AtomicLong();
        race(() -> commentId.set(create(f)), () -> userService.withdraw(f.authorId()));

        assertThat(users.existsById(f.authorId())).isFalse();
        assertThat(notices.existsById(f.noticeId())).isTrue();
        assertThat(comments.findById(commentId.get()).orElseThrow().getAuthorUser()).isNull();
        assertThat(notifications.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("회원 탈퇴가 먼저 잠그면 대기한 댓글 생성은 없는 사용자로 실패하며 고아 댓글을 남기지 않는다")
    void withdrawBeforeCommentCreation() throws Exception {
        Fixture f = fixture(false);
        race(() -> userService.withdraw(f.authorId()), () -> create(f), "USER_NOT_FOUND");
        assertThat(notices.existsById(f.noticeId())).isTrue();
        assertEmpty();
    }

    @Test
    @DisplayName("댓글 생성과 스터디 탈퇴가 겹쳐도 원문이 남은 댓글은 보존한다")
    void createBeforeMembershipLeave() throws Exception {
        Fixture f = fixture(false);
        AtomicLong commentId = new AtomicLong();
        race(() -> commentId.set(create(f)), () -> memberService.leave(f.authorId(), f.studyId()));
        assertThat(comments.findById(commentId.get()).orElseThrow().getAuthorMember()).isNull();
        assertThat(notifications.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("스터디 삭제는 먼저 생성 중인 댓글을 기다리고 하위 댓글·알림을 모두 삭제한다")
    void createBeforeStudyDeletion() throws Exception {
        Fixture f = fixture(false);
        race(() -> create(f), () -> studyService.deleteStudy(f.leaderId(), f.studyId()));
        assertThat(studies.existsById(f.studyId())).isFalse();
        assertEmpty();
    }

    @Test
    @DisplayName("기존 공지 생성과 회원 탈퇴도 같은 잠금 순서로 끝나며 원문은 보존한다")
    void originalNoticeCreationAndWithdrawal() throws Exception {
        Fixture f = fixture(false);
        AtomicLong noticeId = new AtomicLong();
        race(() -> noticeId.set(noticeService.create(f.leaderId(), f.studyId(),
                        new NoticeCreateRequest("새 공지", "내용", null, null)).noticeId()),
                () -> userService.withdraw(f.authorId()));
        assertThat(notices.existsById(noticeId.get())).isTrue();
        assertThat(notifications.findAllByRecipientIdOrderByCreatedAtDesc(f.authorId())).isEmpty();
    }

    @Test
    @DisplayName("리마인더·공지 삭제·회원 탈퇴의 세 트랜잭션이 역순 잠금 없이 끝난다")
    void reminderDeletionAndWithdrawal() throws Exception {
        Fixture f = fixture(true);
        create(f);
        var candidate = reminderService.findCandidates().getFirst();
        CountDownLatch release = new CountDownLatch(1);
        CompletableFuture<Void> held = new CompletableFuture<>();
        CompletableFuture<Integer> deletingPid = new CompletableFuture<>();
        CompletableFuture<Integer> withdrawalPid = new CompletableFuture<>();
        ExecutorService executor = Executors.newFixedThreadPool(3);
        try {
            Future<?> reminder = executor.submit(() -> holding(() -> reminderService.process(candidate), held, release));
            held.get(TIMEOUT, SECONDS);
            Future<?> deletion = executor.submit(() -> waiting(
                    () -> noticeService.delete(f.leaderId(), f.studyId(), f.noticeId()), deletingPid));
            Future<?> withdrawal = executor.submit(() -> waiting(
                    () -> userService.withdraw(f.authorId()), withdrawalPid));
            awaitDatabaseWait(deletingPid.get(TIMEOUT, SECONDS));
            awaitDatabaseWait(withdrawalPid.get(TIMEOUT, SECONDS));
            release.countDown();
            reminder.get(TIMEOUT, SECONDS);
            deletion.get(TIMEOUT, SECONDS);
            withdrawal.get(TIMEOUT, SECONDS);
        } finally {
            release.countDown();
            executor.shutdownNow();
            assertThat(executor.awaitTermination(TIMEOUT, SECONDS)).isTrue();
        }
        assertThat(users.existsById(f.authorId())).isFalse();
        assertThat(notices.existsById(f.noticeId())).isFalse();
        assertEmpty();
    }

    @Test
    @DisplayName("같은 스터디의 서로 다른 공지에는 첫 댓글 트랜잭션이 끝나기 전에 댓글을 등록할 수 있다")
    void createOnDifferentNoticesWithoutWaiting() throws Exception {
        Fixture f = fixture(false);
        Notice other = notices.saveAndFlush(Notice.create(studies.findById(f.studyId()).orElseThrow(), "다른 공지", "내용"));
        CountDownLatch release = new CountDownLatch(1);
        CompletableFuture<Void> held = new CompletableFuture<>();
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<?> first = executor.submit(() -> holding(() -> create(f), held, release));
            held.get(TIMEOUT, SECONDS);
            Future<Long> second = executor.submit(() -> commentService.create(f.authorId(), f.studyId(), other.getId(),
                    new CommentWriteRequest("동시에 등록")).commentId());
            assertThat(second.get(PARALLEL_COMPLETION_TIMEOUT, SECONDS)).isPositive();
            assertThat(first.isDone()).isFalse();
            release.countDown();
            first.get(TIMEOUT, SECONDS);
        } finally {
            release.countDown();
            executor.shutdownNow();
            assertThat(executor.awaitTermination(TIMEOUT, SECONDS)).isTrue();
        }
        assertThat(comments.count()).isEqualTo(2);
    }

    @Test
    @DisplayName("동일 공지의 댓글 등록은 같은 원문 잠금에서 기다린 뒤 모두 저장한다")
    void serializeCommentsOnSameNotice() throws Exception {
        Fixture f = fixture(false);
        race(() -> create(f), () -> create(f));
        assertThat(comments.count()).isEqualTo(2);
        assertThat(notifications.count()).isEqualTo(2);
    }

    @Test
    @DisplayName("스터디 탈퇴가 먼저 끝나면 대기한 댓글 등록은 작성 권한 없음으로 실패한다")
    void leaveBeforeCommentCreation() throws Exception {
        Fixture f = fixture(false);
        race(() -> memberService.leave(f.authorId(), f.studyId()), () -> create(f), "STUDY_ACCESS_DENIED");
        assertEmpty();
    }

    @Test
    @DisplayName("스터디 삭제가 먼저 끝나면 대기한 새 공지 생성은 없는 스터디로 실패한다")
    void studyDeletionBeforeOriginalNoticeCreation() throws Exception {
        Fixture f = fixture(false);
        race(() -> studyService.deleteStudy(f.leaderId(), f.studyId()),
                () -> noticeService.create(f.leaderId(), f.studyId(),
                        new NoticeCreateRequest("새 공지", "내용", null, null)), "STUDY_NOT_FOUND");
        assertThat(notices.count()).isZero();
        assertEmpty();
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @DisplayName("서로의 제출물에 댓글을 작성한 두 사람이 동시에 탈퇴해도 댓글 삭제와 작성자 연결 해제가 교착하지 않는다")
    void simultaneousWithdrawalWithCrossAuthoredSubmissionComments(boolean withdrawAccount) throws Exception {
        Fixture f = fixture(false);
        Study study = studies.findById(f.studyId()).orElseThrow();
        StudyMember first = members.getByStudyIdAndUserIdOrThrow(f.studyId(), f.authorId());
        User secondUser = users.save(User.create("두 번째 작성자", null));
        StudyMember second = members.saveAndFlush(
                StudyMember.create(study, secondUser, "두 번째 작성자", null, StudyMemberRole.MEMBER));
        AssignmentSubmission firstSubmission = submitted(study, first);
        AssignmentSubmission secondSubmission = submitted(study, second);
        comments.saveAndFlush(Comment.create(ResourceType.ASSIGNMENT_SUBMISSION, firstSubmission.getId(), second,
                "상대 제출물 댓글"));
        comments.saveAndFlush(Comment.create(ResourceType.ASSIGNMENT_SUBMISSION, secondSubmission.getId(), first,
                "상대 제출물 댓글"));
        CountDownLatch usersLocked = new CountDownLatch(2);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<?> firstWithdrawal = executor.submit(
                    () -> withdrawAfterBothUsersLocked(first.getUser().getId(), f.studyId(), withdrawAccount, usersLocked));
            Future<?> secondWithdrawal = executor.submit(
                    () -> withdrawAfterBothUsersLocked(secondUser.getId(), f.studyId(), withdrawAccount, usersLocked));
            firstWithdrawal.get(TIMEOUT, SECONDS);
            secondWithdrawal.get(TIMEOUT, SECONDS);
        } finally {
            executor.shutdownNow();
            assertThat(executor.awaitTermination(TIMEOUT, SECONDS)).isTrue();
        }
        assertThat(submissions.count()).isZero();
        assertThat(members.existsById(first.getId())).isFalse();
        assertThat(members.existsById(second.getId())).isFalse();
        assertEmpty();
    }

    private AssignmentSubmission submitted(Study study, StudyMember member) {
        LocalDateTime now = LocalDateTime.now();
        Assignment assignment = assignments.saveAndFlush(Assignment.create(study, "제출 과제", "내용", "링크",
                SubmissionTarget.MEMBERS_ONLY, SubmissionVisibility.ALL_STUDY_MEMBERS, now.plusDays(1), now));
        AssignmentSubmission submission = AssignmentSubmission.create(member, assignment);
        submission.submit("제출 내용", null, now);
        return submissions.saveAndFlush(submission);
    }

    private void withdrawAfterBothUsersLocked(Long userId, Long studyId, boolean withdrawAccount,
                                             CountDownLatch usersLocked) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            jdbc.execute("SET LOCAL lock_timeout = '10s'");
            users.getByIdForUpdateOrThrow(userId);
            usersLocked.countDown();
            try {
                assertThat(usersLocked.await(TIMEOUT, SECONDS)).isTrue();
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(exception);
            }
            if (withdrawAccount) {
                userService.withdraw(userId);
            } else {
                memberService.leave(userId, studyId);
            }
        });
    }

    private void race(Runnable firstAction, Runnable secondAction) throws Exception {
        race(firstAction, secondAction, null);
    }

    private void race(Runnable firstAction, Runnable secondAction, String expectedError) throws Exception {
        CountDownLatch release = new CountDownLatch(1);
        CompletableFuture<Void> held = new CompletableFuture<>();
        CompletableFuture<Integer> secondPid = new CompletableFuture<>();
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<?> first = executor.submit(() -> holding(firstAction, held, release));
            held.get(TIMEOUT, SECONDS);
            Future<?> second = executor.submit(() -> waiting(secondAction, secondPid, expectedError));
            awaitDatabaseWait(secondPid.get(TIMEOUT, SECONDS));
            release.countDown();
            first.get(TIMEOUT, SECONDS);
            second.get(TIMEOUT, SECONDS);
        } finally {
            release.countDown();
            executor.shutdownNow();
            assertThat(executor.awaitTermination(TIMEOUT, SECONDS)).isTrue();
        }
    }

    private void holding(Runnable action, CompletableFuture<Void> held, CountDownLatch release) {
        try {
            new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                action.run();
                held.complete(null);
                try {
                    assertThat(release.await(TIMEOUT, SECONDS)).isTrue();
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(exception);
                }
            });
        } catch (RuntimeException | Error exception) {
            held.completeExceptionally(exception);
            throw exception;
        }
    }

    private void waiting(Runnable action, CompletableFuture<Integer> pid) {
        waiting(action, pid, null);
    }

    private void waiting(Runnable action, CompletableFuture<Integer> pid, String expectedError) {
        try {
            new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                jdbc.execute("SET LOCAL lock_timeout = '10s'");
                pid.complete(jdbc.queryForObject("SELECT pg_backend_pid()", Integer.class));
                action.run();
            });
            assertThat(expectedError).isNull();
        } catch (BusinessException exception) {
            if (expectedError == null) {
                throw exception;
            }
            assertThat(exception.getErrorCode().getCode()).isEqualTo(expectedError);
        }
    }

    private void awaitDatabaseWait(int pid) throws InterruptedException {
        long deadline = System.nanoTime() + Duration.ofSeconds(5).toNanos();
        while (System.nanoTime() < deadline) {
            if (Boolean.TRUE.equals(jdbc.queryForObject("""
                    SELECT EXISTS (SELECT 1 FROM pg_stat_activity
                        WHERE pid = ? AND wait_event_type = 'Lock')
                    """, Boolean.class, pid))) {
                return;
            }
            Thread.sleep(20);
        }
        throw new AssertionError("두 번째 트랜잭션이 DB 잠금을 기다리지 않았다");
    }

    private Fixture fixture(boolean dueReminder) {
        User leader = users.save(User.create("리더", null));
        User author = users.save(User.create("작성자", null));
        Study study = studies.save(Study.create("경합 스터디", "설명"));
        members.saveAndFlush(StudyMember.create(study, leader, "리더", null, StudyMemberRole.LEADER));
        StudyMember member = members.saveAndFlush(
                StudyMember.create(study, author, "작성자", null, StudyMemberRole.MEMBER));
        Notice notice = Notice.create(study, "공지", "내용");
        if (dueReminder) {
            notice.addRecipients(List.of(member));
            LocalDateTime now = LocalDateTime.now();
            notice.addReminders(List.of(now.minusMinutes(1)), now.minusHours(1));
        }
        notices.saveAndFlush(notice);
        return new Fixture(leader.getId(), author.getId(), study.getId(), notice.getId());
    }

    private long create(Fixture f) {
        return commentService.create(f.authorId(), f.studyId(), f.noticeId(),
                new CommentWriteRequest("댓글")).commentId();
    }

    private void assertEmpty() {
        assertThat(comments.count()).isZero();
        assertThat(notifications.count()).isZero();
        assertThat(deliveries.count()).isZero();
    }

    private record Fixture(Long leaderId, Long authorId, Long studyId, Long noticeId) {
    }
}
