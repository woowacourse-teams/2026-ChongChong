package withoutc.chongchong.assignment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import io.restassured.http.ContentType;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import withoutc.chongchong.assignment.entity.Assignment;
import withoutc.chongchong.assignment.repository.AssignmentRepository;
import withoutc.chongchong.auth.support.TestAuthRequest;
import withoutc.chongchong.study.entity.Study;
import withoutc.chongchong.study.entity.StudyMember;
import withoutc.chongchong.study.entity.StudyMemberRole;
import withoutc.chongchong.study.repository.StudyMemberRepository;
import withoutc.chongchong.study.repository.StudyRepository;
import withoutc.chongchong.support.TestDatabaseCleaner;
import withoutc.chongchong.user.entity.User;
import withoutc.chongchong.user.repository.UserRepository;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AssignmentApiTest {

    private static final Clock CLOCK = Clock.system(ZoneId.of("Asia/Seoul"));

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StudyRepository studyRepository;

    @Autowired
    private StudyMemberRepository studyMemberRepository;

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

    private User leaderUser;
    private User memberUser;
    private Study study;
    private StudyMember leader;
    private StudyMember member;
    private StudyMember secondMember;
    private Assignment assignment;
    private LocalDateTime closeAt;
    private LocalDateTime remindAt;

    @BeforeEach
    void setUp() {
        databaseCleaner.clean();

        leaderUser = userRepository.save(User.create("리더", "https://example.com/leader.png"));
        memberUser = userRepository.save(User.create("스터디원", null));
        User secondMemberUser = userRepository.save(User.create("두 번째 스터디원", null));
        study = studyRepository.save(Study.create("자바 스터디", "설명"));
        leader = studyMemberRepository.save(
                StudyMember.create(study, leaderUser, "리더", "https://example.com/leader.png", StudyMemberRole.LEADER)
        );
        member = studyMemberRepository.save(
                StudyMember.create(study, memberUser, "스터디원", null, StudyMemberRole.MEMBER)
        );
        secondMember = studyMemberRepository.save(
                StudyMember.create(study, secondMemberUser, "두 번째 스터디원", null, StudyMemberRole.MEMBER)
        );

        closeAt = LocalDateTime.now(CLOCK).plusDays(30).truncatedTo(ChronoUnit.SECONDS);
        remindAt = closeAt.minusDays(1);
        assignment = createAssignment("기존 과제", "기존 과제 내용", "링크 제출", closeAt, remindAt);
    }

    @AfterEach
    void cleanDatabase() {
        databaseCleaner.clean();
    }

    @Test
    @DisplayName("과제 생성 요청은 201과 id를 반환하고 스터디원별 제출 정보와 리마인더를 저장한다")
    void createAssignmentTest() {
        LocalDateTime newCloseAt = closeAt.plusDays(10);
        LocalDateTime newRemindAt = newCloseAt.minusDays(1);

        Long createdAssignmentId = testAuthRequest.givenAuthenticatedUser(leaderUser.getId())
                .port(port)
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "title": "새 과제",
                          "content": "새 과제 내용",
                          "submissionMethod": "텍스트 제출",
                          "closeAt": "%s",
                          "remindAts": ["%s"]
                        }
                        """.formatted(newCloseAt, newRemindAt))
                .when()
                .post("/studies/{studyId}/assignments", study.getId())
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .extract()
                .jsonPath()
                .getLong("id");

        assertThat(countRows("assignment_submissions", createdAssignmentId)).isEqualTo(2);
        assertThat(countRows("assignment_reminders", createdAssignmentId)).isEqualTo(1);
    }

    @Test
    @DisplayName("스터디원이 과제 상세를 조회하면 클라이언트 계약 필드를 반환한다")
    void getAssignmentDetailTest() {
        testAuthRequest.givenAuthenticatedUser(memberUser.getId())
                .port(port)
                .when()
                .get("/studies/{studyId}/assignments/{assignmentId}", study.getId(), assignment.getId())
                .then()
                .statusCode(200)
                .body("id", equalTo(assignment.getId().intValue()))
                .body("title", equalTo("기존 과제"))
                .body("content", equalTo("기존 과제 내용"))
                .body("submissionMethod", equalTo("링크 제출"))
                .body("closeAt", equalTo(closeAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)));
    }

    @Test
    @DisplayName("리더가 과제 목록을 조회하면 제출 인원과 리마인드 정보를 반환한다")
    void getAssignmentsByLeaderTest() {
        testAuthRequest.givenAuthenticatedUser(leaderUser.getId())
                .port(port)
                .when()
                .get("/studies/{studyId}/assignments", study.getId())
                .then()
                .statusCode(200)
                .body("hasNext", equalTo(false))
                .body("nextCursor", nullValue())
                .body("assignments", hasSize(1))
                .body("assignments[0].id", equalTo(assignment.getId().intValue()))
                .body("assignments[0].submissionType", equalTo("링크 제출"))
                .body("assignments[0].memberCount", equalTo(2))
                .body("assignments[0].completeCount", equalTo(0))
                .body("assignments[0].remindAt", equalTo(remindAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)))
                .body("assignments[0].isComplete", equalTo(false));
    }

    @Test
    @DisplayName("스터디원이 과제 목록을 조회하면 자신의 제출 여부만 반환한다")
    void getAssignmentsByMemberTest() {
        testAuthRequest.givenAuthenticatedUser(memberUser.getId())
                .port(port)
                .when()
                .get("/studies/{studyId}/assignments", study.getId())
                .then()
                .statusCode(200)
                .body("assignments[0].id", equalTo(assignment.getId().intValue()))
                .body("assignments[0].submissionType", equalTo("링크 제출"))
                .body("assignments[0].isComplete", equalTo(false))
                .body("assignments[0]", not(hasKey("memberCount")))
                .body("assignments[0]", not(hasKey("completeCount")))
                .body("assignments[0]", not(hasKey("remindAt")));
    }

    @Test
    @DisplayName("과제 목록을 size만큼 조회하면 다음 페이지를 위한 cursor를 반환한다")
    void getAssignmentsWithCursorTest() {
        Assignment middleAssignment = createAssignment(
                "두 번째 과제", "두 번째 내용", "텍스트 제출", closeAt.plusDays(1), null
        );
        Assignment latestAssignment = createAssignment(
                "세 번째 과제", "세 번째 내용", "파일 제출", closeAt.plusDays(2), null
        );

        testAuthRequest.givenAuthenticatedUser(leaderUser.getId())
                .port(port)
                .queryParam("size", 2)
                .when()
                .get("/studies/{studyId}/assignments", study.getId())
                .then()
                .statusCode(200)
                .body("hasNext", equalTo(true))
                .body("nextCursor", equalTo(middleAssignment.getId().intValue()))
                .body("assignments", hasSize(2))
                .body("assignments[0].id", equalTo(latestAssignment.getId().intValue()))
                .body("assignments[1].id", equalTo(middleAssignment.getId().intValue()));

        testAuthRequest.givenAuthenticatedUser(leaderUser.getId())
                .port(port)
                .queryParam("cursor", middleAssignment.getId())
                .queryParam("size", 2)
                .when()
                .get("/studies/{studyId}/assignments", study.getId())
                .then()
                .statusCode(200)
                .body("hasNext", equalTo(false))
                .body("nextCursor", nullValue())
                .body("assignments", hasSize(1))
                .body("assignments[0].id", equalTo(assignment.getId().intValue()));
    }

    @Test
    @DisplayName("리더가 JSON으로 과제 수정 요청을 보내면 모든 수정 필드와 대기 리마인더를 변경한다")
    void updateAssignmentTest() {
        LocalDateTime newCloseAt = closeAt.plusDays(10);
        LocalDateTime newRemindAt = newCloseAt.minusDays(2);

        testAuthRequest.givenAuthenticatedUser(leaderUser.getId())
                .port(port)
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "title": "수정 과제",
                          "content": "수정 과제 내용",
                          "submissionMethod": "텍스트 제출",
                          "closeAt": "%s",
                          "remindAts": ["%s"]
                        }
                        """.formatted(newCloseAt, newRemindAt))
                .when()
                .patch("/studies/{studyId}/assignments/{assignmentId}", study.getId(), assignment.getId())
                .then()
                .statusCode(204);

        AssignmentRow savedAssignment = jdbcTemplate.queryForObject(
                """
                        SELECT title, content, submission_method, close_at
                        FROM assignments
                        WHERE id = ?
                        """,
                (resultSet, rowNumber) -> new AssignmentRow(
                        resultSet.getString("title"),
                        resultSet.getString("content"),
                        resultSet.getString("submission_method"),
                        resultSet.getTimestamp("close_at").toLocalDateTime()
                ),
                assignment.getId()
        );
        LocalDateTime savedRemindAt = jdbcTemplate.queryForObject(
                "SELECT remind_at FROM assignment_reminders WHERE assignment_id = ? AND status = 'PENDING'",
                LocalDateTime.class,
                assignment.getId()
        );

        assertThat(savedAssignment).isEqualTo(
                new AssignmentRow("수정 과제", "수정 과제 내용", "텍스트 제출", newCloseAt)
        );
        assertThat(savedRemindAt).isEqualTo(newRemindAt);
        assertThat(countRows("assignment_reminders", assignment.getId())).isEqualTo(1);
    }

    @Test
    @DisplayName("리더가 과제를 삭제하면 과제 애그리거트가 삭제되고 더는 조회할 수 없다")
    void deleteAssignmentTest() {
        testAuthRequest.givenAuthenticatedUser(leaderUser.getId())
                .port(port)
                .when()
                .delete("/studies/{studyId}/assignments/{assignmentId}", study.getId(), assignment.getId())
                .then()
                .statusCode(204);

        assertThat(countRows("assignments", assignment.getId())).isZero();
        assertThat(countRows("assignment_submissions", assignment.getId())).isZero();
        assertThat(countRows("assignment_reminders", assignment.getId())).isZero();

        testAuthRequest.givenAuthenticatedUser(memberUser.getId())
                .port(port)
                .when()
                .get("/studies/{studyId}/assignments/{assignmentId}", study.getId(), assignment.getId())
                .then()
                .statusCode(404)
                .body("code", equalTo("ASSIGNMENT_NOT_FOUND"));
    }

    @Test
    @DisplayName("스터디원은 과제를 수정할 수 없다")
    void updateAssignmentByMemberTest() {
        testAuthRequest.givenAuthenticatedUser(memberUser.getId())
                .port(port)
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "title": "권한 없는 수정"
                        }
                        """)
                .when()
                .patch("/studies/{studyId}/assignments/{assignmentId}", study.getId(), assignment.getId())
                .then()
                .statusCode(403)
                .body("code", equalTo("ACCESS_DENIED"));
    }

    @Test
    @DisplayName("다른 스터디의 과제를 상세 조회하면 존재하지 않는 과제로 응답한다")
    void getAssignmentDetailFromOtherStudyTest() {
        Study otherStudy = studyRepository.save(Study.create("다른 스터디", "설명"));
        StudyMember otherLeader = studyMemberRepository.save(
                StudyMember.create(otherStudy, leaderUser, "다른 스터디 리더", null, StudyMemberRole.LEADER)
        );
        Assignment otherAssignment = assignmentRepository.saveAndFlush(
                Assignment.create(
                        otherLeader,
                        "다른 과제",
                        "다른 과제 내용",
                        "링크 제출",
                        closeAt,
                        CLOCK
                )
        );

        testAuthRequest.givenAuthenticatedUser(memberUser.getId())
                .port(port)
                .when()
                .get("/studies/{studyId}/assignments/{assignmentId}", study.getId(), otherAssignment.getId())
                .then()
                .statusCode(404)
                .body("code", equalTo("ASSIGNMENT_NOT_FOUND"));
    }

    @Test
    @DisplayName("스터디에 참여하지 않은 사용자는 과제 목록을 조회할 수 없다")
    void getAssignmentsByNonParticipantTest() {
        User outsider = userRepository.saveAndFlush(User.create("외부 사용자", null));

        testAuthRequest.givenAuthenticatedUser(outsider.getId())
                .port(port)
                .when()
                .get("/studies/{studyId}/assignments", study.getId())
                .then()
                .statusCode(403)
                .body("code", equalTo("STUDY_ACCESS_DENIED"));
    }

    @Test
    @DisplayName("과제 생성 요청의 제목이 공백이면 검증 오류를 반환한다")
    void createAssignmentWithBlankTitleTest() {
        testAuthRequest.givenAuthenticatedUser(leaderUser.getId())
                .port(port)
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "title": " ",
                          "content": "과제 내용",
                          "submissionMethod": "링크 제출",
                          "closeAt": "%s",
                          "remindAts": []
                        }
                        """.formatted(closeAt))
                .when()
                .post("/studies/{studyId}/assignments", study.getId())
                .then()
                .statusCode(400)
                .body("code", equalTo("INVALID_INPUT_VALUE"))
                .body("errors.field", hasItem("title"));
    }

    @Test
    @DisplayName("스터디원의 제출 정보가 없는 과제는 목록에서 제외한다")
    void getAssignmentsWithoutSubmissionTest() {
        jdbcTemplate.update(
                "DELETE FROM assignment_submissions WHERE assignment_id = ? AND member_id = ?",
                assignment.getId(),
                member.getId()
        );

        testAuthRequest.givenAuthenticatedUser(memberUser.getId())
                .port(port)
                .when()
                .get("/studies/{studyId}/assignments", study.getId())
                .then()
                .statusCode(200)
                .body("assignments", hasSize(0));
    }

    private Assignment createAssignment(String title, String content, String submissionMethod,
                                          LocalDateTime assignmentCloseAt, LocalDateTime assignmentRemindAt) {
        Assignment newAssignment = Assignment.create(
                leader,
                title,
                content,
                submissionMethod,
                assignmentCloseAt,
                CLOCK
        );
        newAssignment.initializeSubmissions(List.of(member, secondMember));
        if (assignmentRemindAt != null) {
            newAssignment.addReminders(List.of(assignmentRemindAt), LocalDateTime.now(CLOCK));
        }
        return assignmentRepository.saveAndFlush(newAssignment);
    }

    private int countRows(String tableName, Long assignmentId) {
        String foreignKey = tableName.equals("assignments") ? "id" : "assignment_id";
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + tableName + " WHERE " + foreignKey + " = ?",
                Integer.class,
                assignmentId
        );
    }

    private record AssignmentRow(
            String title,
            String content,
            String submissionMethod,
            LocalDateTime closeAt
    ) {
    }
}
