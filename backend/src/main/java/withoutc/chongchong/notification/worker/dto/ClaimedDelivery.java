package withoutc.chongchong.notification.worker.dto;

import java.time.LocalDateTime;
import withoutc.chongchong.notification.entity.Notification;
import withoutc.chongchong.notification.entity.NotificationDelivery;
import withoutc.chongchong.notification.entity.DeliveryStatus;
import withoutc.chongchong.notification.entity.WebPushSubscription;

public record ClaimedDelivery(
        Long id,
        Long subscriptionId,
        String endpoint,
        String p256dh,
        String auth,
        Long notificationId,
        String title,
        String body,
        String deepLink,
        LocalDateTime readyAt
) {

    public static ClaimedDelivery from(NotificationDelivery notificationDelivery) {
        WebPushSubscription subscription = notificationDelivery.getWebPushSubscription();
        Notification notification = notificationDelivery.getNotification();
        LocalDateTime readyAt;
        if (notificationDelivery.getStatus() == DeliveryStatus.RETRY_WAIT) {
            readyAt = notificationDelivery.getNextRetryAt();
        } else {
            readyAt = notificationDelivery.getCreatedAt();
        }
        return new ClaimedDelivery(
                notificationDelivery.getId(),
                subscription.getId(),
                subscription.getEndpoint(),
                subscription.getP256dh(),
                subscription.getAuth(),
                notification.getId(),
                notification.getTitle(),
                notification.getBody(),
                notification.getDeepLink(),
                readyAt
        );
    }
}
