package withoutc.chongchong.notification.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import withoutc.chongchong.notification.controller.dto.PushTokenCreateRequest;
import withoutc.chongchong.notification.entity.TokenProvider;
import withoutc.chongchong.notification.repository.PushTokenRepository;
import withoutc.chongchong.user.entity.User;
import withoutc.chongchong.user.repository.UserRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PushTokenService {

    private final PushTokenRepository pushTokenRepository;
    private final UserRepository userRepository;

    @Transactional
    public void registerPushToken(Long userId, PushTokenCreateRequest request) {
        User user = userRepository.getByIdForUpdateOrThrow(userId);

        // Provider는 EXPO로 고정, 추후 FCM/APNs 추가되면 request로 받아야 함
        pushTokenRepository.upsert(user.getId(), request.installationId(), TokenProvider.EXPO.name(), request.token(),
                request.platform().name());
    }

    @Transactional
    public void deactivatePushToken(Long userId, String installationId) {
        pushTokenRepository.deactivateByInstallationIdAndUserId(installationId, userId);
    }
}
