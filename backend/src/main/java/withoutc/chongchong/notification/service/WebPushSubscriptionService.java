package withoutc.chongchong.notification.service;

import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import withoutc.chongchong.notification.controller.dto.WebPushSubscriptionRegisterRequest;
import withoutc.chongchong.notification.controller.dto.WebPushSubscriptionRegisterResponse;
import withoutc.chongchong.notification.entity.WebPushSubscription;
import withoutc.chongchong.notification.repository.WebPushSubscriptionRepository;
import withoutc.chongchong.user.entity.User;
import withoutc.chongchong.user.repository.UserRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WebPushSubscriptionService {

    private final WebPushSubscriptionRepository webPushSubscriptionRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    @Transactional
    public WebPushSubscriptionRegisterResponse register(
            Long userId,
            WebPushSubscriptionRegisterRequest request
    ) {
        User user = userRepository.getByIdForUpdateOrThrow(userId);
        WebPushSubscription.create(
                user,
                request.installationId(),
                request.endpoint(),
                request.keys().p256dh(),
                request.keys().auth()
        );

        // 같은 installationId 또는 endpoint를 사용하는 등록 요청 직렬화
        webPushSubscriptionRepository.lockInstallationRegistration(request.installationId());
        webPushSubscriptionRepository.lockEndpointRegistration(request.endpoint());
        LocalDateTime now = LocalDateTime.now(clock);
        webPushSubscriptionRepository.deactivateConflictingActiveSubscriptions(
                userId,
                request.installationId(),
                request.endpoint(),
                now
        );
        webPushSubscriptionRepository.upsert(
                userId,
                request.installationId(),
                request.endpoint(),
                request.keys().p256dh(),
                request.keys().auth(),
                now
        );

        WebPushSubscription subscription = webPushSubscriptionRepository.getByUserIdAndInstallationIdOrThrow(userId,
                request.installationId());
        return new WebPushSubscriptionRegisterResponse(subscription.getId());
    }

    @Transactional
    public void deactivate(Long userId, Long subscriptionId) {
        webPushSubscriptionRepository.deactivateByIdAndUserId(
                subscriptionId,
                userId,
                LocalDateTime.now(clock)
        );
    }
}
