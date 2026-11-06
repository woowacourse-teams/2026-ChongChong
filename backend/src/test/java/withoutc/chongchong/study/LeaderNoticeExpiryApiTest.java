package withoutc.chongchong.study;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import withoutc.chongchong.auth.support.TestAuthRequest;
import withoutc.chongchong.notice.entity.Notice;
import withoutc.chongchong.notice.repository.NoticeRepository;
import withoutc.chongchong.study.entity.Study;
import withoutc.chongchong.study.entity.StudyMember;
import withoutc.chongchong.study.entity.StudyMemberRole;
import withoutc.chongchong.study.repository.StudyMemberRepository;
import withoutc.chongchong.study.repository.StudyRepository;
import withoutc.chongchong.support.TestDatabaseCleaner;
import withoutc.chongchong.support.TestS3PresignerConfiguration;
import withoutc.chongchong.user.entity.User;
import withoutc.chongchong.user.repository.UserRepository;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import({TestS3PresignerConfiguration.class, LeaderNoticeExpiryApiTest.FixedClockConfiguration.class})
class LeaderNoticeExpiryApiTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 6, 10, 0);

    @Autowired
    private UserRepository users;
    @Autowired
    private StudyRepository studies;
    @Autowired
    private StudyMemberRepository members;
    @Autowired
    private NoticeRepository notices;
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
    private StudyMember member;

    @BeforeEach
    void setUp() {
        cleaner.clean();
        leaderUser = users.saveAndFlush(User.create("리더", null));
        memberUser = users.saveAndFlush(User.create("멤버", null));
        study = studies.saveAndFlush(Study.create("스터디", "설명", false));
        members.saveAndFlush(StudyMember.create(study, leaderUser, "리더", null, StudyMemberRole.LEADER));
        member = members.saveAndFlush(StudyMember.create(study, memberUser, "멤버", null, StudyMemberRole.MEMBER));
    }

    @AfterEach
    void tearDown() {
        cleaner.clean();
    }

    @ParameterizedTest
    @CsvSource({"1, 1", "0, 0", "-1, 0"})
    @DisplayName("정확히 7일부터 리더 요약과 개수에서 제외하고 멤버와 전체 조회는 유지한다")
    void expiresAtSevenDays(long secondsAfterCutoff, int expectedCount) {
        Notice notice = createNotice(NOW.minusDays(7).plusSeconds(secondsAfterCutoff), false);
        assertLeaderCount(expectedCount);
        request(memberUser).get("/studies/{id}", study.getId()).then().statusCode(200)
                .body("notices.items", hasSize(1));
        request(memberUser).get("/studies/me").then().statusCode(200)
                .body("studies[0].noticeCount", equalTo(1));
        for (User user : List.of(leaderUser, memberUser)) {
            request(user).get("/studies/{id}/notices", study.getId()).then().statusCode(200)
                    .body("notices", hasSize(1));
            request(user).get("/studies/{id}/notices/{noticeId}", study.getId(), notice.getId())
                    .then().statusCode(200).body("title", equalTo("공지"));
        }
    }

    @Test
    @DisplayName("기간 내 공지도 모두 읽었다면 리더의 미확인 공지에 포함하지 않는다")
    void excludesReadNoticesWithinSevenDays() {
        Notice unread = createNotice(NOW.minusDays(1), false);
        createNotice(NOW.minusDays(2), true);
        createNotice(NOW.minusDays(8), false);
        assertLeaderCount(1);
        request(leaderUser).get("/studies/{id}", study.getId()).then().statusCode(200)
                .body("notices.items[0].id", equalTo(unread.getId().intValue()));
    }

    @Test
    @DisplayName("만료 공지 수정과 재알림 예약은 노출 기간을 연장하지 않는다")
    void updateDoesNotExtendVisibility() {
        Notice notice = createNotice(NOW.minusDays(8), false);
        String path = "/studies/" + study.getId() + "/notices/" + notice.getId();
        request(leaderUser).contentType(ContentType.JSON)
                .body(Map.of("title", "수정한 공지", "content", "수정 내용",
                        "remindAts", List.of(NOW.plusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))))
                .patch(path).then().statusCode(204);
        assertLeaderCount(0);
        request(leaderUser).get(path).then().statusCode(200).body("title", equalTo("수정한 공지"));
        assertThat(notices.findById(notice.getId()).orElseThrow().getCreatedAt()).isEqualTo(NOW.minusDays(8));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM notice_recipients WHERE notice_id = ?",
                Long.class, notice.getId())).isEqualTo(1L);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM notice_reminders WHERE notice_id = ?",
                Long.class, notice.getId())).isEqualTo(1L);
    }

    private void assertLeaderCount(int expectedCount) {
        request(leaderUser).get("/studies/{id}", study.getId()).then().statusCode(200)
                .body("notices.count", equalTo(expectedCount)).body("notices.items", hasSize(expectedCount));
        request(leaderUser).get("/studies/me").then().statusCode(200)
                .body("studies[0].noticeCount", equalTo(expectedCount));
    }

    private Notice createNotice(LocalDateTime createdAt, boolean read) {
        Notice notice = Notice.create(study, "공지", "내용");
        notice.addRecipients(List.of(member));
        if (read) {
            notice.getRecipients().getFirst().markAsRead(NOW);
        }
        notices.saveAndFlush(notice);
        jdbc.update("UPDATE notices SET created_at = ? WHERE id = ?", createdAt, notice.getId());
        return notice;
    }

    private RequestSpecification request(User user) {
        return auth.givenAuthenticatedUser(user.getId()).port(port);
    }

    @TestConfiguration
    static class FixedClockConfiguration {

        @Bean
        @Primary
        Clock noticeExpiryClock() {
            return Clock.fixed(Instant.parse("2026-11-06T01:00:00Z"), ZoneId.of("Asia/Seoul"));
        }
    }
}
