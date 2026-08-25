package withoutc.chongchong.assignment.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import withoutc.chongchong.assignment.controller.dto.AssignmentCreateRequest;
import withoutc.chongchong.assignment.controller.dto.AssignmentCreateResponse;
import withoutc.chongchong.assignment.controller.dto.AssignmentUpdateRequest;
import withoutc.chongchong.assignment.entity.Assignment;
import withoutc.chongchong.assignment.exception.AssignmentErrorCode;
import withoutc.chongchong.assignment.exception.AssignmentException;
import withoutc.chongchong.assignment.repository.AssignmentRepository;
import withoutc.chongchong.auth.exception.AuthErrorCode;
import withoutc.chongchong.auth.exception.AuthException;
import withoutc.chongchong.study.entity.StudyMember;
import withoutc.chongchong.study.repository.StudyMemberRepository;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AssignmentService {

    private final AssignmentRepository assignmentRepository;
    private final StudyMemberRepository studyMemberRepository;

    private final Clock clock;

    @Transactional
    public AssignmentCreateResponse create(Long userId, Long studyId, AssignmentCreateRequest request) {
        validateLeader(studyId, userId);

        List<StudyMember> members = studyMemberRepository.findAllByStudyId(studyId).stream()
                .filter(studyMember -> !studyMember.isLeader()).toList();

        StudyMember writer = studyMemberRepository.getByStudyIdAndUserIdOrThrow(studyId, userId);

        Assignment assignment = Assignment.create(writer, request.title(), request.content(),
                request.submissionMethod(), request.closeAt(), clock);
        LocalDateTime now = LocalDateTime.now(clock);
        assignment.addReminders(request.remindAts(), now);
        assignment.addRecipients(members);

        assignmentRepository.save(assignment);

        return AssignmentCreateResponse.from(assignment);
    }

    @Transactional
    public void update(Long userId, Long studyId, Long assignmentId, AssignmentUpdateRequest request) {
        validateLeader(studyId, userId);

        Assignment assignment = assignmentRepository.getByIdOrThrow(assignmentId);
        validateAssignmentBelongsToStudy(studyId, assignment);

        assignment.update(request.title(), request.content(), request.submissionMethod(), request.closeAt(),
                request.remindAts(), clock);

        assignmentRepository.save(assignment);
    }

    @Transactional
    public void delete(Long userId, Long studyId, Long assignmentId) {
        validateLeader(studyId, userId);

        Assignment assignment = assignmentRepository.getByIdOrThrow(assignmentId);
        validateAssignmentBelongsToStudy(studyId, assignment);

        assignmentRepository.delete(assignment);
    }


    private void validateLeader(Long studyId, Long userId) {
        StudyMember member = studyMemberRepository.getByStudyIdAndUserIdOrThrow(studyId, userId);
        if (!member.isLeader()) {
            throw new AuthException(AuthErrorCode.ACCESS_DENIED);
        }
    }

    private void validateAssignmentBelongsToStudy(Long studyId, Assignment assignment) {
        if (!Objects.equals(assignment.getStudy().getId(), studyId)) {
            throw new AssignmentException(AssignmentErrorCode.ASSIGNMENT_NOT_FOUND);
        }
    }
}
