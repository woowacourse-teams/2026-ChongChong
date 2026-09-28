package withoutc.chongchong.assignment.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import withoutc.chongchong.assignment.controller.dto.AssignmentSubmitRequest;
import withoutc.chongchong.assignment.controller.dto.AssignmentSubmitResponse;
import withoutc.chongchong.assignment.controller.dto.MySubmissionDetailResponse;
import withoutc.chongchong.assignment.controller.dto.SubmissionDetailResponse;
import withoutc.chongchong.assignment.controller.dto.SubmissionListResponse;
import withoutc.chongchong.assignment.controller.dto.SubmissionListResponse.SubmissionSummary;
import withoutc.chongchong.assignment.entity.AssignmentSubmission;
import withoutc.chongchong.assignment.policy.AssignmentAccessPolicy;
import withoutc.chongchong.assignment.repository.AssignmentRepository;
import withoutc.chongchong.assignment.repository.AssignmentSubmissionRepository;
import withoutc.chongchong.notification.service.NotificationService;
import withoutc.chongchong.study.entity.StudyMember;
import withoutc.chongchong.study.entity.StudyMemberRole;
import withoutc.chongchong.study.repository.StudyMemberRepository;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AssignmentSubmissionService {

    private final AssignmentRepository assignmentRepository;
    private final AssignmentSubmissionRepository assignmentSubmissionRepository;
    private final StudyMemberRepository studyMemberRepository;
    private final NotificationService notificationService;

    private final AssignmentAccessPolicy assignmentAccessPolicy;
    private final Clock clock;

    @Transactional
    public AssignmentSubmitResponse submit(Long userId, Long studyId, Long assignmentId,
                                           AssignmentSubmitRequest request) {
        StudyMember actor = studyMemberRepository.getByStudyIdAndUserIdOrThrow(studyId, userId);

        assignmentRepository.getByIdAndStudyIdOrThrow(assignmentId, studyId);

        AssignmentSubmission submission = assignmentSubmissionRepository.getByAssignmentIdAndMemberIdForUpdateOrThrow(
                assignmentId, actor.getId());
        boolean isFirstSubmit = submission.getSubmittedAt() == null;
        submission.submit(request.content(), request.link(), LocalDateTime.now(clock));

        // TODO: 리더가 과제를 제출할 경우 나머지 리더들에게 알림을 보낼지, 아예 안 보낼지 결정 필요
        if (isFirstSubmit) {
            List<StudyMember> leaders = studyMemberRepository.findAllByStudyIdAndRole(studyId, StudyMemberRole.LEADER)
                    .stream()
                    .filter(leader -> !leader.getId().equals(actor.getId()))
                    .toList();
            notificationService.createAssignmentSubmissionSubmittedEventNotifications(submission, leaders);
        }

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
                .orElseGet(MySubmissionDetailResponse::notAssigned);
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
                .findAllByAssignmentIdAndSubmittedAtIsNotNull(assignmentId);

        return SubmissionListResponse.from(submissions.stream().map(SubmissionSummary::from).toList());
    }
}
