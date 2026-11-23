package withoutc.chongchong.assignment.service;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManagerFactory;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import withoutc.chongchong.assignment.controller.dto.AssignmentListResponse;
import withoutc.chongchong.assignment.entity.Assignment;
import withoutc.chongchong.assignment.entity.AssignmentSort;
import withoutc.chongchong.assignment.entity.SubmissionTarget;
import withoutc.chongchong.assignment.entity.SubmissionVisibility;
import withoutc.chongchong.assignment.repository.AssignmentRepository;
import withoutc.chongchong.study.entity.Study;
import withoutc.chongchong.study.entity.StudyMember;
import withoutc.chongchong.study.entity.StudyMemberRole;
import withoutc.chongchong.study.repository.StudyMemberRepository;
import withoutc.chongchong.study.repository.StudyRepository;
import withoutc.chongchong.support.PostgresContainerTest;
import withoutc.chongchong.support.TestDatabaseCleaner;
import withoutc.chongchong.user.entity.User;
import withoutc.chongchong.user.repository.UserRepository;

/**
 * 과제 목록 조회가 실행하는 SQL 수와 응답 시간을 측정한다.
 * 페이지 크기에 따라 쿼리 수가 늘어나면 과제별 지연 로딩(N+1)이 발생하는 것이다.
 */
class AssignmentListQueryCountPostgresTest extends PostgresContainerTest {

    private static final int ASSIGNMENT_COUNT = 40;
    private static final int PAGE_SIZE = 20;

    @Autowired
    private AssignmentService assignmentService;
    @Autowired
    private AssignmentRepository assignmentRepository;
    @Autowired
    private StudyRepository studyRepository;
    @Autowired
    private StudyMemberRepository memberRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private EntityManagerFactory entityManagerFactory;
    @Autowired
    private PlatformTransactionManager transactionManager;
    @Autowired
    private TestDatabaseCleaner databaseCleaner;

    private Statistics statistics;
    private Long studyId;
    private Long leaderUserId;
    private Long memberUserId;

    @BeforeEach
    void setUp() {
        databaseCleaner.clean();
        statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.setStatisticsEnabled(true);
    }

    @AfterEach
    void tearDown() {
        statistics.setStatisticsEnabled(false);
        databaseCleaner.clean();
    }

    @Test
    @DisplayName("리더의 과제 목록 조회 쿼리 수는 페이지 크기와 무관하다")
    void leaderQueryCountDoesNotGrowWithPageSize() {
        saveFixture(10, 1);

        for (AssignmentSort sort : AssignmentSort.values()) {
            for (int size : List.of(5, 10, 20)) {
                System.out.printf("[N+1 측정] sort=%-11s size=%2d  리더=%3d  스터디원=%3d%n", sort, size,
                        countQueries(leaderUserId, size, sort), countQueries(memberUserId, size, sort));
            }
        }

        assertThat(countQueries(leaderUserId, 20, AssignmentSort.LATEST))
                .isEqualTo(countQueries(leaderUserId, 5, AssignmentSort.LATEST));
    }

    @Test
    @DisplayName("스터디원 수별 리더 과제 목록 응답 시간을 측정한다")
    void measureLeaderLatency() {
        for (int memberCount : List.of(10, 50, 100)) {
            databaseCleaner.clean();
            saveFixture(memberCount, 3);
            long queries = countQueries(leaderUserId, PAGE_SIZE, AssignmentSort.DEADLINE);
            long loadedEntities = statistics.getEntityLoadCount();

            for (int i = 0; i < 300; i++) {
                assignmentService.getList(leaderUserId, studyId, null, PAGE_SIZE, AssignmentSort.DEADLINE, null);
            }
            long[] elapsed = new long[500];
            for (int i = 0; i < elapsed.length; i++) {
                long start = System.nanoTime();
                assignmentService.getList(leaderUserId, studyId, null, PAGE_SIZE, AssignmentSort.DEADLINE, null);
                elapsed[i] = System.nanoTime() - start;
            }
            Arrays.sort(elapsed);
            System.out.printf("[지연 측정] 스터디원=%3d  쿼리=%2d  로딩 엔티티=%5d  p50=%6.2fms  p95=%6.2fms  평균=%6.2fms%n",
                    memberCount, queries, loadedEntities, elapsed[250] / 1e6, elapsed[474] / 1e6,
                    Arrays.stream(elapsed).average().orElseThrow() / 1e6);
        }
    }

    private long countQueries(Long userId, int size, AssignmentSort sort) {
        statistics.clear();
        AssignmentListResponse response = assignmentService.getList(userId, studyId, null, size, sort, null);
        assertThat(response.assignments()).hasSize(size);
        return statistics.getPrepareStatementCount();
    }

    private void saveFixture(int memberCount, int remindersPerAssignment) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            LocalDateTime now = LocalDateTime.now();
            Study study = studyRepository.save(Study.create("N+1 측정", "설명", false));
            studyId = study.getId();

            User leaderUser = userRepository.save(User.create("리더", null));
            leaderUserId = leaderUser.getId();
            List<StudyMember> members = new ArrayList<>();
            members.add(memberRepository.save(
                    StudyMember.create(study, leaderUser, "리더", null, StudyMemberRole.LEADER)));
            for (int i = 0; i < memberCount; i++) {
                User user = userRepository.save(User.create("멤버" + i, null));
                if (i == 0) {
                    memberUserId = user.getId();
                }
                members.add(memberRepository.save(
                        StudyMember.create(study, user, "멤버" + i, null, StudyMemberRole.MEMBER)));
            }

            for (int i = 0; i < ASSIGNMENT_COUNT; i++) {
                Assignment assignment = Assignment.create(study, "과제" + i, "내용", null,
                        SubmissionTarget.MEMBERS_ONLY, SubmissionVisibility.LEADER_ONLY,
                        now.plusDays(i + 10), now, members.subList(1, members.size()));
                List<LocalDateTime> remindAts = new ArrayList<>();
                for (int r = 1; r <= remindersPerAssignment; r++) {
                    remindAts.add(now.plusDays(r));
                }
                assignment.addReminders(remindAts, now);
                // 절반은 제출 완료로 만들어 집계 결과가 0이 아니게 한다.
                assignment.getSubmissions().stream().limit(memberCount / 2)
                        .forEach(submission -> submission.submit("제출", null, now));
                assignmentRepository.save(assignment);
            }
        });
    }
}
