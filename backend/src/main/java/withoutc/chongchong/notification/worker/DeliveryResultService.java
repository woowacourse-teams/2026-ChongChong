package withoutc.chongchong.notification.worker;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import withoutc.chongchong.notification.entity.NotificationDelivery;
import withoutc.chongchong.notification.entity.WebPushSubscription;
import withoutc.chongchong.notification.repository.NotificationDeliveryRepository;
import withoutc.chongchong.notification.repository.WebPushSubscriptionRepository;

@Service
@RequiredArgsConstructor
public class DeliveryResultService {

    // TODO: 시도 횟수 정하기
    static final int MAX_ATTEMPTS = 5;
    // attempt 횟수마다 retry 시간 다르게 추가
    private static final List<Duration> BASE_DELAY_MINUTES = List.of(
            Duration.ofMinutes(1),
            Duration.ofMinutes(2),
            Duration.ofMinutes(4),
            Duration.ofMinutes(8)
    );

    private final NotificationDeliveryRepository notificationDeliveryRepository;
    private final WebPushSubscriptionRepository webPushSubscriptionRepository;
    private final Clock clock;

    @Transactional
    public void markSent(Long deliveryId) {
        NotificationDelivery delivery = notificationDeliveryRepository.getByIdOrThrow(deliveryId);
        delivery.markSent(LocalDateTime.now(clock));
    }

    @Transactional
    public void markRetry(Long deliveryId, String error) {
        NotificationDelivery delivery = notificationDeliveryRepository.getByIdOrThrow(deliveryId);
        int attemptCount = delivery.getAttemptCount();

        if (attemptCount >= MAX_ATTEMPTS || attemptCount < 1) {
            delivery.markFailed(error);
            return;
        }
        LocalDateTime nextRetryAt = nextRetryAt(attemptCount, LocalDateTime.now(clock));
        delivery.markRetry(nextRetryAt, error);
    }

    @Transactional
    public void markExpired(Long deliveryId, Long subscriptionId, String error) {
        NotificationDelivery delivery = notificationDeliveryRepository.getByIdOrThrow(deliveryId);
        WebPushSubscription subscription = webPushSubscriptionRepository.getByIdOrThrow(subscriptionId);

        delivery.markFailed(error);
        subscription.deactivate();
    }

    @Transactional
    public void markFailed(Long deliveryId, String error) {
        NotificationDelivery delivery = notificationDeliveryRepository.getByIdOrThrow(deliveryId);
        delivery.markFailed(error);
    }

    private LocalDateTime nextRetryAt(int attemptCount, LocalDateTime now) {
        return now.plus(BASE_DELAY_MINUTES.get(attemptCount - 1));
    }
}
