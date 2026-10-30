package withoutc.chongchong.comment;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static withoutc.chongchong.global.config.ApiPathConfig.API_PREFIX;

import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.ActiveProfiles;
import withoutc.chongchong.auth.support.TestAuthRequest;
import withoutc.chongchong.comment.entity.Comment;
import withoutc.chongchong.comment.controller.dto.CommentWriteRequest;
import withoutc.chongchong.comment.repository.CommentRepository;
import withoutc.chongchong.comment.service.CommentService;
import withoutc.chongchong.comment.service.CommentTarget;
import withoutc.chongchong.notice.entity.Notice;
import withoutc.chongchong.notice.repository.NoticeRepository;
import withoutc.chongchong.notification.entity.ResourceType;
import withoutc.chongchong.notification.entity.WebPushSubscription;
import withoutc.chongchong.notification.repository.NotificationDeliveryRepository;
import withoutc.chongchong.notification.repository.NotificationRepository;
import withoutc.chongchong.notification.repository.WebPushSubscriptionRepository;
import withoutc.chongchong.study.entity.Study;
import withoutc.chongchong.study.entity.StudyMember;
import withoutc.chongchong.study.entity.StudyMemberRole;
import withoutc.chongchong.study.repository.StudyMemberRepository;
import withoutc.chongchong.study.repository.StudyRepository;
import withoutc.chongchong.support.TestDatabaseCleaner;
import withoutc.chongchong.user.entity.User;
import withoutc.chongchong.user.repository.UserRepository;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.jpa.hibernate.ddl-auto=validate", "spring.flyway.enabled=true"
})
class NoticeCommentApiTest {

    private static final String PATH = "/studies/{studyId}/notices/{noticeId}/comments";
    private static final String OVER_LENGTH_CONTENT_MARKER = "x";
    private static final int OVER_MAX_CONTENT_LENGTH = 1001;
    private static final String UPDATE_COMMENT_CREATED_AT_SQL = "UPDATE comments SET created_at = ? WHERE id = ?";

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StudyRepository studyRepository;

    @Autowired
    private StudyMemberRepository memberRepository;

    @Autowired
    private NoticeRepository noticeRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private NotificationDeliveryRepository deliveryRepository;

    @Autowired
    private WebPushSubscriptionRepository subscriptionRepository;

    @Autowired
    private TestDatabaseCleaner cleaner;

    @Autowired
    private TestAuthRequest auth;

    @Autowired
    private JdbcTemplate jdbc;

    @MockitoSpyBean
    private CommentService commentService;

    @LocalServerPort
    private int port;

    private Study study;
    private Notice notice;
    private User leader;
    private User secondLeader;
    private User author;
    private User reader;
    private StudyMember authorMember;

    @BeforeEach
    void setUp() {
        cleaner.clean();
        study = studyRepository.save(Study.create("댓글 스터디", "설명"));
        leader = addMember("리더", StudyMemberRole.LEADER).getUser();
        secondLeader = addMember("다른 리더", StudyMemberRole.LEADER).getUser();
        authorMember = addMember("바니", StudyMemberRole.MEMBER);
        author = authorMember.getUser();
        reader = addMember("새 스터디원", StudyMemberRole.MEMBER).getUser();
        notice = noticeRepository.saveAndFlush(Notice.create(study, "공지", "내용"));
        subscriptionRepository.saveAndFlush(WebPushSubscription.create(leader, "browser",
                "https://push.example.com/leader", "key", "auth"));
        WebPushSubscription inactive = WebPushSubscription.create(secondLeader, "inactive",
                "https://push.example.com/inactive", "key", "auth");
        inactive.deactivate();
        subscriptionRepository.saveAndFlush(inactive);
    }

    @AfterEach
    void clean() {
        cleaner.clean();
    }

    @Test
    @DisplayName("공지 수신자가 아닌 현재 멤버도 댓글을 작성하고 모든 리더에게 인앱 알림을 보낸다")
    void createAndNotifyAllLeaders() {
        long id = create(author, " \t첫  줄\n둘째 줄 \n");

        list(reader).body("comments", hasSize(1))
                .body("comments[0].id", equalTo((int) id))
                .body("comments[0].content", equalTo("첫  줄\n둘째 줄"))
                .body("comments[0].author.name", equalTo("바니"))
                .body("comments[0].author.isDeparted", equalTo(false))
                .body("comments[0].isEdited", equalTo(false))
                .body("comments[0].canEdit", equalTo(false))
                .body("comments[0].canDelete", equalTo(false));
        assertThat(notificationRepository.findAllByRecipientIdOrderByCreatedAtDesc(leader.getId())).hasSize(1);
        assertThat(notificationRepository.findAllByRecipientIdOrderByCreatedAtDesc(secondLeader.getId())).hasSize(1);
        assertThat(notificationRepository.findAllByRecipientIdOrderByCreatedAtDesc(author.getId())).isEmpty();
        assertThat(notificationRepository.findAllByRecipientIdOrderByCreatedAtDesc(reader.getId())).isEmpty();
        assertThat(deliveryRepository.count()).isEqualTo(1);
        notificationRepository.findAll().forEach(notification -> {
            assertThat(notification.getTitle()).isEqualTo("[댓글 스터디] 새 댓글");
            assertThat(notification.getBody()).isEqualTo("바니 스터디원이 댓글을 달았어요");
            assertThat(notification.getDeepLink()).isEqualTo("/studies/%d/notices/%d?focusCommentId=%d"
                    .formatted(study.getId(), notice.getId(), id));
        });
    }

