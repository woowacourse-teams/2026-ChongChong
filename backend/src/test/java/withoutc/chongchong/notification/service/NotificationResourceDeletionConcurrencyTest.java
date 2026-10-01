package withoutc.chongchong.notification.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import withoutc.chongchong.assignment.entity.Assignment;
import withoutc.chongchong.assignment.entity.SubmissionTarget;
import withoutc.chongchong.assignment.repository.AssignmentRepository;
import withoutc.chongchong.assignment.service.AssignmentService;
import withoutc.chongchong.notice.entity.Notice;
import withoutc.chongchong.notice.repository.NoticeRepository;
import withoutc.chongchong.notice.service.NoticeService;
import withoutc.chongchong.notification.repository.NotificationRepository;
import withoutc.chongchong.study.entity.Study;
import withoutc.chongchong.study.entity.StudyMember;
import withoutc.chongchong.study.entity.StudyMemberRole;
import withoutc.chongchong.study.repository.StudyMemberRepository;
import withoutc.chongchong.study.repository.StudyRepository;
import withoutc.chongchong.support.PostgresContainerTest;
import withoutc.chongchong.support.TestDatabaseCleaner;
import withoutc.chongchong.user.entity.User;
import withoutc.chongchong.user.repository.UserRepository;

class NotificationResourceDeletionConcurrencyTest extends PostgresContainerTest {

    private static final Duration TIMEOUT = Duration.ofSeconds(20);
    private static final Duration LOCK_WAIT_TIMEOUT = Duration.ofSeconds(5);
    private static final ZoneId ZONE_ID = ZoneId.of("Asia/Seoul");

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private StudyRepository studyRepository;

    @Autowired
    private StudyMemberRepository studyMemberRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AssignmentRepository assignmentRepository;

    @Autowired
    private AssignmentService assignmentService;

    @Autowired
    private NoticeRepository noticeRepository;

    @Autowired
    private NoticeService noticeService;

    @Autowired
    private TestDatabaseCleaner databaseCleaner;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanDatabase() {
        databaseCleaner.clean();
    }

    @Test
    @DisplayName("과제 삭제는 리마인더 생성 트랜잭션이 끝난 뒤 생성된 알림까지 삭제한다")
    void deleteAssignmentWaitsForReminderCreationAndRemovesItsNotification() throws Exception {
        ResourceFixture fixture = createAssignmentWithDueReminder();
        assertDeletionWaitsForReminderCreation(
                () -> assignmentService.delete(fixture.leaderUserId(), fixture.studyId(), fixture.resourceId()),
                () -> assignmentRepository.existsById(fixture.resourceId())
        );
    }

    @Test
    @DisplayName("공지 삭제는 리마인더 생성 트랜잭션이 끝난 뒤 생성된 알림까지 삭제한다")
    void deleteNoticeWaitsForReminderCreationAndRemovesItsNotification() throws Exception {
        ResourceFixture fixture = createNoticeWithDueReminder();
        assertDeletionWaitsForReminderCreation(
                () -> noticeService.delete(fixture.leaderUserId(), fixture.studyId(), fixture.resourceId()),
                () -> noticeRepository.existsById(fixture.resourceId())
        );
    }

