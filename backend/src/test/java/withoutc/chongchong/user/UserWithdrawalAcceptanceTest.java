package withoutc.chongchong.user;

import io.restassured.response.Response;
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

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class UserWithdrawalAcceptanceTest {

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
}
