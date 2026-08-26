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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import withoutc.chongchong.global.persistence.BaseEntity;
import withoutc.chongchong.user.entity.User;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "push_tokens",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_push_tokens_provider_token",
                columnNames = {"provider", "token"}
        ))
public class PushToken extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private TokenProvider provider;

    @Column(nullable = false)
    private String token;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private DevicePlatform platform;

    @Column(nullable = false)
    private boolean isActive;

    public static PushToken create(
            User user,
            TokenProvider provider,
            String token,
            DevicePlatform devicePlatform
    ) {
        return new PushToken(user, provider, token, devicePlatform);
    }

    private PushToken(
            User user,
            TokenProvider provider,
            String token,
            DevicePlatform platform
    ) {
        this.user = user;
        this.provider = provider;
        this.token = token;
        this.platform = platform;
        this.isActive = true;
    }
}
