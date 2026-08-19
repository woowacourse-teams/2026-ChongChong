package withoutc.chongchong.study;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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
class StudyAcceptanceTest {

    @Autowired
    private StudyRepository studyRepository;

    @Autowired
    private StudyMemberRepository studyMemberRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestDatabaseCleaner databaseCleaner;

    @Autowired
    private TestAuthRequest testAuthRequest;

    @LocalServerPort
    private int port;

    @AfterEach
    void cleanDatabase() {
        databaseCleaner.clean();
    }

    @Test
    @DisplayName("스터디 생성 요청을 보내면 201을 반환하고 생성자를 리더로 등록한다")
    void createStudyTest() {
        User user = userRepository.saveAndFlush(User.create("테스트 사용자", "profile-image-url"));

        testAuthRequest.givenAuthenticatedUser(user.getId())
                .port(port)
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "name": "자바 스터디",
                          "description": "매주 월요일에 진행한다."
                        }
                        """)
                .when()
                .post("/studies")
                .then()
                .statusCode(201)
                .body("id", notNullValue());

        assertThat(studyRepository.findAll())
                .extracting(Study::getName, Study::getDescription)
                .contains(tuple("자바 스터디", "매주 월요일에 진행한다."));

        assertThat(studyMemberRepository.findAll())
                .hasSize(1)
                .first()
                .satisfies(studyMember -> {
                    assertThat(studyMember.getUser().getId()).isEqualTo(user.getId());
                    assertThat(studyMember.getRole()).isEqualTo(StudyMemberRole.LEADER);
                    assertThat(studyMember.getName()).isEqualTo(user.getName());
                    assertThat(studyMember.getProfileImageUrl()).isEqualTo(user.getProfileImageUrl());
                });
    }

    @Test
    @DisplayName("스터디 초대 링크 조회 요청을 보내면 초대 링크를 반환한다")
    void getInviteLinkTest() {
        User user = userRepository.saveAndFlush(User.create("테스트 사용자", "profile-image-url"));
        Study study = studyRepository.saveAndFlush(Study.create("자바 스터디", "설명"));
        studyMemberRepository.saveAndFlush(
                StudyMember.create(study, user, user.getName(), user.getProfileImageUrl(), StudyMemberRole.MEMBER)
        );
        assertThat(study.getId()).isNotNull();

        testAuthRequest.givenAuthenticatedUser(user.getId())
                .port(port)
                .when()
                .get("/studies/{studyId}/invite-link", study.getId())
                .then()
                .statusCode(200)
                .body(
                        "inviteLink",
                        startsWith("https://test.chongchong.app/join?token=")
                );
    }

    @Test
    @DisplayName("스터디 멤버가 아닌 사용자가 초대 링크를 조회하면 403을 반환한다")
    void getInviteLinkForNonMemberTest() {
        User user = userRepository.saveAndFlush(User.create("테스트 사용자", "profile-image-url"));
        Study study = studyRepository.saveAndFlush(Study.create("자바 스터디", "설명"));

        testAuthRequest.givenAuthenticatedUser(user.getId())
                .port(port)
                .when()
                .get("/studies/{studyId}/invite-link", study.getId())
                .then()
                .statusCode(403)
                .body("code", equalTo("NOT_STUDY_MEMBER"));
    }
}
