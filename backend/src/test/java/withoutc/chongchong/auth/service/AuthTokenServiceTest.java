package withoutc.chongchong.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import withoutc.chongchong.auth.entity.AuthSession;
import withoutc.chongchong.auth.exception.AuthErrorCode;
import withoutc.chongchong.auth.exception.AuthException;
import withoutc.chongchong.auth.repository.AuthSessionRepository;
import withoutc.chongchong.auth.token.HashedRefreshToken;
import withoutc.chongchong.auth.token.IssuedTokenPair;
import withoutc.chongchong.auth.token.RefreshTokenHasher;
import withoutc.chongchong.support.TestDatabaseCleaner;
import withoutc.chongchong.user.entity.User;
import withoutc.chongchong.user.repository.UserRepository;

@SpringBootTest
@ActiveProfiles("test")
@Import(AuthTokenServiceTest.FixedClockConfig.class)
class AuthTokenServiceTest {

    private static final Instant NOW = Instant.now().truncatedTo(ChronoUnit.SECONDS);

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
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TestDatabaseCleaner databaseCleaner;

    @BeforeEach
    @AfterEach
    void cleanDatabase() {
        databaseCleaner.clean();
    }

    @Test
    @DisplayName("기존 사용자에게 Access Token과 Refresh Token을 발급하고 인증 세션을 저장한다")
    void issueTokenPairAndStoreAuthSession() {
        User user = saveUser("총총이");

        IssuedTokenPair tokenPair = authTokenService.issue(user.getId());

        Jwt accessToken = jwtDecoder.decode(tokenPair.accessToken().value());
        HashedRefreshToken expectedHash = refreshTokenHasher.hash(tokenPair.refreshToken());
        AuthSession authSession = authSessionRepository.findByUserId(user.getId()).orElseThrow();
        String storedHash = findStoredRefreshTokenHash(authSession.getId());

        assertThat(accessToken.getSubject()).isEqualTo(user.getId().toString());
        assertThat(tokenPair.accessToken().expiresAt()).isEqualTo(NOW.plus(Duration.ofMinutes(30)));
        assertThat(tokenPair.refreshTokenExpiresAt()).isEqualTo(NOW.plus(Duration.ofDays(30)));
        assertThat(authSession.getRefreshTokenHash()).isEqualTo(expectedHash);
        assertThat(authSession.getExpiresAt()).isEqualTo(tokenPair.refreshTokenExpiresAt());
        assertThat(storedHash)
                .isEqualTo(expectedHash.value())
                .isNotEqualTo(tokenPair.refreshToken().value());
        assertThat(tokenPair.toString())
                .doesNotContain(tokenPair.accessToken().value())
                .doesNotContain(tokenPair.refreshToken().value())
                .contains("REDACTED");
    }

    @Test
    @DisplayName("같은 사용자에게 다시 발급하면 기존 인증 세션을 새 Refresh Token으로 교체한다")
    void replaceExistingAuthSession() {
        User user = saveUser("총총이");
        IssuedTokenPair first = authTokenService.issue(user.getId());
        AuthSession firstSession = authSessionRepository.findByUserId(user.getId()).orElseThrow();
        Long sessionId = firstSession.getId();
        HashedRefreshToken firstHash = firstSession.getRefreshTokenHash();

        IssuedTokenPair second = authTokenService.issue(user.getId());

        AuthSession replaced = authSessionRepository.findByUserId(user.getId()).orElseThrow();
        HashedRefreshToken secondHash = refreshTokenHasher.hash(second.refreshToken());
        assertThat(authSessionRepository.count()).isOne();
        assertThat(replaced.getId()).isEqualTo(sessionId);
        assertThat(replaced.getRefreshTokenHash())
                .isEqualTo(secondHash)
                .isNotEqualTo(firstHash);
        assertThat(second.refreshToken()).isNotEqualTo(first.refreshToken());
    }

    @Test
    @DisplayName("서로 다른 사용자의 인증 세션은 독립적으로 유지한다")
    void keepDifferentUserSessionsIndependently() {
        User firstUser = saveUser("첫 번째 사용자");
        User secondUser = saveUser("두 번째 사용자");

        IssuedTokenPair first = authTokenService.issue(firstUser.getId());
        IssuedTokenPair second = authTokenService.issue(secondUser.getId());

        assertThat(authSessionRepository.count()).isEqualTo(2);
        assertThat(authSessionRepository.findByUserId(firstUser.getId()).orElseThrow().getRefreshTokenHash())
                .isEqualTo(refreshTokenHasher.hash(first.refreshToken()));
        assertThat(authSessionRepository.findByUserId(secondUser.getId()).orElseThrow().getRefreshTokenHash())
                .isEqualTo(refreshTokenHasher.hash(second.refreshToken()));
    }

    @Test
    @DisplayName("존재하지 않는 사용자에게 Token을 발급하지 않는다")
    void rejectMissingUser() {
        assertThatThrownBy(() -> authTokenService.issue(999L))
                .isInstanceOf(AuthException.class)
                .extracting(exception -> ((AuthException) exception).getErrorCode())
                .isEqualTo(AuthErrorCode.USER_NOT_FOUND);

        assertThat(authSessionRepository.count()).isZero();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(longs = {0L, -1L})
    @DisplayName("양수가 아닌 사용자 ID로 Token을 발급하지 않는다")
    void rejectInvalidUserId(Long userId) {
        assertThatThrownBy(() -> authTokenService.issue(userId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("사용자 ID는 양수여야 합니다.");

        assertThat(authSessionRepository.count()).isZero();
    }

    private User saveUser(String name) {
        return userRepository.saveAndFlush(User.create(name, null));
    }

    private String findStoredRefreshTokenHash(Long authSessionId) {
        return jdbcTemplate.queryForObject(
                "SELECT refresh_token_hash FROM auth_sessions WHERE id = ?",
                String.class,
                authSessionId
        );
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
