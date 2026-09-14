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
import withoutc.chongchong.notification.exception.PushTokenErrorCode;
import withoutc.chongchong.notification.exception.PushTokenException;
import withoutc.chongchong.user.entity.User;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "push_tokens")
public class PushToken extends BaseEntity {

    private static final int MAX_INSTALLATION_ID_SIZE = 255;
    private static final int MAX_TOKEN_SIZE = 255;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, unique = true)
    private String installationId;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private TokenProvider provider;

    @Column(name = "token", nullable = false)
    private String tokenValue;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private DevicePlatform platform;

    @Column(nullable = false)
    private boolean isActive;

    public static PushToken create(
            User user,
            String installationId,
            TokenProvider provider,
            String tokenValue,
            DevicePlatform platform
    ) {
        return new PushToken(user, installationId, provider, tokenValue, platform);
    }

    private void validateRequiredValues(User user, TokenProvider provider, DevicePlatform platform) {
        if (user == null || provider == null || platform == null) {
            throw new PushTokenException(PushTokenErrorCode.INVALID_PUSH_TOKEN);
        }
    }

    private void validateInstallationId(String installationId) {
        if (installationId == null || installationId.isBlank() || installationId.length() > MAX_INSTALLATION_ID_SIZE) {
            throw new PushTokenException(PushTokenErrorCode.INVALID_INSTALLATION_ID);
        }
    }

    private void validateTokenValue(String tokenValue) {
        if (tokenValue == null || tokenValue.isBlank() || tokenValue.length() > MAX_TOKEN_SIZE) {
            throw new PushTokenException(PushTokenErrorCode.INVALID_TOKEN_VALUE);
        }
    }

    private PushToken(
            User user,
            String installationId,
            TokenProvider provider,
            String tokenValue,
            DevicePlatform platform
    ) {
        validateRequiredValues(user, provider, platform);
        validateInstallationId(installationId);
        validateTokenValue(tokenValue);
        this.user = user;
        this.installationId = installationId;
        this.provider = provider;
        this.tokenValue = tokenValue;
        this.platform = platform;
        this.isActive = true;
    }

    public void deactivate() {
        isActive = false;
    }
}
