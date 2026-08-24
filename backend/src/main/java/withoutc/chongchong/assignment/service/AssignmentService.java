package withoutc.chongchong.assignment.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import withoutc.chongchong.assignment.controller.dto.AssignmentCreateRequest;
import withoutc.chongchong.assignment.controller.dto.AssignmentCreateResponse;
import withoutc.chongchong.assignment.entity.Assignment;
import withoutc.chongchong.assignment.repository.AssignmentRepository;
import withoutc.chongchong.auth.exception.AuthErrorCode;
import withoutc.chongchong.auth.exception.AuthException;
import withoutc.chongchong.study.entity.StudyMember;
import withoutc.chongchong.study.repository.StudyMemberRepository;
import withoutc.chongchong.study.repository.StudyRepository;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AssignmentService {

    private final AssignmentRepository assignmentRepository;
    private final StudyMemberRepository studyMemberRepository;

    private final Clock clock;

    @Transactional
    public AssignmentCreateResponse create(Long userId, Long studyId, AssignmentCreateRequest request) {
        validateLeader(studyId, userId);

        List<StudyMember> members = studyMemberRepository.findAllByStudyId(studyId).stream()
                .filter(studyMember -> !studyMember.isLeader()).toList();

        StudyMember writer = studyMemberRepository.getByStudyIdAndUserIdOrThrow(studyId, userId);

        Assignment assignment = Assignment.create(writer, request.title(), request.content(),
                request.submissionMethod(), request.closeAt());
        LocalDateTime now = LocalDateTime.now(clock);
        assignment.addReminders(request.remindAts(), now);
        assignment.addRecipients(members);

        assignmentRepository.save(assignment);

        return AssignmentCreateResponse.from(assignment);
    }


    private void validateLeader(Long studyId, Long userId) {
        StudyMember member = studyMemberRepository.getByStudyIdAndUserIdOrThrow(studyId, userId);
        if (!member.isLeader()) {
            throw new AuthException(AuthErrorCode.ACCESS_DENIED);
        }
    }
}
