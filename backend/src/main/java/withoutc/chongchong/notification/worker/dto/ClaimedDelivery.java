package withoutc.chongchong.notification.worker.dto;

import withoutc.chongchong.notification.entity.Notification;
import withoutc.chongchong.notification.entity.NotificationDelivery;
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
        String deepLink
) {

    public static ClaimedDelivery from(NotificationDelivery notificationDelivery) {
        WebPushSubscription subscription = notificationDelivery.getWebPushSubscription();
        Notification notification = notificationDelivery.getNotification();
        return new ClaimedDelivery(
                notificationDelivery.getId(),
                subscription.getId(),
                subscription.getEndpoint(),
                subscription.getP256dh(),
                subscription.getAuth(),
                notification.getId(),
                notification.getTitle(),
                notification.getBody(),
                notification.getDeepLink()
        );
    }
}
