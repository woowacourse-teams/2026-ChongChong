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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import withoutc.chongchong.global.persistence.BaseEntity;
import withoutc.chongchong.notification.exception.NotificationErrorCode;
import withoutc.chongchong.notification.exception.NotificationException;
import withoutc.chongchong.user.entity.User;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "notifications")
public class Notification extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipient_id", nullable = false)
    private User recipient;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private NotificationType type;

    @Column(name = "resource_id", nullable = false)
    private Long resourceId;

    @Column(name = "resource_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private NotificationResourceType resourceType;

    @Column(name = "deep_link", nullable = false)
    private String deepLink;

    @Column(name = "is_read", nullable = false)
    private boolean isRead;

    public static Notification create(
            User recipient,
            String title,
            String body,
            NotificationType type,
            Long resourceId,
            NotificationResourceType resourceType,
            String deepLink
    ) {
        return new Notification(recipient, title, body, type, resourceId, resourceType, deepLink);
    }

    private void validateRequiredValues(
            User recipient,
            String title,
            String body,
            NotificationType type,
            Long resourceId,
            NotificationResourceType resourceType,
            String deepLink
    ) {
        if (recipient == null || title == null || title.isBlank() || body == null || body.isBlank()
                || type == null || resourceId == null || resourceType == null || deepLink == null
                || deepLink.isBlank()) {
            throw new NotificationException(NotificationErrorCode.INVALID_NOTIFICATION);
        }
    }

    private Notification(
            User recipient,
            String title,
            String body,
            NotificationType type,
            Long resourceId,
            NotificationResourceType resourceType,
            String deepLink
    ) {
        validateRequiredValues(recipient, title, body, type, resourceId, resourceType, deepLink);
        this.recipient = recipient;
        this.title = title;
        this.body = body;
        this.type = type;
        this.resourceId = resourceId;
        this.resourceType = resourceType;
        this.deepLink = deepLink;
        this.isRead = false;
    }
}
