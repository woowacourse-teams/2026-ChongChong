package withoutc.chongchong.notification.service;

import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import withoutc.chongchong.notification.entity.NotificationDelivery;
import withoutc.chongchong.notification.exception.NotificationErrorCode;
import withoutc.chongchong.notification.exception.NotificationException;
import withoutc.chongchong.notification.repository.NotificationDeliveryRepository;
import withoutc.chongchong.user.entity.User;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationDeliveryService {

    private final NotificationDeliveryRepository notificationDeliveryRepository;

    private final Clock clock;

    @Transactional
    public void click(Long userId, Long deliveryId) {
        NotificationDelivery delivery = notificationDeliveryRepository.getByIdOrThrow(deliveryId);
        User user = delivery.getWebPushSubscription().getUser();

        if (!user.getId().equals(userId)) {
            throw new NotificationException(NotificationErrorCode.NOTIFICATION_DELIVERY_ACCESS_DENIED);
        }

        delivery.click(LocalDateTime.now(clock));
    }
}
