package withoutc.chongchong.assignment.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import withoutc.chongchong.assignment.controller.dto.AssignmentCreateRequest;
import withoutc.chongchong.assignment.controller.dto.AssignmentCreateResponse;
import withoutc.chongchong.assignment.controller.dto.AssignmentDetailResponse;
import withoutc.chongchong.assignment.controller.dto.AssignmentListResponse;
import withoutc.chongchong.assignment.controller.dto.AssignmentSubmissionStatusResponse;
import withoutc.chongchong.assignment.controller.dto.AssignmentSummaryResponse;
import withoutc.chongchong.assignment.controller.dto.AssignmentUpdateRequest;
import withoutc.chongchong.assignment.entity.Assignment;
import withoutc.chongchong.assignment.entity.SubmissionStatus;
import withoutc.chongchong.assignment.entity.SubmissionTarget;
import withoutc.chongchong.assignment.policy.AssignmentAccessPolicy;
import withoutc.chongchong.assignment.repository.AssignmentRepository;
import withoutc.chongchong.assignment.repository.AssignmentSubmissionRepository;
import withoutc.chongchong.assignment.repository.projection.AssignmentSubmissionStatusProjection;
import withoutc.chongchong.assignment.repository.projection.AssignmentSubmitterStatusProjection;
import withoutc.chongchong.global.pagination.CursorPageRequest;
import withoutc.chongchong.global.pagination.CursorPageResponse;
import withoutc.chongchong.study.entity.Study;
import withoutc.chongchong.study.entity.StudyMember;
import withoutc.chongchong.study.repository.StudyMemberRepository;
import withoutc.chongchong.study.repository.StudyRepository;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AssignmentService {

    private final AssignmentRepository assignmentRepository;
    private final AssignmentSubmissionRepository assignmentSubmissionRepository;
    private final StudyMemberRepository studyMemberRepository;
    private final StudyRepository studyRepository;

    private final Clock clock;
    private final AssignmentAccessPolicy assignmentAccessPolicy;

    @Transactional
    public AssignmentCreateResponse create(Long userId, Long studyId, AssignmentCreateRequest request) {
        StudyMember actor = studyMemberRepository.getByStudyIdAndUserIdOrThrow(studyId, userId);
        assignmentAccessPolicy.requireCanCreateAssignment(actor);

        List<StudyMember> submitters = getSubmitters(studyId, request.submissionTarget());

        Study study = studyRepository.getByIdOrThrow(studyId);

        LocalDateTime now = LocalDateTime.now(clock);
        Assignment assignment = Assignment.create(study, request.title(), request.content(),
                request.submissionMethod(), request.submissionTarget(), request.closeAt(), now);
        assignment.addReminders(request.remindAts(), now);

        assignment.initializeSubmissions(submitters);

        assignmentRepository.save(assignment);

        return AssignmentCreateResponse.from(assignment);
    }

    @Transactional
    public void update(Long userId, Long studyId, Long assignmentId, AssignmentUpdateRequest request) {
        StudyMember actor = studyMemberRepository.getByStudyIdAndUserIdOrThrow(studyId, userId);
        assignmentAccessPolicy.requireCanUpdateAssignment(actor);

        Assignment assignment = assignmentRepository.getByIdAndStudyIdOrThrow(assignmentId, studyId);

        LocalDateTime now = LocalDateTime.now(clock);
        assignment.update(actor, request.title(), request.content(), request.submissionMethod(),
                request.submissionTarget(), request.closeAt(),
                request.remindAts(), now);

        assignmentRepository.save(assignment);
    }

    @Transactional
    public void delete(Long userId, Long studyId, Long assignmentId) {
        StudyMember actor = studyMemberRepository.getByStudyIdAndUserIdOrThrow(studyId, userId);
        assignmentAccessPolicy.requireCanDeleteAssignment(actor);

        Assignment assignment = assignmentRepository.getByIdAndStudyIdOrThrow(assignmentId, studyId);

        assignmentRepository.delete(assignment);
    }

    public AssignmentSubmissionStatusResponse getAssignmentSubmissionStatus(Long userId, Long studyId,
                                                                            Long assignmentId) {
        StudyMember actor = studyMemberRepository.getByStudyIdAndUserIdOrThrow(studyId, userId);
        assignmentAccessPolicy.requireCanReadAssignmentSubmissionStatus(actor);

        Assignment assignment = assignmentRepository.getByIdAndStudyIdOrThrow(assignmentId, studyId);

        List<AssignmentSubmitterStatusProjection> statuses = assignmentSubmissionRepository
                .findAllSubmitterStatusesByAssignmentId(assignmentId);

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

    public AssignmentDetailResponse getDetail(Long userId, Long studyId, Long assignmentId) {
        studyMemberRepository.getByStudyIdAndUserIdOrThrow(studyId, userId);

        Assignment assignment = assignmentRepository.getByIdAndStudyIdOrThrow(assignmentId, studyId);

        return AssignmentDetailResponse.from(assignment);
    }

    public AssignmentListResponse getList(Long userId, Long studyId, Long cursor, int size) {
        StudyMember member = studyMemberRepository.getByStudyIdAndUserIdOrThrow(studyId, userId);

        CursorPageRequest pageRequest = CursorPageRequest.of(cursor, size);

        Pageable pageable = PageRequest.of(0, pageRequest.fetchSize());
        List<Assignment> assignments = assignmentRepository.findByCursor(studyId, pageRequest.cursor(), pageable);

        CursorPageResponse<Assignment> assignmentPage = CursorPageResponse.of(assignments, pageRequest,
                Assignment::getId);

        List<AssignmentSummaryResponse> summaries = createAssignmentSummaries(member, assignmentPage.content());
        return AssignmentListResponse.of(assignmentPage.nextCursor(), assignmentPage.hasNext(), summaries);
    }

    private List<StudyMember> getSubmitters(Long studyId, SubmissionTarget submissionTarget) {
        if (submissionTarget.requiresLeaderSubmission()) {
            return studyMemberRepository.findAllByStudyId(studyId);
        }
        return studyMemberRepository.findAllByStudyId(studyId).stream()
                .filter(studyMember -> !studyMember.isLeader()).toList();
    }

    private List<AssignmentSummaryResponse> createAssignmentSummaries(StudyMember member,
                                                                      List<Assignment> assignments) {

        if (assignments.isEmpty()) {
            return List.of();
        }

        List<Long> assignmentIds = assignments.stream().map(Assignment::getId).toList();
        Map<Long, SubmissionStatus> submissionStatusByAssignmentId = assignmentSubmissionRepository
                .findMySubmissionStatusesByAssignmentIdsAndMemberId(assignmentIds, member.getId())
                .stream().collect(Collectors.toMap(AssignmentSubmissionStatusProjection::assignmentId,
                        AssignmentSubmissionStatusProjection::submissionStatus));

        if (member.isLeader()) {
            return assignments.stream().map(assignment -> AssignmentSummaryResponse.forLeader(assignment,
                            submissionStatusByAssignmentId.getOrDefault(assignment.getId(), SubmissionStatus.NOT_ASSIGNED)))
                    .toList();
        }

        return assignments.stream().map(assignment -> AssignmentSummaryResponse.forMember(assignment,
                        submissionStatusByAssignmentId.getOrDefault(assignment.getId(), SubmissionStatus.NOT_ASSIGNED)))
                .toList();
    }
}
