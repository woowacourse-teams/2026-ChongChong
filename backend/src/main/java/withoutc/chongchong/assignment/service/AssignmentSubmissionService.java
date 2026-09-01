package withoutc.chongchong.assignment.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import withoutc.chongchong.assignment.controller.dto.AssignmentStatusesResponse;
import withoutc.chongchong.assignment.controller.dto.AssignmentSubmitRequest;
import withoutc.chongchong.assignment.controller.dto.AssignmentSubmitResponse;
import withoutc.chongchong.assignment.controller.dto.MySubmissionDetailResponse;
import withoutc.chongchong.assignment.controller.dto.SubmissionDetailResponse;
import withoutc.chongchong.assignment.controller.dto.SubmissionListResponse;
import withoutc.chongchong.assignment.controller.dto.SubmissionListResponse.SubmissionSummary;
import withoutc.chongchong.assignment.entity.Assignment;
import withoutc.chongchong.assignment.entity.AssignmentSubmission;
import withoutc.chongchong.assignment.exception.AssignmentErrorCode;
import withoutc.chongchong.assignment.exception.AssignmentException;
import withoutc.chongchong.assignment.repository.AssignmentRepository;
import withoutc.chongchong.assignment.repository.AssignmentSubmissionRepository;
import withoutc.chongchong.assignment.repository.projection.AssignmentSubmitterStatusProjection;
import withoutc.chongchong.auth.exception.AuthErrorCode;
import withoutc.chongchong.auth.exception.AuthException;
import withoutc.chongchong.study.entity.StudyMember;
import withoutc.chongchong.study.repository.StudyMemberRepository;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AssignmentSubmissionService {

    private final AssignmentRepository assignmentRepository;
    private final AssignmentSubmissionRepository assignmentSubmissionRepository;
    private final StudyMemberRepository studyMemberRepository;

    private final Clock clock;

    public AssignmentStatusesResponse getAllSubmittedStatus(Long userId, Long studyId, Long assignmentId) {
        validateLeader(studyId, userId);

        Assignment assignment = assignmentRepository.getByIdOrThrow(assignmentId);
        validateAssignmentBelongsToStudy(studyId, assignment);

        List<AssignmentSubmitterStatusProjection> statues = assignmentSubmissionRepository.findAllSubmitterStatusesByAssignmentId(
                assignmentId);

        List<AssignmentStatusesResponse.CompleteMember> completeMembers = statues.stream()
                .filter(AssignmentSubmitterStatusProjection::isSubmitted)
                .map(status -> AssignmentStatusesResponse.CompleteMember.of(
                        status.memberId(),
                        status.name(),
                        status.profileImageUrl()
                )).toList();

        List<AssignmentStatusesResponse.IncompleteMember> incompleteMembers = statues.stream()
                .filter(status -> !status.isSubmitted())
                .map(status -> AssignmentStatusesResponse.IncompleteMember.of(
                        status.memberId(),
                        status.name(),
                        status.profileImageUrl(),
                        status.lastRemindAt()
                )).toList();

        return AssignmentStatusesResponse.of(assignmentId, assignment.getNextRemindAt(), completeMembers,
                incompleteMembers);
    }

    @Transactional
    public AssignmentSubmitResponse create(Long userId, Long studyId, Long assignmentId,
                                           AssignmentSubmitRequest request) {
        StudyMember member = studyMemberRepository.getByStudyIdAndUserIdOrThrow(studyId, userId);

        Assignment assignment = assignmentRepository.getByIdOrThrow(assignmentId);
        validateAssignmentBelongsToStudy(studyId, assignment);

        AssignmentSubmission submission = assignmentSubmissionRepository.getByAssignmentIdAndMemberIdOrThrow(
                assignmentId, member.getId());
        submission.submit(request.content(), request.link(), LocalDateTime.now(clock));

        return AssignmentSubmitResponse.from(submission);
    }

    @Transactional
    public void update(Long userId, Long studyId, Long assignmentId, Long submissionId,
                       AssignmentSubmitRequest request) {
        StudyMember member = studyMemberRepository.getByStudyIdAndUserIdOrThrow(studyId, userId);

        Assignment assignment = assignmentRepository.getByIdOrThrow(assignmentId);
        validateAssignmentBelongsToStudy(studyId, assignment);

        AssignmentSubmission submission = assignmentSubmissionRepository.getByIdAndAssignmentIdAndMemberIdOrThrow(
                submissionId, assignmentId, member.getId());
        submission.update(request.content(), request.link());
        assignmentSubmissionRepository.save(submission);
    }

    public MySubmissionDetailResponse getMySubmissionDetail(Long userId, Long studyId, Long assignmentId) {
        StudyMember member = studyMemberRepository.getByStudyIdAndUserIdOrThrow(studyId, userId);

        Assignment assignment = assignmentRepository.getByIdOrThrow(assignmentId);
        validateAssignmentBelongsToStudy(studyId, assignment);

        return assignmentSubmissionRepository.findByAssignmentIdAndMemberId(assignmentId, member.getId())
                .map(MySubmissionDetailResponse::from)
                .orElse(null);
    }

    public SubmissionDetailResponse getSubmissionDetail(Long userId, Long studyId, Long assignmentId,
                                                        Long submissionId) {
        StudyMember member = studyMemberRepository.getByStudyIdAndUserIdOrThrow(studyId, userId);

        Assignment assignment = assignmentRepository.getByIdOrThrow(assignmentId);
        validateAssignmentBelongsToStudy(studyId, assignment);

        AssignmentSubmission submission = getAssignmentSubmission(submissionId, assignmentId, member);

        return SubmissionDetailResponse.of(submission, submission.getMember());
    }

    public SubmissionListResponse getSubmissionList(Long userId, Long studyId, Long assignmentId) {
        StudyMember member = studyMemberRepository.getByStudyIdAndUserIdOrThrow(studyId, userId);
        validateLeader(member);

        Assignment assignment = assignmentRepository.getByIdOrThrow(assignmentId);
        validateAssignmentBelongsToStudy(studyId, assignment);

        List<AssignmentSubmission> submissions = assignmentSubmissionRepository
                .findAllByAssignmentIdAndSubmittedTrue(assignmentId);

        return SubmissionListResponse.from(submissions.stream().map(SubmissionSummary::from).toList());
    }

    private AssignmentSubmission getAssignmentSubmission(Long submissionId, Long assignmentId, StudyMember member) {
        if (member.isLeader()) {
            return assignmentSubmissionRepository.getByIdAndAssignmentIdOrThrow(submissionId, assignmentId);
        }
        return assignmentSubmissionRepository.getByIdAndAssignmentIdAndMemberIdOrThrow(submissionId, assignmentId,
                member.getId());
    }

    private void validateLeader(Long studyId, Long userId) {
        StudyMember member = studyMemberRepository.getByStudyIdAndUserIdOrThrow(studyId, userId);
        validateLeader(member);
    }

    private void validateLeader(StudyMember member) {
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
