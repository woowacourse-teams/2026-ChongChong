package withoutc.chongchong.assignment.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import withoutc.chongchong.assignment.entity.Assignment;
import withoutc.chongchong.assignment.entity.AssignmentSubmission;
import withoutc.chongchong.assignment.repository.projection.AssignmentSubmissionStatusProjection;
import withoutc.chongchong.assignment.repository.projection.AssignmentSubmitterStatusProjection;
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

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 20, 9, 0);

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

    @Autowired
    private JdbcTemplate jdbcTemplate;

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
        submitted.submit("제출 내용", null, NOW);
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

    @Test
    @DisplayName("과제 제출 현황은 제출 여부와 해당 과제의 최근 리마인드 시각을 조회한다")
    void findAllSubmitterStatusesByAssignmentIdTest() {
        Study study = studyRepository.save(Study.create("스터디", "설명"));
        StudyMember leader = createMember(study, "리더", StudyMemberRole.LEADER);
        StudyMember submittedMember = createMember(study, "제출자", StudyMemberRole.MEMBER);
        StudyMember incompleteMember = createMember(study, "미제출자", StudyMemberRole.MEMBER);
        Assignment assignment = createAssignment(leader, "과제");
        Assignment otherAssignment = createAssignment(leader, "다른 과제");
        AssignmentSubmission submitted = AssignmentSubmission.create(submittedMember, assignment);
        submitted.submit("제출 내용", null, NOW);
        assignmentSubmissionRepository.saveAllAndFlush(List.of(
                submitted,
                AssignmentSubmission.create(incompleteMember, assignment)
        ));
        LocalDateTime firstRemindAt = NOW.plusHours(1);
        LocalDateTime lastRemindAt = NOW.plusHours(2);
        insertNotification(study.getId(), incompleteMember.getId(), assignment.getId(), "ASSIGNMENT", firstRemindAt);
        insertNotification(study.getId(), incompleteMember.getId(), assignment.getId(), "ASSIGNMENT", lastRemindAt);
        insertNotification(study.getId(), incompleteMember.getId(), otherAssignment.getId(), "ASSIGNMENT",
                NOW.plusHours(3));
        insertNotification(study.getId(), incompleteMember.getId(), assignment.getId(), "NOTICE", NOW.plusHours(4));

        Map<Long, AssignmentSubmitterStatusProjection> statusesByMemberId = assignmentSubmissionRepository
                .findAllSubmitterStatusesByAssignmentId(assignment.getId())
                .stream()
                .collect(Collectors.toMap(AssignmentSubmitterStatusProjection::memberId, status -> status));

        assertThat(statusesByMemberId).hasSize(2);
        assertThat(statusesByMemberId.get(submittedMember.getId()).isSubmitted()).isTrue();
        assertThat(statusesByMemberId.get(submittedMember.getId()).lastRemindAt()).isNull();
        assertThat(statusesByMemberId.get(incompleteMember.getId()).isSubmitted()).isFalse();
        assertThat(statusesByMemberId.get(incompleteMember.getId()).lastRemindAt()).isEqualTo(lastRemindAt);
    }

    @Test
    @DisplayName("멤버의 과제 제출 정보만 모두 삭제한다")
    void deleteAllByMemberIdTest() {
        Study study = studyRepository.save(Study.create("스터디", "설명"));
        StudyMember leader = createMember(study, "리더", StudyMemberRole.LEADER);
        StudyMember target = createMember(study, "삭제 대상", StudyMemberRole.MEMBER);
        StudyMember otherMember = createMember(study, "다른 스터디원", StudyMemberRole.MEMBER);
        Assignment assignment = createAssignment(leader, "과제");
        assignmentSubmissionRepository.saveAllAndFlush(List.of(
                AssignmentSubmission.create(target, assignment),
                AssignmentSubmission.create(otherMember, assignment)
        ));

        int deletedCount = assignmentSubmissionRepository.deleteAllByMemberId(target.getId());

        assertThat(deletedCount).isOne();
        assertThat(assignmentSubmissionRepository.findAll())
                .extracting(submission -> submission.getMember().getId())
                .containsExactly(otherMember.getId());
        assertThat(assignmentRepository.findById(assignment.getId())).isPresent();
    }

    private Assignment createAssignment(StudyMember leader, String title) {
        return assignmentRepository.saveAndFlush(Assignment.create(
                leader,
                title,
                "과제 내용",
                "GitHub PR",
                LocalDateTime.of(2026, 8, 30, 23, 59),
                NOW
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

    private void insertNotification(Long studyId, Long recipientId, Long resourceId, String resourceType,
                                    LocalDateTime createdAt) {
        jdbcTemplate.update("""
                        INSERT INTO notifications (
                            study_id, recipient_id, type, resource_id, resource_type, is_read, created_at, updated_at
                        ) VALUES (?, ?, 'REMIND', ?, ?, false, ?, ?)
                        """,
                studyId, recipientId, resourceId, resourceType, createdAt, createdAt);
    }
}
