package withoutc.chongchong.notification;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import withoutc.chongchong.auth.support.TestAuthRequest;
import withoutc.chongchong.notification.repository.PushTokenRepository;
import withoutc.chongchong.support.TestDatabaseCleaner;
import withoutc.chongchong.user.entity.User;
import withoutc.chongchong.user.repository.UserRepository;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PushTokenApiTest {

    private static final String TOKEN = "ExponentPushToken[test-token]";

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PushTokenRepository pushTokenRepository;

    @Autowired
    private TestDatabaseCleaner databaseCleaner;

    @Autowired
    private TestAuthRequest testAuthRequest;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @LocalServerPort
    private int port;

    @BeforeEach
    void setUp() {
        databaseCleaner.clean();
    }

    @AfterEach
    void cleanDatabase() {
        databaseCleaner.clean();
    }

    @Test
    @DisplayName("인증된 사용자가 푸시 토큰을 저장하면 204를 반환하고 사용자와 함께 저장한다")
    void createPushTokenTest() {
        User user = userRepository.saveAndFlush(User.create("총총이", null));

        testAuthRequest.givenAuthenticatedUser(user.getId())
                .port(port)
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "provider": "EXPO",
                          "token": "%s",
                          "platform": "ANDROID"
                        }
                        """.formatted(TOKEN))
                .when()
                .post("/push-tokens")
                .then()
                .statusCode(204);

        assertThat(pushTokenRepository.count()).isOne();
        PushTokenRow saved = findPushToken();
        assertThat(saved.userId()).isEqualTo(user.getId());
        assertThat(saved.provider()).isEqualTo("EXPO");
        assertThat(saved.token()).isEqualTo(TOKEN);
        assertThat(saved.platform()).isEqualTo("ANDROID");
        assertThat(saved.active()).isTrue();
    }

    @Test
    @DisplayName("푸시 토큰 필수값이 누락되면 입력값 오류를 반환한다")
    void rejectMissingPushTokenFieldsTest() {
        User user = userRepository.saveAndFlush(User.create("총총이", null));

        testAuthRequest.givenAuthenticatedUser(user.getId())
                .port(port)
                .contentType(ContentType.JSON)
                .body("{}")
                .when()
                .post("/push-tokens")
                .then()
                .statusCode(400)
                .body("code", equalTo("INVALID_INPUT_VALUE"))
                .body("errors.field", hasItems("provider", "token", "platform"))
                .body("errors.reason", hasItems(
                        "푸시 토큰 제공자는 필수입니다.",
                        "푸시 토큰 필수입니다.",
                        "디바이스 플랫폼은 필수입니다."
                ));

        assertThat(pushTokenRepository.count()).isZero();
    }

    @Test
    @DisplayName("인증 없이 푸시 토큰을 저장하면 인증 필요 오류를 반환한다")
    void rejectUnauthenticatedRequestTest() {
        given()
                .port(port)
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "provider": "EXPO",
                          "token": "%s",
                          "platform": "ANDROID"
                        }
                        """.formatted(TOKEN))
                .when()
                .post("/push-tokens")
                .then()
                .statusCode(401)
                .body("code", equalTo("AUTHENTICATION_REQUIRED"))
                .body("message", equalTo("인증이 필요합니다."));
    }

    private PushTokenRow findPushToken() {
        return jdbcTemplate.queryForObject(
                """
                        SELECT user_id, provider, token, platform, is_active
                        FROM push_tokens
                        """,
                (resultSet, rowNumber) -> new PushTokenRow(
                        resultSet.getLong("user_id"),
                        resultSet.getString("provider"),
                        resultSet.getString("token"),
                        resultSet.getString("platform"),
                        resultSet.getBoolean("is_active")
                )
        );
    }

    private record PushTokenRow(
            Long userId,
            String provider,
            String token,
            String platform,
            boolean active
    ) {
    }
}
