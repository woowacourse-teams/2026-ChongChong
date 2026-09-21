package withoutc.chongchong.assignment;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import withoutc.chongchong.assignment.controller.dto.AssignmentSubmitRequest;
import withoutc.chongchong.assignment.controller.dto.AssignmentSubmitResponse;
import withoutc.chongchong.assignment.entity.Assignment;
import withoutc.chongchong.assignment.entity.AssignmentSubmission;
import withoutc.chongchong.assignment.entity.SubmissionTarget;
import withoutc.chongchong.assignment.repository.AssignmentRepository;
import withoutc.chongchong.assignment.repository.AssignmentSubmissionRepository;
import withoutc.chongchong.assignment.service.AssignmentSubmissionService;
import withoutc.chongchong.study.entity.Study;
import withoutc.chongchong.study.entity.StudyMember;
import withoutc.chongchong.study.entity.StudyMemberRole;
import withoutc.chongchong.study.repository.StudyMemberRepository;
import withoutc.chongchong.study.repository.StudyRepository;
import withoutc.chongchong.support.PostgresContainerTest;
import withoutc.chongchong.support.TestDatabaseCleaner;
import withoutc.chongchong.user.entity.User;
import withoutc.chongchong.user.repository.UserRepository;

class AssignmentSubmissionConcurrencyTest extends PostgresContainerTest {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    @Autowired
    private AssignmentSubmissionService assignmentSubmissionService;

    @Autowired
    private AssignmentRepository assignmentRepository;

    @Autowired
    private AssignmentSubmissionRepository assignmentSubmissionRepository;

    @Autowired
    private StudyMemberRepository studyMemberRepository;

    @Autowired
    private StudyRepository studyRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private TestDatabaseCleaner databaseCleaner;

    @BeforeEach
    @AfterEach
    void cleanDatabase() {
        databaseCleaner.clean();
    }

    @Test
    @DisplayName("같은 제출을 동시에 두 번 해도 최초 제출 알림은 한 번만 생성한다")
    void createsOnlyOneNotificationWhenSubmittingConcurrently() throws Exception {
        SubmissionFixture fixture = createFixture();
        CyclicBarrier barrier = new CyclicBarrier(2);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<AssignmentSubmitResponse> first = executor.submit(
                    () -> submitAfterSignal(barrier, fixture, "첫 번째 제출")
            );
            Future<AssignmentSubmitResponse> second = executor.submit(
                    () -> submitAfterSignal(barrier, fixture, "두 번째 제출")
            );

            AssignmentSubmitResponse firstResponse = first.get(10, SECONDS);
            AssignmentSubmitResponse secondResponse = second.get(10, SECONDS);

            assertThat(List.of(firstResponse.submissionId(), secondResponse.submissionId()))
                    .containsOnly(fixture.submissionId());
        } finally {
            executor.shutdownNow();
            assertThat(executor.awaitTermination(10, SECONDS)).isTrue();
        }

        assertThat(assignmentSubmissionRepository.findById(fixture.submissionId()))
                .get()
                .extracting(submission -> submission.getSubmittedAt() != null)
                .isEqualTo(true);
        assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM notifications
                WHERE recipient_id = ?
                  AND resource_id = ?
                  AND resource_type = 'ASSIGNMENT_SUBMISSION'
                  AND type = 'SUBMITTED'
                """, Integer.class, fixture.leaderId(), fixture.submissionId()))
                .isOne();
    }

    @Test
    @DisplayName("동시 제출 조회는 기존 제출 행 잠금이 해제될 때까지 대기한다")
    void waitsForSubmissionRowLockToBeReleased() throws Exception {
        SubmissionFixture fixture = createFixture();
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        CountDownLatch lockAcquired = new CountDownLatch(1);
        CountDownLatch releaseLock = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<Void> holder = executor.submit(() -> {
                transactionTemplate.executeWithoutResult(status -> {
                    assignmentSubmissionRepository.findByAssignmentIdAndMemberIdForUpdate(
                            fixture.assignmentId(), fixture.submitterMemberId()
                    ).orElseThrow();
                    lockAcquired.countDown();
                    awaitLatch(releaseLock, "제출 행 잠금 해제 신호를 기다리는 시간이 초과되었습니다.");
                });
                return null;
            });

            assertThat(lockAcquired.await(5, SECONDS)).isTrue();

            Future<Void> waiter = executor.submit(() -> {
                transactionTemplate.executeWithoutResult(status -> {
                    jdbcTemplate.execute("SET LOCAL lock_timeout = '1000ms'");
                    assignmentSubmissionRepository.findByAssignmentIdAndMemberIdForUpdate(
                            fixture.assignmentId(), fixture.submitterMemberId()
                    ).orElseThrow();
                });
                return null;
            });

            assertThatThrownBy(() -> waiter.get(5, SECONDS))
                    .isInstanceOf(ExecutionException.class)
                    .hasCauseInstanceOf(PessimisticLockingFailureException.class);

            releaseLock.countDown();
            holder.get(10, SECONDS);
        } finally {
            releaseLock.countDown();
            executor.shutdownNow();
            assertThat(executor.awaitTermination(10, SECONDS)).isTrue();
        }
    }

    private AssignmentSubmitResponse submitAfterSignal(
            CyclicBarrier barrier,
            SubmissionFixture fixture,
            String content
    ) throws Exception {
        barrier.await(10, SECONDS);
        return assignmentSubmissionService.submit(
                fixture.submitterUserId(),
                fixture.studyId(),
                fixture.assignmentId(),
                new AssignmentSubmitRequest(content, null)
        );
    }

    private void awaitLatch(CountDownLatch latch, String timeoutMessage) {
        try {
            if (!latch.await(10, SECONDS)) {
                throw new IllegalStateException(timeoutMessage);
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("동시성 테스트가 중단되었습니다.", exception);
        }
    }

    private SubmissionFixture createFixture() {
        Study study = studyRepository.saveAndFlush(Study.create("동시성 스터디", "설명"));

        User leaderUser = userRepository.saveAndFlush(User.create("리더", null));
        StudyMember leader = studyMemberRepository.saveAndFlush(
                StudyMember.create(study, leaderUser, leaderUser.getName(), null, StudyMemberRole.LEADER)
        );

        User submitterUser = userRepository.saveAndFlush(User.create("제출자", null));
        StudyMember submitter = studyMemberRepository.saveAndFlush(
                StudyMember.create(study, submitterUser, submitterUser.getName(), null, StudyMemberRole.MEMBER)
        );

        LocalDateTime now = LocalDateTime.now(SEOUL);
        Assignment assignment = assignmentRepository.saveAndFlush(Assignment.create(
                study,
                "동시성 과제",
                "과제 내용",
                "링크 제출",
                SubmissionTarget.MEMBERS_ONLY,
                now.plusDays(1),
                now
        ));
        AssignmentSubmission submission = assignmentSubmissionRepository.saveAndFlush(
                AssignmentSubmission.create(submitter, assignment)
        );

        return new SubmissionFixture(
                study.getId(),
                assignment.getId(),
                submission.getId(),
                submitterUser.getId(),
                leader.getId(),
                submitter.getId()
        );
    }

    private record SubmissionFixture(
            Long studyId,
            Long assignmentId,
            Long submissionId,
            Long submitterUserId,
            Long leaderId,
            Long submitterMemberId
    ) {
    }
}
