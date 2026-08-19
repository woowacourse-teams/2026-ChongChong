package withoutc.chongchong.study;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;

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
import withoutc.chongchong.study.token.StudyInviteTokenProvider;
import withoutc.chongchong.support.TestDatabaseCleaner;
import withoutc.chongchong.user.entity.User;
import withoutc.chongchong.user.repository.UserRepository;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class StudyMemberAcceptanceTest {

    @Autowired
    private StudyMemberRepository studyMemberRepository;

    @Autowired
    private StudyRepository studyRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestDatabaseCleaner databaseCleaner;

    @Autowired
    private TestAuthRequest testAuthRequest;

    @Autowired
    private StudyInviteTokenProvider studyInviteTokenProvider;

    @LocalServerPort
    private int port;

    @AfterEach
    void cleanDatabase() {
        databaseCleaner.clean();
    }

    @Test
    @DisplayName("유효한 초대 토큰으로 스터디에 참여하면 201을 반환한다")
    void joinStudyTest() {
        User leader = userRepository.saveAndFlush(User.create("리더", "leader-profile-image-url"));
        User user = userRepository.saveAndFlush(User.create("참여자", "user-profile-image-url"));
        Study study = studyRepository.saveAndFlush(Study.create("자바 스터디", "설명"));
        studyMemberRepository.saveAndFlush(
                StudyMember.create(study, leader, leader.getName(), leader.getProfileImageUrl(), StudyMemberRole.LEADER)
        );

        testAuthRequest.givenAuthenticatedUser(user.getId())
                .port(port)
                .contentType(ContentType.JSON)
                .body("""
                        {"token": "%s"}
                        """.formatted(studyInviteTokenProvider.generate(study.getId())))
                .when()
                .post("/studies/join")
                .then()
                .statusCode(201);

        assertThat(studyMemberRepository.findByStudyIdAndUserId(study.getId(), user.getId()))
                .get()
                .satisfies(member -> assertThat(member.getRole()).isEqualTo(StudyMemberRole.MEMBER));
    }

    @Test
    @DisplayName("이미 가입한 스터디에 참여하면 409를 반환한다")
    void joinAlreadyJoinedStudyTest() {
        User user = userRepository.saveAndFlush(User.create("테스트 사용자", "profile-image-url"));
        Study study = studyRepository.saveAndFlush(Study.create("자바 스터디", "설명"));
        studyMemberRepository.saveAndFlush(
                StudyMember.create(study, user, user.getName(), user.getProfileImageUrl(), StudyMemberRole.MEMBER)
        );

        testAuthRequest.givenAuthenticatedUser(user.getId())
                .port(port)
                .contentType(ContentType.JSON)
                .body("""
                        {"token": "%s"}
                        """.formatted(studyInviteTokenProvider.generate(study.getId())))
                .when()
                .post("/studies/join")
                .then()
                .statusCode(409)
                .body("code", equalTo("ALREADY_JOINED_STUDY"));
    }

    @Test
    @DisplayName("유효하지 않은 초대 토큰으로 참여하면 400을 반환한다")
    void joinWithInvalidInviteTokenTest() {
        User user = userRepository.saveAndFlush(User.create("테스트 사용자", "profile-image-url"));

        testAuthRequest.givenAuthenticatedUser(user.getId())
                .port(port)
                .contentType(ContentType.JSON)
                .body("""
                        {"token": "invalid-token"}
                        """)
                .when()
                .post("/studies/join")
                .then()
                .statusCode(400)
                .body("code", equalTo("INVALID_INVITE_TOKEN"));
    }
}
