package withoutc.chongchong.assignment.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import withoutc.chongchong.assignment.controller.dto.AssignmentSubmissionStatusResponse;
import withoutc.chongchong.assignment.controller.dto.AssignmentSubmitRequest;
import withoutc.chongchong.assignment.controller.dto.AssignmentSubmitResponse;
import withoutc.chongchong.assignment.controller.dto.MySubmissionDetailResponse;
import withoutc.chongchong.assignment.controller.dto.SubmissionDetailResponse;
import withoutc.chongchong.assignment.controller.dto.SubmissionListResponse;
import withoutc.chongchong.assignment.controller.dto.SubmissionListResponse.SubmissionSummary;
import withoutc.chongchong.assignment.entity.Assignment;
import withoutc.chongchong.assignment.entity.AssignmentSubmission;
import withoutc.chongchong.assignment.policy.AssignmentAccessPolicy;
import withoutc.chongchong.assignment.repository.AssignmentRepository;
import withoutc.chongchong.assignment.repository.AssignmentSubmissionRepository;
import withoutc.chongchong.assignment.repository.projection.AssignmentSubmitterStatusProjection;
import withoutc.chongchong.study.entity.StudyMember;
import withoutc.chongchong.study.repository.StudyMemberRepository;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AssignmentSubmissionService {

    private final AssignmentRepository assignmentRepository;
    private final AssignmentSubmissionRepository assignmentSubmissionRepository;
    private final StudyMemberRepository studyMemberRepository;

    private final AssignmentAccessPolicy assignmentAccessPolicy;
    private final Clock clock;

    public AssignmentSubmissionStatusResponse getAssignmentSubmissionStatus(Long userId, Long studyId,
                                                                             Long assignmentId) {
        StudyMember actor = studyMemberRepository.getByStudyIdAndUserIdOrThrow(studyId, userId);
        assignmentAccessPolicy.requireCanReadAssignmentSubmissionStatus(actor);

        Assignment assignment = assignmentRepository.getByIdAndStudyIdOrThrow(assignmentId, studyId);

        List<AssignmentSubmitterStatusProjection> statuses = assignmentSubmissionRepository.findAllSubmitterStatusesByAssignmentId(
                assignmentId);

        List<AssignmentSubmissionStatusResponse.CompleteMember> completeMembers = statuses.stream()
                .filter(AssignmentSubmitterStatusProjection::isSubmitted)
                .map(status -> AssignmentSubmissionStatusResponse.CompleteMember.of(
                        status.memberId(),
                        status.name(),
                        status.profileImageUrl()
                )).toList();

        List<AssignmentSubmissionStatusResponse.IncompleteMember> incompleteMembers = statuses.stream()
                .filter(status -> !status.isSubmitted())
                .map(status -> AssignmentSubmissionStatusResponse.IncompleteMember.of(
                        status.memberId(),
                        status.name(),
                        status.profileImageUrl(),
                        status.lastRemindAt()
                )).toList();

        return AssignmentSubmissionStatusResponse.of(assignmentId, assignment.getNextRemindAt(), completeMembers,
                incompleteMembers);
    }

    @Transactional
    public AssignmentSubmitResponse submit(Long userId, Long studyId, Long assignmentId,
                                           AssignmentSubmitRequest request) {
        StudyMember actor = studyMemberRepository.getByStudyIdAndUserIdOrThrow(studyId, userId);

        assignmentRepository.getByIdAndStudyIdOrThrow(assignmentId, studyId);

        AssignmentSubmission submission = assignmentSubmissionRepository.getByAssignmentIdAndMemberIdOrThrow(
                assignmentId, actor.getId());
        submission.submit(request.content(), request.link(), LocalDateTime.now(clock));

        return AssignmentSubmitResponse.from(submission);
    }

    @Transactional
    public void updateSubmission(Long userId, Long studyId, Long assignmentId, Long submissionId,
                                 AssignmentSubmitRequest request) {
        StudyMember actor = studyMemberRepository.getByStudyIdAndUserIdOrThrow(studyId, userId);

        assignmentRepository.getByIdAndStudyIdOrThrow(assignmentId, studyId);

        AssignmentSubmission submission = assignmentSubmissionRepository.getByIdAndAssignmentIdOrThrow(submissionId,
                assignmentId);
        assignmentAccessPolicy.requireCanUpdateSubmission(actor, submission);

        submission.update(request.content(), request.link());
        assignmentSubmissionRepository.save(submission);
    }

    public MySubmissionDetailResponse getMySubmissionDetail(Long userId, Long studyId, Long assignmentId) {
        StudyMember member = studyMemberRepository.getByStudyIdAndUserIdOrThrow(studyId, userId);

        assignmentRepository.getByIdAndStudyIdOrThrow(assignmentId, studyId);

        return assignmentSubmissionRepository.findByAssignmentIdAndMemberId(assignmentId, member.getId())
                .map(MySubmissionDetailResponse::from)
                .orElse(null);
    }

    public SubmissionDetailResponse getSubmissionDetail(Long userId, Long studyId, Long assignmentId,
                                                        Long submissionId) {
        StudyMember actor = studyMemberRepository.getByStudyIdAndUserIdOrThrow(studyId, userId);

        assignmentRepository.getByIdAndStudyIdOrThrow(assignmentId, studyId);

        AssignmentSubmission submission = assignmentSubmissionRepository.getByIdAndAssignmentIdOrThrow(submissionId,
                assignmentId);
        assignmentAccessPolicy.requireCanReadSubmission(actor, submission);

        return SubmissionDetailResponse.of(submission, submission.getMember());
    }

    public SubmissionListResponse getSubmissionList(Long userId, Long studyId, Long assignmentId) {
        StudyMember actor = studyMemberRepository.getByStudyIdAndUserIdOrThrow(studyId, userId);
        assignmentAccessPolicy.requireCanReadSubmissionList(actor);

        assignmentRepository.getByIdAndStudyIdOrThrow(assignmentId, studyId);

        List<AssignmentSubmission> submissions = assignmentSubmissionRepository
                .findAllByAssignmentIdAndSubmittedTrue(assignmentId);

        return SubmissionListResponse.from(submissions.stream().map(SubmissionSummary::from).toList());
    }
}