    @Test
    @DisplayName("자기 댓글을 수정하되 작성 시각과 순서를 유지하고 리더의 타인 수정은 거부한다")
    void editOwnCommentAndKeepOrder() {
        long first = create(author, "첫 댓글");
        long second = create(reader, "둘째 댓글");
        String createdAt = list(author).extract().path("comments[0].createdAt");
        request(author).body(Map.of("content", " 첫 댓글 ")).patch(PATH + "/{id}", study.getId(), notice.getId(), first)
                .then().statusCode(204);
        list(author).body("comments[0].isEdited", equalTo(false));

        request(author).body(Map.of("content", "수정한 댓글")).patch(PATH + "/{id}", study.getId(), notice.getId(), first)
                .then().statusCode(204);
        list(author).body("comments[0].id", equalTo((int) first))
                .body("comments[1].id", equalTo((int) second))
                .body("comments[0].createdAt", equalTo(createdAt))
                .body("comments[0].isEdited", equalTo(true))
                .body("comments[0].canEdit", equalTo(true));
        request(leader).body(Map.of("content", "타인 수정"))
                .patch(PATH + "/{id}", study.getId(), notice.getId(), first)
                .then().statusCode(403).body("code", equalTo("ACCESS_DENIED"));
        assertThat(notificationRepository.count()).isEqualTo(4);
    }

    @Test
    @DisplayName("일반 멤버는 타인 댓글을 삭제하지 못하고 리더 또는 작성자는 알림까지 함께 삭제한다")
    void deleteOwnAndOthersComments() {
        long id = create(author, "삭제할 댓글");
        request(reader).delete(PATH + "/{id}", study.getId(), notice.getId(), id)
                .then().statusCode(403);
        request(leader).delete(PATH + "/{id}", study.getId(), notice.getId(), id)
                .then().statusCode(204);
        assertThat(commentRepository.count()).isZero();
        assertThat(notificationRepository.count()).isZero();
        assertThat(deliveryRepository.count()).isZero();
        long own = create(author, "본인 삭제");
        request(author).delete(PATH + "/{id}", study.getId(), notice.getId(), own)
                .then().statusCode(204);
        request(author).delete(PATH + "/{id}", study.getId(), notice.getId(), own)
                .then().statusCode(404).body("code", equalTo("COMMENT_NOT_FOUND"));
    }

    @Test
    @DisplayName("리더가 댓글을 쓰면 본인을 제외한 리더만 받고 이전 댓글 참여자는 수신자로 추가하지 않는다")
    void excludeActorAndPreviousParticipants() {
        create(author, "이전 참여");
        create(leader, "리더 댓글");
        assertThat(notificationRepository.findAllByRecipientIdOrderByCreatedAtDesc(leader.getId())).hasSize(1);
        assertThat(notificationRepository.findAllByRecipientIdOrderByCreatedAtDesc(secondLeader.getId())).hasSize(2);
        assertThat(notificationRepository.findAllByRecipientIdOrderByCreatedAtDesc(author.getId())).isEmpty();
    }

