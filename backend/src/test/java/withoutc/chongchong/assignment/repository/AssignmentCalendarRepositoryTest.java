package withoutc.chongchong.assignment.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import withoutc.chongchong.assignment.entity.Assignment;
import withoutc.chongchong.assignment.entity.AssignmentSubmission;
import withoutc.chongchong.assignment.entity.SubmissionTarget;
import withoutc.chongchong.assignment.entity.SubmissionVisibility;
import withoutc.chongchong.assignment.repository.projection.AssignmentCalendarProjection;
import withoutc.chongchong.study.entity.Study;
import withoutc.chongchong.study.entity.StudyMember;
import withoutc.chongchong.study.entity.StudyMemberRole;
import withoutc.chongchong.study.repository.StudyMemberRepository;
import withoutc.chongchong.study.repository.StudyRepository;
import withoutc.chongchong.user.entity.User;
import withoutc.chongchong.user.repository.UserRepository;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AssignmentCalendarRepositoryTest {

    private static final LocalDateTime START = LocalDateTime.of(2024, 2, 1, 0, 0);
    private static final LocalDateTime END = LocalDateTime.of(2024, 3, 1, 0, 0);

    @Autowired
    private AssignmentRepository assignments;
    @Autowired
    private AssignmentSubmissionRepository submissions;
    @Autowired
    private StudyRepository studies;
    @Autowired
    private StudyMemberRepository members;
    @Autowired
    private UserRepository users;
    @Autowired
    private JdbcTemplate jdbc;

    private Study study;
    private StudyMember member;
    private StudyMember other;
    private StudyMember leader;

    @BeforeEach
    void setUp() {
        study = studies.saveAndFlush(Study.create("조회 스터디", "설명", false));
        member = saveMember(study, "조회 멤버", StudyMemberRole.MEMBER);
        other = saveMember(study, "다른 멤버", StudyMemberRole.MEMBER);
        leader = saveMember(study, "리더", StudyMemberRole.LEADER);
    }

    @Test
    @DisplayName("생성·마감 시각에 월 경계를 각각 적용하고 월말 소수 초를 보존한다")
    void respectsCreationAndDeadlineBoundariesIndependently() {
        Assignment firstCreated = saveAssignment(study, START, END, List.of(member));
        Assignment lastCreated = saveAssignment(study, END.minusNanos(1000), END.plusDays(1), List.of(member));
        Assignment firstDue = saveAssignment(study, START.minusNanos(1000), START, List.of(member));
        Assignment lastDue = saveAssignment(study, START.minusDays(1), END.minusNanos(1000), List.of(member));
        saveAssignment(study, END, END.plusDays(1), List.of(member));
        saveAssignment(study, START.minusDays(2), START.minusNanos(1000), List.of(member));

        assertThat(created(member)).extracting(AssignmentCalendarProjection::id, AssignmentCalendarProjection::createdAt)
                .containsExactlyInAnyOrder(tuple(firstCreated.getId(), START),
                        tuple(lastCreated.getId(), END.minusNanos(1000)));
        assertThat(due(member)).extracting(AssignmentCalendarProjection::id, AssignmentCalendarProjection::closeAt)
                .containsExactlyInAnyOrder(tuple(firstDue.getId(), START),
                        tuple(lastDue.getId(), END.minusNanos(1000)));
    }

    @Test
    @DisplayName("본인 지정 과제만 중복 없이 조회하고 다른 멤버·스터디 과제는 제외하며 리더도 같은 조건을 적용한다")
    void restrictsBothQueriesToStudyAndAssignedMemberIncludingLeader() {
        Assignment assigned = saveAssignment(study, START, START.plusDays(7), List.of(member, other));
        saveAssignment(study, START, START.plusDays(7), List.of(other));
        Assignment leaderAssignment = saveAssignment(study, START, START.plusDays(7), List.of(leader));
        Study anotherStudy = studies.saveAndFlush(Study.create("다른 스터디", "설명", false));
        StudyMember anotherMembership = members.saveAndFlush(StudyMember.create(anotherStudy, member.getUser(),
                "다른 스터디 이름", null, StudyMemberRole.MEMBER));
        saveAssignment(anotherStudy, START, START.plusDays(7), List.of(anotherMembership));

        assertThat(created(member)).extracting(AssignmentCalendarProjection::id).containsExactly(assigned.getId());
        assertThat(due(member)).extracting(AssignmentCalendarProjection::id).containsExactly(assigned.getId());
        assertThat(created(leader)).extracting(AssignmentCalendarProjection::id).containsExactly(leaderAssignment.getId());
        assertThat(due(leader)).extracting(AssignmentCalendarProjection::id).containsExactly(leaderAssignment.getId());
        assertThat(assignments.findCalendarCreatedAssignments(anotherStudy.getId(), member.getId(), START, END)).isEmpty();
        assertThat(assignments.findCalendarDueAssignments(anotherStudy.getId(), member.getId(), START, END)).isEmpty();
    }

    @Test
    @DisplayName("제외 후 보존된 제출물이 있어도 조회에서 빠지고 재지정하면 같은 제출 기록으로 다시 조회된다")
    void excludesPreservedUnassignedSubmissionAndRestoresOnReassignment() {
        Assignment assignment = saveAssignment(study, START, START.plusDays(7), List.of(member, other));
        AssignmentSubmission submission = submissions.findByAssignmentIdAndMemberId(assignment.getId(), member.getId())
                .orElseThrow();
        LocalDateTime submittedAt = START.plusDays(1);
        submission.submit("보존할 내용", "https://example.com/submission", submittedAt);
        assignments.flush();
        Long submissionId = submission.getId();
        assignment.replaceSubmitters(List.of(other));
        assignments.flush();

        assertThat(created(member)).isEmpty();
        assertThat(due(member)).isEmpty();
        AssignmentSubmission preserved = submissions.findById(submissionId).orElseThrow();
        assertThat(preserved.isAssigned()).isFalse();
        assertThat(preserved.getContent()).isEqualTo("보존할 내용");
        assertThat(preserved.getSubmittedAt()).isEqualTo(submittedAt);

        assignment.replaceSubmitters(List.of(member, other));
        assignments.flush();

        assertThat(created(member)).extracting(AssignmentCalendarProjection::id, AssignmentCalendarProjection::submittedAt)
                .containsExactly(tuple(assignment.getId(), submittedAt));
        assertThat(due(member)).extracting(AssignmentCalendarProjection::id, AssignmentCalendarProjection::submittedAt)
                .containsExactly(tuple(assignment.getId(), submittedAt));
        assertThat(submissions.findByAssignmentIdAndMemberId(assignment.getId(), member.getId()).orElseThrow().getId())
                .isEqualTo(submissionId);
    }

    @Test
    @DisplayName("같은 날 생성·마감된 과제와 완료·지각 제출·미제출 과제를 모두 조회하고 본인 제출 시각을 반환한다")
    void keepsBothEventsAndAllSubmissionStates() {
        LocalDateTime closeAt = START.plusHours(12);
        Assignment submitted = saveAssignment(study, START, closeAt, List.of(member, other));
        submissions.findByAssignmentIdAndMemberId(submitted.getId(), member.getId()).orElseThrow()
                .submit(null, null, closeAt.minusHours(1));
        submissions.findByAssignmentIdAndMemberId(submitted.getId(), other.getId()).orElseThrow()
                .submit(null, null, closeAt.plusHours(2));
        Assignment late = saveAssignment(study, START, closeAt, List.of(member));
        submissions.findByAssignmentIdAndMemberId(late.getId(), member.getId()).orElseThrow()
                .submit(null, null, closeAt.plusHours(1));
        Assignment missing = saveAssignment(study, START, closeAt, List.of(member));
        assignments.flush();

        assertThat(created(member)).extracting(AssignmentCalendarProjection::id, AssignmentCalendarProjection::submittedAt)
                .containsExactlyInAnyOrder(tuple(submitted.getId(), closeAt.minusHours(1)),
                        tuple(late.getId(), closeAt.plusHours(1)), tuple(missing.getId(), null));
        assertThat(due(member)).extracting(AssignmentCalendarProjection::id, AssignmentCalendarProjection::submittedAt)
                .containsExactlyInAnyOrder(tuple(submitted.getId(), closeAt.minusHours(1)),
                        tuple(late.getId(), closeAt.plusHours(1)), tuple(missing.getId(), null));
    }

    @Test
    @DisplayName("제목·마감일 수정은 현재 원본을 반환하고 삭제하면 생성·마감 조회에서 모두 제외된다")
    void reflectsCurrentTitleDeadlineAndDeletion() {
        Assignment assignment = saveAssignment(study, START, START.plusDays(7), List.of(member));
        LocalDateTime nextMonthDeadline = END.plusDays(1);
        assignment.update(leader, "수정한 과제", null, null, null, null, nextMonthDeadline, null, START);
        assignments.flush();

        assertThat(created(member)).extracting(AssignmentCalendarProjection::title, AssignmentCalendarProjection::closeAt)
                .containsExactly(tuple("수정한 과제", nextMonthDeadline));
        assertThat(due(member)).isEmpty();
        assertThat(assignments.findCalendarDueAssignments(study.getId(), member.getId(), END, END.plusMonths(1)))
                .extracting(AssignmentCalendarProjection::id).containsExactly(assignment.getId());

        assignments.delete(assignment);
        assignments.flush();

        assertThat(created(member)).isEmpty();
        assertThat(due(member)).isEmpty();
        assertThat(assignments.findCalendarDueAssignments(study.getId(), member.getId(), END, END.plusMonths(1))).isEmpty();
    }

    @Test
    @DisplayName("페이지 크기로 자르지 않고 지정 과제를 모두 반환하며 빈 월은 빈 목록을 반환한다")
    void returnsAllAssignedMonthlyAssignmentsWithoutPagination() {
        for (int index = 0; index < 12; index++) {
            saveAssignment(study, START.plusDays(index), START.plusDays(index + 1), List.of(member));
        }

        assertThat(created(member)).hasSize(12);
        assertThat(due(member)).hasSize(12);
        assertThat(assignments.findCalendarCreatedAssignments(study.getId(), member.getId(), END, END.plusMonths(1)))
                .isEmpty();
        assertThat(assignments.findCalendarDueAssignments(study.getId(), member.getId(), END, END.plusMonths(1))).isEmpty();
    }

    private List<AssignmentCalendarProjection> created(StudyMember requester) {
        return assignments.findCalendarCreatedAssignments(study.getId(), requester.getId(), START, END);
    }

    private List<AssignmentCalendarProjection> due(StudyMember requester) {
        return assignments.findCalendarDueAssignments(study.getId(), requester.getId(), START, END);
    }

    private StudyMember saveMember(Study owner, String name, StudyMemberRole role) {
        User user = users.saveAndFlush(User.create(name, null));
        return members.saveAndFlush(StudyMember.create(owner, user, name, null, role));
    }

    private Assignment saveAssignment(Study owner, LocalDateTime createdAt, LocalDateTime closeAt,
                                      List<StudyMember> submitters) {
        Assignment assignment = Assignment.create(owner, "월간 과제", "본문", "링크 제출", SubmissionTarget.MEMBERS_ONLY,
                SubmissionVisibility.LEADER_ONLY, closeAt, createdAt.minusDays(1));
        assignment.replaceSubmitters(submitters);
        assignments.saveAndFlush(assignment);
        jdbc.update("UPDATE assignments SET created_at = ? WHERE id = ?", createdAt, assignment.getId());
        return assignment;
    }
}
