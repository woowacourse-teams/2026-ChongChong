package withoutc.chongchong.assignment.worker;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import withoutc.chongchong.assignment.controller.dto.AssignmentRecurrenceCreateRequest;
import withoutc.chongchong.assignment.entity.SubmissionTarget;
import withoutc.chongchong.assignment.repository.projection.AssignmentRecurrenceCandidate;
import withoutc.chongchong.assignment.service.AssignmentRecurrenceGenerationService;
import withoutc.chongchong.assignment.entity.SubmissionVisibility;
import withoutc.chongchong.assignment.service.AssignmentRecurrenceService;
import withoutc.chongchong.study.entity.Study;
import withoutc.chongchong.study.entity.StudyMember;
import withoutc.chongchong.study.entity.StudyMemberRole;
import withoutc.chongchong.study.repository.StudyMemberRepository;
import withoutc.chongchong.study.repository.StudyRepository;
import withoutc.chongchong.support.TestDatabaseCleaner;
import withoutc.chongchong.user.entity.User;
import withoutc.chongchong.user.repository.UserRepository;

@ActiveProfiles("test")
@SpringBootTest
class AssignmentRecurrenceWorkerTest {

    private static final Clock CLOCK = Clock.system(ZoneId.of("Asia/Seoul"));

    @Autowired
    private AssignmentRecurrenceWorker worker;

    @Autowired
    private AssignmentRecurrenceService recurrenceService;

    @Autowired
    private AssignmentRecurrenceGenerationService generationService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StudyRepository studyRepository;

    @Autowired
    private StudyMemberRepository studyMemberRepository;

    @Autowired
    private TestDatabaseCleaner databaseCleaner;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private User leaderUser;
    private Study study;
    private StudyMember member;
    private LocalDateTime firstCloseAt;

    @BeforeEach
    void setUp() {
        databaseCleaner.clean();
        leaderUser = userRepository.save(User.create("리더", null));
        User memberUser = userRepository.save(User.create("스터디원", null));
        study = studyRepository.save(Study.create("자바 스터디", "설명", false));
        studyMemberRepository.save(StudyMember.create(study, leaderUser, "리더", null, StudyMemberRole.LEADER));
        member = studyMemberRepository.save(
                StudyMember.create(study, memberUser, "스터디원", null, StudyMemberRole.MEMBER));
        firstCloseAt = LocalDateTime.now(CLOCK).plusDays(2).withHour(23).withMinute(59)
                .truncatedTo(ChronoUnit.MINUTES);
    }

    @AfterEach
    void cleanDatabase() {
        databaseCleaner.clean();
    }

    @Test
    @DisplayName("직전 회차 마감이 지난 규칙은 다음 회차를 만들고 리더에게도 생성 알림을 보낸다")
    void generateNextOccurrence() {
        Long recurrenceId = createRecurrence(SubmissionTarget.MEMBERS_ONLY, null,
                List.of(firstCloseAt.minusHours(1)));
        makeDue(recurrenceId);

        worker.process();

        LocalDateTime nextCloseAt = firstCloseAt.plusWeeks(1);
        Map<String, Object> occurrence = occurrenceOn(recurrenceId, nextCloseAt.toLocalDate());
        assertThat(((Timestamp) occurrence.get("close_at")).toLocalDateTime()).isEqualTo(nextCloseAt);
        Long occurrenceId = ((Number) occurrence.get("id")).longValue();
        assertThat(jdbcTemplate.queryForList(
                "SELECT remind_at FROM assignment_reminders WHERE assignment_id = ?", Timestamp.class, occurrenceId))
                .extracting(Timestamp::toLocalDateTime)
                .containsExactly(nextCloseAt.minusHours(1));
        assertThat(jdbcTemplate.queryForList(
                "SELECT member_id FROM assignment_submissions WHERE assignment_id = ?", Long.class, occurrenceId))
                .containsExactly(member.getId());
        assertThat(jdbcTemplate.queryForList("""
                SELECT recipient_id FROM notifications WHERE resource_type = 'ASSIGNMENT' AND resource_id = ?
                """, Long.class, occurrenceId))
                .contains(leaderUser.getId());
        assertSchedule(recurrenceId, nextCloseAt, nextCloseAt.toLocalDate().plusWeeks(1));
    }

    @Test
    @DisplayName("직전 회차가 아직 마감되지 않은 규칙은 회차를 만들지 않는다")
    void skipNotDueRecurrence() {
        createRecurrence(SubmissionTarget.MEMBERS_ONLY, null, null);

        worker.process();

        assertThat(countAssignments()).isOne();
    }

    @Test
    @DisplayName("서버가 멈춰 지난 회차는 건너뛰고 마감이 미래인 다음 회차 하나만 만든다")
    void skipPastOccurrences() {
        Long recurrenceId = createRecurrence(SubmissionTarget.MEMBERS_ONLY, null, null);
        // 첫 회차와 날짜가 겹치지 않도록 하루 뒤 요일로 3주 밀린 상태를 만든다.
        LocalDateTime expectedCloseAt = firstCloseAt.plusDays(1);
        jdbcTemplate.update("""
                UPDATE assignment_recurrences SET next_occurrence_date = ?, next_publish_at = ? WHERE id = ?
                """, expectedCloseAt.toLocalDate().minusWeeks(3), LocalDateTime.now(CLOCK).minusWeeks(4),
                recurrenceId);

        worker.process();

        assertThat(countAssignments()).isEqualTo(2);
        Map<String, Object> occurrence = occurrenceOn(recurrenceId, expectedCloseAt.toLocalDate());
        assertThat(((Timestamp) occurrence.get("close_at")).toLocalDateTime()).isEqualTo(expectedCloseAt);
    }

