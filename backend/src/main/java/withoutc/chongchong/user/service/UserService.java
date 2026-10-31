package withoutc.chongchong.user.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import withoutc.chongchong.assignment.repository.AssignmentRepository;
import withoutc.chongchong.assignment.repository.AssignmentSubmissionRepository;
import withoutc.chongchong.comment.repository.CommentRepository;
import withoutc.chongchong.notice.repository.NoticeRepository;
import withoutc.chongchong.notification.service.NotificationService;
import withoutc.chongchong.study.entity.StudyMember;
import withoutc.chongchong.study.entity.StudyMemberRole;
import withoutc.chongchong.study.repository.StudyMemberRepository;
import withoutc.chongchong.user.controller.dto.UserProfileNameUpdateRequest;
import withoutc.chongchong.user.controller.dto.UserProfileResponse;
import withoutc.chongchong.user.entity.User;
import withoutc.chongchong.user.exception.UserErrorCode;
import withoutc.chongchong.user.exception.UserException;
import withoutc.chongchong.user.repository.UserRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final StudyMemberRepository studyMemberRepository;
    private final NotificationService notificationService;
    private final NoticeRepository noticeRepository;
    private final AssignmentRepository assignmentRepository;
    private final AssignmentSubmissionRepository assignmentSubmissionRepository;
    private final CommentRepository commentRepository;

    @Transactional
    public void withdraw(Long userId) {
        User user = userRepository.getByIdForUpdateOrThrow(userId);
        if (studyMemberRepository.existsByUserIdAndRole(userId, StudyMemberRole.LEADER)) {
            throw new UserException(UserErrorCode.STUDY_LEADER_WITHDRAWAL_BLOCKED);
        }

        noticeRepository.findAllByCommentAuthorUserIdForUpdate(userId);
        assignmentRepository.findAllAffectedByUserIdForUpdate(userId);
        assignmentSubmissionRepository.findAllByUserIdForUpdate(userId);
        commentRepository.deleteAllForUserSubmissions(userId);
        notificationService.deleteNotificationsForUser(userId);
        userRepository.delete(user);
    }

    public UserProfileResponse getMyProfile(Long userId) {
        User user = userRepository.getByIdOrThrow(userId);
        return UserProfileResponse.from(user);
    }

    @Transactional
    public UserProfileResponse updateMyProfileName(Long userId, UserProfileNameUpdateRequest request) {
        User user = userRepository.getByIdForUpdateOrThrow(userId);
        String newName = request.name();
        user.updateName(newName);
        List<StudyMember> studyMembers = studyMemberRepository.findAllByUserId(userId);
        studyMembers.forEach(studyMember -> studyMember.syncNameFromUser(newName));

        return UserProfileResponse.from(user);
    }
}
