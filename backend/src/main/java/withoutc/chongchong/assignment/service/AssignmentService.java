package withoutc.chongchong.assignment.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
import withoutc.chongchong.assignment.controller.dto.AssignmentSummaryResponse;
import withoutc.chongchong.assignment.controller.dto.AssignmentUpdateRequest;
import withoutc.chongchong.assignment.entity.Assignment;
import withoutc.chongchong.assignment.exception.AssignmentErrorCode;
import withoutc.chongchong.assignment.exception.AssignmentException;
import withoutc.chongchong.assignment.repository.AssignmentRecipientRepository;
import withoutc.chongchong.assignment.repository.AssignmentRepository;
import withoutc.chongchong.assignment.repository.projection.AssignmentSubmitStatusProjection;
import withoutc.chongchong.auth.exception.AuthErrorCode;
import withoutc.chongchong.auth.exception.AuthException;
import withoutc.chongchong.global.pagination.CursorPageRequest;
import withoutc.chongchong.global.pagination.CursorPageResponse;
import withoutc.chongchong.study.entity.StudyMember;
import withoutc.chongchong.study.repository.StudyMemberRepository;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AssignmentService {

    private final AssignmentRepository assignmentRepository;
    private final AssignmentRecipientRepository assignmentRecipientRepository;
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

    public AssignmentDetailResponse getDetail(Long userId, Long studyId, Long noticeId) {
        studyMemberRepository.getByStudyIdAndUserIdOrThrow(studyId, userId);

        Assignment assignment = assignmentRepository.getByIdOrThrow(noticeId);
        validateAssignmentBelongsToStudy(studyId, assignment);

        return AssignmentDetailResponse.from(assignment);
    }

    public AssignmentListResponse getList(Long userId, Long studyId, Long cursor, int size) {
        CursorPageRequest pageRequest = CursorPageRequest.of(cursor, size);
        StudyMember member = studyMemberRepository.getByStudyIdAndUserIdOrThrow(studyId, userId);

        Pageable pageable = PageRequest.of(0, pageRequest.fetchSize());
        List<Assignment> assignments = assignmentRepository.findByCursor(studyId, pageRequest.cursor(), pageable);

        CursorPageResponse<Assignment> assignmentPage = CursorPageResponse.of(assignments, pageRequest,
                Assignment::getId);

        List<AssignmentSummaryResponse> assignmentSummaries = createAssignmentSummaries(member,
                assignmentPage.content());
        return AssignmentListResponse.of(assignmentPage.nextCursor(), assignmentPage.hasNext(), assignmentSummaries);
    }

    private List<AssignmentSummaryResponse> createAssignmentSummaries(StudyMember member,
                                                                      List<Assignment> assignments) {
        if (member.isLeader()) {
            return assignments.stream().map(AssignmentSummaryResponse::forLeader).toList();
        }

        if (assignments.isEmpty()) {
            return List.of();
        }

        List<Long> assignmentIds = assignments.stream().map(Assignment
                ::getId).toList();

        Map<Long, Boolean> submitStatusByAssignmentId = assignmentRecipientRepository.findSubmitStatusesByAssignmentIdsAndMemberId(
                        assignmentIds,
                        member.getId())
                .stream().collect(Collectors.toMap(AssignmentSubmitStatusProjection::assignmentId,
                        AssignmentSubmitStatusProjection::isSubmit));

        return assignments.stream().map(assignment -> AssignmentSummaryResponse.forMember(assignment,
                getSubmitStatus(submitStatusByAssignmentId, assignment.getId()))).toList();
    }

    private boolean getSubmitStatus(Map<Long, Boolean> submitStatusByAssignmentId, Long assignmentId) {
        Boolean isSubmit = submitStatusByAssignmentId.get(assignmentId);
        if (isSubmit == null) {
            throw new AssignmentException(AssignmentErrorCode.ASSIGNMENT_RECIPIENT_NOT_FOUND);
        }
        return isSubmit;
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
