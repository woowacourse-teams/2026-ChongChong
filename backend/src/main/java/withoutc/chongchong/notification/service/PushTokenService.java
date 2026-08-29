package withoutc.chongchong.notification.service;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import withoutc.chongchong.notification.dto.PushTokenCreateRequest;
import withoutc.chongchong.notification.entity.PushToken;
import withoutc.chongchong.notification.repository.PushTokenRepository;
import withoutc.chongchong.user.entity.User;
import withoutc.chongchong.user.exception.UserErrorCode;
import withoutc.chongchong.user.exception.UserException;
import withoutc.chongchong.user.repository.UserRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PushTokenService {

    private final PushTokenRepository pushTokenRepository;
    private final UserRepository userRepository;

    @Transactional
    public void createPushToken(Long userId, PushTokenCreateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));

        Optional<PushToken> pushToken = pushTokenRepository.findByUserIdAndInstallationId(userId,
                request.installationId());

        if (pushToken.isPresent()) {
            PushToken token = pushToken.get();
            token.changeActiveState();
            return;
        }

        PushToken token = PushToken.create(user, request.installationId(), request.provider(), request.token(),
                request.platform());

        try {
            pushTokenRepository.saveAndFlush(token);
        } catch (DataIntegrityViolationException exception) { // Unique 제약 조건 위반이 발생해도 예외 발생 X
            return;
        }
    }
}
