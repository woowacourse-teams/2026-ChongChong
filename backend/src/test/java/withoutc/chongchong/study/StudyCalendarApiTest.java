package withoutc.chongchong.study;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import withoutc.chongchong.assignment.entity.Assignment;
import withoutc.chongchong.assignment.entity.AssignmentSubmission;
import withoutc.chongchong.assignment.entity.SubmissionTarget;
import withoutc.chongchong.assignment.entity.SubmissionVisibility;
import withoutc.chongchong.assignment.repository.AssignmentRepository;
import withoutc.chongchong.assignment.repository.AssignmentSubmissionRepository;
import withoutc.chongchong.auth.support.TestAuthRequest;
import withoutc.chongchong.notice.entity.Notice;
import withoutc.chongchong.notice.repository.NoticeRepository;
import withoutc.chongchong.study.entity.Study;
import withoutc.chongchong.study.entity.StudyMember;
import withoutc.chongchong.study.entity.StudyMemberRole;
import withoutc.chongchong.study.repository.StudyMemberRepository;
import withoutc.chongchong.study.repository.StudyRepository;
import withoutc.chongchong.support.PostgresContainerTest;
import withoutc.chongchong.support.TestDatabaseCleaner;
import withoutc.chongchong.support.TestS3PresignerConfiguration;
import withoutc.chongchong.user.entity.User;
import withoutc.chongchong.user.repository.UserRepository;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import({TestS3PresignerConfiguration.class, StudyCalendarApiTest.FixedClockConfiguration.class})
class StudyCalendarApiTest extends PostgresContainerTest {

    private static final LocalDateTime START = LocalDateTime.of(2026, 11, 1, 0, 0);
    private static final LocalDateTime END = LocalDateTime.of(2026, 12, 1, 0, 0);

    @Autowired
    private UserRepository users;
    @Autowired
    private StudyRepository studies;
    @Autowired
    private StudyMemberRepository members;
    @Autowired
    private NoticeRepository notices;
    @Autowired
    private AssignmentRepository assignments;
    @Autowired
    private AssignmentSubmissionRepository submissions;
    @Autowired
    private TestAuthRequest auth;
    @Autowired
    private TestDatabaseCleaner cleaner;
    @Autowired
    private JdbcTemplate jdbc;

    @LocalServerPort
    private int port;

    private User leaderUser;
    private User memberUser;
    private Study study;
    private StudyMember leader;
    private StudyMember member;
    private StudyMember other;

    @BeforeEach
    void setUp() {
        cleaner.clean();
        leaderUser = users.saveAndFlush(User.create("리더", null));
        memberUser = users.saveAndFlush(User.create("조회 사용자", null));
        User otherUser = users.saveAndFlush(User.create("다른 사용자", null));
        study = studies.saveAndFlush(Study.create("캘린더 스터디", "설명", false));
        leader = saveMember(leaderUser, StudyMemberRole.LEADER);
        member = saveMember(memberUser, StudyMemberRole.MEMBER);
        other = saveMember(otherUser, StudyMemberRole.MEMBER);
    }

    @AfterEach
    void clean() {
        cleaner.clean();
    }

