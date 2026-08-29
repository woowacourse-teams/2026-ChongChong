package withoutc.chongchong.auth.entity;

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
import withoutc.chongchong.auth.social.SocialProvider;
import withoutc.chongchong.global.persistence.BaseEntity;
import withoutc.chongchong.user.entity.User;

@Entity
@Table(
        name = "social_accounts",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_social_accounts_provider_provider_user_id",
                columnNames = {"provider", "provider_user_id"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SocialAccount extends BaseEntity {

    private static final int MAX_PROVIDER_USER_ID_LENGTH = 255;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private SocialProvider provider;

    @Column(name = "provider_user_id", nullable = false, length = MAX_PROVIDER_USER_ID_LENGTH)
    private String providerUserId;

    public static SocialAccount create(
            User user,
            SocialProvider provider,
            String providerUserId
    ) {
        validate(user, provider, providerUserId);
        return new SocialAccount(user, provider, providerUserId);
    }

    private SocialAccount(
            User user,
            SocialProvider provider,
            String providerUserId
    ) {
        this.user = user;
        this.provider = provider;
        this.providerUserId = providerUserId;
    }

    private static void validate(
            User user,
            SocialProvider provider,
            String providerUserId
    ) {
        validateUser(user);
        validateProvider(provider);
        validateProviderUserId(providerUserId);
    }

    private static void validateUser(User user) {
        if (user == null) {
            throw new IllegalArgumentException("소셜 계정의 사용자는 필수입니다.");
        }
    }

    private static void validateProvider(SocialProvider provider) {
        if (provider == null) {
            throw new IllegalArgumentException("소셜 로그인 제공자는 필수입니다.");
        }
    }

    private static void validateProviderUserId(String providerUserId) {
        if (providerUserId == null || providerUserId.isBlank()) {
            throw new IllegalArgumentException("소셜 제공자 사용자 ID는 비어 있을 수 없습니다.");
        }
        if (providerUserId.length() > MAX_PROVIDER_USER_ID_LENGTH) {
            throw new IllegalArgumentException("소셜 제공자 사용자 ID는 255자를 초과할 수 없습니다.");
        }
    }
}
