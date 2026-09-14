package withoutc.chongchong.notification;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static withoutc.chongchong.global.config.ApiPathConfig.API_PREFIX;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import withoutc.chongchong.auth.support.TestAuthRequest;
import withoutc.chongchong.notification.repository.PushTokenRepository;
import withoutc.chongchong.support.PostgresContainerTest;
import withoutc.chongchong.support.TestDatabaseCleaner;
import withoutc.chongchong.user.entity.User;
import withoutc.chongchong.user.repository.UserRepository;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PushTokenApiTest extends PostgresContainerTest {

    private static final String TOKEN = "ExponentPushToken[test-token]";
    private static final String INSTALLATION_ID = "installation-1";

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
    @DisplayName("인증된 사용자가 푸시 토큰을 등록하면 204를 반환하고 사용자와 함께 저장한다")
    void registerPushTokenTest() {
        User user = userRepository.saveAndFlush(User.create("총총이", null));

        testAuthRequest.givenAuthenticatedUser(user.getId())
                .port(port)
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "installationId": "%s",
                          "token": "%s",
                          "platform": "ANDROID"
                        }
                        """.formatted(INSTALLATION_ID, TOKEN))
                .when()
                .post("/push-tokens")
                .then()
                .statusCode(204);

        assertThat(pushTokenRepository.count()).isOne();
        PushTokenRow saved = findPushToken();
        assertThat(saved.userId()).isEqualTo(user.getId());
        assertThat(saved.installationId()).isEqualTo(INSTALLATION_ID);
        assertThat(saved.provider()).isEqualTo("EXPO");
        assertThat(saved.token()).isEqualTo(TOKEN);
        assertThat(saved.platform()).isEqualTo("ANDROID");
        assertThat(saved.active()).isTrue();
    }

    @Test
    @DisplayName("같은 설치 식별자로 다시 등록하면 기존 등록 정보를 갱신하고 중복 저장하지 않는다")
    void updateSamePushTokenRegistrationTest() {
        User user = userRepository.saveAndFlush(User.create("총총이", null));
        String firstRequestBody = """
                {
                  "installationId": "%s",
                  "token": "old-token",
                  "platform": "ANDROID"
                }
                """.formatted(INSTALLATION_ID);
        String secondRequestBody = """
                {
                  "installationId": "%s",
                  "token": "new-token",
                  "platform": "IOS"
                }
                """.formatted(INSTALLATION_ID);

        testAuthRequest.givenAuthenticatedUser(user.getId())
                .port(port)
                .contentType(ContentType.JSON)
                .body(firstRequestBody)
                .when()
                .post("/push-tokens")
                .then()
                .statusCode(204);

        testAuthRequest.givenAuthenticatedUser(user.getId())
                .port(port)
                .contentType(ContentType.JSON)
                .body(secondRequestBody)
                .when()
                .post("/push-tokens")
                .then()
                .statusCode(204);

        assertThat(pushTokenRepository.count()).isOne();
        PushTokenRow updated = findPushToken();
        assertThat(updated.token()).isEqualTo("new-token");
        assertThat(updated.platform()).isEqualTo("IOS");
        assertThat(updated.active()).isTrue();
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
                .body("errors.field", hasItems("installationId", "token", "platform"))
                .body("errors.reason", hasItems(
                        "설치된 앱 식별자는 필수입니다.",
                        "푸시 토큰은 필수입니다.",
                        "디바이스 플랫폼은 필수입니다."
                ));

        assertThat(pushTokenRepository.count()).isZero();
    }

    @Test
    @DisplayName("설치 식별자와 푸시 토큰이 255자를 초과하면 입력값 오류를 반환한다")
    void rejectTooLongPushTokenFieldsTest() {
        User user = userRepository.saveAndFlush(User.create("총총이", null));
        String tooLongValue = "a".repeat(256);

        testAuthRequest.givenAuthenticatedUser(user.getId())
                .port(port)
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "installationId": "%s",
                          "token": "%s",
                          "platform": "ANDROID"
                        }
                        """.formatted(tooLongValue, tooLongValue))
                .when()
                .post("/push-tokens")
                .then()
                .statusCode(400)
                .body("code", equalTo("INVALID_INPUT_VALUE"))
                .body("errors.field", hasItems("installationId", "token"))
                .body("errors.reason", hasItems(
                        "설치된 앱 식별자는 255자 이내여야 합니다.",
                        "푸시 토큰은 255자 이내여야 합니다."
                ));

        assertThat(pushTokenRepository.count()).isZero();
    }

    @Test
    @DisplayName("인증 없이 푸시 토큰을 저장하면 인증 필요 오류를 반환한다")
    void rejectUnauthenticatedRequestTest() {
        given()
                .basePath(API_PREFIX)
                .port(port)
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "installationId": "%s",
                          "token": "%s",
                          "platform": "ANDROID"
                        }
                        """.formatted(INSTALLATION_ID, TOKEN))
                .when()
                .post("/push-tokens")
                .then()
                .statusCode(401)
                .body("code", equalTo("AUTHENTICATION_REQUIRED"))
                .body("message", equalTo("인증이 필요합니다."));
    }

    @Test
    @DisplayName("다른 사용자가 같은 설치 식별자로 등록하면 기존 사용자를 변경한다")
    void rebindPushTokenToAnotherUserTest() {
        User firstUser = userRepository.saveAndFlush(User.create("첫 번째 사용자", null));
        User secondUser = userRepository.saveAndFlush(User.create("두 번째 사용자", null));

        registerPushToken(firstUser.getId(), TOKEN, "ANDROID");
        registerPushToken(secondUser.getId(), "new-token", "IOS");

        assertThat(pushTokenRepository.count()).isOne();
        PushTokenRow updated = findPushToken();
        assertThat(updated.userId()).isEqualTo(secondUser.getId());
        assertThat(updated.token()).isEqualTo("new-token");
        assertThat(updated.platform()).isEqualTo("IOS");
        assertThat(updated.active()).isTrue();
    }

    @Test
    @DisplayName("존재하지 않는 사용자로 푸시 토큰을 등록하면 사용자 없음 오류를 반환한다")
    void rejectMissingUserTest() {
        testAuthRequest.givenAuthenticatedUser(999L)
                .port(port)
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "installationId": "%s",
                          "token": "%s",
                          "platform": "ANDROID"
                        }
                        """.formatted(INSTALLATION_ID, TOKEN))
                .when()
                .post("/push-tokens")
                .then()
                .statusCode(404)
                .body("code", equalTo("USER_NOT_FOUND"));
    }

    @Test
    @DisplayName("푸시 토큰을 삭제하면 해당 사용자의 설치 식별자를 비활성화한다")
    void deactivatePushTokenTest() {
        User user = userRepository.saveAndFlush(User.create("총총이", null));
        registerPushToken(user.getId(), TOKEN, "ANDROID");

        testAuthRequest.givenAuthenticatedUser(user.getId())
                .port(port)
                .when()
                .delete("/push-tokens/{installationId}", INSTALLATION_ID)
                .then()
                .statusCode(204);

        assertThat(findPushToken().active()).isFalse();
    }

    @Test
    @DisplayName("푸시 토큰 비활성화 요청의 설치 식별자가 255자를 초과하면 요청 파라미터 오류를 반환한다")
    void rejectTooLongInstallationIdWhenDeactivatingTest() {
        User user = userRepository.saveAndFlush(User.create("총총이", null));
        String tooLongInstallationId = "a".repeat(256);

        testAuthRequest.givenAuthenticatedUser(user.getId())
                .port(port)
                .when()
                .delete("/push-tokens/{installationId}", tooLongInstallationId)
                .then()
                .statusCode(400)
                .body("code", equalTo("INVALID_REQUEST_PARAMETER"))
                .body("errors.field", hasItems("installationId"))
                .body("errors.reason", hasItems("설치된 앱 식별자는 255자 이내여야 합니다."));

        assertThat(pushTokenRepository.count()).isZero();
    }

    @Test
    @DisplayName("다른 사용자는 푸시 토큰을 비활성화할 수 없다")
    void doNotDeactivateAnotherUsersPushTokenTest() {
        User owner = userRepository.saveAndFlush(User.create("소유자", null));
        User otherUser = userRepository.saveAndFlush(User.create("다른 사용자", null));
        registerPushToken(owner.getId(), TOKEN, "ANDROID");

        testAuthRequest.givenAuthenticatedUser(otherUser.getId())
                .port(port)
                .when()
                .delete("/push-tokens/{installationId}", INSTALLATION_ID)
                .then()
                .statusCode(204);

        PushTokenRow unchanged = findPushToken();
        assertThat(unchanged.userId()).isEqualTo(owner.getId());
        assertThat(unchanged.active()).isTrue();
    }

    private void registerPushToken(Long userId, String token, String platform) {
        testAuthRequest.givenAuthenticatedUser(userId)
                .port(port)
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "installationId": "%s",
                          "token": "%s",
                          "platform": "%s"
                        }
                        """.formatted(INSTALLATION_ID, token, platform))
                .when()
                .post("/push-tokens")
                .then()
                .statusCode(204);
    }

    private PushTokenRow findPushToken() {
        return jdbcTemplate.queryForObject(
                """
                        SELECT user_id, installation_id, provider, token, platform, is_active
                        FROM push_tokens
                        """,
                (resultSet, rowNumber) -> new PushTokenRow(
                        resultSet.getLong("user_id"),
                        resultSet.getString("installation_id"),
                        resultSet.getString("provider"),
                        resultSet.getString("token"),
                        resultSet.getString("platform"),
                        resultSet.getBoolean("is_active")
                )
        );
    }

    private record PushTokenRow(
            Long userId,
            String installationId,
            String provider,
            String token,
            String platform,
            boolean active
    ) {
    }
}
