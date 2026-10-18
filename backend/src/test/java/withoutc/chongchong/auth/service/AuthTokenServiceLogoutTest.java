package withoutc.chongchong.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import withoutc.chongchong.auth.entity.AuthSession;
import withoutc.chongchong.auth.exception.AuthErrorCode;
import withoutc.chongchong.auth.exception.AuthException;
import withoutc.chongchong.auth.repository.AuthSessionRepository;
import withoutc.chongchong.auth.token.HashedRefreshToken;
import withoutc.chongchong.auth.token.IssuedTokenPair;
import withoutc.chongchong.auth.token.RawRefreshToken;
import withoutc.chongchong.support.TestDatabaseCleaner;
import withoutc.chongchong.user.entity.User;
import withoutc.chongchong.user.repository.UserRepository;

@SpringBootTest
@ActiveProfiles("test")
class AuthTokenServiceLogoutTest {

    @Autowired
    private AuthTokenService authTokenService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuthSessionRepository authSessionRepository;

    @Autowired
    private TestDatabaseCleaner databaseCleaner;

    @BeforeEach
    @AfterEach
    void cleanDatabase() {
        databaseCleaner.clean();
    }

    @Test
    @DisplayName("현재 Refresh Token과 일치하는 인증 세션을 삭제한다")
    void deleteMatchingAuthSession() {
        User user = saveUser("총총이");
        IssuedTokenPair tokenPair = authTokenService.issue(user.getId());

        authTokenService.logout(tokenPair.refreshToken());

        assertThat(authSessionRepository.findByUserId(user.getId())).isEmpty();
        assertThat(authSessionRepository.count()).isZero();
    }

    @Test
    @DisplayName("Session이 없거나 이미 제거된 Refresh Token의 로그아웃은 멱등하게 성공한다")
    void logoutIdempotentlyWithoutCurrentSession() {
        User user = saveUser("총총이");
        IssuedTokenPair tokenPair = authTokenService.issue(user.getId());

        authTokenService.logout(tokenPair.refreshToken());

        assertThatCode(() -> {
            authTokenService.logout(tokenPair.refreshToken());
            authTokenService.logout(new RawRefreshToken("already-logged-out-refresh-token"));
        }).doesNotThrowAnyException();

        assertThat(authSessionRepository.count()).isZero();
    }

    @Test
    @DisplayName("같은 사용자의 한 세션을 로그아웃해도 다른 세션은 보존되고 재발급할 수 있다")
    void logoutWithoutChangingOtherSessionOfSameUser() {
        User user = saveUser("총총이");
        IssuedTokenPair first = authTokenService.issue(user.getId());
        Long firstSessionId = authSessionRepository.findByUserId(user.getId()).orElseThrow().getId();
        IssuedTokenPair second = authTokenService.issue(user.getId());
        AuthSession secondSession = authSessionRepository.findAll().stream()
                .filter(session -> !session.getId().equals(firstSessionId))
                .findFirst().orElseThrow();

        authTokenService.logout(first.refreshToken());
        authTokenService.logout(first.refreshToken());

        assertThat(authSessionRepository.existsById(firstSessionId)).isFalse();
        AuthSession preservedSession = authSessionRepository.findById(secondSession.getId()).orElseThrow();
        assertThat(preservedSession.getRefreshTokenHash()).isEqualTo(secondSession.getRefreshTokenHash());
        assertThat(preservedSession.getExpiresAt()).isEqualTo(secondSession.getExpiresAt());
        assertThat(authSessionRepository.count()).isOne();
        assertThatThrownBy(() -> authTokenService.rotate(first.refreshToken()))
                .isInstanceOf(AuthException.class)
                .extracting(exception -> ((AuthException) exception).getErrorCode())
                .isEqualTo(AuthErrorCode.INVALID_REFRESH_TOKEN);
        assertThat(authTokenService.rotate(second.refreshToken()).refreshToken())
                .isNotEqualTo(second.refreshToken());
    }

    @Test
    @DisplayName("한 사용자의 로그아웃은 다른 사용자의 인증 세션에 영향을 주지 않는다")
    void keepOtherUserSession() {
        User firstUser = saveUser("첫 번째 사용자");
        User secondUser = saveUser("두 번째 사용자");
        IssuedTokenPair firstTokenPair = authTokenService.issue(firstUser.getId());
        authTokenService.issue(secondUser.getId());
        AuthSession secondSession = authSessionRepository.findByUserId(secondUser.getId()).orElseThrow();
        HashedRefreshToken secondRefreshTokenHash = secondSession.getRefreshTokenHash();

        authTokenService.logout(firstTokenPair.refreshToken());

        assertThat(authSessionRepository.findByUserId(firstUser.getId())).isEmpty();
        assertThat(authSessionRepository.findByUserId(secondUser.getId()))
                .hasValueSatisfying(session -> assertThat(session.getRefreshTokenHash())
                        .isEqualTo(secondRefreshTokenHash));
        assertThat(authSessionRepository.count()).isOne();
    }

    private User saveUser(String name) {
        return userRepository.saveAndFlush(User.create(name, null));
    }
}
