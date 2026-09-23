package withoutc.chongchong.user;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;

import io.restassured.response.Response;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import withoutc.chongchong.assignment.entity.Assignment;
import withoutc.chongchong.assignment.entity.SubmissionTarget;
import withoutc.chongchong.assignment.repository.AssignmentRepository;
import withoutc.chongchong.assignment.repository.AssignmentSubmissionRepository;
import withoutc.chongchong.auth.entity.AuthSession;
import withoutc.chongchong.auth.entity.SocialAccount;
import withoutc.chongchong.auth.repository.AuthSessionRepository;
import withoutc.chongchong.auth.repository.SocialAccountRepository;
import withoutc.chongchong.auth.social.SocialProvider;
import withoutc.chongchong.auth.support.TestAuthRequest;
import withoutc.chongchong.auth.token.HashedRefreshToken;
import withoutc.chongchong.notice.entity.Notice;
import withoutc.chongchong.notice.repository.NoticeRecipientRepository;
import withoutc.chongchong.notice.repository.NoticeRepository;
import withoutc.chongchong.notification.entity.DevicePlatform;
import withoutc.chongchong.notification.entity.Notification;
import withoutc.chongchong.notification.entity.NotificationDelivery;
import withoutc.chongchong.notification.entity.NotificationType;
import withoutc.chongchong.notification.entity.PushToken;
import withoutc.chongchong.notification.entity.ResourceType;
import withoutc.chongchong.notification.entity.TokenProvider;
import withoutc.chongchong.notification.repository.NotificationDeliveryRepository;
import withoutc.chongchong.notification.repository.NotificationRepository;
import withoutc.chongchong.notification.repository.PushTokenRepository;
import withoutc.chongchong.study.entity.Study;
import withoutc.chongchong.study.entity.StudyMember;
import withoutc.chongchong.study.entity.StudyMemberRole;
import withoutc.chongchong.study.repository.StudyMemberRepository;
import withoutc.chongchong.study.repository.StudyRepository;
import withoutc.chongchong.support.TestDatabaseCleaner;
import withoutc.chongchong.user.entity.User;
import withoutc.chongchong.user.repository.UserRepository;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.datasource.url=jdbc:h2:mem:user-withdrawal-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
                "spring.jpa.hibernate.ddl-auto=validate",
                "spring.flyway.enabled=true"
        }
)
@ActiveProfiles("test")
public class UserWithdrawalAcceptanceTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuthSessionRepository authSessionRepository;

    @Autowired
    private SocialAccountRepository socialAccountRepository;

    @Autowired
    private PushTokenRepository pushTokenRepository;

    @Autowired
    private NoticeRepository noticeRepository;

    @Autowired
    private NoticeRecipientRepository noticeRecipientRepository;

    @Autowired
    private AssignmentRepository assignmentRepository;

    @Autowired
    private AssignmentSubmissionRepository assignmentSubmissionRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private NotificationDeliveryRepository notificationDeliveryRepository;

    @Autowired
    private TestAuthRequest testAuthRequest;

    @Autowired
    private StudyRepository studyRepository;

    @Autowired
    private StudyMemberRepository studyMemberRepository;

    @Autowired
    private TestDatabaseCleaner databaseCleaner;

    @LocalServerPort
    private int port;

    @AfterEach
    void cleanDatabase() {
        databaseCleaner.clean();
    }

    @Test
    @DisplayName("사용자 탈퇴")
    void withdrawUser() {
        User user = userRepository.saveAndFlush(
                User.create("탈퇴할 사용자", null)
        );

        Response response = requestWithdrawal(user.getId());

        assertThat(response.statusCode()).isEqualTo(204);
        assertThat(response.asString()).isEmpty();
        assertThat(userRepository.existsById(user.getId())).isFalse();
    }

    @Test
    @DisplayName("인증 세션이 있는 사용자가 탈퇴하면 세션과 계정이 함께 삭제된다")
    void withdrawUserWithAuthSession() {
        User user = userRepository.saveAndFlush(User.create("탈퇴할 사용자", null));
        AuthSession session = authSessionRepository.saveAndFlush(AuthSession.create(
                user,
                new HashedRefreshToken("a".repeat(64)),
                Instant.parse("2030-01-01T00:00:00Z")
        ));

        Response response = requestWithdrawal(user.getId());

        assertThat(response.statusCode()).isEqualTo(204);
        assertThat(response.asString()).isEmpty();
        assertThat(userRepository.existsById(user.getId())).isFalse();
        assertThat(authSessionRepository.existsById(session.getId())).isFalse();
    }

    @Test
    @DisplayName("탈퇴한 사용자의 소셜 계정과 푸시 토큰만 삭제한다")
    void withdrawUserDeletesDirectDependenciesOnly() {
        User withdrawingUser = userRepository.saveAndFlush(User.create("탈퇴할 사용자", null));
        User remainingUser = userRepository.saveAndFlush(User.create("남는 사용자", null));
        SocialAccount withdrawingAccount = socialAccountRepository.saveAndFlush(
                SocialAccount.create(withdrawingUser, SocialProvider.GOOGLE, "withdraw-google")
        );
        SocialAccount remainingAccount = socialAccountRepository.saveAndFlush(
                SocialAccount.create(remainingUser, SocialProvider.KAKAO, "remain-kakao")
        );
        PushToken withdrawingToken = pushTokenRepository.saveAndFlush(PushToken.create(
                withdrawingUser, "withdraw-installation", TokenProvider.EXPO, "withdraw-token", DevicePlatform.ANDROID
        ));
        PushToken remainingToken = pushTokenRepository.saveAndFlush(PushToken.create(
                remainingUser, "remain-installation", TokenProvider.EXPO, "remain-token", DevicePlatform.ANDROID
        ));

        Response response = requestWithdrawal(withdrawingUser.getId());

        assertThat(response.statusCode()).isEqualTo(204);
        assertThat(userRepository.existsById(withdrawingUser.getId())).isFalse();
        assertThat(socialAccountRepository.existsById(withdrawingAccount.getId())).isFalse();
        assertThat(pushTokenRepository.existsById(withdrawingToken.getId())).isFalse();
        assertThat(userRepository.existsById(remainingUser.getId())).isTrue();
        assertThat(socialAccountRepository.existsById(remainingAccount.getId())).isTrue();
        assertThat(pushTokenRepository.existsById(remainingToken.getId())).isTrue();
    }

    @Test
    @DisplayName("일반 멤버 탈퇴 시 활동 데이터만 삭제하고 스터디와 다른 멤버의 데이터는 보존한다")
    void withdrawMemberDeletesActivityOnly() {
        User leaderUser = userRepository.saveAndFlush(User.create("리더", null));
        User withdrawingUser = userRepository.saveAndFlush(User.create("탈퇴할 멤버", null));
        User remainingUser = userRepository.saveAndFlush(User.create("남는 멤버", null));
        Study study = studyRepository.saveAndFlush(Study.create("자바 스터디", "설명"));
        StudyMember leader = studyMemberRepository.saveAndFlush(StudyMember.create(
                study, leaderUser, leaderUser.getName(), null, StudyMemberRole.LEADER
        ));
        StudyMember withdrawingMember = studyMemberRepository.saveAndFlush(StudyMember.create(
                study, withdrawingUser, withdrawingUser.getName(), null, StudyMemberRole.MEMBER
        ));
        StudyMember remainingMember = studyMemberRepository.saveAndFlush(StudyMember.create(
                study, remainingUser, remainingUser.getName(), null, StudyMemberRole.MEMBER
        ));

        Notice notice = Notice.create(study, "공지", "내용");
        notice.addRecipients(List.of(withdrawingMember, remainingMember));
        noticeRepository.saveAndFlush(notice);

        LocalDateTime now = LocalDateTime.of(2026, 9, 20, 12, 0);
        Assignment assignment = Assignment.create(study, "과제", "내용", "링크",
                SubmissionTarget.MEMBERS_ONLY, now.plusDays(1), now);
        assignment.initializeSubmissions(List.of(withdrawingMember, remainingMember));
        assignmentRepository.saveAndFlush(assignment);

        Notification withdrawingNotification = notificationRepository.saveAndFlush(Notification.create(
                withdrawingUser,
                "[자바 스터디] 새 공지",
                "내용",
                NotificationType.REMIND,
                notice.getId(),
                ResourceType.NOTICE,
                "/studies/%d/notices/%d".formatted(study.getId(), notice.getId())
        ));
        Notification remainingNotification = notificationRepository.saveAndFlush(Notification.create(
                remainingUser,
                "[자바 스터디] 새 공지",
                "내용",
                NotificationType.REMIND,
                notice.getId(),
                ResourceType.NOTICE,
                "/studies/%d/notices/%d".formatted(study.getId(), notice.getId())
        ));
        PushToken withdrawingToken = pushTokenRepository.saveAndFlush(PushToken.create(
                withdrawingUser, "withdraw-member-installation", TokenProvider.EXPO, "withdraw-token",
                DevicePlatform.ANDROID
        ));
        PushToken remainingToken = pushTokenRepository.saveAndFlush(PushToken.create(
                remainingUser, "remain-member-installation", TokenProvider.EXPO, "remain-token", DevicePlatform.ANDROID
        ));
        NotificationDelivery withdrawingDelivery = notificationDeliveryRepository.saveAndFlush(
                NotificationDelivery.create(withdrawingNotification, withdrawingToken)
        );
        NotificationDelivery remainingDelivery = notificationDeliveryRepository.saveAndFlush(
                NotificationDelivery.create(remainingNotification, remainingToken)
        );

        Response response = requestWithdrawal(withdrawingUser.getId());

        assertThat(response.statusCode()).isEqualTo(204);
        assertThat(userRepository.existsById(withdrawingUser.getId())).isFalse();
        assertThat(studyMemberRepository.existsById(withdrawingMember.getId())).isFalse();
        assertThat(noticeRecipientRepository.findByNoticeIdAndMemberId(notice.getId(), withdrawingMember.getId()))
                .isEmpty();
        assertThat(assignmentSubmissionRepository.findByAssignmentIdAndMemberId(
                assignment.getId(), withdrawingMember.getId())).isEmpty();
        assertThat(notificationRepository.existsById(withdrawingNotification.getId())).isFalse();
        assertThat(notificationDeliveryRepository.existsById(withdrawingDelivery.getId())).isFalse();
        assertThat(pushTokenRepository.existsById(withdrawingToken.getId())).isFalse();

        assertThat(studyRepository.existsById(study.getId())).isTrue();
        assertThat(studyMemberRepository.existsById(leader.getId())).isTrue();
        assertThat(studyMemberRepository.existsById(remainingMember.getId())).isTrue();
        assertThat(noticeRepository.existsById(notice.getId())).isTrue();
        assertThat(assignmentRepository.existsById(assignment.getId())).isTrue();
        assertThat(noticeRecipientRepository.findByNoticeIdAndMemberId(notice.getId(), remainingMember.getId()))
                .isPresent();
        assertThat(assignmentSubmissionRepository.findByAssignmentIdAndMemberId(
                assignment.getId(), remainingMember.getId())).isPresent();
        assertThat(notificationRepository.existsById(remainingNotification.getId())).isTrue();
        assertThat(notificationDeliveryRepository.existsById(remainingDelivery.getId())).isTrue();
    }

    @Test
    @DisplayName("인증 없이 탈퇴를 요청하면 401을 반환한다")
    void cannotWithdrawWithoutAuthentication() {
        Response response = given()
                .port(port)
                .when()
                .delete("/api/users/me");

        assertThat(response.statusCode()).isEqualTo(401);
        assertThat(response.jsonPath().getString("code")).isEqualTo("AUTHENTICATION_REQUIRED");
        assertThat(response.header(HttpHeaders.SET_COOKIE)).isNull();
    }

    @Test
    @DisplayName("인증된 사용자가 존재하지 않으면 404를 반환한다")
    void cannotWithdrawMissingUser() {
        Response response = requestWithdrawal(999L);

        assertThat(response.statusCode()).isEqualTo(404);
        assertThat(response.jsonPath().getString("code")).isEqualTo("USER_NOT_FOUND");
        assertThat(response.header(HttpHeaders.SET_COOKIE)).isNull();
    }

    @Test
    void cannotWithdrawWhileLeadingStudy() {
        User user = userRepository.saveAndFlush(User.create("스터디 리더", null));
        Study study = studyRepository.saveAndFlush(Study.create("자바 스터디", "설명"));
        StudyMember leader = studyMemberRepository.saveAndFlush(
                StudyMember.create(
                        study, user, user.getName(), user.getProfileImageUrl(),
                        StudyMemberRole.LEADER
                )
        );

        Response response = requestWithdrawal(user.getId());

        assertThat(response.statusCode()).isEqualTo(409);
        assertThat(response.jsonPath().getString("code"))
                .isEqualTo("STUDY_LEADER_WITHDRAWAL_BLOCKED");
        assertThat(response.header(HttpHeaders.SET_COOKIE)).isNull();
        assertThat(userRepository.existsById(user.getId())).isTrue();
        assertThat(studyRepository.existsById(study.getId())).isTrue();
        assertThat(studyMemberRepository.existsById(leader.getId())).isTrue();
    }

    @Test
    @DisplayName("사용자가 탈퇴하면 계정과 Refresh Cookie를 정리한다")
    void withdrawUserAndDeleteRefreshCookie() {
        User user = userRepository.saveAndFlush(
                User.create("탈퇴할 사용자", null)
        );

        Response response = requestWithdrawal(user.getId());

        assertExpiredRefreshCookie(response);

        assertThat(response.asString()).isEmpty();
        assertThat(userRepository.existsById(user.getId())).isFalse();
    }

    private Response requestWithdrawal(Long userId) {
        return testAuthRequest.givenAuthenticatedUser(userId)
                .port(port)
                .when()
                .delete("/users/me");
    }

    private void assertExpiredRefreshCookie(Response response) {
        response.then()
                .statusCode(204)
                .header(HttpHeaders.SET_COOKIE, containsString("refresh_token="))
                .header(HttpHeaders.SET_COOKIE, containsString("Max-Age=0"))
                .header(HttpHeaders.SET_COOKIE, containsString("Path=/api/auth"))
                .header(HttpHeaders.SET_COOKIE, containsString("Secure"))
                .header(HttpHeaders.SET_COOKIE, containsString("HttpOnly"))
                .header(HttpHeaders.SET_COOKIE, containsString("SameSite=Lax"));
    }
}
