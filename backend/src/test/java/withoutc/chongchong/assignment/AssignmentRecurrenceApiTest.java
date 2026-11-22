package withoutc.chongchong.assignment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;

import io.restassured.http.ContentType;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
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

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AssignmentRecurrenceApiTest {

    private static final Clock CLOCK = Clock.system(ZoneId.of("Asia/Seoul"));
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    private static final String COLLECTION_PATH = "/studies/{studyId}/assignment-recurrences";
    private static final String DETAIL_PATH = "/studies/{studyId}/assignment-recurrences/{recurrenceId}";

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StudyRepository studyRepository;

    @Autowired
    private StudyMemberRepository studyMemberRepository;

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
    private StudyMember member;
    private LocalDateTime closeAt;

    @BeforeEach
    void setUp() {
        databaseCleaner.clean();
        leaderUser = userRepository.save(User.create("리더", null));
        memberUser = userRepository.save(User.create("스터디원", null));
        study = studyRepository.save(Study.create("자바 스터디", "설명", false));
        studyMemberRepository.save(StudyMember.create(study, leaderUser, "리더", null, StudyMemberRole.LEADER));
        member = studyMemberRepository.save(
                StudyMember.create(study, memberUser, "스터디원", null, StudyMemberRole.MEMBER));
        closeAt = LocalDateTime.now(CLOCK).plusDays(3).withHour(23).withMinute(59).truncatedTo(ChronoUnit.MINUTES);
    }

    @AfterEach
    void cleanDatabase() {
        databaseCleaner.clean();
    }

    @Test
    @DisplayName("반복 과제를 등록하면 첫 회차가 생성되고 목록은 다음 회차 기준 일정과 리마인드를 반환한다")
    void createRangeRecurrence() {
        Map<String, Object> body = createBody();
        body.put("submissionTarget", "MEMBERS_ONLY");
        body.put("remindAts", List.of(format(closeAt.minusDays(1)), format(closeAt.minusHours(1))));

        Map<String, Object> created = createRecurrence(body);

        Long assignmentId = ((Number) created.get("assignmentId")).longValue();
        Map<String, Object> occurrence = jdbcTemplate.queryForMap(
                "SELECT recurrence_id, occurrence_date FROM assignments WHERE id = ?", assignmentId);
        assertThat(((Number) occurrence.get("recurrence_id")).longValue())
                .isEqualTo(((Number) created.get("recurrenceId")).longValue());
        assertThat(occurrence.get("occurrence_date").toString()).isEqualTo(closeAt.toLocalDate().toString());

        LocalDateTime nextCloseAt = closeAt.plusWeeks(1);
        givenLeader().when().get(COLLECTION_PATH, study.getId())
                .then().statusCode(200)
                .body("recurrences", hasSize(1))
                .body("recurrences[0].title", equalTo("알고리즘 풀이"))
                .body("recurrences[0].submissionTarget", equalTo("MEMBERS_ONLY"))
                .body("recurrences[0].submitterIds", empty())
                .body("recurrences[0].intervalWeeks", equalTo(1))
                .body("recurrences[0].dayOfWeek", equalTo(nextCloseAt.getDayOfWeek().name()))
                .body("recurrences[0].closeTime", equalTo("23:59:00"))
                .body("recurrences[0].nextCloseAt", equalTo(format(nextCloseAt)))
                .body("recurrences[0].remindAts",
                        contains(format(nextCloseAt.minusDays(1)), format(nextCloseAt.minusHours(1))));
    }

    @Test
    @DisplayName("멤버를 직접 선택한 반복 과제는 제출 대상 범위 없이 멤버 ID를 반환한다")
    void createSelectedRecurrence() {
        Map<String, Object> body = createBody();
        body.put("submitterIds", List.of(member.getId()));
        body.put("intervalWeeks", 2);

        createRecurrence(body);

        givenLeader().when().get(COLLECTION_PATH, study.getId())
                .then().statusCode(200)
                .body("recurrences[0].submissionTarget", nullValue())
                .body("recurrences[0].submitterIds", contains(member.getId().intValue()))
                .body("recurrences[0].nextCloseAt", equalTo(format(closeAt.plusWeeks(2))));
    }

    @Test
    @DisplayName("제출 대상 범위와 멤버 선택을 함께 보내면 거부한다")
    void rejectBothSubmitterChoices() {
        Map<String, Object> body = createBody();
        body.put("submissionTarget", "MEMBERS_ONLY");
        body.put("submitterIds", List.of(member.getId()));

        givenLeader().contentType(ContentType.JSON).body(body)
                .when().post(COLLECTION_PATH, study.getId())
                .then().statusCode(400)
                .body("code", equalTo("INVALID_SUBMITTER_IDS"));
    }

    @Test
    @DisplayName("반복 간격 이상 앞선 리마인드는 다음 회차에 보낼 수 없어 거부한다")
    void rejectRemindAtLongerThanInterval() {
        LocalDateTime farCloseAt = closeAt.plusWeeks(2);
        Map<String, Object> body = createBody();
        body.put("closeAt", format(farCloseAt));
        body.put("submissionTarget", "MEMBERS_ONLY");
        body.put("remindAts", List.of(format(farCloseAt.minusDays(8))));

        givenLeader().contentType(ContentType.JSON).body(body)
                .when().post(COLLECTION_PATH, study.getId())
                .then().statusCode(400)
                .body("code", equalTo("INVALID_REMIND_AT"));
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM assignments", Integer.class)).isZero();
    }

    @Test
    @DisplayName("스터디원은 반복 과제를 등록하거나 조회할 수 없다")
    void forbidMember() {
        Map<String, Object> body = createBody();
        body.put("submissionTarget", "MEMBERS_ONLY");

        testAuthRequest.givenAuthenticatedUser(memberUser.getId()).port(port)
                .contentType(ContentType.JSON).body(body)
                .when().post(COLLECTION_PATH, study.getId())
                .then().statusCode(403);
        testAuthRequest.givenAuthenticatedUser(memberUser.getId()).port(port)
                .when().get(COLLECTION_PATH, study.getId())
                .then().statusCode(403);
    }

    @Test
    @DisplayName("반복 과제를 수정하면 이후 회차 일정만 바뀌고 이미 생성된 회차는 그대로다")
    void updateRecurrenceForFutureOccurrences() {
        Map<String, Object> body = createBody();
        body.put("submissionTarget", "MEMBERS_ONLY");
        Map<String, Object> created = createRecurrence(body);
        LocalDateTime nextCloseAt = closeAt.plusDays(9).withHour(21).withMinute(0);

        givenLeader().contentType(ContentType.JSON)
                .body(Map.of("title", "새 제목", "submitterIds", List.of(member.getId()),
                        "nextCloseAt", format(nextCloseAt), "intervalWeeks", 2,
                        "remindAts", List.of(format(nextCloseAt.minusHours(3)))))
                .when().patch(DETAIL_PATH, study.getId(), created.get("recurrenceId"))
                .then().statusCode(204);

        givenLeader().when().get(COLLECTION_PATH, study.getId())
                .then().statusCode(200)
                .body("recurrences[0].title", equalTo("새 제목"))
                .body("recurrences[0].submissionTarget", nullValue())
                .body("recurrences[0].submitterIds", contains(member.getId().intValue()))
                .body("recurrences[0].intervalWeeks", equalTo(2))
                .body("recurrences[0].closeTime", equalTo("21:00:00"))
                .body("recurrences[0].nextCloseAt", equalTo(format(nextCloseAt)))
                .body("recurrences[0].remindAts", contains(format(nextCloseAt.minusHours(3))));
        assertThat(jdbcTemplate.queryForObject("SELECT title FROM assignments WHERE id = ?", String.class,
                created.get("assignmentId"))).isEqualTo("알고리즘 풀이");
    }

    @Test
    @DisplayName("간격만 바꾸고 다음 회차 마감을 보내지 않으면 거부한다")
    void rejectIntervalWithoutNextCloseAt() {
        Map<String, Object> body = createBody();
        body.put("submissionTarget", "MEMBERS_ONLY");
        Map<String, Object> created = createRecurrence(body);

        givenLeader().contentType(ContentType.JSON).body(Map.of("intervalWeeks", 2))
                .when().patch(DETAIL_PATH, study.getId(), created.get("recurrenceId"))
                .then().statusCode(400)
                .body("code", equalTo("INVALID_CLOSE_AT"));
    }

    @Test
    // 회차의 recurrence_id를 NULL로 바꾸는 FK 동작은 H2 스키마에 없어 AssignmentRecurrencePostgresMigrationTest에서 검증한다.
    @DisplayName("반복 과제를 삭제해도 생성된 회차는 남는다")
    void deleteRecurrenceKeepsOccurrences() {
        Map<String, Object> body = createBody();
        body.put("submissionTarget", "MEMBERS_ONLY");
        Map<String, Object> created = createRecurrence(body);

        givenLeader().when().delete(DETAIL_PATH, study.getId(), created.get("recurrenceId"))
                .then().statusCode(204);

        givenLeader().when().get(COLLECTION_PATH, study.getId())
                .then().statusCode(200)
                .body("recurrences", empty());
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM assignments WHERE id = ?", Integer.class,
                created.get("assignmentId"))).isOne();
    }

    @Test
    @DisplayName("없는 반복 과제는 수정하거나 삭제할 수 없다")
    void notFoundRecurrence() {
        givenLeader().contentType(ContentType.JSON).body(Map.of("title", "새 제목"))
                .when().patch(DETAIL_PATH, study.getId(), 999)
                .then().statusCode(404)
                .body("code", equalTo("ASSIGNMENT_RECURRENCE_NOT_FOUND"));
        givenLeader().when().delete(DETAIL_PATH, study.getId(), 999)
                .then().statusCode(404)
                .body("code", equalTo("ASSIGNMENT_RECURRENCE_NOT_FOUND"));
    }

    private Map<String, Object> createRecurrence(Map<String, Object> body) {
        return givenLeader().contentType(ContentType.JSON).body(body)
                .when().post(COLLECTION_PATH, study.getId())
                .then().statusCode(201)
                .extract().jsonPath().getMap("$");
    }

    private Map<String, Object> createBody() {
        Map<String, Object> body = new HashMap<>();
        body.put("title", "알고리즘 풀이");
        body.put("content", "이번 주 문제를 풀어요.");
        body.put("submissionMethod", "링크 제출");
        body.put("submissionVisibility", "LEADER_ONLY");
        body.put("closeAt", format(closeAt));
        body.put("intervalWeeks", 1);
        return body;
    }

    private io.restassured.specification.RequestSpecification givenLeader() {
        return testAuthRequest.givenAuthenticatedUser(leaderUser.getId()).port(port);
    }

    private static String format(LocalDateTime dateTime) {
        return dateTime.format(FORMATTER);
    }
}