    private void assertDeletionWaitsForReminderCreation(Runnable deletionAction,
                                                         BooleanSupplier resourceExists) throws Exception {
        CountDownLatch releaseScheduler = new CountDownLatch(1);
        CompletableFuture<Integer> createdNotificationCount = new CompletableFuture<>();
        CompletableFuture<Integer> deletionBackendPid = new CompletableFuture<>();
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<?> scheduler = executor.submit(() -> {
                try {
                    new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                        notificationService.createScheduledRemindNotifications();
                        createdNotificationCount.complete(notificationRepository.findAll().size());
                        await(releaseScheduler);
                    });
                } catch (RuntimeException | Error exception) {
                    createdNotificationCount.completeExceptionally(exception);
                    throw exception;
                }
            });
            assertThat(createdNotificationCount.get(TIMEOUT.toSeconds(), TimeUnit.SECONDS)).isEqualTo(1);

            Future<?> deletion = executor.submit(() -> new TransactionTemplate(transactionManager)
                    .executeWithoutResult(status -> {
                        jdbcTemplate.execute("SET LOCAL lock_timeout = '10s'");
                        deletionBackendPid.complete(jdbcTemplate.queryForObject(
                                "SELECT pg_backend_pid()", Integer.class));
                        deletionAction.run();
                    }));

            int backendPid = deletionBackendPid.get(TIMEOUT.toSeconds(), TimeUnit.SECONDS);
            awaitDatabaseLockWait(backendPid);

            releaseScheduler.countDown();
            scheduler.get(TIMEOUT.toSeconds(), TimeUnit.SECONDS);
            deletion.get(TIMEOUT.toSeconds(), TimeUnit.SECONDS);

            assertThat(notificationRepository.findAll()).isEmpty();
            assertThat(resourceExists.getAsBoolean()).isFalse();
        } finally {
            releaseScheduler.countDown();
            executor.shutdown();
            if (!executor.awaitTermination(TIMEOUT.toSeconds(), TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        }
    }

    private ResourceFixture createAssignmentWithDueReminder() {
        LocalDateTime now = LocalDateTime.now(ZONE_ID);
        LocalDateTime creationTime = now.minusDays(1);
        User leaderUser = userRepository.saveAndFlush(User.create("리더", null));
        User recipient = userRepository.saveAndFlush(User.create("스터디원", null));
        Study study = studyRepository.saveAndFlush(Study.create("스터디", "설명"));
        studyMemberRepository.saveAndFlush(
                StudyMember.create(study, leaderUser, "리더", null, StudyMemberRole.LEADER));
        StudyMember member = studyMemberRepository.saveAndFlush(
                StudyMember.create(study, recipient, "스터디원", null, StudyMemberRole.MEMBER));
        Assignment assignment = Assignment.create(study, "과제", "내용", "제출 방법",
                SubmissionTarget.MEMBERS_ONLY, now.plusDays(1), creationTime);
        assignment.initializeSubmissions(List.of(member));
        assignment.addReminders(List.of(now.minusMinutes(1)), creationTime);
        assignmentRepository.saveAndFlush(assignment);
        return new ResourceFixture(leaderUser.getId(), study.getId(), assignment.getId());
    }

    private ResourceFixture createNoticeWithDueReminder() {
        LocalDateTime now = LocalDateTime.now(ZONE_ID);
        LocalDateTime creationTime = now.minusDays(1);
        User leaderUser = userRepository.saveAndFlush(User.create("리더", null));
        User recipient = userRepository.saveAndFlush(User.create("스터디원", null));
        Study study = studyRepository.saveAndFlush(Study.create("스터디", "설명"));
        studyMemberRepository.saveAndFlush(
                StudyMember.create(study, leaderUser, "리더", null, StudyMemberRole.LEADER));
        StudyMember member = studyMemberRepository.saveAndFlush(
                StudyMember.create(study, recipient, "스터디원", null, StudyMemberRole.MEMBER));
        Notice notice = Notice.create(study, "공지", "내용");
        notice.addRecipients(List.of(member));
        notice.addReminders(List.of(now.minusMinutes(1)), creationTime);
        noticeRepository.saveAndFlush(notice);
        return new ResourceFixture(leaderUser.getId(), study.getId(), notice.getId());
    }

    private void awaitDatabaseLockWait(int backendPid) throws InterruptedException {
        long deadline = System.nanoTime() + LOCK_WAIT_TIMEOUT.toNanos();
        boolean waitingOnLock = false;
        while (System.nanoTime() < deadline) {
            waitingOnLock = Boolean.TRUE.equals(jdbcTemplate.queryForObject("""
                    SELECT EXISTS (
                        SELECT 1
                        FROM pg_stat_activity
                        WHERE pid = ?
                          AND wait_event_type = 'Lock'
                    )
                    """, Boolean.class, backendPid));
            if (waitingOnLock) {
                return;
            }
            Thread.sleep(20);
        }
        assertThat(waitingOnLock).as("자원 삭제 트랜잭션이 리마인더 행 잠금을 기다려야 한다").isTrue();
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)) {
                throw new IllegalStateException("리마인더 생성 트랜잭션 해제 대기 시간 초과");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("리마인더 생성 트랜잭션 대기 중 인터럽트 발생", exception);
        }
    }

    private record ResourceFixture(Long leaderUserId, Long studyId, Long resourceId) {
    }
}
