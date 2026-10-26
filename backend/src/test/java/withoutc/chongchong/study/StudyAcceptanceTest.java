package withoutc.chongchong.study;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static withoutc.chongchong.global.config.ApiPathConfig.API_PREFIX;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import withoutc.chongchong.assignment.entity.SubmissionVisibility;
import withoutc.chongchong.assignment.entity.Assignment;
import withoutc.chongchong.assignment.entity.SubmissionTarget;
import withoutc.chongchong.assignment.repository.AssignmentRepository;
import withoutc.chongchong.assignment.repository.AssignmentSubmissionRepository;
import withoutc.chongchong.auth.support.TestAuthRequest;
import withoutc.chongchong.file.entity.FileMetadata;
import withoutc.chongchong.file.repository.FileMetaDataRepository;
import withoutc.chongchong.notice.entity.Notice;
import withoutc.chongchong.notice.repository.NoticeRecipientRepository;
import withoutc.chongchong.notice.repository.NoticeRepository;
import withoutc.chongchong.notification.entity.Notification;
import withoutc.chongchong.notification.entity.NotificationType;
import withoutc.chongchong.notification.entity.ResourceType;
import withoutc.chongchong.notification.repository.NotificationRepository;
import withoutc.chongchong.study.controller.dto.StudyJoinRequest;
import withoutc.chongchong.study.entity.Study;
import withoutc.chongchong.study.entity.StudyMember;
import withoutc.chongchong.study.entity.StudyMemberRole;
import withoutc.chongchong.study.repository.StudyMemberRepository;
import withoutc.chongchong.study.repository.StudyRepository;
import withoutc.chongchong.study.token.StudyInviteTokenProvider;
import withoutc.chongchong.support.TestDatabaseCleaner;
import withoutc.chongchong.support.TestS3PresignerConfiguration;
import withoutc.chongchong.user.entity.User;
import withoutc.chongchong.user.repository.UserRepository;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestS3PresignerConfiguration.class)
class StudyAcceptanceTest {

    private static final LocalDateTime NOTICE_NOW = LocalDateTime.of(2026, 8, 19, 0, 0);
    private static final LocalDateTime ASSIGNMENT_NOW = LocalDateTime.of(2026, 8, 19, 9, 0);

    @Autowired
    private StudyRepository studyRepository;

    @Autowired
    private StudyInviteTokenProvider studyInviteTokenProvider;

    @Autowired
    private Clock clock;

    @Value("${jwt.study-invite-secret}")
    private String studyInviteSecret;

    @Autowired
    private FileMetaDataRepository fileRepository;

    @Autowired
    private StudyMemberRepository studyMemberRepository;

    @Autowired
    private UserRepository userRepository;

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
    private TestDatabaseCleaner databaseCleaner;

    @Autowired
    private TestAuthRequest testAuthRequest;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @LocalServerPort
    private int port;

    @BeforeEach
    void configureCascadeConstraints() {
        configureCascadeIfColumnExists("study_members", "study_id", "studies", "fkb8cp6e23p040p7ml6sswen5cs");
        configureCascadeIfColumnExists("study_members", "user_id", "users", "fkdt5hp8mbe53a5sdcecsf7wpyg");
        configureCascadeIfColumnExists("assignments", "study_id", "studies", "fkqe029wvx6pjp0q8tlypilk0c9");
        configureCascadeIfColumnExists(
                "assignment_reminders", "assignment_id", "assignments", "fk23if7m1gm235fcgs3hleq5do2");
        configureCascadeIfColumnExists(
                "assignment_submissions", "assignment_id", "assignments", "fkm7i7ubgh7y2n6mvg8muw62oax");
        configureCascadeIfColumnExists(
                "assignment_submissions", "member_id", "study_members", "fknsfnkdmpdvd605vnpkpt1g0md");
        configureCascadeIfColumnExists("notices", "study_id", "studies", "fk403omqxfm0hkwwx6trtd12u76");
        configureCascadeIfColumnExists(
                "notice_recipients", "notice_id", "notices", "fky3a9r7igh6bsqigv2lkgmu6o");
        configureCascadeIfColumnExists(
                "notice_recipients", "member_id", "study_members", "fkpm6u1t0n3px52tld2apx6oess");
        configureCascadeIfColumnExists("notice_reminders", "notice_id", "notices", "fko0h19ha7jyrdtym2iy97ip03n");
        configureCascadeIfColumnExists(
                "notifications", "recipient_id", "users", "fk60prjsdd6ahrlv3ayvjjqdlqi");
    }

    @AfterEach
    void cleanDatabase() {
        databaseCleaner.clean();
    }

