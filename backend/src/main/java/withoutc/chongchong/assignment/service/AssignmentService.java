package withoutc.chongchong.assignment.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import withoutc.chongchong.assignment.controller.dto.AssignmentAttachmentResponse;
import withoutc.chongchong.assignment.controller.dto.AssignmentCreateRequest;
import withoutc.chongchong.assignment.controller.dto.AssignmentCreateResponse;
import withoutc.chongchong.assignment.controller.dto.AssignmentDetailResponse;
import withoutc.chongchong.assignment.controller.dto.AssignmentListResponse;
import withoutc.chongchong.assignment.controller.dto.AssignmentSubmissionStatusResponse.CompleteMember;
import withoutc.chongchong.assignment.controller.dto.AssignmentSubmissionStatusResponse.IncompleteMember;
import withoutc.chongchong.assignment.controller.dto.AssignmentSubmissionStatusResponse;
import withoutc.chongchong.assignment.controller.dto.AssignmentSummaryResponse;
import withoutc.chongchong.assignment.controller.dto.AssignmentUpdateRequest;
import withoutc.chongchong.assignment.entity.Assignment;
import withoutc.chongchong.assignment.entity.AssignmentSort;
import withoutc.chongchong.assignment.entity.AssignmentAttachment;
import withoutc.chongchong.assignment.entity.AssignmentSubmission;
import withoutc.chongchong.assignment.entity.SubmissionStatus;
import withoutc.chongchong.assignment.entity.SubmissionTarget;
import withoutc.chongchong.assignment.exception.AssignmentErrorCode;
import withoutc.chongchong.assignment.exception.AssignmentException;
import withoutc.chongchong.assignment.policy.AssignmentAccessPolicy;
import withoutc.chongchong.assignment.repository.AssignmentRepository;
import withoutc.chongchong.assignment.repository.AssignmentSubmissionRepository;
import withoutc.chongchong.assignment.repository.projection.AssignmentSubmitterStatusProjection;
import withoutc.chongchong.comment.repository.CommentRepository;
import withoutc.chongchong.file.entity.FileMetadata;
import withoutc.chongchong.file.service.FileUrlGenerator;
import withoutc.chongchong.global.pagination.CursorPageRequest;
import withoutc.chongchong.global.pagination.CursorPageResponse;
import withoutc.chongchong.notification.entity.ResourceType;
import withoutc.chongchong.notification.service.NotificationService;
import withoutc.chongchong.study.entity.Study;
import withoutc.chongchong.study.entity.StudyMember;
import withoutc.chongchong.study.repository.StudyMemberRepository;
import withoutc.chongchong.study.repository.StudyRepository;
import withoutc.chongchong.study.repository.projection.StudyMemberIdentityProjection;
import withoutc.chongchong.user.repository.UserRepository;
import withoutc.chongchong.user.service.UserProfileImageUrlResolver;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AssignmentService {

    private final AssignmentRepository assignmentRepository;
    private final AssignmentSubmissionRepository assignmentSubmissionRepository;
    private final StudyMemberRepository studyMemberRepository;
    private final StudyRepository studyRepository;
    private final NotificationService notificationService;
    private final AssignmentAttachmentService assignmentAttachmentService;
    private final FileUrlGenerator fileUrlGenerator;
    private final UserRepository userRepository;
    private final CommentRepository commentRepository;

    private final Clock clock;
    private final AssignmentAccessPolicy assignmentAccessPolicy;
    private final UserProfileImageUrlResolver profileImageUrlResolver;

    @Transactional
    public AssignmentCreateResponse create(Long userId, Long studyId, AssignmentCreateRequest request) {
        studyMemberRepository.requireExistsByStudyIdAndUserId(studyId, userId);
        List<StudyMemberIdentityProjection> candidates = studyMemberRepository.findIdentitiesByStudyId(studyId);
        userRepository.findAllByIdInForShare(Stream.concat(Stream.of(userId), candidates.stream()
                .map(StudyMemberIdentityProjection::userId)).distinct().sorted().toList());
        StudyMember actor = studyMemberRepository.getByStudyIdAndUserIdOrThrow(studyId, userId);
        assignmentAccessPolicy.requireCanCreateAssignment(actor);
        Study study = studyRepository.getByIdForShareOrThrow(studyId);

        List<StudyMember> submitters = getSubmitters(studyId, request.submissionTarget(), request.submitterIds()).stream()
                .filter(member -> candidates.stream().anyMatch(candidate -> candidate.matches(member))).toList();

        LocalDateTime now = LocalDateTime.now(clock);
        Assignment assignment = Assignment.create(study, request.title(), request.content(), request.submissionMethod(),
                request.submissionTarget(), request.submissionVisibility(), request.closeAt(), now, submitters);
        assignment.addReminders(request.remindAts(), now);
        assignment.addAttachments(assignmentAttachmentService.prepare(userId, List.of(), request.attachmentFileIds()));

        assignmentRepository.save(assignment);
        notificationService.createAssignmentCreatedEventNotifications(assignment, submitters, userId);

        return AssignmentCreateResponse.from(assignment);
    }

    @Transactional
    public void update(Long userId, Long studyId, Long assignmentId, AssignmentUpdateRequest request) {
        studyMemberRepository.requireExistsByStudyIdAndUserId(studyId, userId);
        List<StudyMemberIdentityProjection> candidates = List.of();
        if (request.submitterIds() != null) {
            candidates = studyMemberRepository.findIdentitiesByStudyId(studyId);
            userRepository.findAllByIdInForShare(Stream.concat(Stream.of(userId), candidates.stream()
                    .map(StudyMemberIdentityProjection::userId)).distinct().sorted().toList());
        }
        StudyMember actor = studyMemberRepository.getByStudyIdAndUserIdOrThrow(studyId, userId);
        assignmentAccessPolicy.requireCanUpdateAssignment(actor);

        Assignment assignment = assignmentRepository.getByIdAndStudyIdForUpdateOrThrow(assignmentId, studyId);

        List<StudyMember> submitters = null;
        SubmissionTarget submissionTarget = request.submissionTarget();
        if (request.submitterIds() != null) {
            submitters = getSubmittersByIds(studyId, request.submitterIds());
            validateSubmitterCandidates(submitters, candidates);
            submissionTarget = null;
        }

        replaceAttachments(userId, assignment, request.attachmentFileIds());

        LocalDateTime now = LocalDateTime.now(clock);
        assignment.update(actor, request.title(), request.content(), request.submissionMethod(),
                submissionTarget,
                request.submissionVisibility(), request.closeAt(), request.remindAts(),
                now);

        if (submitters != null) {
            assignment.replaceSubmitters(submitters);
        }
        assignmentRepository.save(assignment);
    }

    @Transactional
    public void delete(Long userId, Long studyId, Long assignmentId) {
        studyMemberRepository.requireExistsByStudyIdAndUserId(studyId, userId);
        StudyMember actor = studyMemberRepository.getByStudyIdAndUserIdOrThrow(studyId, userId);
        assignmentAccessPolicy.requireCanDeleteAssignment(actor);

        Assignment assignment = assignmentRepository.getByIdAndStudyIdForUpdateOrThrow(assignmentId, studyId);
        assignmentSubmissionRepository.findAllByAssignmentIdForUpdate(assignmentId);
        commentRepository.deleteAllForAssignment(assignmentId);
        notificationService.deleteNotificationsForResource(ResourceType.ASSIGNMENT, assignmentId);
        assignmentRepository.delete(assignment);
    }

    public AssignmentSubmissionStatusResponse getAssignmentSubmissionStatus(Long userId, Long studyId,
                                                                            Long assignmentId) {
        StudyMember actor = studyMemberRepository.getByStudyIdAndUserIdOrThrow(studyId, userId);
        assignmentAccessPolicy.requireCanReadAssignmentSubmissionStatus(actor);

        Assignment assignment = assignmentRepository.getByIdAndStudyIdOrThrow(assignmentId, studyId);

        List<AssignmentSubmitterStatusProjection> statuses = assignmentSubmissionRepository.findAllSubmitterStatusesByAssignmentId(
                assignmentId);
        Map<Long, String> imageUrls = profileImageUrlResolver.resolveForMembers(
                statuses.stream().map(AssignmentSubmitterStatusProjection::memberId).toList());

        return AssignmentSubmissionStatusResponse.of(assignmentId, assignment.getNextRemindAt(),
                toCompleteMembers(statuses, imageUrls), toIncompleteMembers(statuses, imageUrls));
    }

    public AssignmentDetailResponse getDetail(Long userId, Long studyId, Long assignmentId) {
        studyMemberRepository.getByStudyIdAndUserIdOrThrow(studyId, userId);

        Assignment assignment = assignmentRepository.getByIdAndStudyIdOrThrow(assignmentId, studyId);

        List<AssignmentAttachmentResponse> attachments = createAttachmentResponses(assignment);
        return AssignmentDetailResponse.from(assignment, attachments, LocalDateTime.now(clock));
    }

    public AssignmentListResponse getList(Long userId, Long studyId, Long cursor, int size) {
        return getList(userId, studyId, cursor, size, AssignmentSort.LATEST);
    }

    public AssignmentListResponse getList(Long userId, Long studyId, Long cursor, int size, AssignmentSort sort) {
        StudyMember member = studyMemberRepository.getByStudyIdAndUserIdOrThrow(studyId, userId);

        CursorPageRequest pageRequest = CursorPageRequest.of(cursor, size);

        Pageable pageable = PageRequest.of(0, pageRequest.fetchSize());
        List<Assignment> assignments = findSortedAssignments(studyId, member.getId(), pageRequest.cursor(), pageable, sort);

        CursorPageResponse<Assignment> assignmentPage = CursorPageResponse.of(assignments, pageRequest,
                Assignment::getId);

        List<AssignmentSummaryResponse> summaries = createAssignmentSummaries(member, assignmentPage.content());
        return AssignmentListResponse.of(assignmentPage.nextCursor(), assignmentPage.hasNext(), summaries);
    }

    private List<Assignment> findSortedAssignments(Long studyId, Long memberId, Long cursor,
                                                   Pageable pageable, AssignmentSort sort) {
        if (sort == AssignmentSort.LATEST) {
            return assignmentRepository.findByCursor(studyId, cursor, pageable);
        }

        Assignment anchor = cursor == null ? null
                : assignmentRepository.getByIdAndStudyIdOrThrow(cursor, studyId);
        if (sort == AssignmentSort.DEADLINE) {
            LocalDateTime now = LocalDateTime.now(clock);
            LocalDateTime cursorCloseAt = anchor == null ? null : anchor.getCloseAt();
            int cursorRank = cursorCloseAt != null && cursorCloseAt.isBefore(now) ? 1 : 0;
            return assignmentRepository.findByDeadlineCursor(studyId, now, cursor, cursorRank, cursorCloseAt, pageable);
        }

        int cursorRank = anchor == null ? 0 : assignmentSubmissionRepository
                .findByAssignmentIdAndMemberId(cursor, memberId)
                .filter(submission -> submission.isAssigned() && submission.getSubmittedAt() == null)
                .map(submission -> 0).orElse(1);
        return assignmentRepository.findByUnsubmittedCursor(studyId, memberId, cursor, cursorRank, pageable);
    }

    private void replaceAttachments(Long userId, Assignment assignment, List<Long> fileIds) {
        if (fileIds == null) {
            return;
        }
        List<FileMetadata> files = assignmentAttachmentService.prepare(userId,
                assignment.getAttachments().stream().map(AssignmentAttachment::getFile).toList(), fileIds);
        assignment.clearAttachments();
        assignmentRepository.flush();
        assignment.addAttachments(files);
    }

    private List<AssignmentAttachmentResponse> createAttachmentResponses(Assignment assignment) {
        return assignment.getAttachments().stream()
                .map(attachment -> {
                    FileMetadata file = attachment.getFile();
                    String imageUrl = fileUrlGenerator.generateImageUrl(
                            file.getObjectKey(), file.getContentType());
                    String downloadUrl = fileUrlGenerator.generateDownloadUrl(
                            file.getObjectKey(), file.getOriginalFileName());
                    return AssignmentAttachmentResponse.of(file, attachment.getSortOrder(), imageUrl, downloadUrl);
                })
                .toList();
    }

    private List<CompleteMember> toCompleteMembers(List<AssignmentSubmitterStatusProjection> statuses,
                                                  Map<Long, String> imageUrls) {
        return statuses.stream()
                .filter(AssignmentSubmitterStatusProjection::isSubmitted)
                .map(status -> CompleteMember.of(status.memberId(), status.name(), imageUrls.get(status.memberId()),
                        status.submittedAt()))
                .toList();
    }

    private List<IncompleteMember> toIncompleteMembers(List<AssignmentSubmitterStatusProjection> statuses,
                                                      Map<Long, String> imageUrls) {
        return statuses.stream()
                .filter(status -> !status.isSubmitted())
                .map(status -> IncompleteMember.of(status.memberId(), status.name(), imageUrls.get(status.memberId()),
                        status.lastRemindAt()))
                .toList();
    }

    private List<StudyMember> getSubmitters(Long studyId, SubmissionTarget submissionTarget,
                                           List<Long> submitterIds) {
        if (submitterIds != null) {
            return getSubmittersByIds(studyId, submitterIds);
        }
        if (submissionTarget == null) {
            throw new AssignmentException(AssignmentErrorCode.INVALID_SUBMITTER_IDS);
        }
        if (submissionTarget.requiresLeaderSubmission()) {
            return studyMemberRepository.findAllByStudyId(studyId);
        }
        return studyMemberRepository.findAllByStudyId(studyId).stream()
                .filter(studyMember -> !studyMember.isLeader()).toList();
    }

    private List<StudyMember> getSubmittersByIds(Long studyId, List<Long> submitterIds) {
        List<StudyMember> members = studyMemberRepository.findAllByStudyId(studyId).stream()
                .filter(member -> submitterIds.contains(member.getId())).toList();
        if (submitterIds.isEmpty() || new HashSet<>(submitterIds).size() != submitterIds.size()
                || members.size() != submitterIds.size()) {
            throw new AssignmentException(AssignmentErrorCode.INVALID_SUBMITTER_IDS);
        }
        return members;
    }

    private void validateSubmitterCandidates(List<StudyMember> submitters,
                                             List<StudyMemberIdentityProjection> candidates) {
        if (submitters.stream().anyMatch(member -> candidates.stream()
                .noneMatch(candidate -> candidate.matches(member)))) {
            throw new AssignmentException(AssignmentErrorCode.INVALID_SUBMITTER_IDS);
        }
    }

    private SubmissionStatus getAssignmentListSubmissionStatus(AssignmentSubmission submission, LocalDateTime now) {
        if (!submission.isAssigned()) {
            return SubmissionStatus.NOT_ASSIGNED;
        }
        return submission.submissionStatus(now);
    }

    private List<AssignmentSummaryResponse> createAssignmentSummaries(StudyMember member,
                                                                      List<Assignment> assignments) {

        if (assignments.isEmpty()) {
            return List.of();
        }

        LocalDateTime now = LocalDateTime.now(clock);
        List<Long> assignmentIds = assignments.stream().map(Assignment::getId).toList();
        Map<Long, SubmissionStatus> submissionStatusByAssignmentId = assignmentSubmissionRepository
                .findAllByAssignmentIdInAndMemberId(assignmentIds, member.getId())
                .stream().collect(Collectors.toMap(submission -> submission.getAssignment().getId(),
                        submission -> getAssignmentListSubmissionStatus(submission, now)));

        if (member.isLeader()) {
            return assignments.stream().map(assignment ->
                            AssignmentSummaryResponse.forLeader(now, assignment,
                                    submissionStatusByAssignmentId.getOrDefault(assignment.getId(),
                                            SubmissionStatus.NOT_ASSIGNED)))
                    .toList();
        }

        return assignments.stream().map(assignment -> AssignmentSummaryResponse.forMember(assignment,
                        submissionStatusByAssignmentId.getOrDefault(assignment.getId(), SubmissionStatus.NOT_ASSIGNED)))
                .toList();
    }
}