    @Test
    @DisplayName("가입 전 공지 전체와 본인 지정 과제만 조회하며 리더도 동일한 지정 조건을 적용한다")
    void returnsAllNoticesAndOnlyAssignedPersonalAssignments() {
        Notice old = saveNotice("가입 전 공지", START.plusHours(9), List.of(other));
        Notice unread = saveNotice("미읽음 공지", START.plusDays(1).plusHours(9), List.of(member, other));
        Notice read = saveNotice("읽은 공지", START.plusDays(2).plusHours(9), List.of(member));
        request(memberUser).patch(noticePath(read) + "/read").then().statusCode(200);
        Assignment assigned = saveAssignment("지정 과제", START.plusDays(1).plusHours(10),
                START.plusDays(8).plusHours(23), List.of(member, other));
        saveAssignment("다른 멤버 과제", START.plusDays(1), START.plusDays(9), List.of(other));
        Study anotherStudy = studies.saveAndFlush(Study.create("다른 스터디", "설명", false));
        notices.saveAndFlush(Notice.create(anotherStudy, "다른 스터디 공지", "내용"));

        Response response = calendar(memberUser, 2026, 11);

        assertThat(response.jsonPath().getList("events.eventType", String.class)).containsExactly(
                "NOTICE_CREATED", "NOTICE_CREATED", "ASSIGNMENT_CREATED", "NOTICE_CREATED", "ASSIGNMENT_DUE");
        assertThat(event(response, "NOTICE_CREATED", old.getId())).containsEntry("readStatus", "NOT_ASSIGNED")
                .doesNotContainKeys("submissionStatus");
        assertThat(event(response, "NOTICE_CREATED", unread.getId())).containsEntry("readStatus", "UNREAD");
        assertThat(event(response, "NOTICE_CREATED", read.getId())).containsEntry("readStatus", "READ");
        assertThat(event(response, "ASSIGNMENT_CREATED", assigned.getId()))
                .containsEntry("submissionStatus", "NOT_SUBMITTED").doesNotContainKeys("readStatus");
        assertThat(event(response, "ASSIGNMENT_DUE", assigned.getId()))
                .containsEntry("submissionStatus", "NOT_SUBMITTED");
        assertThat(events(calendar(leaderUser, 2026, 11))).hasSize(3)
                .allSatisfy(item -> assertThat(item).containsEntry("resourceType", "NOTICE"));
    }

    @Test
    @DisplayName("완료 과제도 생성·마감을 유지하고 같은 현재 제출 상태로 지각·마감 후 미제출을 구분한다")
    void returnsCompletedLateAndMissingAssignments() {
        Assignment submitted = saveAssignment("제출 완료", START, START.plusDays(2), List.of(member));
        submit(submitted, START.plusDays(1));
        Assignment late = saveAssignment("지각 제출", START, START.plusDays(1), List.of(member));
        submit(late, START.plusDays(2));
        Assignment missing = saveAssignment("미제출", START, START.plusDays(3), List.of(member));

        Response response = calendar(memberUser, 2026, 11);

        assertThat(events(response)).hasSize(6);
        assertAssignmentState(response, submitted.getId(), "SUBMITTED");
        assertAssignmentState(response, late.getId(), "LATE_SUBMITTED");
        assertAssignmentState(response, missing.getId(), "MISSING");
    }

    @Test
    @DisplayName("월 시작을 포함하고 다음 달 시작을 제외하며 월말 소수 초와 이전 월 생성 과제의 마감을 반환한다")
    void respectsMonthlyBoundariesOverHttp() {
        Notice first = saveNotice("월 시작", START, List.of());
        Notice last = saveNotice("월 마지막", END.minusNanos(1000), List.of());
        saveNotice("이전 달", START.minusNanos(1000), List.of());
        saveNotice("다음 달", END, List.of());
        Assignment due = saveAssignment("지난달 생성 과제", START.minusDays(1), START.plusHours(1), List.of(member));

        Response response = calendar(memberUser, 2026, 11);

        assertThat(events(response)).hasSize(3);
        assertThat(event(response, "NOTICE_CREATED", first.getId())).containsEntry("eventAt", "2026-11-01T00:00:00");
        assertThat(event(response, "NOTICE_CREATED", last.getId())).containsEntry("eventAt", "2026-11-30T23:59:59.999999");
        assertThat(event(response, "ASSIGNMENT_DUE", due.getId())).containsEntry("eventAt", "2026-11-01T01:00:00");
    }