    @Test
    @DisplayName("스터디 생성 요청을 보내면 201과 studyId를 반환하고 생성자를 리더로 등록한다")
    void createStudyTest() {
        User user = userRepository.saveAndFlush(User.create("테스트 사용자", "profile-image-url"));

        testAuthRequest.givenAuthenticatedUser(user.getId())
                .port(port)
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "name": "자바 스터디",
                          "description": "매주 월요일에 진행한다.",
                          "isPublic": true
                        }
                        """)
                .when()
                .post("/studies")
                .then()
                .statusCode(201)
                .body("studyId", notNullValue());

        assertThat(studyRepository.findAll())
                .extracting(Study::getName, Study::getDescription, Study::isPublic)
                .contains(tuple("자바 스터디", "매주 월요일에 진행한다.", true));

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
    @DisplayName("스터디 설명이 null인 생성 요청을 보내면 설명을 null로 저장한다")
    void createStudyWithNullDescriptionTest() {
        User user = userRepository.saveAndFlush(User.create("테스트 사용자", "profile-image-url"));

        testAuthRequest.givenAuthenticatedUser(user.getId())
                .port(port)
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "name": "자바 스터디",
                          "description": null,
                          "isPublic": false
                        }
                        """)
                .when()
                .post("/studies")
                .then()
                .statusCode(201)
                .body("studyId", notNullValue());

        assertThat(studyRepository.findAll())
                .singleElement()
                .satisfies(study -> {
                    assertThat(study.getDescription()).isNull();
                    assertThat(study.isPublic()).isFalse();
                });
    }

    @Test
    @DisplayName("스터디 생성 요청의 공개 여부가 없거나 null이면 검증 오류를 반환한다")
    void createStudyWithoutVisibilityTest() {
        User user = userRepository.saveAndFlush(User.create("테스트 사용자", "profile-image-url"));

        for (String requestBody : List.of(
                """
                        { "name": "자바 스터디" }
                        """,
                """
                        { "name": "자바 스터디", "isPublic": null }
                        """)) {
            testAuthRequest.givenAuthenticatedUser(user.getId())
                    .port(port)
                    .contentType(ContentType.JSON)
                    .body(requestBody)
                    .when()
                    .post("/studies")
                    .then()
                    .statusCode(400)
                    .body("code", equalTo("INVALID_INPUT_VALUE"))
                    .body("errors.field", containsInAnyOrder("isPublic"))
                    .body("errors.reason", containsInAnyOrder("스터디 공개 여부는 필수입니다."));
        }

        assertThat(studyRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("스터디 생성 요청의 검증에 실패하면 한글 검증 사유를 반환한다")
    void createStudyWithInvalidRequestTest() {
        User user = userRepository.saveAndFlush(User.create("테스트 사용자", "profile-image-url"));

        testAuthRequest.givenAuthenticatedUser(user.getId())
                .port(port)
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "name": "",
                          "description": "1234567890123456789012345678901",
                          "isPublic": false
                        }
                        """)
                .when()
                .post("/studies")
                .then()
                .statusCode(400)
                .body("code", equalTo("INVALID_INPUT_VALUE"))
                .body("errors.field", containsInAnyOrder("name", "description"))
                .body("errors.reason", containsInAnyOrder(
                        "스터디 이름은 필수입니다.",
                        "스터디 설명은 30자 이내여야 합니다."
                ));
    }

    @Test
    @DisplayName("스터디 리더가 수정 요청을 보내면 스터디 이름과 설명을 변경하고 204를 반환한다")
    void updateStudyTest() {
        User leader = userRepository.saveAndFlush(User.create("리더", "leader-profile-image-url"));
        Study study = studyRepository.saveAndFlush(Study.create("기존 스터디", "기존 설명", false));
        studyMemberRepository.saveAndFlush(
                StudyMember.create(study, leader, leader.getName(), leader.getProfileImageUrl(),
                        StudyMemberRole.LEADER)
        );

        testAuthRequest.givenAuthenticatedUser(leader.getId())
                .port(port)
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "name": "수정 스터디",
                          "description": "수정 설명",
                          "isPublic": true
                        }
                        """)
                .when()
                .patch("/studies/{studyId}", study.getId())
                .then()
                .statusCode(204);

        assertThat(studyRepository.findById(study.getId()))
                .get()
                .satisfies(updatedStudy -> {
                    assertThat(updatedStudy.getName()).isEqualTo("수정 스터디");
                    assertThat(updatedStudy.getDescription()).isEqualTo("수정 설명");
                    assertThat(updatedStudy.isPublic()).isTrue();
                });
    }

    @Test
    @DisplayName("스터디 리더가 공개 여부만 false로 수정하면 이름과 설명은 유지한다")
    void updateStudyVisibilityOnlyTest() {
        User leader = userRepository.saveAndFlush(User.create("리더", "leader-profile-image-url"));
        Study study = studyRepository.saveAndFlush(Study.create("기존 스터디", "기존 설명", true));
        studyMemberRepository.saveAndFlush(
                StudyMember.create(study, leader, leader.getName(), leader.getProfileImageUrl(),
                        StudyMemberRole.LEADER)
        );

        testAuthRequest.givenAuthenticatedUser(leader.getId())
                .port(port)
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "isPublic": false
                        }
                        """)
                .when()
                .patch("/studies/{studyId}", study.getId())
                .then()
                .statusCode(204);

        assertThat(studyRepository.findById(study.getId()))
                .get()
                .satisfies(updatedStudy -> {
                    assertThat(updatedStudy.getName()).isEqualTo("기존 스터디");
                    assertThat(updatedStudy.getDescription()).isEqualTo("기존 설명");
                    assertThat(updatedStudy.isPublic()).isFalse();
                });
    }

    @Test
    @DisplayName("스터디 수정 요청에서 설명이 null이면 기존 설명을 유지한다")
    void updateStudyWithNullDescriptionTest() {
        User leader = userRepository.saveAndFlush(User.create("리더", "leader-profile-image-url"));
        Study study = studyRepository.saveAndFlush(Study.create("기존 스터디", "기존 설명", true));
        studyMemberRepository.saveAndFlush(
                StudyMember.create(study, leader, leader.getName(), leader.getProfileImageUrl(),
                        StudyMemberRole.LEADER)
        );

        testAuthRequest.givenAuthenticatedUser(leader.getId())
                .port(port)
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "name": "수정 스터디",
                          "description": null,
                          "isPublic": null
                        }
                        """)
                .when()
                .patch("/studies/{studyId}", study.getId())
                .then()
                .statusCode(204);

        assertThat(studyRepository.findById(study.getId()))
                .get()
                .satisfies(updatedStudy -> {
                    assertThat(updatedStudy.getName()).isEqualTo("수정 스터디");
                    assertThat(updatedStudy.getDescription()).isEqualTo("기존 설명");
                    assertThat(updatedStudy.isPublic()).isTrue();
                });
    }

    @Test
    @DisplayName("스터디 멤버가 아니면 스터디를 수정할 수 없다")
    void updateStudyForNonMemberTest() {
        User leader = userRepository.saveAndFlush(User.create("리더", "leader-profile-image-url"));
        User user = userRepository.saveAndFlush(User.create("사용자", "profile-image-url"));
        Study study = studyRepository.saveAndFlush(Study.create("기존 스터디", "기존 설명", false));
        studyMemberRepository.saveAndFlush(
                StudyMember.create(study, leader, leader.getName(), leader.getProfileImageUrl(),
                        StudyMemberRole.LEADER)
        );

        testAuthRequest.givenAuthenticatedUser(user.getId())
                .port(port)
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "name": "권한 없는 수정"
                        }
                        """)
                .when()
                .patch("/studies/{studyId}", study.getId())
                .then()
                .statusCode(403)
                .body("code", equalTo("STUDY_ACCESS_DENIED"));

        assertThat(studyRepository.findById(study.getId()))
                .get()
                .satisfies(unchangedStudy -> {
                    assertThat(unchangedStudy.getName()).isEqualTo("기존 스터디");
                    assertThat(unchangedStudy.getDescription()).isEqualTo("기존 설명");
                });
    }

    @Test
    @DisplayName("스터디 리더가 아니면 스터디를 수정할 수 없다")
    void updateStudyForNonLeaderTest() {
        User leader = userRepository.saveAndFlush(User.create("리더", "leader-profile-image-url"));
        User member = userRepository.saveAndFlush(User.create("멤버", "member-profile-image-url"));
        Study study = studyRepository.saveAndFlush(Study.create("기존 스터디", "기존 설명", false));
        studyMemberRepository.saveAndFlush(
                StudyMember.create(study, leader, leader.getName(), leader.getProfileImageUrl(),
                        StudyMemberRole.LEADER)
        );
        studyMemberRepository.saveAndFlush(
                StudyMember.create(study, member, member.getName(), member.getProfileImageUrl(),
                        StudyMemberRole.MEMBER)
        );

        testAuthRequest.givenAuthenticatedUser(member.getId())
                .port(port)
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "name": "권한 없는 수정"
                        }
                        """)
                .when()
                .patch("/studies/{studyId}", study.getId())
                .then()
                .statusCode(403)
                .body("code", equalTo("NOT_STUDY_LEADER"));

        assertThat(studyRepository.findById(study.getId()))
                .get()
                .satisfies(unchangedStudy -> {
                    assertThat(unchangedStudy.getName()).isEqualTo("기존 스터디");
                    assertThat(unchangedStudy.getDescription()).isEqualTo("기존 설명");
                });
    }

    @Test
    @DisplayName("존재하지 않는 스터디는 수정할 수 없다")
    void updateStudyForMissingStudyTest() {
        User user = userRepository.saveAndFlush(User.create("사용자", "profile-image-url"));

        testAuthRequest.givenAuthenticatedUser(user.getId())
                .port(port)
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "name": "수정 스터디"
                        }
                        """)
                .when()
                .patch("/studies/{studyId}", 999L)
                .then()
                .statusCode(404)
                .body("code", equalTo("STUDY_NOT_FOUND"));
    }

    @Test
    @DisplayName("스터디 수정 요청의 이름과 설명이 최대 길이를 초과하면 검증 오류를 반환한다")
    void updateStudyWithInvalidRequestTest() {
        User leader = userRepository.saveAndFlush(User.create("리더", "leader-profile-image-url"));
        Study study = studyRepository.saveAndFlush(Study.create("기존 스터디", "기존 설명", false));
        studyMemberRepository.saveAndFlush(
                StudyMember.create(study, leader, leader.getName(), leader.getProfileImageUrl(),
                        StudyMemberRole.LEADER)
        );

        testAuthRequest.givenAuthenticatedUser(leader.getId())
                .port(port)
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "name": "1234567890123456",
                          "description": "1234567890123456789012345678901"
                        }
                        """)
                .when()
                .patch("/studies/{studyId}", study.getId())
                .then()
                .statusCode(400)
                .body("code", equalTo("INVALID_REQUEST_PARAMETER"))
                .body("errors.reason", containsInAnyOrder(
                        "스터디 이름은 15자 이내여야 합니다.",
                        "스터디 설명은 30자 이내여야 합니다."
                ));

        assertThat(studyRepository.findById(study.getId()))
                .get()
                .satisfies(unchangedStudy -> {
                    assertThat(unchangedStudy.getName()).isEqualTo("기존 스터디");
                    assertThat(unchangedStudy.getDescription()).isEqualTo("기존 설명");
                });
    }

    @Test
    @DisplayName("내 스터디 목록 조회 요청을 보내면 가입 순서와 집계 정보를 반환한다")
    void getMyStudiesTest() {
        User user = userRepository.saveAndFlush(User.create("테스트 사용자", "profile-image-url"));
        User otherUser = userRepository.saveAndFlush(User.create("다른 사용자", "other-profile-image-url"));
        Study olderStudy = studyRepository.saveAndFlush(Study.create("이전 스터디", "이전 스터디 설명", false));
        FileMetadata image = FileMetadata.create(user, "study.png", "study-profiles/study.png", "image/png", 1024);
        image.completeUpload();
        fileRepository.saveAndFlush(image);
        Study newerStudy = Study.create("최근 스터디", "최근 스터디 설명", false);
        newerStudy.changeImage(image);
        newerStudy = studyRepository.saveAndFlush(newerStudy);

        StudyMember olderMember = studyMemberRepository.saveAndFlush(
                StudyMember.create(olderStudy, user, user.getName(), user.getProfileImageUrl(), StudyMemberRole.MEMBER)
        );
        studyMemberRepository.saveAndFlush(
                StudyMember.create(
                        olderStudy,
                        otherUser,
                        otherUser.getName(),
                        otherUser.getProfileImageUrl(),
                        StudyMemberRole.MEMBER
                )
        );
        StudyMember newerMember = studyMemberRepository.saveAndFlush(
                StudyMember.create(newerStudy, user, user.getName(), user.getProfileImageUrl(), StudyMemberRole.LEADER)
        );
        studyMemberRepository.saveAndFlush(
                StudyMember.create(
                        newerStudy,
                        otherUser,
                        otherUser.getName(),
                        otherUser.getProfileImageUrl(),
                        StudyMemberRole.MEMBER
                )
        );
        setCreatedAt(olderMember.getId(), LocalDateTime.of(2026, 1, 1, 0, 0));
        setCreatedAt(newerMember.getId(), LocalDateTime.of(2026, 1, 2, 0, 0));

        testAuthRequest.givenAuthenticatedUser(user.getId())
                .port(port)
                .when()
                .get("/studies/me")
                .then()
                .statusCode(200)
                .body("studyCount", equalTo(2))
                .body("studies.size()", equalTo(2))
                .body("studies[0].id", equalTo(newerStudy.getId().intValue()))
                .body("studies[0].role", equalTo("LEADER"))
                .body("studies[0].memberCount", equalTo(2))
                .body("studies[0].imageUrl", containsString("study-profiles/study.png"))
                .body("studies[1].id", equalTo(olderStudy.getId().intValue()))
                .body("studies[1].role", equalTo("MEMBER"))
                .body("studies[1].memberCount", equalTo(2))
                .body("studies[1].imageUrl", nullValue());
    }

    @Test
    @DisplayName("공개 스터디 목록을 최신순으로 조회하고 응답 커서로 다음 페이지를 조회한다")
    void getPublicStudiesWithCursorTest() {
        User leader = userRepository.saveAndFlush(User.create("리더", null));
        User viewer = userRepository.saveAndFlush(User.create("조회 사용자", null));
        Study sameTimeLowerId = studyRepository.saveAndFlush(Study.create("같은 시각 작은 ID", null, true));
        Study privateNewer = studyRepository.saveAndFlush(Study.create("최신 비공개 스터디", "비공개 설명", false));

        FileMetadata image = FileMetadata.create(leader, "cover.png", "study-images/cover.png", "image/png", 1024);
        image.completeUpload();
        fileRepository.saveAndFlush(image);
        Study sameTimeHigherId = Study.create("같은 시각 큰 ID", "공개 스터디 설명", true);
        sameTimeHigherId.changeImage(image);
        sameTimeHigherId = studyRepository.saveAndFlush(sameTimeHigherId);
        Study olderHigherId = studyRepository.saveAndFlush(Study.create("이전 시각 큰 ID", "이전 스터디 설명", true));
        Study privateOlder = studyRepository.saveAndFlush(Study.create("이전 비공개 스터디", null, false));
        for (Study study : List.of(sameTimeLowerId, sameTimeHigherId, olderHigherId)) {
            studyMemberRepository.saveAndFlush(
                    StudyMember.create(study, leader, leader.getName(), null, StudyMemberRole.LEADER));
        }

        LocalDateTime createdAt = LocalDateTime.of(2026, 10, 26, 12, 0);
        setStudyCreatedAt(sameTimeLowerId.getId(), createdAt);
        setStudyCreatedAt(sameTimeHigherId.getId(), createdAt);
        setStudyCreatedAt(privateNewer.getId(), createdAt.plusHours(1));
        setStudyCreatedAt(olderHigherId.getId(), createdAt.minusHours(1));
        setStudyCreatedAt(privateOlder.getId(), createdAt.minusHours(2));

        Long nextCursor = testAuthRequest.givenAuthenticatedUser(viewer.getId())
                .port(port)
                .queryParam("size", 2)
                .when()
                .get("/studies/public")
                .then()
                .statusCode(200)
                .body("studies.studyId",
                        contains(sameTimeHigherId.getId().intValue(), sameTimeLowerId.getId().intValue()))
                .body("studies[0].studyName", equalTo("같은 시각 큰 ID"))
                .body("studies[0].description", equalTo("공개 스터디 설명"))
                .body("studies[0].memberCount", equalTo(1))
                .body("studies[0].imageUrl", containsString("study-images/cover.png"))
                .body("studies[0].imageUrl", containsString("X-Amz-Signature="))
                .body("studies[1].description", nullValue())
                .body("studies[1].imageUrl", nullValue())
                .body("hasNext", equalTo(true))
                .body("nextCursor", equalTo(sameTimeLowerId.getId().intValue()))
                .extract().jsonPath().getLong("nextCursor");

        testAuthRequest.givenAuthenticatedUser(viewer.getId())
                .port(port)
                .queryParam("cursor", nextCursor)
                .queryParam("size", 2)
                .when()
                .get("/studies/public")
                .then()
                .statusCode(200)
                .body("studies.studyId", contains(olderHigherId.getId().intValue()))
                .body("hasNext", equalTo(false))
                .body("nextCursor", nullValue());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @DisplayName("스터디원이 아닌 인증 사용자가 공개 스터디의 기본 정보를 조회한다")
    void getStudyJoinInfoForPublicStudyTest(boolean withImage) {
        User leader = userRepository.saveAndFlush(User.create("리더", null));
        User viewer = userRepository.saveAndFlush(User.create("조회 사용자", null));
        Study study = Study.create("자바 스터디", null, true);
        if (withImage) {
            FileMetadata image = FileMetadata.create(leader, "cover.png", "study-images/cover.png", "image/png", 1024);
            image.completeUpload();
            fileRepository.saveAndFlush(image);
            study.changeImage(image);
        }
        study = studyRepository.saveAndFlush(study);
        studyMemberRepository.saveAndFlush(
                StudyMember.create(study, leader, leader.getName(), null, StudyMemberRole.LEADER));

        Response response = testAuthRequest.givenAuthenticatedUser(viewer.getId())
                .port(port)
                .queryParam("studyId", study.getId())
                .when()
                .get("/studies/join-info");

        response.then()
                .statusCode(200)
                .body("studyId", equalTo(study.getId().intValue()))
                .body("studyName", equalTo("자바 스터디"))
                .body("description", nullValue())
                .body("memberCount", equalTo(1));
        if (withImage) {
            response.then()
                    .body("imageUrl", containsString("study-images/cover.png"))
                    .body("imageUrl", containsString("X-Amz-Signature="));
        } else {
            response.then().body("imageUrl", nullValue());
        }
        assertThat(studyMemberRepository.findByStudyIdAndUserId(study.getId(), viewer.getId())).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @DisplayName("정원이 찬 스터디는 목록에서 제외하고 참여 전 조회는 허용하며 실제 가입은 거절한다")
    void hideFullStudyButAllowJoinInfoTest(boolean isPublic) {
        User viewer = userRepository.saveAndFlush(User.create("조회 사용자", null));
        Study study = studyRepository.saveAndFlush(Study.create("정원이 찬 스터디", "스터디 설명", isPublic));
        addStudyMembers(study, 50);
        String token = studyInviteTokenProvider.generate(study.getId());

        testAuthRequest.givenAuthenticatedUser(viewer.getId())
                .port(port)
                .when()
                .get("/studies/public")
                .then()
                .statusCode(200)
                .body("studies.size()", equalTo(0))
                .body("hasNext", equalTo(false))
                .body("nextCursor", nullValue());

        testAuthRequest.givenAuthenticatedUser(viewer.getId())
                .port(port)
                .queryParam("studyId", study.getId())
                .queryParam("token", token)
                .when()
                .get("/studies/join-info")
                .then()
                .statusCode(200)
                .body("studyId", equalTo(study.getId().intValue()))
                .body("studyName", equalTo("정원이 찬 스터디"))
                .body("memberCount", equalTo(50));

        testAuthRequest.givenAuthenticatedUser(viewer.getId())
                .port(port)
                .contentType(ContentType.JSON)
                .body(new StudyJoinRequest(null, token))
                .when()
                .post("/studies/join")
                .then()
                .statusCode(409)
                .body("code", equalTo("STUDY_MEMBER_LIMIT_EXCEEDED"))
                .body("message", equalTo("스터디 정원이 가득 찼습니다."));

        assertThat(studyMemberRepository.countByStudyId(study.getId())).isEqualTo(50);
        assertThat(studyMemberRepository.findByStudyIdAndUserId(study.getId(), viewer.getId())).isEmpty();
    }

    @Test
    @DisplayName("비공개 스터디는 가입 여부와 관계없이 초대 토큰 없이 참여 전 조회를 할 수 없다")
    void getStudyJoinInfoForPrivateStudyWithoutTokenTest() {
        User leader = userRepository.saveAndFlush(User.create("리더", null));
        User viewer = userRepository.saveAndFlush(User.create("조회 사용자", null));
        Study study = studyRepository.saveAndFlush(Study.create("비공개 스터디", "비공개 설명", false));
        studyMemberRepository.saveAndFlush(
                StudyMember.create(study, leader, leader.getName(), null, StudyMemberRole.LEADER));

        for (User user : List.of(leader, viewer)) {
            testAuthRequest.givenAuthenticatedUser(user.getId())
                    .port(port)
                    .queryParam("studyId", study.getId())
                    .when()
                    .get("/studies/join-info")
                    .then()
                    .statusCode(403)
                    .body("code", equalTo("STUDY_NOT_PUBLIC"))
                    .body("message", equalTo("공개 스터디가 아닙니다."));
        }
    }

    @Test
    @DisplayName("존재하지 않는 스터디의 참여 전 정보 조회는 404를 반환한다")
    void getStudyJoinInfoForMissingStudyTest() {
        User user = userRepository.saveAndFlush(User.create("조회 사용자", null));

        testAuthRequest.givenAuthenticatedUser(user.getId())
                .port(port)
                .queryParam("studyId", Long.MAX_VALUE)
                .when()
                .get("/studies/join-info")
                .then()
                .statusCode(404)
                .body("code", equalTo("STUDY_NOT_FOUND"));
    }

    @Test
    @DisplayName("인증 없이 스터디 참여 전 정보를 조회하면 401을 반환한다")
    void getStudyJoinInfoWithoutAuthenticationTest() {
        given()
                .basePath(API_PREFIX)
                .port(port)
                .when()
                .get("/studies/join-info")
                .then()
                .statusCode(401)
                .body("code", equalTo("AUTHENTICATION_REQUIRED"));
    }

    @Test
    @DisplayName("양수가 아닌 스터디 ID로 참여 전 정보를 조회하면 400을 반환한다")
    void getStudyJoinInfoForInvalidStudyIdTest() {
        User user = userRepository.saveAndFlush(User.create("조회 사용자", null));

        testAuthRequest.givenAuthenticatedUser(user.getId())
                .port(port)
                .queryParam("studyId", 0L)
                .when()
                .get("/studies/join-info")
                .then()
                .statusCode(400)
                .body("code", equalTo("INVALID_REQUEST_PARAMETER"));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @DisplayName("초대 링크의 토큰만으로 공개·비공개 스터디의 참여 전 정보를 조회한다")
    void getStudyJoinInfoUsingOnlyTokenTest(boolean isPublic) {
        User leader = userRepository.saveAndFlush(User.create("리더", null));
        User viewer = userRepository.saveAndFlush(User.create("조회 사용자", null));
        FileMetadata image = FileMetadata.create(leader, "cover.png", "study-images/invited.png", "image/png", 1024);
        image.completeUpload();
        fileRepository.saveAndFlush(image);
        Study study = Study.create("초대 스터디", "초대 설명", isPublic);
        study.changeImage(image);
        study = studyRepository.saveAndFlush(study);
        studyMemberRepository.saveAndFlush(
                StudyMember.create(study, leader, leader.getName(), null, StudyMemberRole.LEADER));

        testAuthRequest.givenAuthenticatedUser(viewer.getId())
                .port(port)
                .queryParam("token", studyInviteTokenProvider.generate(study.getId()))
                .when()
                .get("/studies/join-info")
                .then()
                .statusCode(200)
                .body("studyId", equalTo(study.getId().intValue()))
                .body("studyName", equalTo("초대 스터디"))
                .body("description", equalTo("초대 설명"))
                .body("memberCount", equalTo(1))
                .body("imageUrl", containsString("study-images/invited.png"))
                .body("imageUrl", containsString("X-Amz-Signature="));

        assertThat(studyMemberRepository.findByStudyIdAndUserId(study.getId(), viewer.getId())).isEmpty();
    }

    @Test
    @DisplayName("다른 스터디의 유효한 초대 토큰으로 참여 전 정보를 조회할 수 없다")
    void getStudyJoinInfoWithMismatchedTokenTest() {
        User viewer = userRepository.saveAndFlush(User.create("조회 사용자", null));
        Study target = studyRepository.saveAndFlush(Study.create("조회 대상 스터디", "비공개 설명", false));
        Study other = studyRepository.saveAndFlush(Study.create("다른 스터디", "다른 설명", false));

        testAuthRequest.givenAuthenticatedUser(viewer.getId())
                .port(port)
                .queryParam("studyId", target.getId())
                .queryParam("token", studyInviteTokenProvider.generate(other.getId()))
                .when()
                .get("/studies/join-info")
                .then()
                .statusCode(400)
                .body("code", equalTo("INVALID_INVITE_TOKEN"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "invalid-token"})
    @DisplayName("공개 스터디라도 빈 값이나 잘못된 초대 토큰을 전달하면 거절한다")
    void getStudyJoinInfoWithInvalidTokenTest(String token) {
        User viewer = userRepository.saveAndFlush(User.create("조회 사용자", null));
        Study study = studyRepository.saveAndFlush(Study.create("공개 스터디", "공개 설명", true));

        testAuthRequest.givenAuthenticatedUser(viewer.getId())
                .port(port)
                .queryParam("studyId", study.getId())
                .queryParam("token", token)
                .when()
                .get("/studies/join-info")
                .then()
                .statusCode(400)
                .body("code", equalTo("INVALID_INVITE_TOKEN"));
    }

    @Test
    @DisplayName("만료된 초대 토큰으로 비공개 스터디의 참여 전 정보를 조회할 수 없다")
    void getStudyJoinInfoWithExpiredTokenTest() {
        User viewer = userRepository.saveAndFlush(User.create("조회 사용자", null));
        Study study = studyRepository.saveAndFlush(Study.create("비공개 스터디", "비공개 설명", false));
        StudyInviteTokenProvider expiredProvider = new StudyInviteTokenProvider(
                studyInviteSecret, Clock.offset(clock, Duration.ofDays(-3)));

        testAuthRequest.givenAuthenticatedUser(viewer.getId())
                .port(port)
                .queryParam("token", expiredProvider.generate(study.getId()))
                .when()
                .get("/studies/join-info")
                .then()
                .statusCode(400)
                .body("code", equalTo("INVALID_INVITE_TOKEN"))
                .body("message", equalTo("유효하지 않거나 만료된 초대 토큰입니다."));
    }

    @Test
    @DisplayName("스터디 ID와 초대 토큰을 모두 생략하면 400을 반환한다")
    void getStudyJoinInfoWithoutIdentifierTest() {
        User viewer = userRepository.saveAndFlush(User.create("조회 사용자", null));

        testAuthRequest.givenAuthenticatedUser(viewer.getId())
                .port(port)
                .when()
                .get("/studies/join-info")
                .then()
                .statusCode(400)
                .body("code", equalTo("INVALID_STUDY_ID"));
    }

    @Test
    @DisplayName("스터디 정보 조회 요청을 보내면 스터디명·설명과 내 역할·이름을 반환한다")
    void getStudyInfoTest() {
        User user = userRepository.saveAndFlush(User.create("테스트 사용자", "profile-image-url"));
        Study study = studyRepository.saveAndFlush(Study.create("자바 스터디", "설명", true));
        studyMemberRepository.saveAndFlush(
                StudyMember.create(study, user, "스터디 내 이름", user.getProfileImageUrl(), StudyMemberRole.MEMBER)
        );

        testAuthRequest.givenAuthenticatedUser(user.getId())
                .port(port)
                .when()
                .get("/studies/{studyId}/info", study.getId())
                .then()
                .statusCode(200)
                .body("studyName", equalTo("자바 스터디"))
                .body("description", equalTo("설명"))
                .body("role", equalTo("MEMBER"))
                .body("userName", equalTo("스터디 내 이름"))
                .body("isPublic", equalTo(true));
    }

    @Test
    @DisplayName("설명이 없는 스터디 정보 조회에서도 description 필드를 null로 반환한다")
    void getStudyInfoWithoutDescriptionTest() {
        User user = userRepository.saveAndFlush(User.create("스터디 리더", "profile-image-url"));
        Study study = studyRepository.saveAndFlush(Study.create("자바 스터디", null, false));
        studyMemberRepository.saveAndFlush(
                StudyMember.create(study, user, user.getName(), user.getProfileImageUrl(), StudyMemberRole.LEADER)
        );

        testAuthRequest.givenAuthenticatedUser(user.getId())
                .port(port)
                .when()
                .get("/studies/{studyId}/info", study.getId())
                .then()
                .statusCode(200)
                .body("$", hasKey("description"))
                .body("description", nullValue())
                .body("isPublic", equalTo(false));
    }

    @Test
    @DisplayName("존재하지 않는 스터디 정보를 조회하면 404를 반환한다")
    void getStudyInfoForMissingStudyTest() {
        User user = userRepository.saveAndFlush(User.create("테스트 사용자", "profile-image-url"));

        testAuthRequest.givenAuthenticatedUser(user.getId())
                .port(port)
                .when()
                .get("/studies/{studyId}/info", 999L)
                .then()
                .statusCode(404)
                .body("code", equalTo("STUDY_NOT_FOUND"));
    }

    @Test
    @DisplayName("스터디 멤버가 아니면 스터디 정보 조회 시 403을 반환한다")
    void getStudyInfoForNonMemberTest() {
        User user = userRepository.saveAndFlush(User.create("테스트 사용자", "profile-image-url"));
        Study study = studyRepository.saveAndFlush(Study.create("자바 스터디", "설명", false));

        testAuthRequest.givenAuthenticatedUser(user.getId())
                .port(port)
                .when()
                .get("/studies/{studyId}/info", study.getId())
                .then()
                .statusCode(403)
                .body("code", equalTo("STUDY_ACCESS_DENIED"));
    }

    @Test
    @DisplayName("리더가 스터디 상세 조회를 요청하면 공지·과제별 대상자 수와 완료 수를 반환한다")
    void getStudyDetailForLeaderTest() {
        User leader = userRepository.saveAndFlush(User.create("리더", "leader-profile-image-url"));
        User firstMember = userRepository.saveAndFlush(User.create("첫 번째 멤버", null));
        User secondMember = userRepository.saveAndFlush(User.create("두 번째 멤버", null));
        Study study = studyRepository.saveAndFlush(Study.create("자바 스터디", "설명", false));
        StudyMember leaderMember = studyMemberRepository.saveAndFlush(
                StudyMember.create(study, leader, leader.getName(), leader.getProfileImageUrl(),
                        StudyMemberRole.LEADER)
        );
        StudyMember firstStudyMember = studyMemberRepository.saveAndFlush(
                StudyMember.create(study, firstMember, firstMember.getName(), null, StudyMemberRole.MEMBER)
        );
        StudyMember secondStudyMember = studyMemberRepository.saveAndFlush(
                StudyMember.create(study, secondMember, secondMember.getName(), null, StudyMemberRole.MEMBER)
        );
        Notice notice = noticeRepository.saveAndFlush(Notice.create(study, "공지", "내용"));
        notice.addRecipients(List.of(firstStudyMember, secondStudyMember));
        notice.getRecipients().getFirst().markAsRead(NOTICE_NOW);
        noticeRepository.saveAndFlush(notice);
        Assignment assignment = assignmentRepository.saveAndFlush(
                Assignment.create(
                        study,
                        "과제",
                        "내용",
                        "링크",
                        SubmissionTarget.MEMBERS_ONLY,
                        SubmissionVisibility.LEADER_ONLY,
                        LocalDateTime.of(2026, 8, 20, 0, 0),
                        ASSIGNMENT_NOW
                )
        );
        assignment.initializeSubmissions(List.of(firstStudyMember, secondStudyMember));
        assignment.getSubmissions().getFirst().submit(null, null, ASSIGNMENT_NOW);
        assignmentRepository.saveAndFlush(assignment);

        testAuthRequest.givenAuthenticatedUser(leader.getId())
                .port(port)
                .when()
                .get("/studies/{studyId}", study.getId())
                .then()
                .statusCode(200)
                .body("notices.count", equalTo(1))
                .body("notices.items[0].id", equalTo(notice.getId().intValue()))
                .body("notices.items[0].title", equalTo("공지"))
                .body("notices.items[0].memberCount", equalTo(2))
                .body("notices.items[0].completeCount", equalTo(1))
                .body("assignments.count", equalTo(1))
                .body("assignments.items[0].id", equalTo(assignment.getId().intValue()))
                .body("assignments.items[0].title", equalTo("과제"))
                .body("assignments.items[0].memberCount", equalTo(2))
                .body("assignments.items[0].completeCount", equalTo(1));
    }

    @Test
    @DisplayName("멤버가 스터디 상세 조회를 요청하면 공지·과제 목록과 전체 개수를 반환한다")
    void getStudyDetailForMemberTest() {
        User leader = userRepository.saveAndFlush(User.create("리더", "leader-profile-image-url"));
        User member = userRepository.saveAndFlush(User.create("멤버", "member-profile-image-url"));
        Study study = studyRepository.saveAndFlush(Study.create("자바 스터디", "설명", false));
        StudyMember leaderMember = studyMemberRepository.saveAndFlush(
                StudyMember.create(study, leader, leader.getName(), leader.getProfileImageUrl(),
                        StudyMemberRole.LEADER)
        );
        StudyMember memberStudyMember = studyMemberRepository.saveAndFlush(
                StudyMember.create(study, member, member.getName(), member.getProfileImageUrl(),
                        StudyMemberRole.MEMBER)
        );
        Notice notice = noticeRepository.saveAndFlush(Notice.create(study, "공지", "내용"));
        notice.addRecipients(List.of(memberStudyMember));
        noticeRepository.saveAndFlush(notice);
        Assignment assignment = assignmentRepository.saveAndFlush(
                Assignment.create(
                        study,
                        "과제",
                        "내용",
                        "링크",
                        SubmissionTarget.MEMBERS_ONLY,
                        SubmissionVisibility.LEADER_ONLY,
                        LocalDateTime.of(2026, 8, 20, 0, 0),
                        ASSIGNMENT_NOW
                )
        );
        assignment.initializeSubmissions(List.of(memberStudyMember));
        assignmentRepository.saveAndFlush(assignment);

        testAuthRequest.givenAuthenticatedUser(member.getId())
                .port(port)
                .when()
                .get("/studies/{studyId}", study.getId())
                .then()
                .statusCode(200)
                .body("totalCount", equalTo(2))
                .body("notices.items[0].id", equalTo(notice.getId().intValue()))
                .body("notices.items[0].title", equalTo("공지"))
                .body("assignments.items[0].id", equalTo(assignment.getId().intValue()))
                .body("assignments.items[0].title", equalTo("과제"));
    }

    @Test
    @DisplayName("스터디 멤버가 아니면 스터디 상세 조회 시 403을 반환한다")
    void getStudyDetailForNonMemberTest() {
        User user = userRepository.saveAndFlush(User.create("테스트 사용자", "profile-image-url"));
        Study study = studyRepository.saveAndFlush(Study.create("자바 스터디", "설명", false));

        testAuthRequest.givenAuthenticatedUser(user.getId())
                .port(port)
                .when()
                .get("/studies/{studyId}", study.getId())
                .then()
                .statusCode(403)
                .body("code", equalTo("STUDY_ACCESS_DENIED"));
    }

    @Test
    @DisplayName("스터디 초대 링크 조회 요청을 보내면 초대 링크를 반환한다")
    void getInviteLinkTest() {
        User user = userRepository.saveAndFlush(User.create("테스트 사용자", "profile-image-url"));
        Study study = studyRepository.saveAndFlush(Study.create("자바 스터디", "설명", false));
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
                        startsWith("https://test.chongchong.app/studies/join?token=")
                );
    }

    @Test
    @DisplayName("스터디 멤버가 아닌 사용자가 초대 링크를 조회하면 403을 반환한다")
    void getInviteLinkForNonMemberTest() {
        User user = userRepository.saveAndFlush(User.create("테스트 사용자", "profile-image-url"));
        Study study = studyRepository.saveAndFlush(Study.create("자바 스터디", "설명", false));

        testAuthRequest.givenAuthenticatedUser(user.getId())
                .port(port)
                .when()
                .get("/studies/{studyId}/invite-link", study.getId())
                .then()
                .statusCode(403)
                .body("code", equalTo("STUDY_ACCESS_DENIED"));
    }

    @Test
    @DisplayName("스터디 삭제 시 공지·과제·제출물 알림을 지우고 다른 스터디 알림은 유지한다")
    void deleteStudyTest() {
        User leader = userRepository.saveAndFlush(User.create("리더", "leader-profile-image-url"));
        User member = userRepository.saveAndFlush(User.create("멤버", "member-profile-image-url"));
        Study study = studyRepository.saveAndFlush(Study.create("자바 스터디", "설명", false));
        StudyMember leaderMember = studyMemberRepository.saveAndFlush(
                StudyMember.create(study, leader, leader.getName(), leader.getProfileImageUrl(), StudyMemberRole.LEADER)
        );
        StudyMember memberStudyMember = studyMemberRepository.saveAndFlush(
                StudyMember.create(study, member, member.getName(), member.getProfileImageUrl(), StudyMemberRole.MEMBER)
        );

        Notice notice = Notice.create(study, "공지", "내용");
        notice.addRecipients(List.of(memberStudyMember));
        noticeRepository.saveAndFlush(notice);

        Assignment assignment = Assignment.create(
                study,
                "과제",
                "내용",
                "링크",
                SubmissionTarget.MEMBERS_ONLY,
                SubmissionVisibility.LEADER_ONLY,
                LocalDateTime.of(2026, 8, 20, 0, 0),
                ASSIGNMENT_NOW
        );
        assignment.initializeSubmissions(List.of(memberStudyMember));
        assignmentRepository.saveAndFlush(assignment);

        saveNotification(study, memberStudyMember, notice.getId(), ResourceType.NOTICE);
        saveNotification(study, memberStudyMember, assignment.getId(), ResourceType.ASSIGNMENT);
        Long submissionId = assignment.getSubmissions().getFirst().getId();
        notificationRepository.saveAndFlush(Notification.create(
                leader, "새 제출물", "알림", NotificationType.NEW,
                submissionId, ResourceType.ASSIGNMENT_SUBMISSION,
                "/studies/%d/assignments/%d/submissions/%d".formatted(study.getId(), assignment.getId(),
                        submissionId)
        ));

        Study otherStudy = studyRepository.saveAndFlush(Study.create("다른 스터디", "설명", false));
        StudyMember otherLeader = studyMemberRepository.saveAndFlush(
                StudyMember.create(otherStudy, member, member.getName(), member.getProfileImageUrl(),
                        StudyMemberRole.LEADER)
        );
        Notice otherNotice = Notice.create(otherStudy, "다른 공지", "내용");
        otherNotice.addRecipients(List.of(otherLeader));
        noticeRepository.saveAndFlush(otherNotice);
        saveNotification(otherStudy, otherLeader, otherNotice.getId(), ResourceType.NOTICE);
        assertThat(notificationRepository.count()).isEqualTo(4);

        testAuthRequest.givenAuthenticatedUser(leader.getId())
                .port(port)
                .when()
                .delete("/studies/{studyId}", study.getId())
                .then()
                .statusCode(204);

        assertThat(studyRepository.findById(study.getId())).isEmpty();
        assertThat(studyMemberRepository.findAll())
                .extracting(StudyMember::getId)
                .containsExactly(otherLeader.getId());
        assertThat(noticeRepository.findAll())
                .extracting(Notice::getId)
                .containsExactly(otherNotice.getId());
        assertThat(noticeRecipientRepository.findAll()).hasSize(1);
        assertThat(assignmentRepository.findAll()).isEmpty();
        assertThat(assignmentSubmissionRepository.findAll()).isEmpty();
        assertThat(notificationRepository.findAll()).singleElement().satisfies(notification -> {
            assertThat(notification.getResourceType()).isEqualTo(ResourceType.NOTICE);
            assertThat(notification.getResourceId()).isEqualTo(otherNotice.getId());
        });
    }

    @Test
    @DisplayName("스터디 멤버가 아닌 사용자는 스터디를 삭제할 수 없다")
    void deleteStudyForNonMemberTest() {
        User leader = userRepository.saveAndFlush(User.create("리더", "leader-profile-image-url"));
        User user = userRepository.saveAndFlush(User.create("사용자", "profile-image-url"));
        Study study = studyRepository.saveAndFlush(Study.create("자바 스터디", "설명", false));
        studyMemberRepository.saveAndFlush(
                StudyMember.create(study, leader, leader.getName(), leader.getProfileImageUrl(), StudyMemberRole.LEADER)
        );

        testAuthRequest.givenAuthenticatedUser(user.getId())
                .port(port)
                .when()
                .delete("/studies/{studyId}", study.getId())
                .then()
                .statusCode(403)
                .body("code", equalTo("STUDY_ACCESS_DENIED"));

        assertThat(studyRepository.findById(study.getId())).isPresent();
    }

    @Test
    @DisplayName("스터디 리더가 아닌 멤버는 스터디를 삭제할 수 없다")
    void deleteStudyForNonLeaderTest() {
        User leader = userRepository.saveAndFlush(User.create("리더", "leader-profile-image-url"));
        User member = userRepository.saveAndFlush(User.create("멤버", "member-profile-image-url"));
        Study study = studyRepository.saveAndFlush(Study.create("자바 스터디", "설명", false));
        studyMemberRepository.saveAndFlush(
                StudyMember.create(study, leader, leader.getName(), leader.getProfileImageUrl(), StudyMemberRole.LEADER)
        );
        studyMemberRepository.saveAndFlush(
                StudyMember.create(study, member, member.getName(), member.getProfileImageUrl(), StudyMemberRole.MEMBER)
        );

        testAuthRequest.givenAuthenticatedUser(member.getId())
                .port(port)
                .when()
                .delete("/studies/{studyId}", study.getId())
                .then()
                .statusCode(403)
                .body("code", equalTo("NOT_STUDY_LEADER"));

        assertThat(studyRepository.findById(study.getId())).isPresent();
    }

    @Test
    @DisplayName("존재하지 않는 스터디는 삭제할 수 없다")
    void deleteStudyForMissingStudyTest() {
        User user = userRepository.saveAndFlush(User.create("사용자", "profile-image-url"));

        testAuthRequest.givenAuthenticatedUser(user.getId())
                .port(port)
                .when()
                .delete("/studies/{studyId}", 999L)
                .then()
                .statusCode(404)
                .body("code", equalTo("STUDY_NOT_FOUND"));
    }

    private void addStudyMembers(Study study, int count) {
        for (int i = 0; i < count; i++) {
            User member = userRepository.save(User.create("멤버" + i, null));
            StudyMemberRole role = i == 0 ? StudyMemberRole.LEADER : StudyMemberRole.MEMBER;
            studyMemberRepository.save(StudyMember.create(study, member, member.getName(), null, role));
        }
        studyMemberRepository.flush();
    }

    private void setStudyCreatedAt(Long studyId, LocalDateTime createdAt) {
        jdbcTemplate.update(
                "UPDATE studies SET created_at = ? WHERE id = ?",
                Timestamp.valueOf(createdAt),
                studyId
        );
    }

    private void setCreatedAt(Long studyMemberId, LocalDateTime createdAt) {
        jdbcTemplate.update(
                "UPDATE study_members SET created_at = ? WHERE id = ?",
                Timestamp.valueOf(createdAt),
                studyMemberId
        );
    }

    private void configureCascadeIfColumnExists(
            String tableName,
            String columnName,
            String referencedTableName,
            String constraintName
    ) {
        Integer columnCount = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*)
                        FROM INFORMATION_SCHEMA.COLUMNS
                        WHERE TABLE_SCHEMA = 'PUBLIC'
                          AND TABLE_NAME = ?
                          AND COLUMN_NAME = ?
                        """,
                Integer.class,
                tableName.toUpperCase(),
                columnName.toUpperCase()
        );
        if (columnCount == null || columnCount == 0) {
            return;
        }

        jdbcTemplate.execute("ALTER TABLE %s DROP CONSTRAINT IF EXISTS %s"
                .formatted(tableName, constraintName));
        jdbcTemplate.execute("ALTER TABLE %s ADD CONSTRAINT %s FOREIGN KEY (%s) REFERENCES %s (id) ON DELETE CASCADE"
                .formatted(tableName, constraintName, columnName, referencedTableName));
    }

    private void saveNotification(
            Study study,
            StudyMember recipient,
            Long resourceId,
            ResourceType resourceType
    ) {
        String resourcePath = resourceType == ResourceType.NOTICE ? "notices" : "assignments";
        Notification notification = Notification.create(
                recipient.getUser(),
                "[스터디] 새 " + resourceType.getName(),
                "알림",
                NotificationType.REMIND,
                resourceId,
                resourceType,
                "/studies/%d/%s/%d".formatted(study.getId(), resourcePath, resourceId)
        );
        notificationRepository.saveAndFlush(notification);
    }
}
