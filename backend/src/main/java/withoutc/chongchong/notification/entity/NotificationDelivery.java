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

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "notification_deliveries",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_notification_delivery_notification_push_token",
                columnNames = {"notification_id", "push_token_id"}
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
    @JoinColumn(name = "push_token_id", nullable = false)
    private PushToken pushToken;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private DeliveryStatus status;

    @Column(nullable = false)
    private int attemptCount;

    private LocalDateTime nextRetryAt;

    private String lastError;

    public static NotificationDelivery create(
            Notification notification,
            PushToken pushToken
    ) {
        return new NotificationDelivery(notification, pushToken);
    }

    private NotificationDelivery(
            Notification notification,
            PushToken pushToken
    ) {
        this.notification = notification;
        this.pushToken = pushToken;
        this.status = DeliveryStatus.PENDING;
        this.attemptCount = 0;
        this.nextRetryAt = null;
        this.lastError = null;
    }
}
