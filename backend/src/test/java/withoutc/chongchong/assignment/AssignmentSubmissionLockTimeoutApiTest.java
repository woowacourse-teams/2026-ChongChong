package withoutc.chongchong.assignment;

import static org.assertj.core.api.Assertions.assertThat;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import withoutc.chongchong.assignment.entity.Assignment;
import withoutc.chongchong.assignment.entity.AssignmentSubmission;
import withoutc.chongchong.assignment.entity.SubmissionTarget;
import withoutc.chongchong.assignment.entity.SubmissionVisibility;
import withoutc.chongchong.assignment.repository.AssignmentRepository;
import withoutc.chongchong.assignment.repository.AssignmentSubmissionRepository;
import withoutc.chongchong.auth.support.TestAuthRequest;
import withoutc.chongchong.study.entity.Study;
import withoutc.chongchong.study.entity.StudyMember;
import withoutc.chongchong.study.entity.StudyMemberRole;
import withoutc.chongchong.study.repository.StudyMemberRepository;
import withoutc.chongchong.study.repository.StudyRepository;
import withoutc.chongchong.support.PostgresApiTest;
import withoutc.chongchong.support.TestDatabaseCleaner;
import withoutc.chongchong.user.entity.User;
import withoutc.chongchong.user.repository.UserRepository;

class AssignmentSubmissionLockTimeoutApiTest extends PostgresApiTest {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private StudyRepository studyRepository;
    @Autowired
    private StudyMemberRepository memberRepository;
    @Autowired
    private AssignmentRepository assignmentRepository;
    @Autowired
    private AssignmentSubmissionRepository submissionRepository;
    @Autowired
    private TestAuthRequest auth;
    @Autowired
    private TestDatabaseCleaner databaseCleaner;
    @Autowired
    private PlatformTransactionManager transactionManager;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private Clock clock;

    @LocalServerPort
    private int port;

    private Study study;
    private User submitter;
    private Assignment assignment;
    private AssignmentSubmission submission;

    @BeforeEach
    void setUp() {
        databaseCleaner.clean();
        User leader = userRepository.saveAndFlush(User.create("리더", null));
        submitter = userRepository.saveAndFlush(User.create("제출자", null));
        study = studyRepository.saveAndFlush(Study.create("스터디", "설명", false));
        memberRepository.saveAndFlush(StudyMember.create(study, leader, "리더", null, StudyMemberRole.LEADER));
        StudyMember submitterMember = memberRepository.saveAndFlush(
                StudyMember.create(study, submitter, "제출자", null, StudyMemberRole.MEMBER));
        LocalDateTime now = LocalDateTime.now(clock);
        assignment = assignmentRepository.saveAndFlush(Assignment.create(study, "과제", "내용", "안내",
                SubmissionTarget.MEMBERS_ONLY, SubmissionVisibility.LEADER_ONLY, now.plusDays(7), now));
        submission = submissionRepository.saveAndFlush(AssignmentSubmission.create(submitterMember, assignment));
    }

    @Test
    @DisplayName("과제 행 잠금을 3초 넘게 기다리면 503을 반환하고 제출·알림 없이 롤백되며 잠금 해제 후 재시도할 수 있다")
    void timesOutWaitingForAssignmentLockAndAllowsRetry() throws Exception {
        CountDownLatch acquired = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<?> holder = executor.submit(() -> new TransactionTemplate(transactionManager)
                    .executeWithoutResult(status -> {
                        jdbc.queryForList("SELECT id FROM assignments WHERE id = ? FOR UPDATE", assignment.getId());
                        acquired.countDown();
                        awaitRelease(release);
                    }));
            assertThat(acquired.await(5, TimeUnit.SECONDS)).isTrue();

            long started = System.nanoTime();
            Response response = submit();
            long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);

            response.then().statusCode(503);
            assertThat(response.jsonPath().getString("code")).isEqualTo("ASSIGNMENT_SUBMISSION_BUSY");
            assertThat(elapsedMillis).isBetween(2800L, 8000L);
            assertThat(submittedAt()).isNull();
            assertThat(submissionNotificationCount()).isZero();

            release.countDown();
            holder.get(5, TimeUnit.SECONDS);
            submit().then().statusCode(201);
            assertThat(submittedAt()).isNotNull();
            assertThat(submissionNotificationCount()).isOne();
        } finally {
            release.countDown();
            executor.shutdownNow();
            assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
        }
    }

    @Test
    @DisplayName("제출용 과제 잠금 제한은 그 조회에만 적용되고 다음 트랜잭션으로 이어지지 않는다")
    void appliesLockTimeoutOnlyToSubmitLookup() {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        transaction.executeWithoutResult(status -> {
            assignmentRepository.getByIdAndStudyIdForSubmitOrThrow(assignment.getId(), study.getId());
            assignmentRepository.getByIdAndStudyIdForUpdateOrThrow(assignment.getId(), study.getId());
            assertThat(jdbc.queryForObject("SHOW lock_timeout", String.class)).isEqualTo("0");
        });
        transaction.executeWithoutResult(status ->
                assertThat(jdbc.queryForObject("SHOW lock_timeout", String.class)).isEqualTo("0"));
    }

    private Response submit() {
        return auth.givenAuthenticatedUser(submitter.getId()).port(port)
                .contentType(ContentType.JSON).body(Map.of("content", "제출 내용"))
                .when().post("/studies/{studyId}/assignments/{assignmentId}/submissions",
                        study.getId(), assignment.getId());
    }

    private Object submittedAt() {
        return jdbc.queryForObject("SELECT submitted_at FROM assignment_submissions WHERE id = ?", Object.class,
                submission.getId());
    }

    private int submissionNotificationCount() {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM notifications WHERE resource_type = 'ASSIGNMENT_SUBMISSION' AND resource_id = ?",
                Integer.class, submission.getId());
    }

    private void awaitRelease(CountDownLatch release) {
        try {
            if (!release.await(15, TimeUnit.SECONDS)) {
                throw new IllegalStateException("과제 잠금 해제 대기 시간이 초과되었습니다.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
}
