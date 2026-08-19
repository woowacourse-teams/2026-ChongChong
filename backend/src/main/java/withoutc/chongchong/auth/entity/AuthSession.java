package withoutc.chongchong.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import withoutc.chongchong.auth.token.HashedRefreshToken;
import withoutc.chongchong.global.persistence.BaseEntity;
import withoutc.chongchong.user.entity.User;

@Entity
@Table(
        name = "auth_sessions",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_auth_sessions_user_id", columnNames = "user_id"),
                @UniqueConstraint(
                        name = "uk_auth_sessions_refresh_token_hash",
                        columnNames = "refresh_token_hash"
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuthSession extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Convert(converter = HashedRefreshTokenConverter.class)
    @Column(name = "refresh_token_hash", nullable = false, length = 64)
    private HashedRefreshToken refreshTokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    public static AuthSession create(
            User user,
            HashedRefreshToken refreshTokenHash,
            Instant expiresAt
    ) {
        validate(user, refreshTokenHash, expiresAt);
        return new AuthSession(user, refreshTokenHash, expiresAt);
    }

    private AuthSession(
            User user,
            HashedRefreshToken refreshTokenHash,
            Instant expiresAt
    ) {
        this.user = user;
        this.refreshTokenHash = refreshTokenHash;
        this.expiresAt = expiresAt;
    }

    public void replaceRefreshToken(
            HashedRefreshToken refreshTokenHash,
            Instant expiresAt
    ) {
        validateRefreshTokenHash(refreshTokenHash);
        validateExpiresAt(expiresAt);
        this.refreshTokenHash = refreshTokenHash;
        this.expiresAt = expiresAt;
    }

    public boolean isExpiredAt(Instant referenceTime) {
        if (referenceTime == null) {
            throw new IllegalArgumentException("만료 여부를 확인할 기준 시각은 필수입니다.");
        }
        return !referenceTime.isBefore(expiresAt);
    }

    private static void validate(
            User user,
            HashedRefreshToken refreshTokenHash,
            Instant expiresAt
    ) {
        if (user == null) {
            throw new IllegalArgumentException("인증 세션의 사용자는 필수입니다.");
        }
        validateRefreshTokenHash(refreshTokenHash);
        validateExpiresAt(expiresAt);
    }

    private static void validateRefreshTokenHash(HashedRefreshToken refreshTokenHash) {
        if (refreshTokenHash == null) {
            throw new IllegalArgumentException("인증 세션의 Refresh Token 해시는 필수입니다.");
        }
    }

    private static void validateExpiresAt(Instant expiresAt) {
        if (expiresAt == null) {
            throw new IllegalArgumentException("인증 세션의 만료 시각은 필수입니다.");
        }
    }
}
