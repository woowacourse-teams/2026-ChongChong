package withoutc.chongchong.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import withoutc.chongchong.auth.entity.AuthSession;
import withoutc.chongchong.auth.exception.AuthErrorCode;
import withoutc.chongchong.auth.exception.AuthException;
import withoutc.chongchong.auth.repository.AuthSessionRepository;
import withoutc.chongchong.auth.token.HashedRefreshToken;
import withoutc.chongchong.auth.token.IssuedTokenPair;
import withoutc.chongchong.auth.token.RawRefreshToken;
import withoutc.chongchong.auth.token.RefreshTokenHasher;
import withoutc.chongchong.support.TestDatabaseCleaner;
import withoutc.chongchong.user.entity.User;
import withoutc.chongchong.user.repository.UserRepository;

@SpringBootTest
@ActiveProfiles("test")
@Import(AuthTokenServiceRefreshTest.FixedClockConfig.class)
class AuthTokenServiceRefreshTest {

    private static final Instant NOW = Instant.now()
            .truncatedTo(ChronoUnit.SECONDS)
            .plusNanos(123_456_789);

    @Autowired
    private AuthTokenService authTokenService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuthSessionRepository authSessionRepository;

    @Autowired
    private RefreshTokenHasher refreshTokenHasher;

    @Autowired
    private JwtDecoder jwtDecoder;

    @Autowired
    private TestDatabaseCleaner databaseCleaner;

    @BeforeEach
    @AfterEach
    void cleanDatabase() {
        databaseCleaner.clean();
    }

    @Test
    @DisplayName("유효한 Refresh Token을 새 Access Token과 Refresh Token으로 회전한다")
    void rotateValidRefreshToken() {
        User user = saveUser();
        IssuedTokenPair original = authTokenService.issue(user.getId());
        AuthSession originalSession = findSession(user);
        Long sessionId = originalSession.getId();
        HashedRefreshToken originalHash = originalSession.getRefreshTokenHash();

        IssuedTokenPair rotated = authTokenService.rotate(original.refreshToken());

        AuthSession currentSession = findSession(user);
        HashedRefreshToken rotatedHash = refreshTokenHasher.hash(rotated.refreshToken());
        assertThat(authSessionRepository.count()).isOne();
        assertThat(currentSession.getId()).isEqualTo(sessionId);
        assertThat(currentSession.getRefreshTokenHash())
                .isEqualTo(rotatedHash)
                .isNotEqualTo(originalHash);
        assertThat(currentSession.getExpiresAt()).isEqualTo(rotated.refreshTokenExpiresAt());
        assertThat(rotated.refreshToken()).isNotEqualTo(original.refreshToken());
        assertThat(jwtDecoder.decode(rotated.accessToken().value()).getSubject())
                .isEqualTo(user.getId().toString());
    }

    @Test
    @DisplayName("같은 사용자의 두 세션은 독립적으로 재발급되며 다른 세션을 변경하지 않는다")
    void rotateSessionsIndependentlyForSameUser() {
        User user = saveUser();
        IssuedTokenPair first = authTokenService.issue(user.getId());
        Long firstSessionId = findSession(user).getId();
        IssuedTokenPair second = authTokenService.issue(user.getId());
        AuthSession secondSession = authSessionRepository.findAllByUserId(user.getId()).stream()
                .filter(session -> !session.getId().equals(firstSessionId))
                .findFirst().orElseThrow();
        Long secondSessionId = secondSession.getId();
        HashedRefreshToken secondHash = secondSession.getRefreshTokenHash();
        Instant secondExpiresAt = secondSession.getExpiresAt();

        IssuedTokenPair rotatedFirst = authTokenService.rotate(first.refreshToken());

        AuthSession preservedSecond = authSessionRepository.findById(secondSessionId).orElseThrow();
        assertThat(preservedSecond.getRefreshTokenHash()).isEqualTo(secondHash);
        assertThat(preservedSecond.getExpiresAt()).isEqualTo(secondExpiresAt);
        assertInvalidRefreshToken(() -> authTokenService.rotate(first.refreshToken()));

        IssuedTokenPair rotatedSecond = authTokenService.rotate(second.refreshToken());

        assertThat(authSessionRepository.count()).isEqualTo(2);
        assertThat(authSessionRepository.findById(firstSessionId).orElseThrow().getRefreshTokenHash())
                .isEqualTo(refreshTokenHasher.hash(rotatedFirst.refreshToken()));
        assertThat(authSessionRepository.findById(secondSessionId).orElseThrow().getRefreshTokenHash())
                .isEqualTo(refreshTokenHasher.hash(rotatedSecond.refreshToken()));
        assertInvalidRefreshToken(() -> authTokenService.rotate(second.refreshToken()));
        assertThat(jwtDecoder.decode(rotatedFirst.accessToken().value()).getSubject())
                .isEqualTo(user.getId().toString());
        assertThat(jwtDecoder.decode(rotatedSecond.accessToken().value()).getSubject())
                .isEqualTo(user.getId().toString());
    }

