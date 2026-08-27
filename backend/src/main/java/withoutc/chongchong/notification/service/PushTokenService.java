package withoutc.chongchong.notification.service;

import static withoutc.chongchong.notification.exception.PushTokenErrorCode.PUSH_TOKEN_ALREADY_EXISTS;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import withoutc.chongchong.notification.dto.PushTokenCreateRequest;
import withoutc.chongchong.notification.entity.PushToken;
import withoutc.chongchong.notification.exception.PushTokenException;
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
        if (pushTokenRepository.existsByProviderAndToken(request.provider(), request.token())) {
            throw new PushTokenException(PUSH_TOKEN_ALREADY_EXISTS);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_FOUND));

        PushToken pushToken = PushToken.create(user, request.provider(), request.token(), request.platform());

        try {
            pushTokenRepository.saveAndFlush(pushToken);
        } catch (DataIntegrityViolationException e) {
            throw new PushTokenException(PUSH_TOKEN_ALREADY_EXISTS);
        }
    }
}
