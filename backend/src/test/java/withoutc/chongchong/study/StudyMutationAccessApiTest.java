package withoutc.chongchong.study;

import static org.hamcrest.Matchers.equalTo;

import io.restassured.http.ContentType;
import io.restassured.http.Method;
import io.restassured.specification.RequestSpecification;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import withoutc.chongchong.auth.support.TestAuthRequest;
import withoutc.chongchong.support.TestDatabaseCleaner;
import withoutc.chongchong.user.entity.User;
import withoutc.chongchong.user.repository.UserRepository;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class StudyMutationAccessApiTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestDatabaseCleaner databaseCleaner;

    @Autowired
    private TestAuthRequest testAuthRequest;

    @LocalServerPort
    private int port;

    private Long userId;

    @BeforeEach
    void setUp() {
        databaseCleaner.clean();
        userId = userRepository.save(User.create("스터디 외부 사용자", null)).getId();
    }

    @ParameterizedTest
    @MethodSource("mutationRequests")
    @DisplayName("멤버십이 필요한 변경 요청은 스터디 존재 확인보다 접근 거부를 먼저 반환한다")
    void rejectUnknownStudyBeforeResourceLookup(Method method, String suffix, Map<String, Object> body) {
        RequestSpecification request = testAuthRequest.givenAuthenticatedUser(userId).port(port);
        if (body != null) {
            request.contentType(ContentType.JSON).body(body);
        }

        request.request(method, "/studies/" + Long.MAX_VALUE + suffix)
                .then().statusCode(403)
                .body("code", equalTo("STUDY_ACCESS_DENIED"));
    }

    private static Stream<Arguments> mutationRequests() {
        return Stream.of(
                Arguments.of(Method.POST, "/notices", Map.of("title", "공지", "content", "내용")),
                Arguments.of(Method.PATCH, "/notices/1", Map.of("title", "수정 공지")),
                Arguments.of(Method.DELETE, "/notices/1", null),
                Arguments.of(Method.POST, "/assignments", Map.of("title", "과제", "content", "내용",
                        "submissionTarget", "MEMBERS_ONLY", "submissionVisibility", "LEADER_ONLY",
                        "closeAt", "2099-12-31T23:59:00")),
                Arguments.of(Method.PATCH, "/assignments/1", Map.of("title", "수정 과제")),
                Arguments.of(Method.DELETE, "/assignments/1", null),
                Arguments.of(Method.POST, "/assignments/1/submissions", Map.of("content", "제출 내용")),
                Arguments.of(Method.DELETE, "/members/1", null),
                Arguments.of(Method.DELETE, "/members/me", null)
        );
    }
}
