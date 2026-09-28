package withoutc.chongchong.notification.service;

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

        // 모든 등록 요청은 installationId -> endpoint 순서로 같은 키를 잠가 교차 충돌을 직렬화한다.
        webPushSubscriptionRepository.lockInstallationRegistration(request.installationId());
        webPushSubscriptionRepository.lockEndpointRegistration(request.endpoint());
        webPushSubscriptionRepository.deactivateConflictingActiveSubscriptions(
                userId,
                request.installationId(),
                request.endpoint()
        );
        webPushSubscriptionRepository.upsert(
                userId,
                request.installationId(),
                request.endpoint(),
                request.keys().p256dh(),
                request.keys().auth()
        );

        WebPushSubscription subscription = webPushSubscriptionRepository.getByUserIdAndInstallationIdOrThrow(userId,
                request.installationId());
        return new WebPushSubscriptionRegisterResponse(subscription.getId());
    }

    @Transactional
    public void deactivate(Long userId, Long subscriptionId) {
        webPushSubscriptionRepository.deactivateByIdAndUserId(subscriptionId, userId);
    }
}
