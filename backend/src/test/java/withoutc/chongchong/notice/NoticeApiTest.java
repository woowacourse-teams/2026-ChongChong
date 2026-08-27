package withoutc.chongchong.notice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static withoutc.chongchong.global.config.ApiPathConfig.API_PREFIX;

import io.restassured.http.ContentType;
import java.time.LocalDateTime;
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
import withoutc.chongchong.auth.support.TestAuthRequest;
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

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class NoticeApiTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StudyRepository studyRepository;

    @Autowired
    private StudyMemberRepository studyMemberRepository;

    @Autowired
    private NoticeRepository noticeRepository;

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
    private Notice notice;
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
        remindAt = LocalDateTime.now().plusDays(30).truncatedTo(ChronoUnit.SECONDS);
        notice = Notice.create(leader, "기존 공지", "기존 공지 내용");
        notice.addRecipients(List.of(member, secondMember));
        notice.addReminders(List.of(remindAt), LocalDateTime.now());
        noticeRepository.saveAndFlush(notice);
    }

    @AfterEach
    void cleanDatabase() {
        databaseCleaner.clean();
    }

    @Test
    @DisplayName("공지 생성 요청을 보내면 201과 공지 id를 반환하고 리더를 제외한 수신자를 저장한다")
    void createNoticeTest() {
        LocalDateTime newRemindAt = remindAt.plusDays(1);

        Long createdNoticeId = testAuthRequest.givenAuthenticatedUser(leaderUser.getId())
                .port(port)
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "title": "새 공지",
                          "content": "새 공지 내용",
                          "remindAts": ["%s"]
                        }
                        """.formatted(newRemindAt))
                .when()
                .post("/studies/{studyId}/notices", study.getId())
                .then()
                .statusCode(201)
                .body("noticeId", notNullValue())
                .extract()
                .jsonPath()
                .getLong("noticeId");

        Integer recipientCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM notice_recipients WHERE notice_id = ?",
                Integer.class,
                createdNoticeId
        );
        Integer reminderCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM notice_reminders WHERE notice_id = ?",
                Integer.class,
                createdNoticeId
        );
        assertThat(recipientCount).isEqualTo(2);
        assertThat(reminderCount).isEqualTo(1);
    }

    @Test
    @DisplayName("리마인드 시각을 생략해도 공지를 생성하고 목록에서 예정 시각을 제외한다")
    void createNoticeWithoutRemindAtTest() {
        Long createdNoticeId = testAuthRequest.givenAuthenticatedUser(leaderUser.getId())
                .port(port)
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "title": "리마인드 없는 공지",
                          "content": "공지 내용"
                        }
                        """)
                .when()
                .post("/studies/{studyId}/notices", study.getId())
                .then()
                .statusCode(201)
                .extract()
                .jsonPath()
                .getLong("noticeId");

        assertThat(countRows("notice_reminders", createdNoticeId)).isZero();
        testAuthRequest.givenAuthenticatedUser(leaderUser.getId())
                .port(port)
                .when()
                .get("/studies/{studyId}/notices", study.getId())
                .then()
                .statusCode(200)
                .body("notices[0].id", equalTo(createdNoticeId.intValue()))
                .body("notices[0]", not(hasKey("remindAt")));
    }

    @Test
    @DisplayName("리더가 공지 목록을 조회하면 수신 인원과 다음 리마인드 예정 시각을 반환한다")
    void getNoticesTest() {
        testAuthRequest.givenAuthenticatedUser(leaderUser.getId())
                .port(port)
                .when()
                .get("/studies/{studyId}/notices", study.getId())
                .then()
                .statusCode(200)
                .body("hasNext", equalTo(false))
                .body("notices[0].id", equalTo(notice.getId().intValue()))
                .body("notices[0].recipientCount", equalTo(2))
                .body("notices[0].readRecipientCount", equalTo(0))
                .body("notices[0].remindAt", equalTo(remindAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)))
                .body("notices[0].isComplete", equalTo(false));
    }

    @Test
    @DisplayName("스터디원이 공지 목록을 조회하면 읽음 여부만 반환한다")
    void getNoticesByMemberTest() {
        testAuthRequest.givenAuthenticatedUser(memberUser.getId())
                .port(port)
                .when()
                .get("/studies/{studyId}/notices", study.getId())
                .then()
                .statusCode(200)
                .body("notices[0].id", equalTo(notice.getId().intValue()))
                .body("notices[0].isComplete", equalTo(false))
                .body("notices[0]", not(hasKey("recipientCount")))
                .body("notices[0]", not(hasKey("readRecipientCount")))
                .body("notices[0]", not(hasKey("remindAt")));
    }

    @Test
    @DisplayName("공지 목록을 size만큼 조회하면 다음 조회 기준인 cursor를 반환한다")
    void getNoticesWithCursorTest() {
        Notice middleNotice = noticeRepository.save(
                Notice.create(leader, "두 번째 공지", "두 번째 공지 내용")
        );
        Notice latestNotice = noticeRepository.saveAndFlush(
                Notice.create(leader, "세 번째 공지", "세 번째 공지 내용")
        );

        testAuthRequest.givenAuthenticatedUser(leaderUser.getId())
                .port(port)
                .queryParam("size", 2)
                .when()
                .get("/studies/{studyId}/notices", study.getId())
                .then()
                .statusCode(200)
                .body("hasNext", equalTo(true))
                .body("nextCursor", equalTo(middleNotice.getId().intValue()))
                .body("notices", hasSize(2))
                .body("notices[0].id", equalTo(latestNotice.getId().intValue()))
                .body("notices[1].id", equalTo(middleNotice.getId().intValue()));

        testAuthRequest.givenAuthenticatedUser(leaderUser.getId())
                .port(port)
                .queryParam("cursor", middleNotice.getId())
                .queryParam("size", 2)
                .when()
                .get("/studies/{studyId}/notices", study.getId())
                .then()
                .statusCode(200)
                .body("hasNext", equalTo(false))
                .body("nextCursor", nullValue())
                .body("notices", hasSize(1))
                .body("notices[0].id", equalTo(notice.getId().intValue()));
    }

    @Test
    @DisplayName("스터디원이 공지 상세 조회 요청을 보내면 공지 내용을 반환한다")
    void getNoticeDetailTest() {
        testAuthRequest.givenAuthenticatedUser(memberUser.getId())
                .port(port)
                .when()
                .get("/studies/{studyId}/notices/{noticeId}", study.getId(), notice.getId())
                .then()
                .statusCode(200)
                .body("id", equalTo(notice.getId().intValue()))
                .body("title", equalTo("기존 공지"))
                .body("writer", equalTo("리더"))
                .body("profileImageUrl", equalTo("https://example.com/leader.png"))
                .body("content", equalTo("기존 공지 내용"))
                .body("createdAt", notNullValue());
    }

    @Test
    @DisplayName("모든 수신자가 공지를 읽으면 리더와 스터디원에게 완료 상태를 반환한다")
    void getCompletedNoticeListTest() {
        jdbcTemplate.update(
                "UPDATE notice_recipients SET read_at = ? WHERE notice_id = ?",
                LocalDateTime.now(),
                notice.getId()
        );

        testAuthRequest.givenAuthenticatedUser(leaderUser.getId())
                .port(port)
                .when()
                .get("/studies/{studyId}/notices", study.getId())
                .then()
                .statusCode(200)
                .body("notices[0].recipientCount", equalTo(2))
                .body("notices[0].readRecipientCount", equalTo(2))
                .body("notices[0].isComplete", equalTo(true));

        testAuthRequest.givenAuthenticatedUser(memberUser.getId())
                .port(port)
                .when()
                .get("/studies/{studyId}/notices", study.getId())
                .then()
                .statusCode(200)
                .body("notices[0].isComplete", equalTo(true));
    }

    @Test
    @DisplayName("존재하지 않는 공지 상세 조회 요청은 비즈니스 오류 응답을 반환한다")
    void getMissingNoticeDetailTest() {
        testAuthRequest.givenAuthenticatedUser(memberUser.getId())
                .port(port)
                .when()
                .get("/studies/{studyId}/notices/{noticeId}", study.getId(), Long.MAX_VALUE)
                .then()
                .statusCode(404)
                .body("code", equalTo("NOTICE_NOT_FOUND"))
                .body("message", equalTo("존재하지 않는 공지입니다."));
    }

    @Test
    @DisplayName("다른 스터디의 공지 상세 조회 요청은 존재하지 않는 공지로 응답한다")
    void getNoticeDetailFromOtherStudyTest() {
        Study otherStudy = studyRepository.save(Study.create("다른 스터디", "설명"));
        StudyMember otherLeader = studyMemberRepository.save(
                StudyMember.create(otherStudy, leaderUser, "리더", null, StudyMemberRole.LEADER)
        );
        Notice otherNotice = noticeRepository.saveAndFlush(
                Notice.create(otherLeader, "다른 공지", "다른 공지 내용")
        );

        testAuthRequest.givenAuthenticatedUser(memberUser.getId())
                .port(port)
                .when()
                .get("/studies/{studyId}/notices/{noticeId}", study.getId(), otherNotice.getId())
                .then()
                .statusCode(404)
                .body("code", equalTo("NOTICE_NOT_FOUND"))
                .body("message", equalTo("존재하지 않는 공지입니다."));
    }

    @Test
    @DisplayName("리더가 공지 수정 요청을 보내면 공지와 대기 리마인더를 변경한다")
    void updateNoticeTest() {
        LocalDateTime newRemindAt = remindAt.plusDays(1);

        testAuthRequest.givenAuthenticatedUser(leaderUser.getId())
                .port(port)
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "title": "수정 공지",
                          "content": "수정 공지 내용",
                          "remindAts": ["%s"]
                        }
                        """.formatted(newRemindAt))
                .when()
                .patch("/studies/{studyId}/notices/{noticeId}", study.getId(), notice.getId())
                .then()
                .statusCode(204);

        String title = jdbcTemplate.queryForObject(
                "SELECT title FROM notices WHERE id = ?",
                String.class,
                notice.getId()
        );
        LocalDateTime savedRemindAt = jdbcTemplate.queryForObject(
                "SELECT remind_at FROM notice_reminders WHERE notice_id = ? AND status = 'PENDING'",
                LocalDateTime.class,
                notice.getId()
        );
        assertThat(title).isEqualTo("수정 공지");
        assertThat(savedRemindAt).isEqualTo(newRemindAt);
    }

    @Test
    @DisplayName("모든 수정 필드를 생략하면 기존 공지 상태를 유지한다")
    void updateNoticeWithoutFieldsTest() {
        testAuthRequest.givenAuthenticatedUser(leaderUser.getId())
                .port(port)
                .contentType(ContentType.JSON)
                .body("{}")
                .when()
                .patch("/studies/{studyId}/notices/{noticeId}", study.getId(), notice.getId())
                .then()
                .statusCode(204);

        String title = jdbcTemplate.queryForObject(
                "SELECT title FROM notices WHERE id = ?",
                String.class,
                notice.getId()
        );
        assertThat(title).isEqualTo("기존 공지");
        assertThat(countRows("notice_reminders", notice.getId())).isEqualTo(1);
    }

    @Test
    @DisplayName("리더가 공지 삭제 요청을 보내면 공지 애그리거트가 함께 삭제된다")
    void deleteNoticeTest() {
        testAuthRequest.givenAuthenticatedUser(leaderUser.getId())
                .port(port)
                .when()
                .delete("/studies/{studyId}/notices/{noticeId}", study.getId(), notice.getId())
                .then()
                .statusCode(204);

        assertThat(countRows("notices", notice.getId())).isZero();
        assertThat(countRows("notice_recipients", notice.getId())).isZero();
        assertThat(countRows("notice_reminders", notice.getId())).isZero();
    }

    @Test
    @DisplayName("공지 생성 요청의 제목이 공백이면 검증 오류 응답을 반환한다")
    void createNoticeWithBlankTitleTest() {
        testAuthRequest.givenAuthenticatedUser(leaderUser.getId())
                .port(port)
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "title": " ",
                          "content": "공지 내용",
                          "remindAts": []
                        }
                        """)
                .when()
                .post("/studies/{studyId}/notices", study.getId())
                .then()
                .statusCode(400)
                .body("code", equalTo("INVALID_INPUT_VALUE"))
                .body("errors.field", hasItem("title"));
    }

    @Test
    @DisplayName("공지 생성 요청의 리마인드 시각이 미래가 아니면 검증 오류 응답을 반환한다")
    void createNoticeWithPastRemindAtTest() {
        testAuthRequest.givenAuthenticatedUser(leaderUser.getId())
                .port(port)
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "title": "공지 제목",
                          "content": "공지 내용",
                          "remindAts": ["2000-01-01T00:00:00"]
                        }
                        """)
                .when()
                .post("/studies/{studyId}/notices", study.getId())
                .then()
                .statusCode(400)
                .body("code", equalTo("INVALID_INPUT_VALUE"))
                .body("message", equalTo("입력값이 올바르지 않습니다."));
    }

    @Test
    @DisplayName("참여하지 않은 스터디에 공지를 생성하면 접근 거부 응답을 반환한다")
    void createNoticeWithoutStudyAccessTest() {
        testAuthRequest.givenAuthenticatedUser(leaderUser.getId())
                .port(port)
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "title": "공지 제목",
                          "content": "공지 내용",
                          "remindAts": []
                        }
                        """)
                .when()
                .post("/studies/{studyId}/notices", Long.MAX_VALUE)
                .then()
                .statusCode(403)
                .body("code", equalTo("STUDY_ACCESS_DENIED"))
                .body("message", equalTo("해당 스터디에 대한 접근 권한이 없습니다."));
    }

    @Test
    @DisplayName("공지 수정 요청의 제목이 공백이면 공지 도메인 오류 응답을 반환한다")
    void updateNoticeWithBlankTitleTest() {
        testAuthRequest.givenAuthenticatedUser(leaderUser.getId())
                .port(port)
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "title": " "
                        }
                        """)
                .when()
                .patch("/studies/{studyId}/notices/{noticeId}", study.getId(), notice.getId())
                .then()
                .statusCode(400)
                .body("code", equalTo("INVALID_TITLE"))
                .body("message", equalTo("공지 제목이 올바르지 않습니다."));
    }

    @Test
    @DisplayName("공지 목록 요청의 cursor와 size가 양수가 아니면 파라미터 오류 응답을 반환한다")
    void getNoticesWithNonPositiveParameterTest() {
        testAuthRequest.givenAuthenticatedUser(leaderUser.getId())
                .port(port)
                .queryParam("cursor", 0)
                .when()
                .get("/studies/{studyId}/notices", study.getId())
                .then()
                .statusCode(400)
                .body("code", equalTo("INVALID_REQUEST_PARAMETER"))
                .body("errors.field", hasItem("cursor"));

        testAuthRequest.givenAuthenticatedUser(leaderUser.getId())
                .port(port)
                .queryParam("size", 0)
                .when()
                .get("/studies/{studyId}/notices", study.getId())
                .then()
                .statusCode(400)
                .body("code", equalTo("INVALID_REQUEST_PARAMETER"))
                .body("errors.field", hasItem("size"));
    }

    @Test
    @DisplayName("공지 목록의 size는 최대 100까지 허용한다")
    void getNoticesWithMaximumSizeTest() {
        testAuthRequest.givenAuthenticatedUser(leaderUser.getId())
                .port(port)
                .queryParam("size", 100)
                .when()
                .get("/studies/{studyId}/notices", study.getId())
                .then()
                .statusCode(200);
    }

    @Test
    @DisplayName("공지 목록의 size가 최대값을 초과하면 파라미터 오류 응답을 반환한다")
    void getNoticesWithSizeOverMaximumTest() {
        testAuthRequest.givenAuthenticatedUser(leaderUser.getId())
                .port(port)
                .queryParam("size", 101)
                .when()
                .get("/studies/{studyId}/notices", study.getId())
                .then()
                .statusCode(400)
                .body("code", equalTo("INVALID_REQUEST_PARAMETER"))
                .body("errors.field", hasItem("size"));

        testAuthRequest.givenAuthenticatedUser(leaderUser.getId())
                .port(port)
                .queryParam("size", Integer.MAX_VALUE)
                .when()
                .get("/studies/{studyId}/notices", study.getId())
                .then()
                .statusCode(400)
                .body("code", equalTo("INVALID_REQUEST_PARAMETER"))
                .body("errors.field", hasItem("size"));
    }

    @Test
    @DisplayName("스터디에 참여하지 않은 사용자가 공지 목록을 조회하면 접근 거부 응답을 반환한다")
    void getNoticesByNonParticipantTest() {
        User outsider = userRepository.saveAndFlush(User.create("외부 사용자", null));

        testAuthRequest.givenAuthenticatedUser(outsider.getId())
                .port(port)
                .when()
                .get("/studies/{studyId}/notices", study.getId())
                .then()
                .statusCode(403)
                .body("code", equalTo("STUDY_ACCESS_DENIED"))
                .body("message", equalTo("해당 스터디에 대한 접근 권한이 없습니다."));
    }

    @Test
    @DisplayName("공지 수신자 정보가 없는 공지는 목록에서 제외한다")
    void getNoticesWithoutRecipientTest() {
        jdbcTemplate.update(
                "DELETE FROM notice_recipients WHERE notice_id = ? AND member_id = ?",
                notice.getId(),
                member.getId()
        );

        testAuthRequest.givenAuthenticatedUser(memberUser.getId())
                .port(port)
                .when()
                .get("/studies/{studyId}/notices", study.getId())
                .then()
                .statusCode(200)
                .body("notices", hasSize(0));
    }

    @Test
    @DisplayName("스터디원이 공지 수정 요청을 보내면 접근 거부 응답을 반환한다")
    void updateNoticeByMemberTest() {
        testAuthRequest.givenAuthenticatedUser(memberUser.getId())
                .port(port)
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "title": "수정 공지"
                        }
                        """)
                .when()
                .patch("/studies/{studyId}/notices/{noticeId}", study.getId(), notice.getId())
                .then()
                .statusCode(403)
                .body("code", equalTo("ACCESS_DENIED"));
    }

    @Test
    @DisplayName("수신자가 공지를 읽으면 읽음 시각을 응답하고 저장한다")
    void markNoticeAsReadTest() {
        String readAt = testAuthRequest.givenAuthenticatedUser(memberUser.getId())
                .port(port)
                .when()
                .patch("/studies/{studyId}/notices/{noticeId}/read", study.getId(), notice.getId())
                .then()
                .statusCode(200)
                .body("readAt", notNullValue())
                .extract()
                .jsonPath()
                .getString("readAt");

        LocalDateTime persistedReadAt = jdbcTemplate.queryForObject(
                "SELECT read_at FROM notice_recipients WHERE notice_id = ? AND member_id = ?",
                LocalDateTime.class,
                notice.getId(),
                member.getId()
        );

        assertThat(persistedReadAt).isNotNull();
        assertThat(readAt).isEqualTo(persistedReadAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
    }

    @Test
    @DisplayName("수신자가 자신의 공지 읽음 상태를 조회하면 읽지 않음 상태를 반환한다")
    void getMyNoticeReadStatusTest() {
        testAuthRequest.givenAuthenticatedUser(memberUser.getId())
                .port(port)
                .when()
                .get("/studies/{studyId}/notices/{noticeId}/status/me", study.getId(), notice.getId())
                .then()
                .statusCode(200)
                .body("isRead", equalTo(false))
                .body("readAt", nullValue());
    }

    @Test
    @DisplayName("리더가 공지 읽음 현황을 조회하면 읽음 여부와 가장 최근 리마인드 시각을 반환한다")
    void getAllReadStatusesByLeaderTest() {
        LocalDateTime readAt = LocalDateTime.of(2026, 8, 24, 10, 0);
        LocalDateTime lastRemindAt = LocalDateTime.of(2026, 8, 24, 10, 5);
        jdbcTemplate.update(
                "UPDATE notice_recipients SET read_at = ? WHERE notice_id = ? AND member_id = ?",
                readAt,
                notice.getId(),
                member.getId()
        );
        insertNotification(secondMember.getId(), notice.getId(), "NOTICE", lastRemindAt);

        testAuthRequest.givenAuthenticatedUser(leaderUser.getId())
                .port(port)
                .when()
                .get("/studies/{studyId}/notices/{noticeId}/status", study.getId(), notice.getId())
                .then()
                .statusCode(200)
                .body("id", equalTo(notice.getId().intValue()))
                .body("memberCount", equalTo(2))
                .body("readCount", equalTo(1))
                .body("remindAt", equalTo(remindAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)))
                .body("readMembers", hasSize(1))
                .body("readMembers[0].id", equalTo(member.getId().intValue()))
                .body("readMembers[0].name", equalTo("스터디원"))
                .body("unreadMembers", hasSize(1))
                .body("unreadMembers[0].id", equalTo(secondMember.getId().intValue()))
                .body("unreadMembers[0].name", equalTo("두 번째 스터디원"))
                .body("unreadMembers[0].lastRemindAt",
                        equalTo(lastRemindAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)));
    }

    @Test
    @DisplayName("스터디원이 공지 읽음 현황을 조회하면 접근 거부 응답을 반환한다")
    void getAllReadStatusesByMemberTest() {
        testAuthRequest.givenAuthenticatedUser(memberUser.getId())
                .port(port)
                .when()
                .get("/studies/{studyId}/notices/{noticeId}/status", study.getId(), notice.getId())
                .then()
                .statusCode(403)
                .body("code", equalTo("ACCESS_DENIED"));
    }

    @Test
    @DisplayName("인증 없이 공지 읽음 현황을 조회하면 인증 실패 응답을 반환한다")
    void getAllReadStatusesWithoutAuthenticationTest() {
        io.restassured.RestAssured.given()
                .basePath(API_PREFIX)
                .port(port)
                .when()
                .get("/studies/{studyId}/notices/{noticeId}/status", study.getId(), notice.getId())
                .then()
                .statusCode(401);
    }

    private void insertNotification(Long recipientId, Long resourceId, String resourceType, LocalDateTime createdAt) {
        jdbcTemplate.update(
                """
                        INSERT INTO notifications (
                            study_id, recipient_id, type, resource_id, resource_type, is_read, created_at, updated_at
                        ) VALUES (?, ?, 'REMIND', ?, ?, false, ?, ?)
                        """,
                study.getId(),
                recipientId,
                resourceId,
                resourceType,
                createdAt,
                createdAt
        );
    }

    private int countRows(String tableName, Long noticeId) {
        String foreignKey = tableName.equals("notices") ? "id" : "notice_id";
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + tableName + " WHERE " + foreignKey + " = ?",
                Integer.class,
                noticeId
        );
    }
}
