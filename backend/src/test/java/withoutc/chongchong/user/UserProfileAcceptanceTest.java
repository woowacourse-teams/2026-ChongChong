package withoutc.chongchong.user;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import withoutc.chongchong.auth.support.TestAuthRequest;
import withoutc.chongchong.study.entity.Study;
import withoutc.chongchong.study.entity.StudyMember;
import withoutc.chongchong.study.entity.StudyMemberRole;
import withoutc.chongchong.study.repository.StudyMemberRepository;
import withoutc.chongchong.study.repository.StudyRepository;
import withoutc.chongchong.support.TestDatabaseCleaner;
import withoutc.chongchong.user.entity.User;
import withoutc.chongchong.user.repository.UserRepository;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class UserProfileAcceptanceTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StudyRepository studyRepository;

    @Autowired
    private StudyMemberRepository studyMemberRepository;

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

    @Test
    @DisplayName("홈 이름을 수정하면 변경된 프로필을 반환하고 이름을 저장한다")
    void updateMyProfileName() {
        User user = userRepository.saveAndFlush(User.create(
                "기존이름",
                "https://cdn.example.com/profiles/uuid.webp"
        ));

        Response response = updateProfileName(user.getId(), "{\"name\":\"바니\"}");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.jsonPath().getMap("$"))
                .containsOnlyKeys("name", "profileImageUrl")
                .containsEntry("name", "바니")
                .containsEntry("profileImageUrl", "https://cdn.example.com/profiles/uuid.webp");
        assertThat(userRepository.findById(user.getId()))
                .get()
                .satisfies(updatedUser -> {
                    assertThat(updatedUser.getName()).isEqualTo("바니");
                    assertThat(updatedUser.getProfileImageUrl())
                            .isEqualTo("https://cdn.example.com/profiles/uuid.webp");
                });
    }

    @Test
    @DisplayName("프로필 이미지가 없으면 이름 수정 응답에도 profileImageUrl을 null로 포함한다")
    void updateMyProfileNameWithoutProfileImage() {
        User user = userRepository.saveAndFlush(User.create("기존이름", null));

        Response response = updateProfileName(user.getId(), "{\"name\":\"바니\"}");
        Map<String, Object> responseBody = response.jsonPath().getMap("$");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(responseBody).containsOnlyKeys("name", "profileImageUrl");
        assertThat(responseBody.get("name")).isEqualTo("바니");
        assertThat(responseBody.get("profileImageUrl")).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{}",
            "{\"name\":null}",
            "{\"name\":\"\"}",
            "{\"name\":\" \"}",
            "{\"name\":\"가나다라마바사아자\"}"
    })
    @DisplayName("유효하지 않은 이름은 거부하고 홈과 스터디의 기존 이름을 유지한다")
    void rejectInvalidProfileNameUpdate(String requestBody) {
        User user = userRepository.saveAndFlush(User.create("기존이름", null));
        Study study = studyRepository.saveAndFlush(Study.create("기존 스터디", null));
        StudyMember member = studyMemberRepository.saveAndFlush(StudyMember.create(
                study, user, user.getName(), null, StudyMemberRole.MEMBER
        ));

        Response response = updateProfileName(user.getId(), requestBody);

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.jsonPath().getString("code")).isEqualTo("INVALID_USER_NAME");
        assertThat(userRepository.findById(user.getId()))
                .get()
                .extracting(User::getName)
                .isEqualTo("기존이름");
        assertThat(studyMemberRepository.findById(member.getId()))
                .get()
                .extracting(StudyMember::getName)
                .isEqualTo("기존이름");
    }

    @Test
    @DisplayName("인증 없이 홈 이름을 수정하면 401을 반환한다")
    void rejectUnauthenticatedProfileNameUpdate() {
        Response response = given()
                .port(port)
                .contentType(ContentType.JSON)
                .body("{\"name\":\"바니\"}")
                .when()
                .patch("/api/users/me");

        assertThat(response.statusCode()).isEqualTo(401);
        assertThat(response.jsonPath().getString("code")).isEqualTo("AUTHENTICATION_REQUIRED");
    }

    @Test
    @DisplayName("인증된 사용자가 존재하지 않으면 홈 이름 수정 시 404를 반환한다")
    void rejectMissingUserProfileNameUpdate() {
        Response response = updateProfileName(999L, "{\"name\":\"바니\"}");

        assertThat(response.statusCode()).isEqualTo(404);
        assertThat(response.jsonPath().getString("code")).isEqualTo("USER_NOT_FOUND");
    }

    @Test
    @DisplayName("홈 이름 수정은 모든 참여 스터디에 반영하고 이후 새 스터디에도 변경된 이름을 복사한다")
    void updateHomeNameAcrossExistingAndNewStudies() {
        User user = userRepository.saveAndFlush(User.create(
                "기존이름", "https://cdn.example.com/profiles/user.webp"
        ));
        User otherUser = userRepository.saveAndFlush(User.create("다른이름", null));
        Study leaderStudy = studyRepository.saveAndFlush(Study.create("리더 스터디", null));
        Study memberStudy = studyRepository.saveAndFlush(Study.create("멤버 스터디", null));
        StudyMember leader = studyMemberRepository.saveAndFlush(StudyMember.create(
                leaderStudy, user, user.getName(), "https://cdn.example.com/profiles/leader.webp",
                StudyMemberRole.LEADER
        ));
        StudyMember member = studyMemberRepository.saveAndFlush(StudyMember.create(
                memberStudy, user, user.getName(), "https://cdn.example.com/profiles/member.webp",
                StudyMemberRole.MEMBER
        ));
        StudyMember otherMember = studyMemberRepository.saveAndFlush(StudyMember.create(
                leaderStudy, otherUser, otherUser.getName(), null, StudyMemberRole.MEMBER
        ));

        Response updateResponse = updateProfileName(user.getId(), "{\"name\":\"새이름\"}");

        assertThat(updateResponse.statusCode()).isEqualTo(200);
        assertThat(updateResponse.jsonPath().getMap("$"))
                .containsEntry("name", "새이름")
                .containsEntry("profileImageUrl", "https://cdn.example.com/profiles/user.webp");
        assertThat(studyMemberRepository.findById(leader.getId()))
                .get()
                .satisfies(updatedLeader -> {
                    assertThat(updatedLeader.getName()).isEqualTo("새이름");
                    assertThat(updatedLeader.getProfileImageUrl())
                            .isEqualTo("https://cdn.example.com/profiles/leader.webp");
                });
        assertThat(studyMemberRepository.findById(member.getId()))
                .get()
                .satisfies(updatedMember -> {
                    assertThat(updatedMember.getName()).isEqualTo("새이름");
                    assertThat(updatedMember.getProfileImageUrl())
                            .isEqualTo("https://cdn.example.com/profiles/member.webp");
                });
        assertThat(studyMemberRepository.findById(otherMember.getId()))
                .get()
                .extracting(StudyMember::getName)
                .isEqualTo("다른이름");
        assertThat(userRepository.findById(otherUser.getId()))
                .get()
                .extracting(User::getName)
                .isEqualTo("다른이름");

        Response createResponse = testAuthRequest.givenAuthenticatedUser(user.getId())
                .port(port)
                .contentType(ContentType.JSON)
                .body("{\"name\":\"새 스터디\"}")
                .when()
                .post("/studies");

        assertThat(createResponse.statusCode()).isEqualTo(201);
        Long newStudyId = createResponse.jsonPath().getLong("studyId");
        assertThat(studyMemberRepository.getByStudyIdAndUserIdOrThrow(newStudyId, user.getId()).getName())
                .isEqualTo("새이름");
    }

    private Response requestProfile(Long userId) {
        return testAuthRequest.givenAuthenticatedUser(userId)
                .port(port)
                .when()
                .get("/users/me");
    }

    private Response updateProfileName(Long userId, String requestBody) {
        return testAuthRequest.givenAuthenticatedUser(userId)
                .port(port)
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .patch("/users/me");
    }
}
