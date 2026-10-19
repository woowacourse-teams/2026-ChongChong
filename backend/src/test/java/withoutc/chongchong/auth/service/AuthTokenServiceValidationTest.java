package withoutc.chongchong.auth.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
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
import withoutc.chongchong.auth.exception.AuthErrorCode;
import withoutc.chongchong.auth.exception.AuthException;
import withoutc.chongchong.auth.repository.AuthSessionRepository;
import withoutc.chongchong.auth.token.AccessTokenIssuer;
import withoutc.chongchong.auth.token.RefreshTokenGenerator;
import withoutc.chongchong.auth.token.RefreshTokenHasher;
import withoutc.chongchong.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class AuthTokenServiceValidationTest {

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

    private void assertAuthError(Runnable invocation, AuthErrorCode expected) {
        assertThatThrownBy(invocation::run)
                .isInstanceOf(AuthException.class)
                .extracting(exception -> ((AuthException) exception).getErrorCode())
                .isEqualTo(expected);
    }
}
