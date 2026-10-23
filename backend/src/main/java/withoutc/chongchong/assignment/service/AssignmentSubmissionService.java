package withoutc.chongchong.assignment.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import withoutc.chongchong.assignment.controller.dto.AssignmentAttachmentResponse;
import withoutc.chongchong.assignment.controller.dto.AssignmentSubmitRequest;
import withoutc.chongchong.assignment.controller.dto.AssignmentSubmitResponse;
import withoutc.chongchong.assignment.controller.dto.MySubmissionDetailResponse;
import withoutc.chongchong.assignment.controller.dto.SubmissionDetailResponse;
import withoutc.chongchong.assignment.controller.dto.SubmissionListResponse.SubmissionSummary;
import withoutc.chongchong.assignment.controller.dto.SubmissionListResponse;
import withoutc.chongchong.assignment.entity.Assignment;
import withoutc.chongchong.assignment.entity.AssignmentSubmission;
import withoutc.chongchong.assignment.entity.SubmissionAttachment;
import withoutc.chongchong.assignment.policy.AssignmentAccessPolicy;
import withoutc.chongchong.assignment.policy.AssignmentAttachmentPolicy;
import withoutc.chongchong.assignment.repository.AssignmentRepository;
import withoutc.chongchong.assignment.repository.AssignmentSubmissionRepository;
import withoutc.chongchong.file.entity.FileMetadata;
import withoutc.chongchong.file.service.FileUrlGenerator;
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
    private final AssignmentAttachmentService assignmentAttachmentService;
    private final FileUrlGenerator fileUrlGenerator;

    private final AssignmentAttachmentPolicy assignmentAttachmentPolicy;
    private final AssignmentAccessPolicy assignmentAccessPolicy;
    private final Clock clock;

    @Transactional
    public AssignmentSubmitResponse submit(Long userId, Long studyId, Long assignmentId,
                                           AssignmentSubmitRequest request) {
        StudyMember actor = studyMemberRepository.getByStudyIdAndUserIdOrThrow(studyId, userId);

        assignmentRepository.getByIdAndStudyIdOrThrow(assignmentId, studyId);

        AssignmentSubmission submission = assignmentSubmissionRepository.getWithLockByAssignmentIdAndMemberIdOrThrow(
                assignmentId, actor.getId());
        LocalDateTime now = LocalDateTime.now(clock);
        boolean isFirstSubmit = !submission.isSubmit(now);

        List<FileMetadata> files = prepareAttachments(userId, submission, request);
        assignmentAttachmentPolicy.validateSubmission(request.content(), request.link(), files);
        submission.submit(request.content(), request.link(), now);
        replaceAttachments(submission, request, files);

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

        List<FileMetadata> files = prepareAttachments(userId, submission, request);
        submission.update(request.content(), request.link());
        assignmentAttachmentPolicy.validateSubmission(submission.getContent(), submission.getLink(), files);
        replaceAttachments(submission, request, files);
        assignmentSubmissionRepository.save(submission);
    }

    public MySubmissionDetailResponse getMySubmissionDetail(Long userId, Long studyId, Long assignmentId) {
        StudyMember member = studyMemberRepository.getByStudyIdAndUserIdOrThrow(studyId, userId);

        assignmentRepository.getByIdAndStudyIdOrThrow(assignmentId, studyId);

        Optional<AssignmentSubmission> foundSubmission = assignmentSubmissionRepository
                .findByAssignmentIdAndMemberId(assignmentId, member.getId());
        if (foundSubmission.isEmpty()) {
            return MySubmissionDetailResponse.notAssigned();
        }

        AssignmentSubmission submission = foundSubmission.get();
        List<AssignmentAttachmentResponse> attachments = submission.getAttachments().stream()
                .map(attachment -> {
                    FileMetadata file = attachment.getFile();
                    String imageUrl = fileUrlGenerator.generateImageUrl(
                            file.getObjectKey(), file.getContentType());
                    String downloadUrl = fileUrlGenerator.generateDownloadUrl(
                            file.getObjectKey(), file.getOriginalFileName());
                    return AssignmentAttachmentResponse.of(file, attachment.getSortOrder(), imageUrl, downloadUrl);
                })
                .toList();
        return MySubmissionDetailResponse.of(LocalDateTime.now(clock), submission, attachments);
    }

    public SubmissionDetailResponse getSubmissionDetail(Long userId, Long studyId, Long assignmentId,
                                                        Long submissionId) {
        StudyMember actor = studyMemberRepository.getByStudyIdAndUserIdOrThrow(studyId, userId);
        Assignment assignment = assignmentRepository.getByIdAndStudyIdOrThrow(assignmentId, studyId);

        AssignmentSubmission submission = assignmentSubmissionRepository.getByIdAndAssignmentIdOrThrow(submissionId,
                assignmentId);
        assignmentAccessPolicy.requireCanReadSubmission(actor, assignment, submission);

        List<AssignmentAttachmentResponse> attachments = submission.getAttachments().stream()
                .map(attachment -> {
                    FileMetadata file = attachment.getFile();
                    String imageUrl = fileUrlGenerator.generateImageUrl(
                            file.getObjectKey(), file.getContentType());
                    String downloadUrl = fileUrlGenerator.generateDownloadUrl(
                            file.getObjectKey(), file.getOriginalFileName());
                    return AssignmentAttachmentResponse.of(file, attachment.getSortOrder(), imageUrl, downloadUrl);
                })
                .toList();
        return SubmissionDetailResponse.of(submission, submission.getMember(), attachments);
    }

    public SubmissionListResponse getSubmissionList(Long userId, Long studyId, Long assignmentId) {
        StudyMember actor = studyMemberRepository.getByStudyIdAndUserIdOrThrow(studyId, userId);
        Assignment assignment = assignmentRepository.getByIdAndStudyIdOrThrow(assignmentId, studyId);
        assignmentAccessPolicy.requireCanReadSubmissionList(assignment, actor);

        List<AssignmentSubmission> submissions = assignmentSubmissionRepository
                .findAllByAssignmentIdAndSubmittedAtIsNotNull(assignmentId);

        LocalDateTime now = LocalDateTime.now(clock);
        return SubmissionListResponse.from(submissions.stream().map(
                submission -> SubmissionSummary.of(now, submission)).toList());
    }

    private List<FileMetadata> prepareAttachments(Long userId, AssignmentSubmission submission,
                                                  AssignmentSubmitRequest request) {
        return assignmentAttachmentService.prepare(userId,
                submission.getAttachments().stream().map(SubmissionAttachment::getFile).toList(),
                request.attachmentFileIds());
    }

    private void replaceAttachments(AssignmentSubmission submission, AssignmentSubmitRequest request,
                                    List<FileMetadata> files) {
        if (request.attachmentFileIds() == null) {
            return;
        }
        submission.clearAttachments();
        assignmentSubmissionRepository.flush();
        submission.addAttachments(files);
    }
}
