package withoutc.chongchong.notification.service;

import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import withoutc.chongchong.notification.entity.NotificationDelivery;
import withoutc.chongchong.notification.repository.NotificationDeliveryRepository;
import withoutc.chongchong.notification.token.WebPushNotificationClickTokenProvider;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationDeliveryService {

    private final NotificationDeliveryRepository notificationDeliveryRepository;

    private final WebPushNotificationClickTokenProvider clickTokenProvider;

    private final Clock clock;

    @Transactional
    public void click(Long deliveryId, String clickToken) {
        clickTokenProvider.verifyToken(deliveryId, clickToken);
        NotificationDelivery delivery = notificationDeliveryRepository.getByIdOrThrow(deliveryId);
        delivery.click(LocalDateTime.now(clock));
    }
}
