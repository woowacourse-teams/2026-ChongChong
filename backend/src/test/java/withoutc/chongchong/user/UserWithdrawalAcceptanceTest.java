package withoutc.chongchong.user;

import io.restassured.response.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import withoutc.chongchong.auth.entity.AuthSession;
import withoutc.chongchong.auth.repository.AuthSessionRepository;
import withoutc.chongchong.auth.support.TestAuthRequest;
import withoutc.chongchong.auth.token.HashedRefreshToken;
import withoutc.chongchong.study.entity.Study;
import withoutc.chongchong.study.entity.StudyMember;
import withoutc.chongchong.study.entity.StudyMemberRole;
import withoutc.chongchong.study.repository.StudyMemberRepository;
import withoutc.chongchong.study.repository.StudyRepository;
import withoutc.chongchong.support.TestDatabaseCleaner;
import withoutc.chongchong.user.entity.User;
import withoutc.chongchong.user.repository.UserRepository;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

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

        Response response = testAuthRequest.givenAuthenticatedUser(user.getId())
                .port(port)
                .when()
                .delete("/users/me");

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

        Response response = testAuthRequest.givenAuthenticatedUser(user.getId())
                .port(port)
                .when()
                .delete("/users/me");

        assertThat(response.statusCode()).isEqualTo(204);
        assertThat(response.asString()).isEmpty();
        assertThat(userRepository.existsById(user.getId())).isFalse();
        assertThat(authSessionRepository.existsById(session.getId())).isFalse();
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

        Response response = testAuthRequest.givenAuthenticatedUser(user.getId())
                .port(port)
                .when()
                .delete("/users/me");

        assertThat(response.statusCode()).isEqualTo(409);
        assertThat(response.jsonPath().getString("code"))
                .isEqualTo("STUDY_LEADER_WITHDRAWAL_BLOCKED");
        assertThat(userRepository.existsById(user.getId())).isTrue();
        assertThat(studyRepository.existsById(study.getId())).isTrue();
        assertThat(studyMemberRepository.existsById(leader.getId())).isTrue();
    }
}