    @Test
    @DisplayName("직접 선택한 멤버가 모두 나가 대상이 없으면 그 회차는 만들지 않고 다음 차례로 넘어간다")
    void skipOccurrenceWithoutSubmitters() {
        Long recurrenceId = createRecurrence(null, List.of(member.getId()), null);
        makeDue(recurrenceId);
        jdbcTemplate.update("DELETE FROM assignment_recurrence_submitters WHERE recurrence_id = ?", recurrenceId);

        worker.process();

        assertThat(countAssignments()).isOne();
        LocalDateTime skippedCloseAt = firstCloseAt.plusWeeks(1);
        assertSchedule(recurrenceId, skippedCloseAt, skippedCloseAt.toLocalDate().plusWeeks(1));
    }

    @Test
    @DisplayName("같은 회차가 이미 있으면 새로 만들지 않고 다음 차례로 넘어간다")
    void skipExistingOccurrence() {
        Long recurrenceId = createRecurrence(SubmissionTarget.MEMBERS_ONLY, null, null);
        makeDue(recurrenceId);
        LocalDate nextOccurrenceDate = firstCloseAt.toLocalDate().plusWeeks(1);
        jdbcTemplate.update("""
                INSERT INTO assignments (title, content, submission_method, close_at, submission_target,
                    submission_visibility, study_id, recurrence_id, occurrence_date, created_at, updated_at)
                VALUES ('이미 있는 회차', '내용', '', ?, 'MEMBERS_ONLY', 'LEADER_ONLY', ?, ?, ?, ?, ?)
                """, nextOccurrenceDate.atTime(23, 59), study.getId(), recurrenceId, nextOccurrenceDate,
                LocalDateTime.now(CLOCK), LocalDateTime.now(CLOCK));

        worker.process();

        assertThat(countAssignments()).isEqualTo(2);
        LocalDateTime existingCloseAt = firstCloseAt.plusWeeks(1);
        assertSchedule(recurrenceId, existingCloseAt, existingCloseAt.toLocalDate().plusWeeks(1));
    }

    @Test
    @DisplayName("생성에 실패한 규칙은 다음 생성 시각을 재시도 대기 시간만큼 미룬다")
    void postponeFailedRecurrence() {
        Long recurrenceId = createRecurrence(SubmissionTarget.MEMBERS_ONLY, null, null);
        makeDue(recurrenceId);
        LocalDateTime before = LocalDateTime.now(CLOCK);

        generationService.postpone(new AssignmentRecurrenceCandidate(recurrenceId, study.getId()),
                Duration.ofMinutes(10));

        LocalDateTime nextPublishAt = jdbcTemplate.queryForObject(
                "SELECT next_publish_at FROM assignment_recurrences WHERE id = ?", Timestamp.class, recurrenceId)
                .toLocalDateTime();
        assertThat(nextPublishAt).isBetween(before.plusMinutes(10), LocalDateTime.now(CLOCK).plusMinutes(10));
        worker.process();
        assertThat(countAssignments()).isOne();
    }

    private Long createRecurrence(SubmissionTarget submissionTarget, List<Long> submitterIds,
                                  List<LocalDateTime> remindAts) {
        AssignmentRecurrenceCreateRequest request = new AssignmentRecurrenceCreateRequest("알고리즘 풀이",
                "이번 주 문제를 풀어요.", "링크 제출", submissionTarget, SubmissionVisibility.LEADER_ONLY,
                firstCloseAt, 1, remindAts, submitterIds);
        return recurrenceService.create(leaderUser.getId(), study.getId(), request).recurrenceId();
    }

    // 첫 회차 마감을 기다리지 않도록 다음 생성 시각만 과거로 옮긴다.
    private void makeDue(Long recurrenceId) {
        jdbcTemplate.update("UPDATE assignment_recurrences SET next_publish_at = ? WHERE id = ?",
                LocalDateTime.now(CLOCK).minusMinutes(1), recurrenceId);
    }

    private Map<String, Object> occurrenceOn(Long recurrenceId, LocalDate occurrenceDate) {
        return jdbcTemplate.queryForMap(
                "SELECT id, close_at FROM assignments WHERE recurrence_id = ? AND occurrence_date = ?",
                recurrenceId, occurrenceDate);
    }

    private void assertSchedule(Long recurrenceId, LocalDateTime nextPublishAt, LocalDate nextOccurrenceDate) {
        Map<String, Object> schedule = jdbcTemplate.queryForMap(
                "SELECT next_publish_at, next_occurrence_date FROM assignment_recurrences WHERE id = ?",
                recurrenceId);
        assertThat(((Timestamp) schedule.get("next_publish_at")).toLocalDateTime()).isEqualTo(nextPublishAt);
        assertThat(schedule.get("next_occurrence_date").toString()).isEqualTo(nextOccurrenceDate.toString());
    }

    private int countAssignments() {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM assignments", Integer.class);
    }
}
