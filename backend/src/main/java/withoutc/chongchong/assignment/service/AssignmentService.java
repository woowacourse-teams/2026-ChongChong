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
import withoutc.chongchong.assignment.repository.AssignmentSubmissionRepository;
import withoutc.chongchong.assignment.repository.AssignmentRepository;
import withoutc.chongchong.assignment.repository.projection.AssignmentSubmissionStatusProjection;
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
    private final AssignmentSubmissionRepository assignmentSubmissionRepository;
    private final StudyMemberRepository studyMemberRepository;

    private final Clock clock;

    @Transactional
    public AssignmentCreateResponse create(Long userId, Long studyId, AssignmentCreateRequest request) {
        validateLeader(studyId, userId);

        List<StudyMember> members = studyMemberRepository.findAllByStudyId(studyId).stream()
                .filter(studyMember -> !studyMember.isLeader()).toList();

        StudyMember writer = studyMemberRepository.getByStudyIdAndUserIdOrThrow(studyId, userId);

        LocalDateTime now = LocalDateTime.now(clock);
        Assignment assignment = Assignment.create(writer, request.title(), request.content(),
                request.submissionMethod(), request.closeAt(), now);
        assignment.addReminders(request.remindAts(), now);
        assignment.initializeSubmissions(members);

        assignmentRepository.save(assignment);

        return AssignmentCreateResponse.from(assignment);
    }

    @Transactional
    public void update(Long userId, Long studyId, Long assignmentId, AssignmentUpdateRequest request) {
        validateLeader(studyId, userId);

        Assignment assignment = assignmentRepository.getByIdOrThrow(assignmentId);
        validateAssignmentBelongsToStudy(studyId, assignment);

        LocalDateTime now = LocalDateTime.now(clock);
        assignment.update(request.title(), request.content(), request.submissionMethod(), request.closeAt(),
                request.remindAts(), now);

        assignmentRepository.save(assignment);
    }

    @Transactional
    public void delete(Long userId, Long studyId, Long assignmentId) {
        validateLeader(studyId, userId);

        Assignment assignment = assignmentRepository.getByIdOrThrow(assignmentId);
        validateAssignmentBelongsToStudy(studyId, assignment);

        assignmentRepository.delete(assignment);
    }

    public AssignmentDetailResponse getDetail(Long userId, Long studyId, Long assignmentId) {
        studyMemberRepository.getByStudyIdAndUserIdOrThrow(studyId, userId);

        Assignment assignment = assignmentRepository.getByIdOrThrow(assignmentId);
        validateAssignmentBelongsToStudy(studyId, assignment);

        return AssignmentDetailResponse.from(assignment);
    }

    public AssignmentListResponse getList(Long userId, Long studyId, Long cursor, int size) {
        CursorPageRequest pageRequest = CursorPageRequest.of(cursor, size);
        StudyMember member = studyMemberRepository.getByStudyIdAndUserIdOrThrow(studyId, userId);

        Pageable pageable = PageRequest.of(0, pageRequest.fetchSize());
        List<Assignment> assignments;
        if (member.isLeader()) {
            assignments = assignmentRepository.findByCursor(studyId, pageRequest.cursor(), pageable);
        } else {
            assignments = assignmentRepository.findByCursorAndMemberId(
                    studyId,
                    member.getId(),
                    pageRequest.cursor(),
                    pageable
            );
        }

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

        List<Long> assignmentIds = assignments.stream().map(Assignment::getId).toList();

        Map<Long, Boolean> submissionStatusByAssignmentId = assignmentSubmissionRepository
                .findMySubmissionStatusesByAssignmentIdsAndMemberId(assignmentIds, member.getId())
                .stream().collect(Collectors.toMap(AssignmentSubmissionStatusProjection::assignmentId,
                        AssignmentSubmissionStatusProjection::submitted));

        return assignments.stream().map(assignment -> AssignmentSummaryResponse.forMember(assignment,
                requireSubmissionStatus(submissionStatusByAssignmentId, assignment.getId()))).toList();
    }

    private boolean requireSubmissionStatus(Map<Long, Boolean> submissionStatusByAssignmentId, Long assignmentId) {
        Boolean submitted = submissionStatusByAssignmentId.get(assignmentId);
        if (submitted == null) {
            throw new AssignmentException(AssignmentErrorCode.ASSIGNMENT_SUBMISSION_NOT_FOUND);
        }
        return submitted;
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
