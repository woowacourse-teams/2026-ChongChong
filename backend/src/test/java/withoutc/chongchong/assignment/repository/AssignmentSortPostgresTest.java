package withoutc.chongchong.assignment.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;
import withoutc.chongchong.assignment.entity.Assignment;
import withoutc.chongchong.assignment.entity.SubmissionTarget;
import withoutc.chongchong.assignment.entity.SubmissionVisibility;
import withoutc.chongchong.study.entity.Study;
import withoutc.chongchong.study.entity.StudyMember;
import withoutc.chongchong.study.entity.StudyMemberRole;
import withoutc.chongchong.study.repository.StudyMemberRepository;
import withoutc.chongchong.study.repository.StudyRepository;
import withoutc.chongchong.support.PostgresContainerTest;
import withoutc.chongchong.user.entity.User;
import withoutc.chongchong.user.repository.UserRepository;

@Transactional
class AssignmentSortPostgresTest extends PostgresContainerTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 10, 12, 0);

    @Autowired
    private AssignmentRepository assignmentRepository;
    @Autowired
    private StudyRepository studyRepository;
    @Autowired
    private StudyMemberRepository memberRepository;
    @Autowired
    private UserRepository userRepository;

    private Study study;
    private StudyMember member;

    @BeforeEach
    void setUp() {
        study = studyRepository.save(Study.create("정렬 테스트", "설명", false));
        User user = userRepository.save(User.create("멤버", null));
        member = memberRepository.save(StudyMember.create(study, user, "멤버", null, StudyMemberRole.MEMBER));
    }

    @Test
    @DisplayName("PostgreSQL 마감순은 정확한 마감 시각을 마감 전으로 취급하고 그룹 경계를 이어 조회한다")
    void paginateDeadlineGroups() {
        Assignment expired = saveAssignment(NOW.minusSeconds(1));
        Assignment dueNow = saveAssignment(NOW);
        Assignment future = saveAssignment(NOW.plusDays(1));
        Assignment sameDeadline = saveAssignment(NOW.plusDays(1));
        assignmentRepository.flush();

        assertThat(assignmentRepository.findByDeadlineCursor(study.getId(), NOW, null, 0, null, PageRequest.of(0, 2)))
                .extracting(Assignment::getId).containsExactly(dueNow.getId(), sameDeadline.getId());
        assertThat(assignmentRepository.findByDeadlineCursor(study.getId(), NOW, sameDeadline.getId(), 0,
                sameDeadline.getCloseAt(), PageRequest.of(0, 2)))
                .extracting(Assignment::getId).containsExactly(future.getId(), expired.getId());
        assertThat(assignmentRepository.findByDeadlineCursor(study.getId(), NOW, expired.getId(), 1,
                expired.getCloseAt(), PageRequest.of(0, 2))).isEmpty();
    }

    @Test
    @DisplayName("PostgreSQL 미제출순은 본인 대상 여부와 제출 상태를 기준으로 그룹 사이를 이어 조회한다")
    void paginateUnsubmittedGroups() {
        Assignment pending = saveAssignment(NOW.plusDays(1));
        Assignment submitted = saveAssignment(NOW.plusDays(1));
        submitted.getSubmissions().getFirst().submit("제출", null, NOW);
        Assignment unassigned = saveAssignment(NOW.plusDays(1));
        unassigned.getSubmissions().getFirst().unassign();
        assignmentRepository.flush();

        assertThat(assignmentRepository.findByUnsubmittedCursor(study.getId(), member.getId(), null, 0,
                PageRequest.of(0, 1))).extracting(Assignment::getId).containsExactly(pending.getId());
        assertThat(assignmentRepository.findByUnsubmittedCursor(study.getId(), member.getId(), pending.getId(), 0,
                PageRequest.of(0, 1))).extracting(Assignment::getId).containsExactly(unassigned.getId());
        assertThat(assignmentRepository.findByUnsubmittedCursor(study.getId(), member.getId(), unassigned.getId(), 1,
                PageRequest.of(0, 1))).extracting(Assignment::getId).containsExactly(submitted.getId());
        assertThat(assignmentRepository.findByUnsubmittedCursor(study.getId(), member.getId(), submitted.getId(), 1,
                PageRequest.of(0, 1))).isEmpty();
    }

    private Assignment saveAssignment(LocalDateTime closeAt) {
        Assignment assignment = Assignment.create(study, "과제", "내용", null, SubmissionTarget.MEMBERS_ONLY,
                SubmissionVisibility.LEADER_ONLY, closeAt, NOW.minusDays(1));
        assignment.initializeSubmissions(List.of(member));
        return assignmentRepository.save(assignment);
    }
}
