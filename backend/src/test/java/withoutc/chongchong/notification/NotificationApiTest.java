package withoutc.chongchong.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static withoutc.chongchong.global.config.ApiPathConfig.API_PREFIX;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
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
                NotificationType.NEW,
                ResourceType.NOTICE,
                10L,
                "/studies/2/notices/10",
                EARLIER
        );
        Notification later = saveNotification(
                user,
                "[스터디] 새 제출물",
                "홍길동 스터디원이 과제를 제출했어요",
                NotificationType.NEW,
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
                .body("nextCursor", nullValue())
                .body("hasNext", equalTo(false))
                .body("notifications", hasSize(2))
                .body("notifications[0].id", equalTo(later.getId().intValue()))
                .body("notifications[0].title", equalTo("[스터디] 새 제출물"))
                .body("notifications[0].body", equalTo("홍길동 스터디원이 과제를 제출했어요"))
                .body("notifications[0].type", equalTo("NEW"))
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
    @DisplayName("알림 목록은 페이지 크기를 생략하면 10개를 반환한다")
    void getMyNotificationsWithDefaultSizeTest() {
        User user = userRepository.saveAndFlush(User.create("알림 사용자", null));
        List<Notification> notifications = new ArrayList<>();
        for (int index = 0; index < 11; index++) {
            notifications.add(saveNotification(user));
        }

        testAuthRequest.givenAuthenticatedUser(user.getId())
                .port(port)
                .when()
                .get("/notifications")
                .then()
                .statusCode(200)
                .body("notifications", hasSize(10))
                .body("notifications[0].id", equalTo(notifications.getLast().getId().intValue()))
                .body("notifications[9].id", equalTo(notifications.get(1).getId().intValue()))
                .body("nextCursor", equalTo(notifications.get(1).getId().intValue()))
                .body("hasNext", equalTo(true));
    }

    @Test
    @DisplayName("새 알림 추가와 커서 알림 삭제 후에도 본인 알림의 다음 페이지를 중복 없이 조회한다")
    void getMyNotificationsByCursorTest() {
        User user = userRepository.saveAndFlush(User.create("알림 사용자", null));
        User otherUser = userRepository.saveAndFlush(User.create("다른 사용자", null));
        Notification first = saveNotification(user);
        Notification second = saveNotification(user);
        saveNotification(otherUser);
        Notification third = saveNotification(user);
        Notification fourth = saveNotification(user);

        Response firstPage = testAuthRequest.givenAuthenticatedUser(user.getId())
                .port(port)
                .queryParam("size", 2)
                .when()
                .get("/notifications")
                .then()
                .statusCode(200)
                .body("notifications.id", equalTo(List.of(fourth.getId().intValue(), third.getId().intValue())))
                .body("nextCursor", equalTo(third.getId().intValue()))
                .body("hasNext", equalTo(true))
                .extract().response();

        Long cursor = firstPage.jsonPath().getLong("nextCursor");
        saveNotification(user);
        notificationRepository.deleteById(cursor);

        testAuthRequest.givenAuthenticatedUser(user.getId())
                .port(port)
                .queryParam("cursor", cursor)
                .queryParam("size", 2)
                .when()
                .get("/notifications")
                .then()
                .statusCode(200)
                .body("notifications.id", equalTo(List.of(second.getId().intValue(), first.getId().intValue())))
                .body("nextCursor", nullValue())
                .body("hasNext", equalTo(false));

        testAuthRequest.givenAuthenticatedUser(user.getId())
                .port(port)
                .queryParam("cursor", first.getId())
                .queryParam("size", 2)
                .when()
                .get("/notifications")
                .then()
                .statusCode(200)
                .body("notifications", empty())
                .body("nextCursor", nullValue())
                .body("hasNext", equalTo(false));
    }

    @ParameterizedTest
    @CsvSource({"cursor, 0", "cursor, -1", "size, 0", "size, -1", "size, 101", "size, 2147483647"})
    @DisplayName("알림 커서가 양수가 아니거나 페이지 크기가 범위를 벗어나면 파라미터 오류를 반환한다")
    void getMyNotificationsWithInvalidPageParameterTest(String field, long value) {
        User user = userRepository.saveAndFlush(User.create("알림 사용자", null));

        testAuthRequest.givenAuthenticatedUser(user.getId())
                .port(port)
                .queryParam(field, value)
                .when()
                .get("/notifications")
                .then()
                .statusCode(400)
                .body("code", equalTo("INVALID_REQUEST_PARAMETER"))
                .body("errors.field", hasItem(field));
    }

    @ParameterizedTest
    @ValueSource(strings = {"cursor", "size"})
    @DisplayName("알림 커서나 페이지 크기가 숫자가 아니면 잘못된 요청 오류를 반환한다")
    void getMyNotificationsWithMalformedPageParameterTest(String field) {
        User user = userRepository.saveAndFlush(User.create("알림 사용자", null));

        testAuthRequest.givenAuthenticatedUser(user.getId())
                .port(port)
                .queryParam(field, "invalid")
                .when()
                .get("/notifications")
                .then()
                .statusCode(400)
                .body("code", equalTo("INVALID_REQUEST"));
    }

    @Test
    @DisplayName("사용자는 자신의 알림을 읽음 처리하고 다시 요청해도 읽음 상태를 유지한다")
    void readNotificationTest() {
        User user = userRepository.saveAndFlush(User.create("알림 사용자", null));
        Notification notification = saveNotification(
                user,
                "[스터디] 새 공지",
                "공지 내용",
                NotificationType.NEW,
                ResourceType.NOTICE,
                10L,
                "/studies/2/notices/10",
                EARLIER
        );

        testAuthRequest.givenAuthenticatedUser(user.getId())
                .port(port)
                .when()
                .patch("/notifications/{notificationId}", notification.getId())
                .then()
                .statusCode(204);

        Boolean isRead = jdbcTemplate.queryForObject(
                "SELECT is_read FROM notifications WHERE id = ?",
                Boolean.class,
                notification.getId()
        );
        assertThat(isRead).isTrue();

        testAuthRequest.givenAuthenticatedUser(user.getId())
                .port(port)
                .when()
                .patch("/notifications/{notificationId}", notification.getId())
                .then()
                .statusCode(204);
    }

    @Test
    @DisplayName("다른 사용자의 알림을 읽음 처리하면 알림 없음 오류를 반환한다")
    void readOtherUserNotificationTest() {
        User user = userRepository.saveAndFlush(User.create("알림 사용자", null));
        User otherUser = userRepository.saveAndFlush(User.create("다른 사용자", null));
        Notification notification = saveNotification(
                otherUser,
                "[스터디] 새 공지",
                "공지 내용",
                NotificationType.NEW,
                ResourceType.NOTICE,
                10L,
                "/studies/2/notices/10",
                EARLIER
        );

        testAuthRequest.givenAuthenticatedUser(user.getId())
                .port(port)
                .when()
                .patch("/notifications/{notificationId}", notification.getId())
                .then()
                .statusCode(404)
                .body("code", equalTo("NOTIFICATION_NOT_FOUND"));
    }

    @Test
    @DisplayName("존재하지 않는 알림을 읽음 처리하면 알림 없음 오류를 반환한다")
    void readMissingNotificationTest() {
        User user = userRepository.saveAndFlush(User.create("알림 사용자", null));

        testAuthRequest.givenAuthenticatedUser(user.getId())
                .port(port)
                .when()
                .patch("/notifications/{notificationId}", 999999L)
                .then()
                .statusCode(404)
                .body("code", equalTo("NOTIFICATION_NOT_FOUND"));
    }

    @Test
    @DisplayName("인증 없이 알림을 읽음 처리하면 인증 필요 오류를 반환한다")
    void readNotificationWithoutAuthenticationTest() {
        RestAssured.given()
                .basePath(API_PREFIX)
                .port(port)
                .when()
                .patch("/notifications/{notificationId}", 1L)
                .then()
                .statusCode(401)
                .body("code", equalTo("AUTHENTICATION_REQUIRED"));
    }

    @Test
    @DisplayName("양수가 아닌 알림 ID로 읽음 처리하면 파라미터 오류를 반환한다")
    void readNotificationWithInvalidIdTest() {
        User user = userRepository.saveAndFlush(User.create("알림 사용자", null));

        testAuthRequest.givenAuthenticatedUser(user.getId())
                .port(port)
                .when()
                .patch("/notifications/{notificationId}", 0L)
                .then()
                .statusCode(400)
                .body("code", equalTo("INVALID_REQUEST_PARAMETER"))
                .body("errors.field", hasItem("notificationId"));
    }

    @Test
    @DisplayName("알림이 없으면 최대 페이지 크기로 조회해도 빈 페이지를 반환한다")
    void getMyNotificationsWhenEmptyTest() {
        User user = userRepository.saveAndFlush(User.create("알림 사용자", null));

        testAuthRequest.givenAuthenticatedUser(user.getId())
                .port(port)
                .queryParam("size", 100)
                .when()
                .get("/notifications")
                .then()
                .statusCode(200)
                .body("notifications", empty())
                .body("nextCursor", nullValue())
                .body("hasNext", equalTo(false));
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

    private Notification saveNotification(User recipient) {
        return saveNotification(recipient, "[스터디] 새 공지", "공지 내용", NotificationType.NEW,
                ResourceType.NOTICE, 10L, "/studies/2/notices/10", LATER);
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
