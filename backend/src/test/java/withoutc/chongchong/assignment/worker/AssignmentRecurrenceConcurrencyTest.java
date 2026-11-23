package withoutc.chongchong.assignment.worker;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import withoutc.chongchong.assignment.controller.dto.AssignmentRecurrenceCreateRequest;
import withoutc.chongchong.assignment.entity.SubmissionTarget;
import withoutc.chongchong.assignment.entity.SubmissionVisibility;
import withoutc.chongchong.assignment.repository.projection.AssignmentRecurrenceCandidate;
import withoutc.chongchong.assignment.service.AssignmentRecurrenceGenerationService;
import withoutc.chongchong.assignment.service.AssignmentRecurrenceService;
import withoutc.chongchong.study.entity.Study;
import withoutc.chongchong.study.entity.StudyMember;
import withoutc.chongchong.study.entity.StudyMemberRole;
import withoutc.chongchong.study.repository.StudyMemberRepository;
import withoutc.chongchong.study.repository.StudyRepository;
import withoutc.chongchong.study.service.StudyService;
import withoutc.chongchong.support.PostgresContainerTest;
import withoutc.chongchong.support.TestDatabaseCleaner;
import withoutc.chongchong.user.entity.User;
import withoutc.chongchong.user.repository.UserRepository;

class AssignmentRecurrenceConcurrencyTest extends PostgresContainerTest {

    private static final Clock CLOCK = Clock.system(ZoneId.of("Asia/Seoul"));

    @Autowired
    private AssignmentRecurrenceGenerationService generationService;

    @Autowired
    private AssignmentRecurrenceService recurrenceService;

    @Autowired
    private StudyService studyService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StudyRepository studyRepository;

    @Autowired
    private StudyMemberRepository studyMemberRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private TestDatabaseCleaner databaseCleaner;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final ExecutorService executor = Executors.newFixedThreadPool(2);
    private User leaderUser;
    private AssignmentRecurrenceCandidate candidate;

    @BeforeEach
    void setUp() {
        databaseCleaner.clean();
        leaderUser = userRepository.save(User.create("리더", null));
        User memberUser = userRepository.save(User.create("스터디원", null));
        Study study = studyRepository.save(Study.create("자바 스터디", "설명", false));
        studyMemberRepository.save(StudyMember.create(study, leaderUser, "리더", null, StudyMemberRole.LEADER));
        studyMemberRepository.save(StudyMember.create(study, memberUser, "스터디원", null, StudyMemberRole.MEMBER));

        LocalDateTime closeAt = LocalDateTime.now(CLOCK).plusDays(2).truncatedTo(ChronoUnit.MINUTES);
        Long recurrenceId = recurrenceService.create(leaderUser.getId(), study.getId(),
                new AssignmentRecurrenceCreateRequest("알고리즘 풀이", "내용", "링크 제출",
                        SubmissionTarget.MEMBERS_ONLY, SubmissionVisibility.LEADER_ONLY, closeAt, 1, null, null))
                .recurrenceId();
        jdbcTemplate.update("UPDATE assignment_recurrences SET next_publish_at = ? WHERE id = ?",
                LocalDateTime.now(CLOCK).minusMinutes(1), recurrenceId);
        candidate = new AssignmentRecurrenceCandidate(recurrenceId, study.getId());
    }

    @AfterEach
    void tearDown() {
        executor.shutdownNow();
        databaseCleaner.clean();
    }

    @Test
    @DisplayName("두 워커가 같은 규칙을 동시에 처리해도 회차는 하나만 생긴다")
    void generateOccurrenceOnlyOnce() throws Exception {
        CountDownLatch firstGenerated = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);
        CompletableFuture<Void> first = CompletableFuture.runAsync(() -> inTransaction(() -> {
            generationService.generate(candidate);
            firstGenerated.countDown();
            await(releaseFirst);
        }), executor);
        await(firstGenerated);

        CompletableFuture.runAsync(() -> generationService.generate(candidate), executor).get(10, TimeUnit.SECONDS);
        releaseFirst.countDown();
        first.get(10, TimeUnit.SECONDS);
        generationService.generate(candidate);

        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM assignments", Integer.class)).isEqualTo(2);
    }

    @Test
    @DisplayName("워커가 회차를 만드는 중에 스터디를 삭제해도 교착 없이 순서대로 끝난다")
    void deleteStudyWhileGenerating() throws Exception {
        CountDownLatch firstGenerated = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);
        CompletableFuture<Void> worker = CompletableFuture.runAsync(() -> inTransaction(() -> {
            generationService.generate(candidate);
            firstGenerated.countDown();
            await(releaseFirst);
        }), executor);
        await(firstGenerated);

        CompletableFuture<Void> deletion = CompletableFuture.runAsync(
                () -> studyService.deleteStudy(leaderUser.getId(), candidate.studyId()), executor);
        Thread.sleep(300);
        assertThat(deletion).isNotDone();
        releaseFirst.countDown();

        worker.get(10, TimeUnit.SECONDS);
        deletion.get(10, TimeUnit.SECONDS);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM assignment_recurrences", Integer.class))
                .isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM assignments", Integer.class)).isZero();
    }

    private void inTransaction(Runnable runnable) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> runnable.run());
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("대기 시간 초과");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(exception);
        }
    }
}