    @Test
    @DisplayName("같은 시각의 공지·과제 생성·마감은 종류와 원본 ID 순서로 직렬화한다")
    void ordersSameTimeEventsWithoutCollapsingCreationAndDeadline() {
        LocalDateTime at = START.plusDays(5);
        Notice first = saveNotice("첫 공지", at, List.of());
        Notice second = saveNotice("둘째 공지", at, List.of());
        Assignment assignment = saveAssignment("같은 시각 생성·마감", at, at, List.of(member));

        Response response = calendar(memberUser, 2026, 11);

        assertThat(response.jsonPath().getList("events.eventType", String.class))
                .containsExactly("NOTICE_CREATED", "NOTICE_CREATED", "ASSIGNMENT_CREATED", "ASSIGNMENT_DUE");
        assertThat(response.jsonPath().getList("events.resourceId", Long.class))
                .containsExactly(first.getId(), second.getId(), assignment.getId(), assignment.getId());
    }

    @Test
    @DisplayName("대상 제외 시 보존된 제출물은 캘린더에서 빠지고 재지정하면 같은 제출 기록으로 다시 표시된다")
    void reflectsAssignmentExclusionAndReassignmentThroughApi() {
        Assignment assignment = saveAssignment("제출한 과제", START, START.plusDays(2), List.of(member, other));
        submit(assignment, START.plusDays(1));
        Long submissionId = submissions.findByAssignmentIdAndMemberId(assignment.getId(), member.getId())
                .orElseThrow().getId();
        assertThat(events(calendar(memberUser, 2026, 11))).hasSize(2);

        patchAssignment(assignment, Map.of("submitterIds", List.of(other.getId())));

        assertThat(events(calendar(memberUser, 2026, 11))).isEmpty();
        assertThat(submissions.findById(submissionId).orElseThrow().isAssigned()).isFalse();

        patchAssignment(assignment, Map.of("submitterIds", List.of(member.getId(), other.getId())));

        Response response = calendar(memberUser, 2026, 11);
        assertThat(events(response)).hasSize(2);
        assertAssignmentState(response, assignment.getId(), "SUBMITTED");
        assertThat(submissions.findByAssignmentIdAndMemberId(assignment.getId(), member.getId()).orElseThrow().getId())
                .isEqualTo(submissionId);
    }

    @Test
    @DisplayName("원본 제목·마감 월 수정과 삭제 API의 결과를 다음 캘린더 조회에 반영한다")
    void reflectsOriginalUpdatesAndDeletionThroughApi() {
        Notice notice = saveNotice("원본 공지", START, List.of());
        Assignment assignment = saveAssignment("원본 과제", START.plusDays(1), START.plusDays(8), List.of(member));
        patchAssignment(assignment, Map.of("title", "수정한 과제", "closeAt", "2026-12-02T00:00:00"));

        Response current = calendar(memberUser, 2026, 11);
        assertThat(events(current)).hasSize(2);
        assertThat(event(current, "ASSIGNMENT_CREATED", assignment.getId())).containsEntry("title", "수정한 과제");
        Response next = calendar(memberUser, 2026, 12);
        assertThat(events(next)).hasSize(1);
        assertThat(event(next, "ASSIGNMENT_DUE", assignment.getId()))
                .containsEntry("eventAt", "2026-12-02T00:00:00").containsEntry("title", "수정한 과제");

        request(leaderUser).delete(assignmentPath(assignment)).then().statusCode(204);
        request(leaderUser).delete(noticePath(notice)).then().statusCode(204);

        assertThat(events(calendar(memberUser, 2026, 11))).isEmpty();
        assertThat(events(calendar(memberUser, 2026, 12))).isEmpty();
    }

    @Test
    @DisplayName("기본 페이지 크기를 넘는 월간 이벤트도 한 응답에 모두 반환한다")
    void returnsAllMonthlyEvents() {
        for (int index = 0; index < 12; index++) {
            saveNotice("공지 " + index, START.plusDays(index), List.of());
            saveAssignment("과제 " + index, START.plusDays(index), START.plusDays(index + 1), List.of(member));
        }

        assertThat(events(calendar(memberUser, 2026, 11))).hasSize(36);
    }

