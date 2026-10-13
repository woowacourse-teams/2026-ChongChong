package withoutc.chongchong.notification;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static withoutc.chongchong.global.config.ApiPathConfig.API_PREFIX;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import withoutc.chongchong.notification.entity.Notification;
import withoutc.chongchong.notification.entity.NotificationDelivery;
import withoutc.chongchong.notification.entity.NotificationType;
import withoutc.chongchong.notification.entity.ResourceType;
import withoutc.chongchong.notification.entity.WebPushSubscription;
import withoutc.chongchong.notification.repository.NotificationDeliveryRepository;
import withoutc.chongchong.notification.repository.NotificationRepository;
import withoutc.chongchong.notification.repository.WebPushSubscriptionRepository;
import withoutc.chongchong.notification.token.WebPushNotificationClickTokenProvider;
import withoutc.chongchong.support.PostgresContainerTest;
import withoutc.chongchong.support.TestDatabaseCleaner;
import withoutc.chongchong.user.entity.User;
import withoutc.chongchong.user.repository.UserRepository;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class NotificationDeliveryApiTest extends PostgresContainerTest {

    private static final String INSTALLATION_ID = "notification-click-test-installation";
    private static final String ENDPOINT = "https://push.example.com/subscription";
    private static final String P256DH = "p256dh-key";
    private static final String AUTH = "auth-secret";

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private WebPushSubscriptionRepository webPushSubscriptionRepository;

    @Autowired
    private NotificationDeliveryRepository notificationDeliveryRepository;

    @Autowired
    private TestDatabaseCleaner databaseCleaner;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private WebPushNotificationClickTokenProvider clickTokenProvider;

    @Autowired
    private Clock clock;

    @LocalServerPort
    private int port;

    @BeforeEach
    void setUp() {
        databaseCleaner.clean();
    }

    @Test
    @DisplayName("클릭 토큰이 있으면 로그인 없이 푸시 클릭을 기록하고 최초 시각을 유지한다")
    void recordFirstClickAndKeepItOnRepeatedRequest() {
        User user = userRepository.saveAndFlush(User.create("알림 사용자", null));
        NotificationDelivery delivery = saveDelivery(user);
        LocalDateTime requestStartedAt = LocalDateTime.now(clock).minusSeconds(1);
        String clickToken = clickTokenProvider.createClickToken(delivery.getId());

        clickWithToken(delivery.getId(), clickToken).then().statusCode(204);

        Timestamp firstClickedAt = findClickedAt(delivery.getId());
        assertThat(firstClickedAt).isNotNull();
        assertThat(firstClickedAt.toLocalDateTime())
                .isBetween(requestStartedAt, LocalDateTime.now(clock).plusSeconds(1));

        clickWithToken(delivery.getId(), clickToken).then().statusCode(204);

        assertThat(findClickedAt(delivery.getId())).isEqualTo(firstClickedAt);
    }

    @Test
    @DisplayName("다른 발송 내역 ID에 사용한 토큰은 거부한다")
    void rejectClickTokenForDifferentDelivery() {
        User owner = userRepository.saveAndFlush(User.create("소유자", null));
        NotificationDelivery delivery = saveDelivery(owner);
        NotificationDelivery anotherDelivery = saveDelivery(owner);
        String clickToken = clickTokenProvider.createClickToken(delivery.getId());

        clickWithToken(anotherDelivery.getId(), clickToken)
                .then()
                .statusCode(400)
                .body("code", equalTo("INVALID_CLICK_TOKEN"));

        assertThat(findClickedAt(anotherDelivery.getId())).isNull();
    }

    @Test
    @DisplayName("존재하지 않는 발송 내역을 클릭하면 발송 내역 없음 오류를 반환한다")
    void rejectClickForMissingDelivery() {
        long deliveryId = 999999L;
        String clickToken = clickTokenProvider.createClickToken(deliveryId);

        clickWithToken(deliveryId, clickToken)
                .then()
                .statusCode(404)
                .body("code", equalTo("NOTIFICATION_DELIVERY_NOT_FOUND"));
    }

    @Test
    @DisplayName("양수가 아닌 발송 내역 ID로 클릭하면 파라미터 오류를 반환한다")
    void rejectClickWithInvalidId() {
        String clickToken = clickTokenProvider.createClickToken(1L);

        clickWithToken(0L, clickToken)
                .then()
                .statusCode(400)
                .body("code", equalTo("INVALID_REQUEST_PARAMETER"))
                .body("errors.field", hasItem("deliveryId"));
    }

    @Test
    @DisplayName("서명이 올바르지 않은 클릭 토큰은 거부한다")
    void rejectInvalidClickToken() {
        clickWithToken(1L, "invalid-token")
                .then()
                .statusCode(400)
                .body("code", equalTo("INVALID_CLICK_TOKEN"));
    }

    private NotificationDelivery saveDelivery(User owner) {
        String subscriptionSuffix = UUID.randomUUID().toString();
        Notification notification = notificationRepository.saveAndFlush(
                Notification.create(
                        owner,
                        "새 공지",
                        "공지 내용",
                        NotificationType.NEW,
                        10L,
                        ResourceType.NOTICE,
                        "/studies/2/notices/10"
                )
        );
        WebPushSubscription subscription = webPushSubscriptionRepository.saveAndFlush(
                WebPushSubscription.create(
                        owner,
                        INSTALLATION_ID + subscriptionSuffix,
                        ENDPOINT + "/" + subscriptionSuffix,
                        P256DH,
                        AUTH
                )
        );
        return notificationDeliveryRepository.saveAndFlush(
                NotificationDelivery.create(notification, subscription)
        );
    }

    private Response clickWithToken(Long deliveryId, String clickToken) {
        return given()
                .basePath(API_PREFIX)
                .port(port)
                .contentType(ContentType.JSON)
                .body(Map.of("clickToken", clickToken))
                .when()
                .post("/notification-deliveries/{deliveryId}/click", deliveryId);
    }

    private Timestamp findClickedAt(Long deliveryId) {
        return jdbcTemplate.queryForObject(
                "SELECT clicked_at FROM notification_deliveries WHERE id = ?",
                Timestamp.class,
                deliveryId
        );
    }
}
