package withoutc.chongchong.study;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;

import io.restassured.http.ContentType;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import withoutc.chongchong.auth.support.TestAuthRequest;
import withoutc.chongchong.assignment.entity.Assignment;
import withoutc.chongchong.assignment.repository.AssignmentRepository;
import withoutc.chongchong.notice.entity.Notice;
import withoutc.chongchong.notice.repository.NoticeRepository;
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
    private NoticeRepository noticeRepository;

    @Autowired
    private AssignmentRepository assignmentRepository;

    @Autowired
    private TestDatabaseCleaner databaseCleaner;

    @Autowired
    private TestAuthRequest testAuthRequest;

    @Autowired
    private JdbcTemplate jdbcTemplate;

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
                .body("code", equalTo("NOT_STUDY_MEMBER"));
    }

    @Test
    @DisplayName("리더가 스터디 상세 조회를 요청하면 멤버 수와 공지·과제 완료 수를 반환한다")
    void getStudyDetailForLeaderTest() {
        User leader = userRepository.saveAndFlush(User.create("리더", "leader-profile-image-url"));
        Study study = studyRepository.saveAndFlush(Study.create("자바 스터디", "설명"));
        StudyMember leaderMember = studyMemberRepository.saveAndFlush(
                StudyMember.create(study, leader, leader.getName(), leader.getProfileImageUrl(),
                        StudyMemberRole.LEADER)
        );
        Notice notice = noticeRepository.saveAndFlush(Notice.create(study, leaderMember, "공지", "내용"));
        Assignment assignment = assignmentRepository.saveAndFlush(
                Assignment.create(study, leaderMember, "과제", "내용", "링크", LocalDateTime.of(2026, 8, 20, 0, 0))
        );

        testAuthRequest.givenAuthenticatedUser(leader.getId())
                .port(port)
                .when()
                .get("/studies/{studyId}", study.getId())
                .then()
                .statusCode(200)
                .body("memberCount", equalTo(1))
                .body("notices.count", equalTo(1))
                .body("notices.items[0].id", equalTo(notice.getId().intValue()))
                .body("notices.items[0].title", equalTo("공지"))
                .body("notices.items[0].completeCount", equalTo(2))
                .body("assignments.count", equalTo(1))
                .body("assignments.items[0].id", equalTo(assignment.getId().intValue()))
                .body("assignments.items[0].title", equalTo("과제"))
                .body("assignments.items[0].completeCount", equalTo(2));
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
        studyMemberRepository.saveAndFlush(
                StudyMember.create(study, member, member.getName(), member.getProfileImageUrl(),
                        StudyMemberRole.MEMBER)
        );
        Notice notice = noticeRepository.saveAndFlush(Notice.create(study, leaderMember, "공지", "내용"));
        Assignment assignment = assignmentRepository.saveAndFlush(
                Assignment.create(study, leaderMember, "과제", "내용", "링크", LocalDateTime.of(2026, 8, 20, 0, 0))
        );

        testAuthRequest.givenAuthenticatedUser(member.getId())
                .port(port)
                .when()
                .get("/studies/{studyId}", study.getId())
                .then()
                .statusCode(200)
                .body("totalCount", equalTo(4))
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
                .body("code", equalTo("NOT_STUDY_MEMBER"));
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

    @Test
    @DisplayName("스터디 리더가 삭제 요청을 보내면 스터디와 하위 데이터가 모두 삭제된다")
    void deleteStudyTest() {
        User leader = userRepository.saveAndFlush(User.create("리더", "leader-profile-image-url"));
        User member = userRepository.saveAndFlush(User.create("멤버", "member-profile-image-url"));
        Study study = studyRepository.saveAndFlush(Study.create("자바 스터디", "설명"));
        StudyMember leaderMember = studyMemberRepository.saveAndFlush(
                StudyMember.create(study, leader, leader.getName(), leader.getProfileImageUrl(), StudyMemberRole.LEADER)
        );
        studyMemberRepository.saveAndFlush(
                StudyMember.create(study, member, member.getName(), member.getProfileImageUrl(), StudyMemberRole.MEMBER)
        );
        noticeRepository.saveAndFlush(Notice.create(study, leaderMember, "공지", "내용"));
        assignmentRepository.saveAndFlush(
                Assignment.create(study, leaderMember, "과제", "내용", "링크", LocalDateTime.of(2026, 8, 20, 0, 0))
        );

        testAuthRequest.givenAuthenticatedUser(leader.getId())
                .port(port)
                .when()
                .delete("/studies/{studyId}", study.getId())
                .then()
                .statusCode(204);

        assertThat(studyRepository.findById(study.getId())).isEmpty();
        assertThat(studyMemberRepository.findAll()).isEmpty();
        assertThat(noticeRepository.findAll()).isEmpty();
        assertThat(assignmentRepository.findAll()).isEmpty();
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
                .body("code", equalTo("NOT_STUDY_MEMBER"));

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
}