    @ParameterizedTest
    @CsvSource({"1,1", "9998,12", "2024,2", "2026,11"})
    @DisplayName("지원 연도 경계·윤년·빈 월도 연·월과 빈 배열을 반환한다")
    void returnsEmptyCalendar(int year, int month) {
        assertThat(events(calendar(memberUser, year, month))).isEmpty();
    }

    @ParameterizedTest
    @CsvSource({"0,11,year", "-1,11,year", "9999,11,year", "2026,0,month", "2026,-1,month", "2026,13,month"})
    @DisplayName("연·월 범위 위반은 필드 오류를 포함한 INVALID_REQUEST_PARAMETER를 반환한다")
    void rejectsOutOfRangeParameters(int year, int month, String field) {
        request(memberUser).queryParam("year", year).queryParam("month", month).get(calendarPath(study.getId()))
                .then().statusCode(400).body("code", equalTo("INVALID_REQUEST_PARAMETER"))
                .body("errors.field", hasItem(field));
    }

    @ParameterizedTest
    @MethodSource("invalidQueries")
    @DisplayName("필수 쿼리 누락·정수 변환 실패는 INVALID_REQUEST를 반환한다")
    void rejectsMissingOrMalformedParameters(Map<String, String> query) {
        request(memberUser).queryParams(query).get(calendarPath(study.getId()))
                .then().statusCode(400).body("code", equalTo("INVALID_REQUEST"));
    }

    @ParameterizedTest
    @ValueSource(longs = {0L, -1L})
    @DisplayName("양수가 아닌 스터디 ID는 파라미터 검증 오류를 반환한다")
    void rejectsInvalidStudyId(long studyId) {
        request(memberUser).queryParam("year", 2026).queryParam("month", 11).get(calendarPath(studyId))
                .then().statusCode(400).body("code", equalTo("INVALID_REQUEST_PARAMETER"))
                .body("errors.field", hasItem("studyId"));
    }

