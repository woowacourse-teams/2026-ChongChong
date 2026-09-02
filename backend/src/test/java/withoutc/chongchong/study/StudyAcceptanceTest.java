package withoutc.chongchong.study;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;

import io.restassured.http.ContentType;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import withoutc.chongchong.auth.support.TestAuthRequest;
import withoutc.chongchong.assignment.entity.Assignment;
import withoutc.chongchong.assignment.repository.AssignmentRepository;
import withoutc.chongchong.assignment.repository.AssignmentSubmissionRepository;
import withoutc.chongchong.notice.entity.Notice;
import withoutc.chongchong.notice.repository.NoticeRecipientRepository;
import withoutc.chongchong.notice.repository.NoticeRepository;
import withoutc.chongchong.notification.entity.Notification;
import withoutc.chongchong.notification.entity.NotificationResourceType;
import withoutc.chongchong.notification.entity.NotificationType;
import withoutc.chongchong.notification.repository.NotificationRepository;
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

    private static final LocalDateTime NOTICE_NOW = LocalDateTime.of(2026, 8, 19, 0, 0);
    private static final LocalDateTime ASSIGNMENT_NOW = LocalDateTime.of(2026, 8, 19, 9, 0);

    @Autowired
    private StudyRepository studyRepository;

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
        configureCascadeIfColumnExists("notifications", "study_id", "studies", "fko5m57o40ivnn0td7m511dx42k");
        configureCascadeIfColumnExists(
                "notifications", "recipient_id", "study_members", "fk60prjsdd6ahrlv3ayvjjqdlqi");
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
                          "description": "매주 월요일에 진행한다."
                        }
                        """)
                .when()
                .post("/studies")
                .then()
                .statusCode(201)
                .body("studyId", notNullValue());

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
    @DisplayName("스터디 설명이 null인 생성 요청을 보내면 설명을 null로 저장한다")
    void createStudyWithNullDescriptionTest() {
        User user = userRepository.saveAndFlush(User.create("테스트 사용자", "profile-image-url"));

        testAuthRequest.givenAuthenticatedUser(user.getId())
                .port(port)
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "name": "자바 스터디",
                          "description": null
                        }
                        """)
                .when()
                .post("/studies")
                .then()
                .statusCode(201)
                .body("studyId", notNullValue());

        assertThat(studyRepository.findAll())
                .singleElement()
                .satisfies(study -> assertThat(study.getDescription()).isNull());
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
                          "description": "1234567890123456789012345678901"
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
        Study study = studyRepository.saveAndFlush(Study.create("기존 스터디", "기존 설명"));
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
                          "description": "수정 설명"
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
                });
    }

    @Test
    @DisplayName("스터디 수정 요청에서 설명이 null이면 기존 설명을 유지한다")
    void updateStudyWithNullDescriptionTest() {
        User leader = userRepository.saveAndFlush(User.create("리더", "leader-profile-image-url"));
        Study study = studyRepository.saveAndFlush(Study.create("기존 스터디", "기존 설명"));
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
                          "description": null
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
                });
    }

    @Test
    @DisplayName("스터디 멤버가 아니면 스터디를 수정할 수 없다")
    void updateStudyForNonMemberTest() {
        User leader = userRepository.saveAndFlush(User.create("리더", "leader-profile-image-url"));
        User user = userRepository.saveAndFlush(User.create("사용자", "profile-image-url"));
        Study study = studyRepository.saveAndFlush(Study.create("기존 스터디", "기존 설명"));
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
        Study study = studyRepository.saveAndFlush(Study.create("기존 스터디", "기존 설명"));
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
        Study study = studyRepository.saveAndFlush(Study.create("기존 스터디", "기존 설명"));
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
        Study olderStudy = studyRepository.saveAndFlush(Study.create("이전 스터디", "이전 스터디 설명"));
        Study newerStudy = studyRepository.saveAndFlush(Study.create("최근 스터디", "최근 스터디 설명"));

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
                .body("studies[1].id", equalTo(olderStudy.getId().intValue()))
                .body("studies[1].role", equalTo("MEMBER"))
                .body("studies[1].memberCount", equalTo(2));
    }

    @Test
    @DisplayName("스터디 정보 조회 요청을 보내면 스터디명과 내 역할·이름을 반환한다")
    void getStudyInfoTest() {
        User user = userRepository.saveAndFlush(User.create("테스트 사용자", "profile-image-url"));
        Study study = studyRepository.saveAndFlush(Study.create("자바 스터디", "설명"));
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
                .body("role", equalTo("MEMBER"))
                .body("userName", equalTo("스터디 내 이름"));
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
        Study study = studyRepository.saveAndFlush(Study.create("자바 스터디", "설명"));

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
        Study study = studyRepository.saveAndFlush(Study.create("자바 스터디", "설명"));
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
                        LocalDateTime.of(2026, 8, 20, 0, 0),
                        ASSIGNMENT_NOW
                )
        );
        assignment.initializeSubmissions(List.of(firstStudyMember, secondStudyMember));
        ReflectionTestUtils.setField(assignment.getSubmissions().getFirst(), "submitted", true);
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
        Study study = studyRepository.saveAndFlush(Study.create("자바 스터디", "설명"));
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
        Study study = studyRepository.saveAndFlush(Study.create("자바 스터디", "설명"));

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
                .body("code", equalTo("STUDY_ACCESS_DENIED"));
    }

    @Test
    @DisplayName("스터디 리더가 삭제 요청을 보내면 스터디와 하위 데이터가 모두 삭제된다")
    void deleteStudyTest() {
        User leader = userRepository.saveAndFlush(User.create("리더", "leader-profile-image-url"));
        User member = userRepository.saveAndFlush(User.create("멤버", "member-profile-image-url"));
        Study study = studyRepository.saveAndFlush(Study.create("자바 스터디", "설명"));
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
                LocalDateTime.of(2026, 8, 20, 0, 0),
                ASSIGNMENT_NOW
        );
        assignment.initializeSubmissions(List.of(memberStudyMember));
        assignmentRepository.saveAndFlush(assignment);

        saveNotification(study, memberStudyMember, notice.getId(), NotificationResourceType.NOTICE);
        saveNotification(study, memberStudyMember, assignment.getId(), NotificationResourceType.ASSIGNMENT);

        testAuthRequest.givenAuthenticatedUser(leader.getId())
                .port(port)
                .when()
                .delete("/studies/{studyId}", study.getId())
                .then()
                .statusCode(204);

        assertThat(studyRepository.findById(study.getId())).isEmpty();
        assertThat(studyMemberRepository.findAll()).isEmpty();
        assertThat(noticeRepository.findAll()).isEmpty();
        assertThat(noticeRecipientRepository.findAll()).isEmpty();
        assertThat(assignmentRepository.findAll()).isEmpty();
        assertThat(assignmentSubmissionRepository.findAll()).isEmpty();
        assertThat(notificationRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("스터디 멤버가 아닌 사용자는 스터디를 삭제할 수 없다")
    void deleteStudyForNonMemberTest() {
        User leader = userRepository.saveAndFlush(User.create("리더", "leader-profile-image-url"));
        User user = userRepository.saveAndFlush(User.create("사용자", "profile-image-url"));
        Study study = studyRepository.saveAndFlush(Study.create("자바 스터디", "설명"));
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
        Study study = studyRepository.saveAndFlush(Study.create("자바 스터디", "설명"));
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
            NotificationResourceType resourceType
    ) {
        Notification notification = BeanUtils.instantiateClass(Notification.class);
        ReflectionTestUtils.setField(notification, "study", study);
        ReflectionTestUtils.setField(notification, "recipient", recipient);
        ReflectionTestUtils.setField(notification, "type", NotificationType.REMIND);
        ReflectionTestUtils.setField(notification, "resourceId", resourceId);
        ReflectionTestUtils.setField(notification, "resourceType", resourceType);
        notificationRepository.saveAndFlush(notification);
    }
}
