package withoutc.chongchong.notification.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import withoutc.chongchong.notification.controller.dto.WebPushSubscriptionRegisterRequest;
import withoutc.chongchong.notification.controller.dto.WebPushSubscriptionRegisterResponse;
import withoutc.chongchong.notification.entity.WebPushSubscription;
import withoutc.chongchong.notification.exception.WebPushErrorCode;
import withoutc.chongchong.notification.exception.WebPushException;
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
                request.endpoint(),
                request.keys().p256dh(),
                request.keys().auth()
        );

        int affectedRows = webPushSubscriptionRepository.upsert(
                userId,
                request.endpoint(),
                request.keys().p256dh(),
                request.keys().auth()
        );
        if (affectedRows == 0) {
            throw new WebPushException(
                    WebPushErrorCode.WEB_PUSH_SUBSCRIPTION_ALREADY_REGISTERED
            );
        }

        WebPushSubscription subscription = webPushSubscriptionRepository.findByEndpoint(request.endpoint())
                .orElseThrow(() -> new WebPushException(
                        WebPushErrorCode.INVALID_WEB_PUSH_SUBSCRIPTION
                ));
        return new WebPushSubscriptionRegisterResponse(subscription.getId());
    }

    @Transactional
    public void deactivate(Long userId, Long subscriptionId) {
        webPushSubscriptionRepository.deactivateByIdAndUserId(subscriptionId, userId);
    }
}
