package withoutc.chongchong.user.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import withoutc.chongchong.study.entity.StudyMemberRole;
import withoutc.chongchong.study.repository.StudyMemberRepository;
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

    @Transactional
    public void withdraw(Long userId) {
        User user = userRepository.getByIdForUpdateOrThrow(userId);
        if (studyMemberRepository.existsByUserIdAndRole(userId, StudyMemberRole.LEADER)) {
            throw new UserException(UserErrorCode.STUDY_LEADER_WITHDRAWAL_BLOCKED);
        }

        userRepository.delete(user);
    }

    public UserProfileResponse getMyProfile(Long userId) {
        User user = userRepository.getByIdOrThrow(userId);
        return UserProfileResponse.from(user);
    }
}
