package withoutc.chongchong.study.service;

import static withoutc.chongchong.study.entity.Study.MAX_STUDY_MEMBER_COUNT;
import static withoutc.chongchong.study.service.StudyService.MAX_JOINED_STUDY_COUNT;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import withoutc.chongchong.assignment.repository.AssignmentRepository;
import withoutc.chongchong.assignment.repository.AssignmentSubmissionRepository;
import withoutc.chongchong.notice.repository.NoticeRepository;
import withoutc.chongchong.comment.repository.CommentRepository;
import withoutc.chongchong.notification.service.NotificationService;
import withoutc.chongchong.study.controller.dto.StudyJoinRequest;
import withoutc.chongchong.study.controller.dto.StudyMemberJoinResponse;
import withoutc.chongchong.study.controller.dto.StudyMembersResponse;
import withoutc.chongchong.study.entity.Study;
import withoutc.chongchong.study.entity.StudyMember;
import withoutc.chongchong.study.entity.StudyMemberRole;
import withoutc.chongchong.study.exception.StudyErrorCode;
import withoutc.chongchong.study.exception.StudyException;
import withoutc.chongchong.study.exception.StudyMemberErrorCode;
import withoutc.chongchong.study.exception.StudyMemberException;
import withoutc.chongchong.study.repository.StudyMemberRepository;
import withoutc.chongchong.study.repository.StudyRepository;
import withoutc.chongchong.study.token.StudyInviteTokenProvider;
import withoutc.chongchong.user.entity.User;
import withoutc.chongchong.user.repository.UserRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudyMemberService {

    private final StudyMemberRepository studyMemberRepository;
    private final StudyRepository studyRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final CommentRepository commentRepository;
    private final NoticeRepository noticeRepository;
    private final AssignmentRepository assignmentRepository;
    private final AssignmentSubmissionRepository assignmentSubmissionRepository;

    private final StudyInviteTokenProvider studyInviteTokenProvider;

    @Transactional
    public StudyMemberJoinResponse join(Long userId, StudyJoinRequest request) {
        User user = userRepository.getByIdForUpdateOrThrow(userId);
        validateStudyCountLimit(userId);

        Long studyId = resolveStudyId(request);
        Study study = studyRepository.getByIdForUpdateOrThrow(studyId);
        if (!study.isPublic() && request.token() == null) {
            throw new StudyException(StudyErrorCode.STUDY_NOT_PUBLIC);
        }

        validateJoin(study.getId(), user.getId());

        StudyMember studyMember = StudyMember.create(study, user, user.getName(), user.getProfileImageUrl(),
                StudyMemberRole.MEMBER);

        return StudyMemberJoinResponse.from(studyMemberRepository.save(studyMember));
    }

    private Long resolveStudyId(StudyJoinRequest request) {
        if (request.token() == null) {
            if (request.studyId() == null) {
                throw new StudyException(StudyErrorCode.INVALID_STUDY_ID);
            }
            return request.studyId();
        }

        Long invitedStudyId = studyInviteTokenProvider.verifyAndExtractStudyId(request.token());
        if (request.studyId() != null && !request.studyId().equals(invitedStudyId)) {
            throw new StudyException(StudyErrorCode.INVALID_INVITE_TOKEN);
        }
        return invitedStudyId;
    }

    public StudyMembersResponse getAllStudyMembers(Long userId, Long studyId) {
        studyRepository.getByIdOrThrow(studyId);
        studyMemberRepository.getByStudyIdAndUserIdOrThrow(studyId, userId);
        return StudyMembersResponse.from(MAX_STUDY_MEMBER_COUNT,
                studyMemberRepository.findAllSummariesByStudyId(studyId));
    }

    @Transactional
    public void expel(Long userId, Long studyId, Long memberId) {
        studyMemberRepository.requireExistsByStudyIdAndUserId(studyId, userId);
        StudyMember requester = studyMemberRepository.getByStudyIdAndUserIdOrThrow(studyId, userId);
        validateLeader(requester);

        Long targetUserId = studyMemberRepository.getUserIdByStudyIdAndIdOrThrow(studyId, memberId);
        userRepository.getByIdForUpdateOrThrow(targetUserId);
        lockMemberResources(memberId);
        StudyMember target = studyMemberRepository.getByStudyIdAndIdOrThrow(studyId, memberId);
        if (target.isLeader()) {
            throw new StudyMemberException(StudyMemberErrorCode.STUDY_LEADER_CANNOT_BE_REMOVED);
        }
        commentRepository.deleteAllForMemberSubmissions(memberId);
        notificationService.deleteNotificationsForStudyMember(memberId);
        studyMemberRepository.delete(target);
    }

    @Transactional
    public void leave(Long userId, Long studyId) {
        studyMemberRepository.requireExistsByStudyIdAndUserId(studyId, userId);
        userRepository.getByIdForUpdateOrThrow(userId);
        Long memberId = studyMemberRepository.getIdByStudyIdAndUserIdOrThrow(studyId, userId);
        lockMemberResources(memberId);
        StudyMember member = studyMemberRepository.getByStudyIdAndUserIdOrThrow(studyId, userId);
        if (member.isLeader()) {
            throw new StudyMemberException(StudyMemberErrorCode.STUDY_LEADER_CANNOT_LEAVE);
        }

        commentRepository.deleteAllForMemberSubmissions(member.getId());
        notificationService.deleteNotificationsForStudyMember(member.getId());
        studyMemberRepository.delete(member);
    }

    private void lockMemberResources(Long memberId) {
        noticeRepository.findAllByCommentAuthorMemberIdForUpdate(memberId);
        assignmentRepository.findAllAffectedByMemberIdForUpdate(memberId);
        assignmentSubmissionRepository.findAllByMemberIdForUpdate(memberId);
    }

    private void validateStudyCountLimit(Long userId) {
        if (studyMemberRepository.countByUserId(userId) >= MAX_JOINED_STUDY_COUNT) {
            throw new StudyMemberException(StudyMemberErrorCode.JOINED_STUDY_LIMIT_EXCEEDED);
        }
    }

    private void validateJoin(Long studyId, Long userId) {
        if (studyMemberRepository.findByStudyIdAndUserId(studyId, userId).isPresent()) {
            throw new StudyMemberException(StudyMemberErrorCode.ALREADY_JOINED_STUDY);
        }

        if (studyMemberRepository.countByStudyId(studyId) >= MAX_STUDY_MEMBER_COUNT) {
            throw new StudyMemberException(StudyMemberErrorCode.STUDY_MEMBER_LIMIT_EXCEEDED);
        }
    }

    private void validateLeader(StudyMember member) {
        if (!member.isLeader()) {
            throw new StudyMemberException(StudyMemberErrorCode.NOT_STUDY_LEADER);
        }
    }
}
