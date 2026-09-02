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
import withoutc.chongchong.assignment.controller.dto.AssignmentSummaryResponse;
import withoutc.chongchong.assignment.controller.dto.AssignmentUpdateRequest;
import withoutc.chongchong.assignment.entity.Assignment;
import withoutc.chongchong.assignment.exception.AssignmentErrorCode;
import withoutc.chongchong.assignment.exception.AssignmentException;
import withoutc.chongchong.assignment.policy.AssignmentAccessPolicy;
import withoutc.chongchong.assignment.repository.AssignmentSubmissionRepository;
import withoutc.chongchong.assignment.repository.AssignmentRepository;
import withoutc.chongchong.assignment.repository.projection.AssignmentSubmissionStatusProjection;
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
    private final AssignmentAccessPolicy assignmentAccessPolicy;

    @Transactional
    public AssignmentCreateResponse create(Long userId, Long studyId, AssignmentCreateRequest request) {
        // 해당 유저가 해당 스터디의 리더가 맞는지 user - study
        StudyMember actor = studyMemberRepository.getByStudyIdAndUserIdOrThrow(studyId, userId);
        assignmentAccessPolicy.requireCanCreateAssignment(actor);

        // TODO V2에서 리더에게도 과제를 생성하도록 수정 필요
        List<StudyMember> members = studyMemberRepository.findAllByStudyId(studyId).stream()
                .filter(studyMember -> !studyMember.isLeader()).toList();

        LocalDateTime now = LocalDateTime.now(clock);
        Assignment assignment = Assignment.create(actor, request.title(), request.content(),
                request.submissionMethod(), request.closeAt(), now);
        assignment.addReminders(request.remindAts(), now);
        // TODO 과제 제출물이 현재는 생성 시점 이전에 가입한 멤버에게만 생성(신규 가입자에게는 보이지 않음) 논의 필요
        assignment.initializeSubmissions(members);

        assignmentRepository.save(assignment);

        return AssignmentCreateResponse.from(assignment);
    }

    @Transactional
    public void update(Long userId, Long studyId, Long assignmentId, AssignmentUpdateRequest request) {
        // 해당 유저가 해당 스터디의 리더가 맞는지 user - study
        StudyMember actor = studyMemberRepository.getByStudyIdAndUserIdOrThrow(studyId, userId);
        assignmentAccessPolicy.requireCanUpdateAssignment(actor);

        // 해당 과제가 해당 스터디의 것이 맞는지 assignment - study
        Assignment assignment = assignmentRepository.getByIdAndStudyIdOrThrow(assignmentId, studyId);

        LocalDateTime now = LocalDateTime.now(clock);
        assignment.update(request.title(), request.content(), request.submissionMethod(), request.closeAt(),
                request.remindAts(), now);

        assignmentRepository.save(assignment);
    }

    @Transactional
    public void delete(Long userId, Long studyId, Long assignmentId) {
        // 해당 유저가 해당 스터디의 리더가 맞는지 user - study
        StudyMember actor = studyMemberRepository.getByStudyIdAndUserIdOrThrow(studyId, userId);
        assignmentAccessPolicy.requireCanDeleteAssignment(actor);

        // 해당 과제가 해당 스터디의 것이 맞는지 assignment - study
        Assignment assignment = assignmentRepository.getByIdAndStudyIdOrThrow(assignmentId, studyId);

        assignmentRepository.delete(assignment);
    }

    public AssignmentDetailResponse getDetail(Long userId, Long studyId, Long assignmentId) {
        // 해당 유저가 해당 스터디의 소속이 맞는지 user - study
        studyMemberRepository.getByStudyIdAndUserIdOrThrow(studyId, userId);

        // 해당 과제가 해당 스터디의 것이 맞는지 assignment - study
        Assignment assignment = assignmentRepository.getByIdAndStudyIdOrThrow(assignmentId, studyId);

        return AssignmentDetailResponse.from(assignment);
    }

    public AssignmentListResponse getList(Long userId, Long studyId, Long cursor, int size) {
        // 해당 유저가 해당 스터디의 소속이 맞는지 user - study
        StudyMember member = studyMemberRepository.getByStudyIdAndUserIdOrThrow(studyId, userId);

        CursorPageRequest pageRequest = CursorPageRequest.of(cursor, size);

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
}
