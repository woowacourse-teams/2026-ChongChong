package withoutc.chongchong.assignment.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import withoutc.chongchong.assignment.controller.dto.AssignmentRecurrenceCreateRequest;
import withoutc.chongchong.assignment.controller.dto.AssignmentRecurrenceCreateResponse;
import withoutc.chongchong.assignment.controller.dto.AssignmentRecurrenceListResponse;
import withoutc.chongchong.assignment.controller.dto.AssignmentRecurrenceResponse;
import withoutc.chongchong.assignment.controller.dto.AssignmentRecurrenceUpdateRequest;
import withoutc.chongchong.assignment.entity.Assignment;
import withoutc.chongchong.assignment.entity.AssignmentRecurrence;
import withoutc.chongchong.assignment.entity.SubmissionTarget;
import withoutc.chongchong.assignment.entity.SubmitterMode;
import withoutc.chongchong.assignment.exception.AssignmentErrorCode;
import withoutc.chongchong.assignment.exception.AssignmentException;
import withoutc.chongchong.assignment.policy.AssignmentAccessPolicy;
import withoutc.chongchong.assignment.repository.AssignmentRecurrenceRepository;
import withoutc.chongchong.study.entity.StudyMember;
import withoutc.chongchong.study.repository.StudyMemberRepository;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AssignmentRecurrenceService {

    private final AssignmentRecurrenceRepository assignmentRecurrenceRepository;
    private final StudyMemberRepository studyMemberRepository;
    private final AssignmentService assignmentService;
    private final AssignmentAccessPolicy assignmentAccessPolicy;

    // 첫 회차는 일반 과제 생성과 같은 경로(권한·잠금·알림)로 만들고, 그 과제를 템플릿으로 규칙을 저장한다.
    @Transactional
    public AssignmentRecurrenceCreateResponse create(Long userId, Long studyId,
                                                     AssignmentRecurrenceCreateRequest request) {
        SubmitterMode submitterMode = submitterModeOf(request.submissionTarget(), request.submitterIds());
        Assignment firstOccurrence = assignmentService.createAssignment(userId, studyId,
                request.toFirstOccurrenceRequest());
        AssignmentRecurrence recurrence = assignmentRecurrenceRepository.save(
                AssignmentRecurrence.startWith(firstOccurrence, request.intervalWeeks(), submitterMode));
        firstOccurrence.linkRecurrence(recurrence.getId());

        return new AssignmentRecurrenceCreateResponse(recurrence.getId(), firstOccurrence.getId());
    }

    public AssignmentRecurrenceListResponse getList(Long userId, Long studyId) {
        requireLeader(userId, studyId);
        List<AssignmentRecurrenceResponse> recurrences = assignmentRecurrenceRepository
                .findAllByStudyIdOrderByIdDesc(studyId).stream()
                .map(AssignmentRecurrenceResponse::from)
                .toList();

        return new AssignmentRecurrenceListResponse(recurrences);
    }

    @Transactional
    public void update(Long userId, Long studyId, Long recurrenceId, AssignmentRecurrenceUpdateRequest request) {
        requireLeader(userId, studyId);
        AssignmentRecurrence recurrence = assignmentRecurrenceRepository
                .getByIdAndStudyIdForUpdateOrThrow(recurrenceId, studyId);

        recurrence.changeTemplate(request.title(), request.content(), request.submissionMethod());
        recurrence.changeSubmissionVisibility(request.submissionVisibility());
        changeSubmitters(studyId, recurrence, request);
        recurrence.changeSchedule(request.nextCloseAt(), request.intervalWeeks(), request.remindAts());
    }

    // 이미 생성된 회차는 남기고 규칙만 지운다. 회차의 recurrence_id는 DB가 NULL로 바꾼다.
    @Transactional
    public void delete(Long userId, Long studyId, Long recurrenceId) {
        requireLeader(userId, studyId);
        AssignmentRecurrence recurrence = assignmentRecurrenceRepository
                .getByIdAndStudyIdForUpdateOrThrow(recurrenceId, studyId);

        assignmentRecurrenceRepository.delete(recurrence);
    }

    private void requireLeader(Long userId, Long studyId) {
        StudyMember actor = studyMemberRepository.getByStudyIdAndUserIdOrThrow(studyId, userId);
        assignmentAccessPolicy.requireCanManageAssignmentRecurrence(actor);
    }

    private SubmitterMode submitterModeOf(SubmissionTarget submissionTarget, List<Long> submitterIds) {
        if ((submissionTarget == null) == (submitterIds == null)) {
            throw new AssignmentException(AssignmentErrorCode.INVALID_SUBMITTER_CHOICE);
        }
        if (submitterIds != null) {
            return SubmitterMode.SELECTED;
        }
        return SubmitterMode.RANGE;
    }

    private void changeSubmitters(Long studyId, AssignmentRecurrence recurrence,
                                  AssignmentRecurrenceUpdateRequest request) {
        if (request.submissionTarget() == null && request.submitterIds() == null) {
            return;
        }
        if (submitterModeOf(request.submissionTarget(), request.submitterIds()) == SubmitterMode.RANGE) {
            recurrence.useRange(request.submissionTarget());
            return;
        }
        validateStudyMembers(studyId, request.submitterIds());
        recurrence.selectSubmitters(request.submitterIds());
    }

    private void validateStudyMembers(Long studyId, List<Long> submitterIds) {
        Set<Long> memberIds = studyMemberRepository.findAllByStudyId(studyId).stream()
                .map(StudyMember::getId)
                .collect(Collectors.toSet());
        if (new HashSet<>(submitterIds).size() != submitterIds.size() || !memberIds.containsAll(submitterIds)) {
            throw new AssignmentException(AssignmentErrorCode.INVALID_SUBMITTER_IDS);
        }
    }
}