    @Test
    @DisplayName("인증 정보가 없는 요청은 AUTHENTICATION_REQUIRED를 반환한다")
    void rejectsUnauthenticatedUser() {
        given().port(port).basePath("/api").queryParam("year", 2026).queryParam("month", 11)
                .get(calendarPath(study.getId())).then().statusCode(401)
                .body("code", equalTo("AUTHENTICATION_REQUIRED"));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @DisplayName("공개 여부와 관계없이 해당 스터디 비멤버에게는 접근 거부를 반환한다")
    void rejectsNonMemberOfPublicOrPrivateStudy(boolean isPublic) {
        Study anotherStudy = studies.saveAndFlush(Study.create("비멤버 스터디", "설명", isPublic));

        request(memberUser).queryParam("year", 2026).queryParam("month", 11).get(calendarPath(anotherStudy.getId()))
                .then().statusCode(403).body("code", equalTo("STUDY_ACCESS_DENIED"));
    }

    @Test
    @DisplayName("탈퇴·방출로 현재 멤버십이 제거된 사용자에게는 접근 거부를 반환한다")
    void rejectsRemovedMember() {
        members.deleteById(member.getId());
        members.flush();

        request(memberUser).queryParam("year", 2026).queryParam("month", 11).get(calendarPath(study.getId()))
                .then().statusCode(403).body("code", equalTo("STUDY_ACCESS_DENIED"));
    }

    @Test
    @DisplayName("존재하지 않는 스터디는 STUDY_NOT_FOUND를 반환한다")
    void rejectsMissingStudy() {
        request(memberUser).queryParam("year", 2026).queryParam("month", 11).get(calendarPath(Long.MAX_VALUE))
                .then().statusCode(404).body("code", equalTo("STUDY_NOT_FOUND"));
    }

    private RequestSpecification request(User user) {
        return auth.givenAuthenticatedUser(user.getId()).port(port);
    }

    private Response calendar(User user, int year, int month) {
        return request(user).queryParam("year", year).queryParam("month", month).get(calendarPath(study.getId()))
                .then().statusCode(200).body("year", equalTo(year)).body("month", equalTo(month))
                .extract().response();
    }

    private List<Map<String, Object>> events(Response response) {
        return response.jsonPath().getList("events");
    }

    private Map<String, Object> event(Response response, String type, Long id) {
        return events(response).stream()
                .filter(item -> type.equals(item.get("eventType"))
                        && ((Number) item.get("resourceId")).longValue() == id)
                .findFirst().orElseThrow();
    }

    private void assertAssignmentState(Response response, Long id, String status) {
        assertThat(event(response, "ASSIGNMENT_CREATED", id)).containsEntry("submissionStatus", status);
        assertThat(event(response, "ASSIGNMENT_DUE", id)).containsEntry("submissionStatus", status);
    }

    private void patchAssignment(Assignment assignment, Map<String, Object> body) {
        request(leaderUser).contentType(ContentType.JSON).body(body).patch(assignmentPath(assignment))
                .then().statusCode(204);
    }

    private String calendarPath(Long studyId) {
        return "/studies/" + studyId + "/calendar";
    }

    private String assignmentPath(Assignment assignment) {
        return "/studies/" + study.getId() + "/assignments/" + assignment.getId();
    }

    private String noticePath(Notice notice) {
        return "/studies/" + study.getId() + "/notices/" + notice.getId();
    }

    private StudyMember saveMember(User user, StudyMemberRole role) {
        return members.saveAndFlush(StudyMember.create(study, user, user.getName(), null, role));
    }

    private Notice saveNotice(String title, LocalDateTime createdAt, List<StudyMember> recipients) {
        Notice notice = Notice.create(study, title, "내용");
        notice.addRecipients(recipients);
        notices.saveAndFlush(notice);
        jdbc.update("UPDATE notices SET created_at = ? WHERE id = ?", createdAt, notice.getId());
        return notice;
    }

    private Assignment saveAssignment(String title, LocalDateTime createdAt, LocalDateTime closeAt,
                                      List<StudyMember> submitters) {
        Assignment assignment = Assignment.create(study, title, "내용", "링크 제출", SubmissionTarget.MEMBERS_ONLY,
                SubmissionVisibility.LEADER_ONLY, closeAt, createdAt.minusDays(1));
        assignment.replaceSubmitters(submitters);
        assignments.saveAndFlush(assignment);
        jdbc.update("UPDATE assignments SET created_at = ? WHERE id = ?", createdAt, assignment.getId());
        return assignment;
    }

    private void submit(Assignment assignment, LocalDateTime submittedAt) {
        AssignmentSubmission submission = submissions.findByAssignmentIdAndMemberId(assignment.getId(), member.getId())
                .orElseThrow();
        submission.submit("제출 내용", null, submittedAt);
        submissions.saveAndFlush(submission);
    }

    private static Stream<Arguments> invalidQueries() {
        return Stream.of(
                Arguments.of(Map.of("month", "11")), Arguments.of(Map.of("year", "2026")),
                Arguments.of(Map.of()), Arguments.of(Map.of("year", "abc", "month", "11")),
                Arguments.of(Map.of("year", "2026.5", "month", "11")),
                Arguments.of(Map.of("year", "2026", "month", "abc")),
                Arguments.of(Map.of("year", "2026", "month", "1.5")),
                Arguments.of(Map.of("year", "2147483648", "month", "11")));
    }

    @TestConfiguration
    static class FixedClockConfiguration {

        @Bean
        @Primary
        Clock calendarTestClock() {
            return Clock.fixed(Instant.parse("2026-11-05T01:00:00Z"), ZoneId.of("Asia/Seoul"));
        }
    }
}
