package withoutc.chongchong.study;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static withoutc.chongchong.global.config.ApiPathConfig.API_PREFIX;

import io.restassured.http.ContentType;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import withoutc.chongchong.assignment.entity.Assignment;
import withoutc.chongchong.assignment.repository.AssignmentRepository;
import withoutc.chongchong.assignment.repository.AssignmentSubmissionRepository;
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
    private AssignmentRepository assignmentRepository;

    @Autowired
    private AssignmentSubmissionRepository assignmentSubmissionRepository;

    @Autowired
    private TestDatabaseCleaner databaseCleaner;

    @Autowired
    private TestAuthRequest testAuthRequest;

    @Autowired
    private StudyInviteTokenProvider studyInviteTokenProvider;

    @Autowired
    private Clock clock;

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
    @DisplayName("기존 과제가 있는 스터디에 가입해도 제출 정보를 생성하지 않는다")
    void joinStudyWithExistingAssignmentTest() {
        User leader = userRepository.saveAndFlush(User.create("리더", "leader-profile-image-url"));
        User user = userRepository.saveAndFlush(User.create("참여자", "user-profile-image-url"));
        Study study = studyRepository.saveAndFlush(Study.create("자바 스터디", "설명"));
        StudyMember leaderMember = studyMemberRepository.saveAndFlush(
                StudyMember.create(study, leader, leader.getName(), leader.getProfileImageUrl(),
                        StudyMemberRole.LEADER)
        );
        LocalDateTime now = LocalDateTime.now(clock);
        Assignment assignment = assignmentRepository.saveAndFlush(
                Assignment.create(
                        leaderMember,
                        "기존 과제",
                        "과제 내용",
                        "링크 제출",
                        now.plusDays(1),
                        now
                )
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

        testAuthRequest.givenAuthenticatedUser(user.getId())
                .port(port)
                .when()
                .get("/studies/{studyId}/assignments", study.getId())
                .then()
                .statusCode(200)
                .body("assignments", hasSize(0));

        StudyMember joinedMember = studyMemberRepository
                .getByStudyIdAndUserIdOrThrow(study.getId(), user.getId());
        assertThat(assignmentSubmissionRepository.findMySubmissionStatusesByAssignmentIdsAndMemberId(
                List.of(assignment.getId()), joinedMember.getId()))
                .isEmpty();
    }

    @Test
    @DisplayName("스터디 참여 요청의 토큰 검증에 실패하면 한글 검증 사유를 반환한다")
    void joinStudyWithInvalidRequestTest() {
        User user = userRepository.saveAndFlush(User.create("테스트 사용자", "profile-image-url"));

        testAuthRequest.givenAuthenticatedUser(user.getId())
                .port(port)
                .contentType(ContentType.JSON)
                .body("""
                        {"token": ""}
                        """)
                .when()
                .post("/studies/join")
                .then()
                .statusCode(400)
                .body("code", equalTo("INVALID_INPUT_VALUE"))
                .body("errors[0].field", equalTo("token"))
                .body("errors[0].reason", equalTo("초대 토큰은 필수입니다."));
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

    @Test
    @DisplayName("스터디 멤버가 멤버 목록을 조회하면 200과 멤버 정보를 반환한다")
    void getStudyMembersTest() {
        User leaderUser = userRepository.saveAndFlush(User.create("리더", "leader-profile-image-url"));
        User memberUser = userRepository.saveAndFlush(User.create("멤버", null));
        Study study = studyRepository.saveAndFlush(Study.create("자바 스터디", "설명"));
        StudyMember leader = studyMemberRepository.saveAndFlush(
                StudyMember.create(study, leaderUser, "리더", "leader-profile-image-url", StudyMemberRole.LEADER)
        );
        StudyMember member = studyMemberRepository.saveAndFlush(
                StudyMember.create(study, memberUser, "스터디 내 이름", null, StudyMemberRole.MEMBER)
        );

        testAuthRequest.givenAuthenticatedUser(memberUser.getId())
                .port(port)
                .when()
                .get("/studies/{studyId}/members", study.getId())
                .then()
                .statusCode(200)
                .body("members", hasSize(2))
                .body("members[0].id", equalTo(leader.getId().intValue()))
                .body("members[0].name", equalTo("리더"))
                .body("members[0].profileImage", equalTo("leader-profile-image-url"))
                .body("members[0].role", equalTo("LEADER"))
                .body("members[1].id", equalTo(member.getId().intValue()))
                .body("members[1].name", equalTo("스터디 내 이름"))
                .body("members[1].profileImage", nullValue())
                .body("members[1].role", equalTo("MEMBER"));
    }

    @Test
    @DisplayName("스터디 리더도 멤버 목록을 조회할 수 있다")
    void getStudyMembersByLeaderTest() {
        User leaderUser = userRepository.saveAndFlush(User.create("리더", "leader-profile-image-url"));
        Study study = studyRepository.saveAndFlush(Study.create("자바 스터디", "설명"));
        studyMemberRepository.saveAndFlush(
                StudyMember.create(
                        study,
                        leaderUser,
                        leaderUser.getName(),
                        leaderUser.getProfileImageUrl(),
                        StudyMemberRole.LEADER
                )
        );

        testAuthRequest.givenAuthenticatedUser(leaderUser.getId())
                .port(port)
                .when()
                .get("/studies/{studyId}/members", study.getId())
                .then()
                .statusCode(200);
    }

    @Test
    @DisplayName("스터디 멤버가 아니면 멤버 목록 조회 시 403을 반환한다")
    void getStudyMembersByNonMemberTest() {
        User user = userRepository.saveAndFlush(User.create("비멤버", "profile-image-url"));
        Study study = studyRepository.saveAndFlush(Study.create("자바 스터디", "설명"));

        testAuthRequest.givenAuthenticatedUser(user.getId())
                .port(port)
                .when()
                .get("/studies/{studyId}/members", study.getId())
                .then()
                .statusCode(403)
                .body("code", equalTo("STUDY_ACCESS_DENIED"));
    }

    @Test
    @DisplayName("존재하지 않는 스터디의 멤버 목록을 조회하면 404를 반환한다")
    void getStudyMembersWithMissingStudyTest() {
        User user = userRepository.saveAndFlush(User.create("사용자", "profile-image-url"));

        testAuthRequest.givenAuthenticatedUser(user.getId())
                .port(port)
                .when()
                .get("/studies/{studyId}/members", Long.MAX_VALUE)
                .then()
                .statusCode(404)
                .body("code", equalTo("STUDY_NOT_FOUND"));
    }

    @Test
    @DisplayName("스터디 ID가 양수가 아니면 멤버 목록 조회 시 400을 반환한다")
    void getStudyMembersWithInvalidStudyIdTest() {
        User user = userRepository.saveAndFlush(User.create("사용자", "profile-image-url"));

        testAuthRequest.givenAuthenticatedUser(user.getId())
                .port(port)
                .when()
                .get("/studies/{studyId}/members", 0)
                .then()
                .statusCode(400)
                .body("code", equalTo("INVALID_REQUEST_PARAMETER"));
    }

    @Test
    @DisplayName("인증 없이 멤버 목록을 조회하면 401을 반환한다")
    void getStudyMembersWithoutAuthenticationTest() {
        given()
                .basePath(API_PREFIX)
                .port(port)
                .when()
                .get("/studies/{studyId}/members", 1L)
                .then()
                .statusCode(401)
                .body("code", equalTo("AUTHENTICATION_REQUIRED"));
    }

    @Test
    @DisplayName("스터디 리더가 일반 멤버를 방출하면 204를 반환한다")
    void expelStudyMemberTest() {
        Study study = studyRepository.saveAndFlush(Study.create("자바 스터디", "설명"));
        StudyMember leader = createMember(study, "리더", StudyMemberRole.LEADER);
        StudyMember target = createMember(study, "방출 대상", StudyMemberRole.MEMBER);
        Long targetId = target.getId();
        Long targetUserId = target.getUser().getId();

        testAuthRequest.givenAuthenticatedUser(leader.getUser().getId())
                .port(port)
                .when()
                .delete("/studies/{studyId}/members/{memberId}", study.getId(), targetId)
                .then()
                .statusCode(204);

        assertThat(studyMemberRepository.findById(targetId)).isEmpty();
        testAuthRequest.givenAuthenticatedUser(targetUserId)
                .port(port)
                .when()
                .get("/studies/{studyId}/members", study.getId())
                .then()
                .statusCode(403)
                .body("code", equalTo("STUDY_ACCESS_DENIED"));
    }

    @Test
    @DisplayName("스터디 멤버가 아닌 사용자가 방출을 요청하면 403을 반환한다")
    void expelStudyMemberByNonMemberTest() {
        User requester = userRepository.saveAndFlush(User.create("비멤버", null));
        Study study = studyRepository.saveAndFlush(Study.create("자바 스터디", "설명"));
        createMember(study, "리더", StudyMemberRole.LEADER);
        StudyMember target = createMember(study, "방출 대상", StudyMemberRole.MEMBER);

        testAuthRequest.givenAuthenticatedUser(requester.getId())
                .port(port)
                .when()
                .delete("/studies/{studyId}/members/{memberId}", study.getId(), target.getId())
                .then()
                .statusCode(403)
                .body("code", equalTo("STUDY_ACCESS_DENIED"));
    }

    @Test
    @DisplayName("스터디 리더가 아닌 멤버가 방출을 요청하면 403을 반환한다")
    void expelStudyMemberByNonLeaderTest() {
        Study study = studyRepository.saveAndFlush(Study.create("자바 스터디", "설명"));
        createMember(study, "리더", StudyMemberRole.LEADER);
        StudyMember requester = createMember(study, "일반 멤버", StudyMemberRole.MEMBER);
        StudyMember target = createMember(study, "방출 대상", StudyMemberRole.MEMBER);

        testAuthRequest.givenAuthenticatedUser(requester.getUser().getId())
                .port(port)
                .when()
                .delete("/studies/{studyId}/members/{memberId}", study.getId(), target.getId())
                .then()
                .statusCode(403)
                .body("code", equalTo("NOT_STUDY_LEADER"));
    }

    @Test
    @DisplayName("다른 스터디의 멤버를 방출하려 하면 404를 반환한다")
    void expelMemberFromOtherStudyTest() {
        Study study = studyRepository.saveAndFlush(Study.create("자바 스터디", "설명"));
        Study otherStudy = studyRepository.saveAndFlush(Study.create("다른 스터디", "설명"));
        StudyMember leader = createMember(study, "리더", StudyMemberRole.LEADER);
        StudyMember otherStudyMember = createMember(otherStudy, "다른 스터디 멤버", StudyMemberRole.MEMBER);

        testAuthRequest.givenAuthenticatedUser(leader.getUser().getId())
                .port(port)
                .when()
                .delete("/studies/{studyId}/members/{memberId}", study.getId(), otherStudyMember.getId())
                .then()
                .statusCode(404)
                .body("code", equalTo("STUDY_MEMBER_NOT_FOUND"));
    }

    @Test
    @DisplayName("존재하지 않는 멤버를 방출하려 하면 404를 반환한다")
    void expelMissingStudyMemberTest() {
        Study study = studyRepository.saveAndFlush(Study.create("자바 스터디", "설명"));
        StudyMember leader = createMember(study, "리더", StudyMemberRole.LEADER);

        testAuthRequest.givenAuthenticatedUser(leader.getUser().getId())
                .port(port)
                .when()
                .delete("/studies/{studyId}/members/{memberId}", study.getId(), Long.MAX_VALUE)
                .then()
                .statusCode(404)
                .body("code", equalTo("STUDY_MEMBER_NOT_FOUND"));
    }

    @Test
    @DisplayName("스터디 리더를 방출하려 하면 403을 반환한다")
    void expelStudyLeaderTest() {
        Study study = studyRepository.saveAndFlush(Study.create("자바 스터디", "설명"));
        StudyMember leader = createMember(study, "리더", StudyMemberRole.LEADER);

        testAuthRequest.givenAuthenticatedUser(leader.getUser().getId())
                .port(port)
                .when()
                .delete("/studies/{studyId}/members/{memberId}", study.getId(), leader.getId())
                .then()
                .statusCode(403)
                .body("code", equalTo("STUDY_LEADER_CANNOT_BE_REMOVED"));
    }

    @Test
    @DisplayName("멤버 ID가 양수가 아니면 방출 요청 시 400을 반환한다")
    void expelStudyMemberWithInvalidMemberIdTest() {
        User user = userRepository.saveAndFlush(User.create("사용자", null));

        testAuthRequest.givenAuthenticatedUser(user.getId())
                .port(port)
                .when()
                .delete("/studies/{studyId}/members/{memberId}", 1L, 0)
                .then()
                .statusCode(400)
                .body("code", equalTo("INVALID_REQUEST_PARAMETER"));
    }

    @Test
    @DisplayName("인증 없이 멤버 방출을 요청하면 401을 반환한다")
    void expelStudyMemberWithoutAuthenticationTest() {
        given()
                .basePath(API_PREFIX)
                .port(port)
                .when()
                .delete("/studies/{studyId}/members/{memberId}", 1L, 1L)
                .then()
                .statusCode(401)
                .body("code", equalTo("AUTHENTICATION_REQUIRED"));
    }

    private StudyMember createMember(Study study, String name, StudyMemberRole role) {
        User user = userRepository.saveAndFlush(User.create(name, null));
        return studyMemberRepository.saveAndFlush(StudyMember.create(study, user, name, null, role));
    }
}
