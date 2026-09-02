package withoutc.chongchong.assignment.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;
import withoutc.chongchong.assignment.entity.Assignment;
import withoutc.chongchong.assignment.exception.AssignmentErrorCode;
import withoutc.chongchong.assignment.exception.AssignmentException;
import withoutc.chongchong.assignment.repository.projection.LeaderAssignmentSummaryProjection;
import withoutc.chongchong.study.entity.Study;
import withoutc.chongchong.study.entity.StudyMember;
import withoutc.chongchong.study.entity.StudyMemberRole;
import withoutc.chongchong.study.repository.StudyMemberRepository;
import withoutc.chongchong.study.repository.StudyRepository;
import withoutc.chongchong.user.entity.User;
import withoutc.chongchong.user.repository.UserRepository;

@ActiveProfiles("test")
@Transactional
@SpringBootTest
class AssignmentRepositoryTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 20, 9, 0);

    @Autowired
    private AssignmentRepository assignmentRepository;

    @Autowired
    private StudyRepository studyRepository;

    @Autowired
    private StudyMemberRepository studyMemberRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("스터디별 과제를 최신순으로 조회하고 cursor보다 작은 id부터 다음 페이지를 조회한다")
    void findByCursorTest() {
        StudyFixture firstStudy = createStudyFixture("첫 번째 스터디", "첫 번째 리더");
        StudyFixture secondStudy = createStudyFixture("두 번째 스터디", "두 번째 리더");
        List<Assignment> assignments = new ArrayList<>();
        for (int index = 1; index <= 5; index++) {
            assignments.add(assignmentRepository.save(
                    Assignment.create(
                            firstStudy.study(),
                            "과제 " + index,
                            "과제 내용 " + index,
                            "GitHub PR",
                            LocalDateTime.of(2026, 8, 30, 23, 59),
                            NOW
                    )
            ));
        }
        assignmentRepository.save(Assignment.create(
                secondStudy.study(),
                "다른 과제",
                "다른 과제 내용",
                "GitHub PR",
                LocalDateTime.of(2026, 8, 30, 23, 59),
                NOW
        ));
        assignmentRepository.flush();

        List<Assignment> firstPage = assignmentRepository.findByCursor(
                firstStudy.study().getId(),
                null,
                PageRequest.of(0, 3)
        );
        List<Assignment> secondPage = assignmentRepository.findByCursor(
                firstStudy.study().getId(),
                firstPage.getLast().getId(),
                PageRequest.of(0, 3)
        );

        assertThat(firstPage)
                .extracting(Assignment::getId)
                .containsExactly(assignments.get(4).getId(), assignments.get(3).getId(), assignments.get(2).getId());
        assertThat(secondPage)
                .extracting(Assignment::getId)
                .containsExactly(assignments.get(1).getId(), assignments.getFirst().getId());
    }

    @Test
    @DisplayName("멤버별 과제 cursor 조회는 제출 정보가 존재하는 과제만 반환한다")
    void findByCursorAndMemberIdTest() {
        StudyWithMembersFixture fixture = createStudyWithMembersFixture();
        Assignment firstMemberAssignment = createAssignment(
                fixture.study(), "첫 번째 멤버 과제", List.of(fixture.firstMember()), 0
        );
        createAssignment(fixture.study(), "두 번째 멤버 과제", List.of(fixture.secondMember()), 0);
        assignmentRepository.flush();

        List<Assignment> assignments = assignmentRepository.findByCursorAndMemberId(
                fixture.study().getId(),
                fixture.firstMember().getId(),
                null,
                PageRequest.of(0, 11)
        );

        assertThat(assignments)
                .extracting(Assignment::getId)
                .containsExactly(firstMemberAssignment.getId());
    }

    @Test
    @DisplayName("과제 조회에 성공하면 해당 과제를 반환하고, 없으면 과제 없음 예외를 던진다")
    void getByIdOrThrowTest() {
        StudyFixture fixture = createStudyFixture("스터디", "리더");
        Assignment assignment = assignmentRepository.save(Assignment.create(
                fixture.study(),
                "과제",
                "과제 내용",
                "GitHub PR",
                LocalDateTime.of(2026, 8, 30, 23, 59),
                NOW
        ));

        Assignment found = assignmentRepository.getByIdOrThrow(assignment.getId());

        assertThat(found.getId()).isEqualTo(assignment.getId());
        assertThatThrownBy(() -> assignmentRepository.getByIdOrThrow(Long.MAX_VALUE))
                .isInstanceOfSatisfying(AssignmentException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(AssignmentErrorCode.ASSIGNMENT_NOT_FOUND)
                );
    }

    @Test
    @DisplayName("리더용 미완료 과제 요약은 미제출자가 있는 과제만 반환하고 제출 수를 센다")
    void findIncompleteAssignmentSummariesByStudyIdTest() {
        StudyWithMembersFixture fixture = createStudyWithMembersFixture();
        Assignment incompleteAssignment = createAssignment(
                fixture.study(), "일부 제출 과제", List.of(fixture.firstMember(), fixture.secondMember()), 1
        );
        Assignment completeAssignment = createAssignment(
                fixture.study(), "완료 과제", List.of(fixture.firstMember(), fixture.secondMember()), 2
        );
        Assignment unsubmittedAssignment = createAssignment(
                fixture.study(), "미제출 과제", List.of(fixture.firstMember(), fixture.secondMember()), 0
        );
        assignmentRepository.flush();

        List<LeaderAssignmentSummaryProjection> summaries =
                assignmentRepository.findIncompleteAssignmentSummariesByStudyId(fixture.study().getId());

        assertThat(summaries)
                .extracting(LeaderAssignmentSummaryProjection::id,
                        LeaderAssignmentSummaryProjection::title,
                        LeaderAssignmentSummaryProjection::memberCount,
                        LeaderAssignmentSummaryProjection::completeCount)
                .containsExactlyInAnyOrder(
                        tuple(incompleteAssignment.getId(), "일부 제출 과제", 2L, 1L),
                        tuple(unsubmittedAssignment.getId(), "미제출 과제", 2L, 0L)
                );
    }

    @Test
    @DisplayName("멤버용 미완료 과제는 해당 멤버가 제출하지 않은 과제만 반환한다")
    void findIncompleteAssignmentsByStudyIdAndMemberIdTest() {
        StudyWithMembersFixture fixture = createStudyWithMembersFixture();
        Assignment incompleteAssignment = createAssignment(
                fixture.study(), "미제출 과제", List.of(fixture.firstMember()), 0
        );
        Assignment completeAssignment = createAssignment(
                fixture.study(), "제출 과제", List.of(fixture.firstMember()), 1
        );
        assignmentRepository.flush();

        List<Assignment> assignments = assignmentRepository.findIncompleteAssignmentsByStudyIdAndMemberId(
                fixture.study().getId(), fixture.firstMember().getId());

        assertThat(assignments)
                .extracting(Assignment::getId)
                .containsExactly(incompleteAssignment.getId());
    }

    @Test
    @DisplayName("리더용 미완료 과제 개수는 미제출자가 있는 과제만 센다")
    void countIncompleteAssignmentByStudyIdTest() {
        StudyWithMembersFixture fixture = createStudyWithMembersFixture();
        createAssignment(
                fixture.study(), "일부 제출 과제", List.of(fixture.firstMember(), fixture.secondMember()), 1
        );
        createAssignment(
                fixture.study(), "완료 과제", List.of(fixture.firstMember(), fixture.secondMember()), 2
        );
        assignmentRepository.flush();

        long count = assignmentRepository.countIncompleteAssignmentByStudyId(fixture.study().getId());

        assertThat(count).isEqualTo(1L);
    }

    @Test
    @DisplayName("스터디원용 미완료 과제 개수는 해당 멤버가 제출하지 않은 과제만 센다")
    void countIncompleteAssignmentByStudyIdAndMemberIdTest() {
        StudyWithMembersFixture fixture = createStudyWithMembersFixture();
        createAssignment(fixture.study(), "첫 번째 멤버 미제출 과제", List.of(fixture.firstMember()), 0);
        createAssignment(fixture.study(), "첫 번째 멤버 제출 과제", List.of(fixture.firstMember()), 1);
        createAssignment(fixture.study(), "두 번째 멤버 미제출 과제", List.of(fixture.secondMember()), 0);
        assignmentRepository.flush();

        long firstMemberCount = assignmentRepository.countIncompleteAssignmentByStudyIdAndMemberId(
                fixture.study().getId(), fixture.firstMember().getId());
        long secondMemberCount = assignmentRepository.countIncompleteAssignmentByStudyIdAndMemberId(
                fixture.study().getId(), fixture.secondMember().getId());

        assertThat(firstMemberCount).isEqualTo(1L);
        assertThat(secondMemberCount).isEqualTo(1L);
    }

    private StudyFixture createStudyFixture(String studyName, String userName) {
        User user = userRepository.save(User.create(userName, null));
        Study study = studyRepository.save(Study.create(studyName, "설명"));
        StudyMember leader = studyMemberRepository.save(
                StudyMember.create(study, user, userName, null, StudyMemberRole.LEADER)
        );
        return new StudyFixture(study, leader);
    }

    private Assignment createAssignment(
            Study study,
            String title,
            List<StudyMember> members,
            int submittedCount
    ) {
        Assignment assignment = assignmentRepository.save(Assignment.create(
                study,
                title,
                "과제 내용",
                "GitHub PR",
                LocalDateTime.of(2026, 8, 30, 23, 59),
                NOW
        ));
        assignment.initializeSubmissions(members);
        assignment.getSubmissions().stream()
                .limit(submittedCount)
                .forEach(submission -> ReflectionTestUtils.setField(submission, "submitted", true));
        return assignment;
    }

    private StudyWithMembersFixture createStudyWithMembersFixture() {
        User leaderUser = userRepository.save(User.create("리더", null));
        User firstMemberUser = userRepository.save(User.create("첫 번째 멤버", null));
        User secondMemberUser = userRepository.save(User.create("두 번째 멤버", null));
        Study study = studyRepository.save(Study.create("스터디", "설명"));
        StudyMember leader = studyMemberRepository.save(
                StudyMember.create(study, leaderUser, leaderUser.getName(), null, StudyMemberRole.LEADER)
        );
        StudyMember firstMember = studyMemberRepository.save(
                StudyMember.create(study, firstMemberUser, firstMemberUser.getName(), null, StudyMemberRole.MEMBER)
        );
        StudyMember secondMember = studyMemberRepository.save(
                StudyMember.create(study, secondMemberUser, secondMemberUser.getName(), null, StudyMemberRole.MEMBER)
        );
        return new StudyWithMembersFixture(study, leader, firstMember, secondMember);
    }

    private record StudyFixture(Study study, StudyMember leader) {
    }

    private record StudyWithMembersFixture(
            Study study,
            StudyMember leader,
            StudyMember firstMember,
            StudyMember secondMember
    ) {
    }
}
