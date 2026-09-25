package withoutc.chongchong.notification.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import withoutc.chongchong.global.persistence.BaseEntity;
import withoutc.chongchong.notification.exception.NotificationErrorCode;
import withoutc.chongchong.notification.exception.NotificationException;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "notification_deliveries",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_notification_delivery_notification_web_push_subscription",
                columnNames = {"notification_id", "web_push_subscription_id"}
        )
)
public class NotificationDelivery extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "notification_id", nullable = false)
    private Notification notification;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "web_push_subscription_id", nullable = false)
    private WebPushSubscription webPushSubscription;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private DeliveryStatus status;

    @Column(name = "claimed_at")
    private LocalDateTime claimedAt;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(nullable = false)
    private int attemptCount;

    private LocalDateTime nextRetryAt;

    private String lastError;

    public void claim(LocalDateTime time) {
        validateTime(time);
        this.status = DeliveryStatus.PROCESSING;
        this.claimedAt = time;
    }

    public static NotificationDelivery create(
            Notification notification,
            WebPushSubscription webPushSubscription
    ) {
        return new NotificationDelivery(notification, webPushSubscription);
    }

    private NotificationDelivery(
            Notification notification,
            WebPushSubscription webPushSubscription
    ) {
        validateRequiredValues(notification, webPushSubscription);
        this.notification = notification;
        this.webPushSubscription = webPushSubscription;
        this.status = DeliveryStatus.PENDING;
        this.claimedAt = null;
        this.sentAt = null;
        this.attemptCount = 0;
        this.nextRetryAt = null;
        this.lastError = null;
    }

    private void validateRequiredValues(Notification notification, WebPushSubscription webPushSubscription) {
        if (notification == null || webPushSubscription == null) {
            throw new NotificationException(NotificationErrorCode.INVALID_NOTIFICATION_DELIVERY);
        }
    }

    private void validateTime(LocalDateTime time) {
        if (LocalDateTime.now().isAfter(time) || time == null) {
            throw new NotificationException(NotificationErrorCode.INVALID_NOTIFICATION_DELIVERY_TIME);
        }
    }
}
