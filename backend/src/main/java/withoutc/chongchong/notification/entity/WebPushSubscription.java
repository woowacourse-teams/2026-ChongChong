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
                name = "uk_web_push_subscriptions_endpoint",
                columnNames = "endpoint"
        )
)
public class WebPushSubscription extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

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
            String endpoint,
            String p256dh,
            String auth
    ) {
        return new WebPushSubscription(user, endpoint, p256dh, auth);
    }

    public void deactivate() {
        isActive = false;
    }

    private WebPushSubscription(
            User user,
            String endpoint,
            String p256dh,
            String auth
    ) {
        validateRequiredValues(user, endpoint, p256dh, auth);
        this.user = user;
        this.endpoint = endpoint;
        this.p256dh = p256dh;
        this.auth = auth;
        this.isActive = true;
    }

    private void validateRequiredValues(User user, String endpoint, String p256dh, String auth) {
        if (user == null || isBlank(endpoint) || isBlank(p256dh) || isBlank(auth)) {
            throw new WebPushException(WebPushErrorCode.INVALID_WEB_PUSH_SUBSCRIPTION);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
