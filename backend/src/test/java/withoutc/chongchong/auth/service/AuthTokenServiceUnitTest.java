package withoutc.chongchong.auth.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import withoutc.chongchong.auth.config.RefreshTokenProperties;
import withoutc.chongchong.auth.entity.AuthSession;
import withoutc.chongchong.auth.exception.AuthErrorCode;
import withoutc.chongchong.auth.exception.AuthException;
import withoutc.chongchong.auth.repository.AuthSessionRepository;
import withoutc.chongchong.auth.token.AccessTokenIssuer;
import withoutc.chongchong.auth.token.HashedRefreshToken;
import withoutc.chongchong.auth.token.RawRefreshToken;
import withoutc.chongchong.auth.token.RefreshTokenGenerator;
import withoutc.chongchong.auth.token.RefreshTokenHasher;
import withoutc.chongchong.user.entity.User;
import withoutc.chongchong.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class AuthTokenServiceUnitTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuthSessionRepository authSessionRepository;

    @Mock
    private AccessTokenIssuer accessTokenIssuer;

    @Mock
    private RefreshTokenGenerator refreshTokenGenerator;

    @Mock
    private RefreshTokenHasher refreshTokenHasher;

    @Mock
    private RefreshTokenProperties refreshTokenProperties;

    @Mock
    private Clock clock;

    @InjectMocks
    private AuthTokenService authTokenService;

    @ParameterizedTest
    @NullSource
    @ValueSource(longs = {0L, -1L})
    @DisplayName("양수가 아닌 사용자 ID는 저장소 조회나 Token 생성 전에 거부한다")
    void rejectInvalidUserId(Long userId) {
        assertAuthError(() -> authTokenService.issue(userId), AuthErrorCode.INVALID_USER_ID);

        verifyNoInteractions(userRepository, authSessionRepository, accessTokenIssuer,
                refreshTokenGenerator, refreshTokenHasher);
    }

    @Test
    @DisplayName("존재하지 않는 사용자에게 Token을 발급하거나 세션을 저장하지 않는다")
    void rejectMissingUser() {
        when(userRepository.findByIdForUpdate(999L)).thenReturn(Optional.empty());

        assertAuthError(() -> authTokenService.issue(999L), AuthErrorCode.USER_NOT_FOUND);

        verifyNoInteractions(authSessionRepository, accessTokenIssuer,
                refreshTokenGenerator, refreshTokenHasher);
    }

    @Test
    @DisplayName("누락된 Refresh Token은 저장소 조회나 Token 생성 전에 거부한다")
    void rejectNullRefreshToken() {
        assertAuthError(() -> authTokenService.rotate(null), AuthErrorCode.INVALID_REFRESH_TOKEN);

        verifyNoInteractions(userRepository, authSessionRepository, accessTokenIssuer,
                refreshTokenGenerator, refreshTokenHasher);
    }

    @Test
    @DisplayName("로그아웃은 원문 토큰의 해시로 잠금 조회한 세션만 삭제한다")
    void deleteMatchingAuthSession() {
        RawRefreshToken token = new RawRefreshToken("refresh-token");
        HashedRefreshToken hash = new HashedRefreshToken("a".repeat(64));
        AuthSession session = AuthSession.create(User.create("총총이", null), hash,
                Instant.parse("2026-12-05T00:00:00Z"));
        when(refreshTokenHasher.hash(token)).thenReturn(hash);
        when(authSessionRepository.findByRefreshTokenHashForUpdate(hash)).thenReturn(Optional.of(session));

        authTokenService.logout(token);

        verify(authSessionRepository).findByRefreshTokenHashForUpdate(hash);
        verify(authSessionRepository).delete(session);
        verifyNoInteractions(userRepository, accessTokenIssuer, refreshTokenGenerator);
    }

    @Test
    @DisplayName("세션이 없는 토큰으로 반복 로그아웃해도 삭제나 토큰 발급을 하지 않는다")
    void logoutIdempotentlyWithoutCurrentSession() {
        RawRefreshToken token = new RawRefreshToken("already-logged-out-token");
        HashedRefreshToken hash = new HashedRefreshToken("b".repeat(64));
        when(refreshTokenHasher.hash(token)).thenReturn(hash);
        when(authSessionRepository.findByRefreshTokenHashForUpdate(hash)).thenReturn(Optional.empty());

        authTokenService.logout(token);
        authTokenService.logout(token);

        verify(authSessionRepository, times(2)).findByRefreshTokenHashForUpdate(hash);
        verify(authSessionRepository, never()).delete(any());
        verifyNoInteractions(userRepository, accessTokenIssuer, refreshTokenGenerator);
    }

    private void assertAuthError(Runnable invocation, AuthErrorCode expected) {
        assertThatThrownBy(invocation::run)
                .isInstanceOf(AuthException.class)
                .extracting(exception -> ((AuthException) exception).getErrorCode())
                .isEqualTo(expected);
    }
}
