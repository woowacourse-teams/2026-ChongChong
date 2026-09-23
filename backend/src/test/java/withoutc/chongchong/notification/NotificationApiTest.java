package withoutc.chongchong.notification;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static withoutc.chongchong.global.config.ApiPathConfig.API_PREFIX;

import io.restassured.RestAssured;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import withoutc.chongchong.auth.support.TestAuthRequest;
import withoutc.chongchong.notification.entity.Notification;
import withoutc.chongchong.notification.entity.NotificationType;
import withoutc.chongchong.notification.entity.ResourceType;
import withoutc.chongchong.notification.repository.NotificationRepository;
import withoutc.chongchong.support.PostgresContainerTest;
import withoutc.chongchong.support.TestDatabaseCleaner;
import withoutc.chongchong.user.entity.User;
import withoutc.chongchong.user.repository.UserRepository;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class NotificationApiTest extends PostgresContainerTest {

    private static final LocalDateTime EARLIER = LocalDateTime.of(2026, 9, 21, 11, 0);
    private static final LocalDateTime LATER = LocalDateTime.of(2026, 9, 21, 12, 0);
    private static final DateTimeFormatter RESPONSE_DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationRepository notificationRepository;

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

    @Test
    @DisplayName("인증된 사용자는 본인 알림만 최신순으로 조회한다")
    void getMyNotificationsTest() {
        User user = userRepository.saveAndFlush(User.create("알림 사용자", null));
        User otherUser = userRepository.saveAndFlush(User.create("다른 사용자", null));

        Notification earlier = saveNotification(
                user,
                "[스터디] 새 공지",
                "공지 내용",
                NotificationType.CREATED,
                ResourceType.NOTICE,
                10L,
                "/studies/2/notices/10",
                EARLIER
        );
        Notification later = saveNotification(
                user,
                "[스터디] 새 제출물",
                "홍길동 스터디원이 과제를 제출했어요",
                NotificationType.SUBMITTED,
                ResourceType.ASSIGNMENT_SUBMISSION,
                20L,
                "/studies/2/assignments/3/submissions/20",
                LATER
        );
        saveNotification(
                otherUser,
                "다른 사용자의 알림",
                "알림 내용",
                NotificationType.REMIND,
                ResourceType.ASSIGNMENT,
                30L,
                "/studies/2/assignments/30",
                LATER.plusHours(1)
        );
        markAsRead(later);

        testAuthRequest.givenAuthenticatedUser(user.getId())
                .port(port)
                .when()
                .get("/notifications")
                .then()
                .statusCode(200)
                .body("notifications", hasSize(2))
                .body("notifications[0].id", equalTo(later.getId().intValue()))
                .body("notifications[0].title", equalTo("[스터디] 새 제출물"))
                .body("notifications[0].body", equalTo("홍길동 스터디원이 과제를 제출했어요"))
                .body("notifications[0].type", equalTo("SUBMITTED"))
                .body("notifications[0].resourceType", equalTo("ASSIGNMENT_SUBMISSION"))
                .body("notifications[0].resourceId", equalTo(20))
                .body("notifications[0].deepLink", equalTo("/studies/2/assignments/3/submissions/20"))
                .body("notifications[0].isRead", equalTo(true))
                .body("notifications[0].createdAt", equalTo(LATER.format(RESPONSE_DATE_TIME_FORMATTER)))
                .body("notifications[1].id", equalTo(earlier.getId().intValue()))
                .body("notifications[1].resourceType", equalTo("NOTICE"))
                .body("notifications[1].isRead", equalTo(false))
                .body("notifications[1].createdAt", equalTo(EARLIER.format(RESPONSE_DATE_TIME_FORMATTER)));
    }

    @Test
    @DisplayName("알림이 없으면 빈 notifications를 반환한다")
    void getMyNotificationsWhenEmptyTest() {
        User user = userRepository.saveAndFlush(User.create("알림 사용자", null));

        testAuthRequest.givenAuthenticatedUser(user.getId())
                .port(port)
                .when()
                .get("/notifications")
                .then()
                .statusCode(200)
                .body("notifications", empty());
    }

    @Test
    @DisplayName("인증 없이 알림 목록을 조회하면 인증 필요 오류를 반환한다")
    void getMyNotificationsWithoutAuthenticationTest() {
        RestAssured.given()
                .basePath(API_PREFIX)
                .port(port)
                .when()
                .get("/notifications")
                .then()
                .statusCode(401)
                .body("code", equalTo("AUTHENTICATION_REQUIRED"));
    }

    private Notification saveNotification(
            User recipient,
            String title,
            String body,
            NotificationType type,
            ResourceType resourceType,
            Long resourceId,
            String deepLink,
            LocalDateTime createdAt
    ) {
        Notification notification = notificationRepository.saveAndFlush(
                Notification.create(recipient, title, body, type, resourceId, resourceType, deepLink)
        );
        jdbcTemplate.update(
                "UPDATE notifications SET created_at = ?, updated_at = ? WHERE id = ?",
                Timestamp.valueOf(createdAt),
                Timestamp.valueOf(createdAt),
                notification.getId()
        );
        return notification;
    }

    private void markAsRead(Notification notification) {
        jdbcTemplate.update(
                "UPDATE notifications SET is_read = true WHERE id = ?",
                notification.getId()
        );
    }
}