    @Test
    @DisplayName("회전이 끝난 이전 Refresh Token은 다시 사용할 수 없다")
    void rejectRotatedRefreshToken() {
        User user = saveUser();
        IssuedTokenPair original = authTokenService.issue(user.getId());
        IssuedTokenPair rotated = authTokenService.rotate(original.refreshToken());
        HashedRefreshToken currentHash = refreshTokenHasher.hash(rotated.refreshToken());

        assertInvalidRefreshToken(() -> authTokenService.rotate(original.refreshToken()));

        assertThat(findSession(user).getRefreshTokenHash()).isEqualTo(currentHash);
    }

    @Test
    @DisplayName("DB에 없는 Refresh Token은 거부하고 현재 Session을 변경하지 않는다")
    void rejectUnknownRefreshTokenWithoutChangingSession() {
        User user = saveUser();
        authTokenService.issue(user.getId());
        HashedRefreshToken currentHash = findSession(user).getRefreshTokenHash();

        assertInvalidRefreshToken(() -> authTokenService.rotate(
                new RawRefreshToken("unknown-refresh-token")
        ));

        assertThat(findSession(user).getRefreshTokenHash()).isEqualTo(currentHash);
    }

    @Test
    @DisplayName("만료된 Refresh Token은 거부하고 Session을 변경하지 않는다")
    void rejectExpiredRefreshTokenWithoutChangingSession() {
        User user = saveUser();
        RawRefreshToken expiredRefreshToken = new RawRefreshToken("expired-refresh-token");
        HashedRefreshToken expiredHash = refreshTokenHasher.hash(expiredRefreshToken);
        Instant expiredAt = NOW.minusSeconds(1).truncatedTo(ChronoUnit.MICROS);
        AuthSession expiredSession = authSessionRepository.saveAndFlush(
                AuthSession.create(user, expiredHash, expiredAt)
        );

        assertInvalidRefreshToken(() -> authTokenService.rotate(expiredRefreshToken));

        AuthSession currentSession = authSessionRepository.findById(expiredSession.getId()).orElseThrow();
        assertThat(currentSession.getRefreshTokenHash()).isEqualTo(expiredHash);
        assertThat(currentSession.getExpiresAt()).isEqualTo(expiredAt);
    }

    private void assertInvalidRefreshToken(Runnable invocation) {
        assertThatThrownBy(invocation::run)
                .isInstanceOf(AuthException.class)
                .extracting(exception -> ((AuthException) exception).getErrorCode())
                .isEqualTo(AuthErrorCode.INVALID_REFRESH_TOKEN);
    }

    private User saveUser() {
        return userRepository.saveAndFlush(User.create("총총이", null));
    }

    private AuthSession findSession(User user) {
        var sessions = authSessionRepository.findAllByUserId(user.getId());
        assertThat(sessions).hasSize(1);
        return sessions.getFirst();
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class FixedClockConfig {

        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(NOW, ZoneOffset.UTC);
        }
    }
}