    @Test
    @DisplayName("ID 커서로 이어 조회하고 기준 댓글이 삭제되어도 다음 목록을 반환한다")
    void paginateAfterDeletedCursorAndFocus() {
        List<Comment> comments = new ArrayList<>();
        for (int i = 0; i < 13; i++) {
            comments.add(commentRepository.save(Comment.create(ResourceType.NOTICE, notice.getId(),
                    authorMember, "댓글 " + i)));
        }
        LocalDateTime sameTime = LocalDateTime.of(2026, 10, 30, 10, 0);
        jdbc.update("UPDATE comments SET created_at = ? WHERE resource_type = 'NOTICE' AND resource_id = ?",
                sameTime, notice.getId());
        long cursor = list(author).body("comments", hasSize(10)).body("hasNext", equalTo(true))
                .extract().jsonPath().getLong("nextCursor");
        long anchor = comments.get(9).getId();
        assertThat(cursor).isEqualTo(anchor);

        request(reader).queryParam("cursor", cursor).get(PATH, study.getId(), notice.getId())
                .then().statusCode(200).body("comments", hasSize(3))
                .body("comments[0].id", equalTo(comments.get(10).getId().intValue()))
                .body("hasNext", equalTo(false)).body("nextCursor", nullValue());
        request(reader).queryParam("focusCommentId", comments.get(5).getId()).queryParam("size", 2)
                .get(PATH, study.getId(), notice.getId()).then().statusCode(200)
                .body("comments[0].id", equalTo(comments.get(5).getId().intValue()))
                .body("comments[1].id", equalTo(comments.get(6).getId().intValue()))
                .body("nextCursor", equalTo(comments.get(6).getId().intValue())).body("hasNext", equalTo(true));
        request(reader).queryParam("focusCommentId", comments.getFirst().getId()).queryParam("cursor", cursor)
                .get(PATH, study.getId(), notice.getId()).then().statusCode(400)
                .body("code", equalTo("COMMENT_INVALID_PAGE_REQUEST"));
        Notice another = noticeRepository.saveAndFlush(Notice.create(study, "다른 공지", "내용"));
        request(reader).queryParam("cursor", cursor).get(PATH, study.getId(), another.getId())
                .then().statusCode(200).body("comments", hasSize(0)).body("hasNext", equalTo(false));
        request(reader).queryParam("focusCommentId", comments.getFirst().getId())
                .get(PATH, study.getId(), another.getId()).then().statusCode(404);
        request(reader).queryParam("cursor", Long.MAX_VALUE).get(PATH, study.getId(), notice.getId())
                .then().statusCode(200).body("comments", hasSize(0)).body("nextCursor", nullValue());

        request(author).delete(PATH + "/{id}", study.getId(), notice.getId(), anchor).then().statusCode(204);
        request(reader).queryParam("cursor", cursor).get(PATH, study.getId(), notice.getId())
                .then().statusCode(200).body("comments", hasSize(3))
                .body("comments[0].id", equalTo(comments.get(10).getId().intValue()))
                .body("hasNext", equalTo(false)).body("nextCursor", nullValue());
        request(reader).queryParam("focusCommentId", anchor).get(PATH, study.getId(), notice.getId())
                .then().statusCode(404).body("code", equalTo("COMMENT_NOT_FOUND"));
    }

