package withoutc.chongchong.user.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import withoutc.chongchong.study.entity.StudyMember;
import withoutc.chongchong.study.repository.StudyMemberRepository;
import withoutc.chongchong.user.entity.User;
import withoutc.chongchong.user.exception.UserErrorCode;
import withoutc.chongchong.user.exception.UserException;
import withoutc.chongchong.user.repository.UserRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final StudyMemberRepository studyMemberRepository;

    @Transactional
    public void withdraw(Long userId) {
        User user = userRepository.getByIdForUpdateOrThrow(userId);
        List<StudyMember> studyMembers = studyMemberRepository.findAllByUserIdOrderByCreatedAtDesc(userId);

        boolean hasLeader = studyMembers.stream()
                .anyMatch(StudyMember::isLeader);

        if (hasLeader) {
            throw new UserException(UserErrorCode.STUDY_LEADER_WITHDRAWAL_BLOCKED);
        }

        userRepository.delete(user);
    }
}
