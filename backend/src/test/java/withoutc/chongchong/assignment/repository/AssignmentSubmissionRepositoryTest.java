package withoutc.chongchong.assignment.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;
import withoutc.chongchong.assignment.entity.Assignment;
import withoutc.chongchong.assignment.entity.AssignmentSubmission;
import withoutc.chongchong.assignment.repository.projection.AssignmentSubmissionStatusProjection;
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
class AssignmentSubmissionRepositoryTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-08-20T00:00:00Z"), ZoneId.of("Asia/Seoul")
    );

    @Autowired
    private AssignmentSubmissionRepository assignmentSubmissionRepository;

    @Autowired
    private AssignmentRepository assignmentRepository;

    @Autowired
    private StudyRepository studyRepository;

    @Autowired
    private StudyMemberRepository studyMemberRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("여러 과제의 제출 상태를 StudyMember id로 한 번에 조회한다")
    void findMySubmissionStatusesByAssignmentIdsAndMemberIdTest() {
        Study study = studyRepository.save(Study.create("스터디", "설명"));
        StudyMember leader = createMember(study, "리더", StudyMemberRole.LEADER);
        StudyMember member = createMemberWithIdDifferentFromUserId(study);
        StudyMember otherMember = createMember(study, "다른 스터디원", StudyMemberRole.MEMBER);
        Assignment submittedAssignment = createAssignment(leader, "제출한 과제");
        Assignment unsubmittedAssignment = createAssignment(leader, "미제출 과제");
        Assignment otherMemberAssignment = createAssignment(leader, "다른 사람 과제");

        AssignmentSubmission submitted = AssignmentSubmission.create(member, submittedAssignment);
        ReflectionTestUtils.setField(submitted, "submitted", true);
        assignmentSubmissionRepository.saveAllAndFlush(List.of(
                submitted,
                AssignmentSubmission.create(member, unsubmittedAssignment),
                AssignmentSubmission.create(otherMember, otherMemberAssignment)
        ));

        assertThat(member.getId()).isNotEqualTo(member.getUser().getId());
        assertThat(assignmentSubmissionRepository.findMySubmissionStatusesByAssignmentIdsAndMemberId(
                List.of(submittedAssignment.getId(), unsubmittedAssignment.getId(), otherMemberAssignment.getId()),
                member.getId()
        ))
                .extracting(AssignmentSubmissionStatusProjection::assignmentId,
                        AssignmentSubmissionStatusProjection::submitted)
                .containsExactlyInAnyOrder(
                        tuple(submittedAssignment.getId(), true),
                        tuple(unsubmittedAssignment.getId(), false)
                );
    }

    @Test
    @DisplayName("같은 과제와 스터디원으로 제출 정보를 중복 저장할 수 없다")
    void rejectDuplicateAssignmentSubmissionTest() {
        Study study = studyRepository.save(Study.create("스터디", "설명"));
        StudyMember leader = createMember(study, "리더", StudyMemberRole.LEADER);
        StudyMember member = createMember(study, "스터디원", StudyMemberRole.MEMBER);
        Assignment assignment = createAssignment(leader, "과제");
        assignmentSubmissionRepository.saveAndFlush(AssignmentSubmission.create(member, assignment));

        assertThatThrownBy(() -> assignmentSubmissionRepository.saveAndFlush(
                AssignmentSubmission.create(member, assignment)
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    private Assignment createAssignment(StudyMember leader, String title) {
        return assignmentRepository.saveAndFlush(Assignment.create(
                leader,
                title,
                "과제 내용",
                "GitHub PR",
                LocalDateTime.of(2026, 8, 30, 23, 59),
                CLOCK
        ));
    }

    private StudyMember createMemberWithIdDifferentFromUserId(Study study) {
        StudyMember candidate = createMember(study, "스터디원", StudyMemberRole.MEMBER);
        if (!candidate.getId().equals(candidate.getUser().getId())) {
            return candidate;
        }

        userRepository.save(User.create("ID 간격 생성용 사용자", null));
        return createMember(study, "다른 스터디원", StudyMemberRole.MEMBER);
    }

    private StudyMember createMember(Study study, String name, StudyMemberRole role) {
        User user = userRepository.save(User.create(name, null));
        return studyMemberRepository.save(StudyMember.create(study, user, name, null, role));
    }
}
