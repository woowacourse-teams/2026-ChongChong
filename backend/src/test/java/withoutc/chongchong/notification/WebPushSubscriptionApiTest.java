package withoutc.chongchong.notification;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static withoutc.chongchong.global.config.ApiPathConfig.API_PREFIX;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import withoutc.chongchong.auth.support.TestAuthRequest;
import withoutc.chongchong.notification.repository.WebPushSubscriptionRepository;
import withoutc.chongchong.support.PostgresContainerTest;
import withoutc.chongchong.support.TestDatabaseCleaner;
import withoutc.chongchong.user.entity.User;
import withoutc.chongchong.user.repository.UserRepository;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class WebPushSubscriptionApiTest extends PostgresContainerTest {

    private static final String ENDPOINT = "https://push.example.com/subscription";
    private static final String P256DH = "p256dh-key";
    private static final String AUTH = "auth-secret";

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WebPushSubscriptionRepository webPushSubscriptionRepository;

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
    @DisplayName("인증된 사용자가 Web Push 구독을 등록하면 ID와 함께 저장한다")
    void registerWebPushSubscriptionTest() {
        User user = userRepository.saveAndFlush(User.create("총총이", null));

        Response response = register(user.getId(), ENDPOINT, P256DH, AUTH);

        response.then().statusCode(200).body("id", equalTo(1));
        assertThat(webPushSubscriptionRepository.count()).isOne();
        WebPushSubscriptionRow saved = findSubscription();
        assertThat(saved.userId()).isEqualTo(user.getId());
        assertThat(saved.endpoint()).isEqualTo(ENDPOINT);
        assertThat(saved.p256dh()).isEqualTo(P256DH);
        assertThat(saved.auth()).isEqualTo(AUTH);
        assertThat(saved.active()).isTrue();
    }

    @Test
    @DisplayName("같은 endpoint로 다시 등록하면 기존 구독 정보를 갱신하고 중복 저장하지 않는다")
    void updateSameWebPushSubscriptionTest() {
        User user = userRepository.saveAndFlush(User.create("총총이", null));

        register(user.getId(), ENDPOINT, "old-p256dh", "old-auth").then().statusCode(200);
        register(user.getId(), ENDPOINT, "new-p256dh", "new-auth").then().statusCode(200);

        assertThat(webPushSubscriptionRepository.count()).isOne();
        WebPushSubscriptionRow updated = findSubscription();
        assertThat(updated.p256dh()).isEqualTo("new-p256dh");
        assertThat(updated.auth()).isEqualTo("new-auth");
        assertThat(updated.active()).isTrue();
    }

    @Test
    @DisplayName("다른 사용자가 이미 등록한 endpoint는 등록할 수 없다")
    void rejectEndpointOwnedByAnotherUserTest() {
        User owner = userRepository.saveAndFlush(User.create("소유자", null));
        User otherUser = userRepository.saveAndFlush(User.create("다른 사용자", null));

        register(owner.getId(), ENDPOINT, P256DH, AUTH).then().statusCode(200);

        register(otherUser.getId(), ENDPOINT, "other-p256dh", "other-auth")
                .then()
                .statusCode(409)
                .body("code", equalTo("WEB_PUSH_SUBSCRIPTION_ALREADY_REGISTERED"));

        WebPushSubscriptionRow saved = findSubscription();
        assertThat(saved.userId()).isEqualTo(owner.getId());
        assertThat(saved.p256dh()).isEqualTo(P256DH);
        assertThat(saved.auth()).isEqualTo(AUTH);
    }

    @Test
    @DisplayName("Web Push 구독 필수값이 누락되면 입력값 오류를 반환한다")
    void rejectMissingWebPushSubscriptionFieldsTest() {
        User user = userRepository.saveAndFlush(User.create("총총이", null));

        testAuthRequest.givenAuthenticatedUser(user.getId())
                .port(port)
                .contentType(ContentType.JSON)
                .body("{}")
                .when()
                .post("/web-push-subscriptions")
                .then()
                .statusCode(400)
                .body("code", equalTo("INVALID_INPUT_VALUE"));

        assertThat(webPushSubscriptionRepository.count()).isZero();
    }

    @Test
    @DisplayName("인증 없이 Web Push 구독을 저장하면 인증 필요 오류를 반환한다")
    void rejectUnauthenticatedRequestTest() {
        given()
                .basePath(API_PREFIX)
                .port(port)
                .contentType(ContentType.JSON)
                .body(requestBody(ENDPOINT, P256DH, AUTH))
                .when()
                .post("/web-push-subscriptions")
                .then()
                .statusCode(401)
                .body("code", equalTo("AUTHENTICATION_REQUIRED"));
    }

    @Test
    @DisplayName("인증 없이 VAPID public key를 조회할 수 있다")
    void getWebPushConfigTest() {
        given()
                .basePath(API_PREFIX)
                .port(port)
                .when()
                .get("/web-push/config")
                .then()
                .statusCode(200)
                .body("publicKey", equalTo(
                        "BDcLQBzrmGYpdNmsaQrk9_uU9Q_wQb2ziEN5AIcoUq4bi2uKlC2UKcmfBuUdJj_NzcDu-ldeUpif_na9i-u3HKw"
                ));
    }

    @Test
    @DisplayName("존재하지 않는 사용자로 Web Push 구독을 등록하면 사용자 없음 오류를 반환한다")
    void rejectMissingUserTest() {
        testAuthRequest.givenAuthenticatedUser(999L)
                .port(port)
                .contentType(ContentType.JSON)
                .body(requestBody(ENDPOINT, P256DH, AUTH))
                .when()
                .post("/web-push-subscriptions")
                .then()
                .statusCode(404)
                .body("code", equalTo("USER_NOT_FOUND"));
    }

    @Test
    @DisplayName("Web Push 구독을 삭제하면 해당 사용자의 구독을 비활성화한다")
    void deactivateWebPushSubscriptionTest() {
        User user = userRepository.saveAndFlush(User.create("총총이", null));
        long subscriptionId = ((Number) register(user.getId(), ENDPOINT, P256DH, AUTH)
                .then()
                .statusCode(200)
                .extract()
                .path("id")).longValue();

        testAuthRequest.givenAuthenticatedUser(user.getId())
                .port(port)
                .when()
                .delete("/web-push-subscriptions/{subscriptionId}", subscriptionId)
                .then()
                .statusCode(204);

        assertThat(findSubscription().active()).isFalse();
    }

    @Test
    @DisplayName("다른 사용자는 Web Push 구독을 비활성화할 수 없다")
    void doNotDeactivateAnotherUsersWebPushSubscriptionTest() {
        User owner = userRepository.saveAndFlush(User.create("소유자", null));
        User otherUser = userRepository.saveAndFlush(User.create("다른 사용자", null));
        long subscriptionId = ((Number) register(owner.getId(), ENDPOINT, P256DH, AUTH)
                .then()
                .statusCode(200)
                .extract()
                .path("id")).longValue();

        testAuthRequest.givenAuthenticatedUser(otherUser.getId())
                .port(port)
                .when()
                .delete("/web-push-subscriptions/{subscriptionId}", subscriptionId)
                .then()
                .statusCode(204);

        WebPushSubscriptionRow unchanged = findSubscription();
        assertThat(unchanged.userId()).isEqualTo(owner.getId());
        assertThat(unchanged.active()).isTrue();
    }

    @Test
    @DisplayName("Web Push 구독 ID가 양수가 아니면 요청 파라미터 오류를 반환한다")
    void rejectInvalidSubscriptionIdTest() {
        User user = userRepository.saveAndFlush(User.create("총총이", null));

        testAuthRequest.givenAuthenticatedUser(user.getId())
                .port(port)
                .when()
                .delete("/web-push-subscriptions/{subscriptionId}", 0)
                .then()
                .statusCode(400)
                .body("code", equalTo("INVALID_REQUEST_PARAMETER"));
    }

    private Response register(Long userId, String endpoint, String p256dh, String auth) {
        return testAuthRequest.givenAuthenticatedUser(userId)
                .port(port)
                .contentType(ContentType.JSON)
                .body(requestBody(endpoint, p256dh, auth))
                .when()
                .post("/web-push-subscriptions")
                .andReturn();
    }

    private String requestBody(String endpoint, String p256dh, String auth) {
        return """
                {
                  "endpoint": "%s",
                  "keys": {
                    "p256dh": "%s",
                    "auth": "%s"
                  }
                }
                """.formatted(endpoint, p256dh, auth);
    }

    private WebPushSubscriptionRow findSubscription() {
        return jdbcTemplate.queryForObject(
                """
                        SELECT user_id, endpoint, p256dh, auth, is_active
                        FROM web_push_subscriptions
                        """,
                (resultSet, rowNumber) -> new WebPushSubscriptionRow(
                        resultSet.getLong("user_id"),
                        resultSet.getString("endpoint"),
                        resultSet.getString("p256dh"),
                        resultSet.getString("auth"),
                        resultSet.getBoolean("is_active")
                )
        );
    }

    private record WebPushSubscriptionRow(
            Long userId,
            String endpoint,
            String p256dh,
            String auth,
            boolean active
    ) {
    }
}
