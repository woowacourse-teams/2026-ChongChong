package withoutc.chongchong.notification.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import withoutc.chongchong.global.persistence.BaseEntity;
import withoutc.chongchong.notification.exception.WebPushErrorCode;
import withoutc.chongchong.notification.exception.WebPushException;
import withoutc.chongchong.user.entity.User;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "web_push_subscriptions",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_web_push_subscriptions_user_installation_id",
                columnNames = {"user_id", "installation_id"}
        )
)
public class WebPushSubscription extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "installation_id", nullable = false, length = 255)
    private String installationId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String endpoint;

    @Column(name = "p256dh", nullable = false, columnDefinition = "TEXT")
    private String p256dh;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String auth;

    @Column(nullable = false)
    private boolean isActive;

    public static WebPushSubscription create(
            User user,
            String installationId,
            String endpoint,
            String p256dh,
            String auth
    ) {
        return new WebPushSubscription(user, installationId, endpoint, p256dh, auth);
    }

    public void deactivate() {
        isActive = false;
    }

    private WebPushSubscription(
            User user,
            String installationId,
            String endpoint,
            String p256dh,
            String auth
    ) {
        validateRequiredValues(user, installationId, endpoint, p256dh, auth);
        this.user = user;
        this.installationId = installationId;
        this.endpoint = endpoint;
        this.p256dh = p256dh;
        this.auth = auth;
        this.isActive = true;
    }

    private void validateRequiredValues(
            User user,
            String installationId,
            String endpoint,
            String p256dh,
            String auth
    ) {
        if (user == null || isBlank(installationId) || isBlank(endpoint) || isBlank(p256dh) || isBlank(auth)) {
            throw new WebPushException(WebPushErrorCode.INVALID_WEB_PUSH_SUBSCRIPTION);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
