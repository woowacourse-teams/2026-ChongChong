package withoutc.chongchong.auth.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import withoutc.chongchong.auth.token.HashedRefreshToken;
import withoutc.chongchong.user.entity.User;

class AuthSessionTest {

    private static final Instant EXPIRES_AT = Instant.parse("2026-09-18T00:00:00Z");
    private static final HashedRefreshToken REFRESH_TOKEN_HASH = new HashedRefreshToken("a".repeat(64));

    @Test
    @DisplayName("사용자와 Refresh Token 해시 및 만료 시각으로 인증 세션을 생성한다")
    void createAuthSession() {
        User user = User.create("총총이", null);

        AuthSession authSession = AuthSession.create(user, REFRESH_TOKEN_HASH, EXPIRES_AT);

        assertThat(authSession.getUser()).isSameAs(user);
        assertThat(authSession.getRefreshTokenHash()).isEqualTo(REFRESH_TOKEN_HASH);
        assertThat(authSession.getExpiresAt()).isEqualTo(EXPIRES_AT);
    }

    @Test
    @DisplayName("사용자 없이 인증 세션을 생성하지 않는다")
    void rejectNullUser() {
        assertThatThrownBy(() -> AuthSession.create(null, REFRESH_TOKEN_HASH, EXPIRES_AT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("인증 세션의 사용자는 필수입니다.");
    }

    @Test
    @DisplayName("Refresh Token 해시 없이 인증 세션을 생성하지 않는다")
    void rejectNullRefreshTokenHash() {
        User user = User.create("총총이", null);

        assertThatThrownBy(() -> AuthSession.create(user, null, EXPIRES_AT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("인증 세션의 Refresh Token 해시는 필수입니다.");
    }

    @Test
    @DisplayName("만료 시각 없이 인증 세션을 생성하지 않는다")
    void rejectNullExpiresAt() {
        User user = User.create("총총이", null);

        assertThatThrownBy(() -> AuthSession.create(user, REFRESH_TOKEN_HASH, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("인증 세션의 만료 시각은 필수입니다.");
    }

    @Test
    @DisplayName("기준 시각이 만료 시각과 같거나 이후이면 인증 세션이 만료되었다")
    void determineExpirationAtBoundary() {
        AuthSession authSession = AuthSession.create(
                User.create("총총이", null),
                REFRESH_TOKEN_HASH,
                EXPIRES_AT
        );

        assertThat(authSession.isExpiredAt(EXPIRES_AT.minusNanos(1))).isFalse();
        assertThat(authSession.isExpiredAt(EXPIRES_AT)).isTrue();
        assertThat(authSession.isExpiredAt(EXPIRES_AT.plusNanos(1))).isTrue();
    }

    @Test
    @DisplayName("기준 시각 없이 인증 세션의 만료 여부를 확인하지 않는다")
    void rejectNullExpirationReferenceTime() {
        AuthSession authSession = AuthSession.create(
                User.create("총총이", null),
                REFRESH_TOKEN_HASH,
                EXPIRES_AT
        );

        assertThatThrownBy(() -> authSession.isExpiredAt(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("만료 여부를 확인할 기준 시각은 필수입니다.");
    }

    @Test
    @DisplayName("인증 세션의 Refresh Token 해시와 만료 시각을 교체한다")
    void replaceRefreshToken() {
        AuthSession authSession = AuthSession.create(
                User.create("총총이", null),
                REFRESH_TOKEN_HASH,
                EXPIRES_AT
        );
        HashedRefreshToken newRefreshTokenHash = new HashedRefreshToken("b".repeat(64));
        Instant newExpiresAt = EXPIRES_AT.plusSeconds(60);

        authSession.replaceRefreshToken(newRefreshTokenHash, newExpiresAt);

        assertThat(authSession.getRefreshTokenHash()).isEqualTo(newRefreshTokenHash);
        assertThat(authSession.getExpiresAt()).isEqualTo(newExpiresAt);
    }
}
