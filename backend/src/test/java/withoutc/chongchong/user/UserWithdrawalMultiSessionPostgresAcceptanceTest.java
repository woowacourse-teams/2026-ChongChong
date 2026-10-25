package withoutc.chongchong.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;

import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import withoutc.chongchong.auth.entity.AuthSession;
import withoutc.chongchong.auth.exception.AuthErrorCode;
import withoutc.chongchong.auth.exception.AuthException;
import withoutc.chongchong.auth.repository.AuthSessionRepository;
import withoutc.chongchong.auth.service.AuthTokenService;
import withoutc.chongchong.auth.support.TestAuthRequest;
import withoutc.chongchong.auth.token.IssuedTokenPair;
import withoutc.chongchong.support.TestDatabaseCleaner;
import withoutc.chongchong.user.entity.User;
import withoutc.chongchong.user.repository.UserRepository;

@Testcontainers(disabledWithoutDocker = true)
@ActiveProfiles("test")
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.flyway.enabled=true",
                "spring.jpa.hibernate.ddl-auto=validate"
        }
)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class UserWithdrawalMultiSessionPostgresAcceptanceTest {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres:16-alpine")
    )
            .withDatabaseName("chongchong")
            .withUsername("postgres")
            .withPassword("postgres");

    @DynamicPropertySource
    static void registerPostgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    }

    @LocalServerPort
    private int port;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuthSessionRepository authSessionRepository;

    @Autowired
    private AuthTokenService authTokenService;

    @Autowired
    private TestAuthRequest testAuthRequest;

    @Autowired
    private TestDatabaseCleaner databaseCleaner;

    @AfterEach
    void cleanDatabase() {
        databaseCleaner.clean();
    }

    @Test
    @DisplayName("사용자 탈퇴 시 모든 인증 세션을 삭제하고 다른 사용자의 세션은 보존한다")
    void withdrawUserWithMultipleAuthSessions() {
        User user = userRepository.saveAndFlush(User.create("탈퇴할 사용자", null));
        User otherUser = userRepository.saveAndFlush(User.create("남는 사용자", null));
        IssuedTokenPair first = authTokenService.issue(user.getId());
        IssuedTokenPair second = authTokenService.issue(user.getId());
        IssuedTokenPair other = authTokenService.issue(otherUser.getId());
        List<Long> sessionIds = authSessionRepository.findAll().stream()
                .filter(session -> session.getUser().getId().equals(user.getId()))
                .map(AuthSession::getId)
                .toList();
        assertThat(sessionIds).hasSize(2);

        testAuthRequest.givenAuthenticatedUser(user.getId())
                .port(port)
                .when()
                .delete("/users/me")
                .then()
                .statusCode(204)
                .header(HttpHeaders.SET_COOKIE, containsString("Max-Age=0"))
                .header(HttpHeaders.SET_COOKIE, containsString("Path=/api/auth"));

        assertThat(userRepository.existsById(user.getId())).isFalse();
        assertThat(authSessionRepository.findAllById(sessionIds)).isEmpty();
        assertThat(authSessionRepository.count()).isOne();
        for (IssuedTokenPair tokenPair : List.of(first, second)) {
            assertThatThrownBy(() -> authTokenService.rotate(tokenPair.refreshToken()))
                    .isInstanceOf(AuthException.class)
                    .extracting(exception -> ((AuthException) exception).getErrorCode())
                    .isEqualTo(AuthErrorCode.INVALID_REFRESH_TOKEN);
        }
        assertThat(userRepository.existsById(otherUser.getId())).isTrue();
        assertThat(authTokenService.rotate(other.refreshToken()).refreshToken())
                .isNotEqualTo(other.refreshToken());
    }
}
