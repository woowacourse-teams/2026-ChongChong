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
import withoutc.chongchong.study.entity.Study;
import withoutc.chongchong.study.entity.StudyMember;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "notifications")
public class Notification extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "study_id", nullable = false)
    private Study study;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipient_id", nullable = false)
    private StudyMember recipient;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private NotificationType type;

    @Column(name = "resource_id", nullable = false)
    private Long resourceId;

    @Column(name = "resource_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private NotificationResourceType resourceType;

    @Column(name = "is_read", nullable = false)
    private boolean isRead;

    public static Notification create(
            Study study,
            StudyMember recipient,
            NotificationType type,
            Long resourceId,
            NotificationResourceType resourceType
    ) {
        return new Notification(study, recipient, type, resourceId, resourceType);
    }

    private void validateRequiredValues(Study study, StudyMember recipient, NotificationType type, Long resourceId,
                                        NotificationResourceType resourceType) {
        if (study == null || recipient == null || type == null || resourceId == null || resourceType == null) {
            throw new NotificationException(NotificationErrorCode.INVALID_NOTIFICATION);
        }
    }

    private Notification(
            Study study,
            StudyMember recipient,
            NotificationType type,
            Long resourceId,
            NotificationResourceType resourceType
    ) {
        validateRequiredValues(study, recipient, type, resourceId, resourceType);
        this.study = study;
        this.recipient = recipient;
        this.type = type;
        this.resourceId = resourceId;
        this.resourceType = resourceType;
        this.isRead = false;
    }
}
