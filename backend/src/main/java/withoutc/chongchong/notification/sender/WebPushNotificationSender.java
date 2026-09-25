package withoutc.chongchong.notification.sender;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import withoutc.chongchong.notification.entity.NotificationDelivery;
import withoutc.chongchong.notification.repository.NotificationDeliveryRepository;

@Component
@RequiredArgsConstructor
public class WebPushNotificationSender implements NotificationSender {

    // TODO: 적절한 배치 사이즈 결정
    private static final int BATCH_SIZE = 100;

    private final NotificationDeliveryRepository notificationDeliveryRepository;

    private final Clock clock;

    @Override
    @Transactional
    public void sendNotifications(NotificationEvent event) {
        LocalDateTime now = LocalDateTime.now(clock);
        List<NotificationDelivery> deliveries = notificationDeliveryRepository.findClaimableForUpdate(now, BATCH_SIZE);
        for (NotificationDelivery delivery : deliveries) {
            delivery.claim(LocalDateTime.now(clock));
        }
    }
}
