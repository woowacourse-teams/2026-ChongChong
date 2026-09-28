package withoutc.chongchong.notification.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import withoutc.chongchong.assignment.entity.Assignment;
import withoutc.chongchong.assignment.entity.AssignmentReminder;
import withoutc.chongchong.assignment.entity.SubmissionTarget;
import withoutc.chongchong.assignment.repository.AssignmentReminderRepository;
import withoutc.chongchong.assignment.repository.AssignmentRepository;
import withoutc.chongchong.notice.entity.Notice;
import withoutc.chongchong.notice.entity.NoticeReminder;
import withoutc.chongchong.notice.entity.NoticeReminderStatus;
import withoutc.chongchong.notice.repository.NoticeReminderRepository;
import withoutc.chongchong.notice.repository.NoticeRepository;
import withoutc.chongchong.study.entity.Study;
import withoutc.chongchong.study.repository.StudyRepository;
import withoutc.chongchong.support.PostgresContainerTest;

class ReminderClaimPostgresRepositoryTest extends PostgresContainerTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 20, 10, 0);
    private static final LocalDateTime CREATION_TIME = NOW.minusDays(1);
    private static final Duration TEST_TIMEOUT = Duration.ofSeconds(30);
    private static final Duration SLOW_SECOND_WORKER_DELAY = Duration.ofSeconds(6);

    @Autowired
    private NoticeReminderRepository noticeReminderRepository;

    @Autowired
    private NoticeRepository noticeRepository;

    @Autowired
    private AssignmentReminderRepository assignmentReminderRepository;

    @Autowired
    private AssignmentRepository assignmentRepository;

    @Autowired
    private StudyRepository studyRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @BeforeEach
    void clearReminderFixtures() {
        noticeReminderRepository.deleteAllInBatch();
        assignmentReminderRepository.deleteAllInBatch();
        noticeRepository.deleteAllInBatch();
        assignmentRepository.deleteAllInBatch();
        studyRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("공지 리마인더는 도래한 PENDING 상태만 batch size만큼 조회한다")
    void claimDueNoticeRemindersWithinBatchSize() {
        NoticeReminder dueReminder = saveNoticeReminder(NOW.minusMinutes(1));
        saveNoticeReminder(NOW.plusMinutes(1));

        List<Long> claimedIds = newTransactionTemplate().execute(status ->
                noticeReminderRepository.findClaimableForUpdate(NOW, 1).stream()
                        .map(NoticeReminder::getId)
                        .toList()
        );

        assertThat(claimedIds).containsExactly(dueReminder.getId());
    }

    @Test
    @DisplayName("과제 리마인더는 도래한 PENDING 상태를 조회한다")
    void claimDueAssignmentReminders() {
        AssignmentReminder dueReminder = saveAssignmentReminder(NOW.minusMinutes(1));

        List<Long> claimedIds = newTransactionTemplate().execute(status ->
                assignmentReminderRepository.findClaimableForUpdate(NOW, 100).stream()
                        .map(AssignmentReminder::getId)
                        .toList()
        );

        assertThat(claimedIds).containsExactly(dueReminder.getId());
    }

    @Test
    @DisplayName("동시에 조회한 두 워커 중 하나만 같은 공지 리마인더를 claim한다")
    void skipLockedNoticeReminderClaim() throws Exception {
        NoticeReminder reminder = saveNoticeReminder(NOW.minusMinutes(1));
        CountDownLatch releaseFirst = new CountDownLatch(1);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        CompletableFuture<Void> firstWorkerReady = new CompletableFuture<>();

        try {
            Future<Long> firstWorker = executor.submit(() -> {
                try {
                    return newTransactionTemplate().execute(status -> {
                        List<NoticeReminder> reminders = noticeReminderRepository.findClaimableForUpdate(NOW, 1);
                        NoticeReminder claimedReminder = reminders.getFirst();
                        firstWorkerReady.complete(null);
                        await(releaseFirst);
                        claimedReminder.markAsProcessing();
                        noticeReminderRepository.flush();
                        return claimedReminder.getId();
                    });
                } catch (RuntimeException | Error exception) {
                    firstWorkerReady.completeExceptionally(exception);
                    throw exception;
                }
            });

            firstWorkerReady.get(TEST_TIMEOUT.toSeconds(), TimeUnit.SECONDS);

            List<NoticeReminder> secondWorker = newTransactionTemplate().execute(status ->
                    noticeReminderRepository.findClaimableForUpdate(NOW, 1)
            );

            assertThat(secondWorker).isEmpty();
            waitForSlowSecondWorker();
            releaseFirst.countDown();
            assertThat(firstWorker.get(TEST_TIMEOUT.toSeconds(), TimeUnit.SECONDS)).isEqualTo(reminder.getId());
            assertThat(noticeReminderRepository.findById(reminder.getId()).orElseThrow().getStatus())
                    .isEqualTo(NoticeReminderStatus.PROCESSING);
        } finally {
            releaseFirst.countDown();
            executor.shutdownNow();
        }
    }

    private NoticeReminder saveNoticeReminder(LocalDateTime remindAt) {
        Study study = studyRepository.saveAndFlush(Study.create("공지 테스트", "설명"));
        Notice notice = Notice.create(study, "공지 제목", "공지 내용");
        notice.addReminders(List.of(remindAt), CREATION_TIME);
        noticeRepository.saveAndFlush(notice);
        return notice.getReminders().getFirst();
    }

    private AssignmentReminder saveAssignmentReminder(LocalDateTime remindAt) {
        Study study = studyRepository.saveAndFlush(Study.create("과제 테스트", "설명"));
        Assignment assignment = Assignment.create(
                study,
                "과제 제목",
                "과제 내용",
                "제출 방법",
                SubmissionTarget.MEMBERS_ONLY,
                NOW.plusDays(1),
                CREATION_TIME
        );
        assignment.addReminders(List.of(remindAt), CREATION_TIME);
        assignmentRepository.saveAndFlush(assignment);
        return assignment.getReminders().getFirst();
    }

    private TransactionTemplate newTransactionTemplate() {
        return new TransactionTemplate(transactionManager);
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(TEST_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)) {
                throw new IllegalStateException("첫 번째 워커 해제를 기다리는 중 시간 초과");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("첫 번째 워커 대기 중 인터럽트 발생", exception);
        }
    }

    private static void waitForSlowSecondWorker() {
        try {
            Thread.sleep(SLOW_SECOND_WORKER_DELAY.toMillis());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("두 번째 워커 검증 중 인터럽트 발생", exception);
        }
    }
}