    @Test
    @DisplayName("작성 시각 순서와 달라도 ID 오름차순으로 누락 없이 페이지를 조회한다")
    void paginateByIdRegardlessOfCreatedAt() {
        Comment firstId = commentRepository.saveAndFlush(
                Comment.create(ResourceType.NOTICE, notice.getId(), authorMember, "첫 ID"));
        Comment oldest = commentRepository.saveAndFlush(
                Comment.create(ResourceType.NOTICE, notice.getId(), authorMember, "가장 이른 댓글"));
        Comment lastId = commentRepository.saveAndFlush(
                Comment.create(ResourceType.NOTICE, notice.getId(), authorMember, "마지막 ID"));
        LocalDateTime earlier = LocalDateTime.of(2026, 10, 30, 10, 0);
        LocalDateTime later = earlier.plusSeconds(1);
        jdbc.update(UPDATE_COMMENT_CREATED_AT_SQL, later, firstId.getId());
        jdbc.update(UPDATE_COMMENT_CREATED_AT_SQL, earlier, oldest.getId());
        jdbc.update(UPDATE_COMMENT_CREATED_AT_SQL, later, lastId.getId());

        long firstCursor = request(reader).queryParam("size", 1).get(PATH, study.getId(), notice.getId())
                .then().statusCode(200).body("comments", hasSize(1))
                .body("comments[0].id", equalTo(firstId.getId().intValue())).body("hasNext", equalTo(true))
                .extract().jsonPath().getLong("nextCursor");
        assertThat(firstCursor).isEqualTo(firstId.getId());
        long secondCursor = request(reader).queryParam("size", 1).queryParam("cursor", firstCursor)
                .get(PATH, study.getId(), notice.getId()).then().statusCode(200)
                .body("comments", hasSize(1)).body("comments[0].id", equalTo(oldest.getId().intValue()))
                .body("hasNext", equalTo(true)).extract().jsonPath().getLong("nextCursor");
        assertThat(secondCursor).isEqualTo(oldest.getId());
        request(reader).queryParam("size", 1).queryParam("cursor", secondCursor)
                .get(PATH, study.getId(), notice.getId()).then().statusCode(200)
                .body("comments", hasSize(1)).body("comments[0].id", equalTo(lastId.getId().intValue()))
                .body("hasNext", equalTo(false)).body("nextCursor", nullValue());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "\n\t", OVER_LENGTH_CONTENT_MARKER})
    @DisplayName("빈 본문과 정규화 후 1000자를 초과한 본문을 거부한다")
    void rejectInvalidContent(String value) {
        String content = value;
        if (value.equals(OVER_LENGTH_CONTENT_MARKER)) {
            content = "가".repeat(OVER_MAX_CONTENT_LENGTH);
        }
        request(author).body(Map.of("content", content)).post(PATH, study.getId(), notice.getId())
                .then().statusCode(400).body("code", equalTo("COMMENT_INVALID_CONTENT"));
        assertThat(commentRepository.count()).isZero();
        assertThat(notificationRepository.count()).isZero();
    }

    @Test
    @DisplayName("본문 누락은 검증 오류이며 정규화 전 1000자를 초과해도 정규화 후 유효하면 저장한다")
    void validateBodyAndNormalizeBeforeLengthCheck() {
        request(author).body(Map.of()).post(PATH, study.getId(), notice.getId())
                .then().statusCode(400).body("code", equalTo("INVALID_REQUEST_PARAMETER"))
                .body("errors[0].reason", equalTo("댓글 내용은 필수 값입니다."));
        create(author, " ".repeat(1000) + "가".repeat(1000) + "\n");
        list(author).body("comments[0].content", equalTo("가".repeat(1000)));
    }

    @Test
    @DisplayName("알려진 댓글 잠금 실패는 저장 없이 409 응답으로 변환한다")
    void translateKnownLockFailure() {
        doThrow(new CannotAcquireLockException("lock timeout"))
                .when(commentService).create(anyLong(), anyLong(), any(CommentTarget.class), any(CommentWriteRequest.class));
        request(author).body(Map.of("content", "충돌")).post(PATH, study.getId(), notice.getId())
                .then().statusCode(409).body("code", equalTo("COMMENT_STATE_CHANGED"));
        assertThat(commentRepository.count()).isZero();
        assertThat(notificationRepository.count()).isZero();
    }

    @Test
    @DisplayName("비회원·다른 스터디의 원문·다른 원문의 댓글을 사용한 접근을 거부한다")
    void rejectUnauthorizedAndMixedScope() {
        User outsider = userRepository.save(User.create("외부인", null));
        request(outsider).get(PATH, study.getId(), notice.getId()).then().statusCode(403);
        request(outsider).body(Map.of("content", "외부 댓글")).post(PATH, study.getId(), notice.getId())
                .then().statusCode(403);
        long id = create(author, "내 댓글");
        Notice another = noticeRepository.saveAndFlush(Notice.create(study, "다른 원문", "내용"));
        request(author).body(Map.of("content", "잘못된 경로"))
                .patch(PATH + "/{id}", study.getId(), another.getId(), id)
                .then().statusCode(404).body("code", equalTo("COMMENT_NOT_FOUND"));
        Study otherStudy = studyRepository.save(Study.create("다른 스터디", "설명"));
        memberRepository.save(StudyMember.create(otherStudy, author, "바니", null, StudyMemberRole.MEMBER));
        request(author).get(PATH, otherStudy.getId(), notice.getId())
                .then().statusCode(404).body("code", equalTo("NOTICE_NOT_FOUND"));
        request(author).queryParam("size", 0).get(PATH, study.getId(), notice.getId()).then().statusCode(400);
        given().port(port).basePath(API_PREFIX).get(PATH, study.getId(), notice.getId()).then().statusCode(401);
    }

    @ParameterizedTest
    @CsvSource({"0, INVALID_REQUEST_PARAMETER", "-1, INVALID_REQUEST_PARAMETER",
            "broken, INVALID_REQUEST", "9223372036854775808, INVALID_REQUEST"})
    @DisplayName("커서가 양수 Long 형식이 아니면 공통 파라미터 오류를 반환한다")
    void rejectInvalidCursor(String cursor, String errorCode) {
        request(author).queryParam("cursor", cursor).get(PATH, study.getId(), notice.getId())
                .then().statusCode(400).body("code", equalTo(errorCode));
    }

    private StudyMember addMember(String name, StudyMemberRole role) {
        User user = userRepository.save(User.create(name, null));
        return memberRepository.saveAndFlush(StudyMember.create(study, user, name, null, role));
    }

    private RequestSpecification request(User user) {
        return auth.givenAuthenticatedUser(user.getId()).port(port).contentType(ContentType.JSON);
    }

    private long create(User user, String content) {
        return request(user).body(Map.of("content", content)).post(PATH, study.getId(), notice.getId())
                .then().statusCode(201).extract().jsonPath().getLong("commentId");
    }

    private ValidatableResponse list(User user) {
        return request(user).get(PATH, study.getId(), notice.getId()).then().statusCode(200);
    }
}
