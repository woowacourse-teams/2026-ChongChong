package withoutc.chongchong.user;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import io.restassured.response.Response;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import withoutc.chongchong.auth.support.TestAuthRequest;
import withoutc.chongchong.support.TestDatabaseCleaner;
import withoutc.chongchong.user.entity.User;
import withoutc.chongchong.user.repository.UserRepository;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class UserProfileAcceptanceTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestAuthRequest testAuthRequest;

    @Autowired
    private TestDatabaseCleaner databaseCleaner;

    @LocalServerPort
    private int port;

    @AfterEach
    void cleanDatabase() {
        databaseCleaner.clean();
    }

    @Test
    @DisplayName("인증된 사용자가 자신의 홈 프로필을 조회한다")
    void getMyProfile() {
        User user = userRepository.saveAndFlush(User.create(
                "이든",
                "https://cdn.example.com/profiles/uuid.webp"
        ));

        Response response = requestProfile(user.getId());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.jsonPath().getMap("$"))
                .containsOnlyKeys("name", "profileImageUrl")
                .containsEntry("name", "이든")
                .containsEntry("profileImageUrl", "https://cdn.example.com/profiles/uuid.webp");
    }

    @Test
    @DisplayName("프로필 이미지가 없는 사용자를 조회하면 profileImageUrl을 null로 반환한다")
    void getMyProfileWithoutProfileImage() {
        User user = userRepository.saveAndFlush(User.create("이든", null));

        Response response = requestProfile(user.getId());
        Map<String, Object> responseBody = response.jsonPath().getMap("$");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(responseBody)
                .containsOnlyKeys("name", "profileImageUrl")
                .containsEntry("name", "이든")
                .containsKey("profileImageUrl");
        assertThat(responseBody.get("profileImageUrl")).isNull();
    }

    @Test
    @DisplayName("인증 없이 홈 프로필을 조회하면 401을 반환한다")
    void rejectUnauthenticatedProfileRequest() {
        Response response = given()
                .port(port)
                .when()
                .get("/api/users/me");

        assertThat(response.statusCode()).isEqualTo(401);
        assertThat(response.jsonPath().getString("code")).isEqualTo("AUTHENTICATION_REQUIRED");
    }

    @Test
    @DisplayName("인증된 사용자가 존재하지 않으면 홈 프로필 조회 시 404를 반환한다")
    void rejectMissingUserProfileRequest() {
        Response response = requestProfile(999L);

        assertThat(response.statusCode()).isEqualTo(404);
        assertThat(response.jsonPath().getString("code")).isEqualTo("USER_NOT_FOUND");
    }

    private Response requestProfile(Long userId) {
        return testAuthRequest.givenAuthenticatedUser(userId)
                .port(port)
                .when()
                .get("/users/me");
    }
}
